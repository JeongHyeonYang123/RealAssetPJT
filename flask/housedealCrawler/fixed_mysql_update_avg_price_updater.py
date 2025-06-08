# fixed_mysql_update_avg_price_updater.py
import pymysql
import logging
from datetime import datetime

logger = logging.getLogger(__name__)


class FixedMySQLUpdateAvgPriceUpdater:
    """MySQL UPDATE 제한사항을 해결한 평균가격 업데이터"""

    def __init__(self, db_config):
        self.db_config = db_config
        self.connection = None

    def connect(self):
        try:
            self.connection = pymysql.connect(**self.db_config, charset='utf8mb4', autocommit=False)
            logger.info("✅ MySQL UPDATE 수정 평균가격 업데이터 DB 연결 성공")
        except Exception as e:
            logger.error(f"❌ DB 연결 실패: {e}")
            raise

    def update_dong_avg_prices(self):
        """✅ Derived Table을 사용한 동별 평균가격 업데이트"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            logger.info("📊 1단계: 동별 평균가격 업데이트 시작 (MySQL UPDATE 제한 해결)")

            # ✅ 이중 서브쿼리로 MySQL 제한사항 회피
            sql = """
            UPDATE dong_code_superman d
            JOIN (
                SELECT dong_code, avg_price, apt_count FROM (
                    SELECT
                        CONCAT(hi.sgg_cd COLLATE utf8mb4_unicode_ci, hi.umd_cd COLLATE utf8mb4_unicode_ci) AS dong_code,
                        AVG(CAST(REPLACE(lh.deal_amount, ',', '') AS UNSIGNED)) AS avg_price,
                        COUNT(*) AS apt_count
                    FROM houseinfos hi
                    JOIN latest_housedeals lh ON hi.apt_seq COLLATE utf8mb4_unicode_ci = lh.apt_seq COLLATE utf8mb4_unicode_ci
                    WHERE lh.deal_amount IS NOT NULL AND lh.deal_amount != ''
                    GROUP BY CONCAT(hi.sgg_cd COLLATE utf8mb4_unicode_ci, hi.umd_cd COLLATE utf8mb4_unicode_ci)
                ) AS inner_subquery
            ) t ON d.dong_code COLLATE utf8mb4_unicode_ci = t.dong_code COLLATE utf8mb4_unicode_ci
            SET d.avg_price = t.avg_price,
                d.apt_count = t.apt_count
            """

            cursor.execute(sql)
            affected_rows = cursor.rowcount
            self.connection.commit()

            logger.info(f"✅ 동별 평균가격 업데이트 완료: {affected_rows}개 동")
            return affected_rows

        except Exception as e:
            logger.error(f"❌ 동별 평균가격 업데이트 실패: {e}")
            self.connection.rollback()
            # 실패 시 대안 방법
            return self._update_dong_alternative()
        finally:
            cursor.close()

    def _update_dong_alternative(self):
        """동별 업데이트 대안 방법 (housedeals만 사용)"""
        cursor = self.connection.cursor()

        try:
            logger.info("🔄 동별 업데이트 대안 방법 실행")

            # ✅ 서브쿼리 방식으로 MySQL 제한 회피
            sql = """
            UPDATE dong_code_superman d
            SET d.avg_price = (
                SELECT AVG(price) FROM (
                    SELECT CAST(REPLACE(h.deal_amount, ',', '') AS UNSIGNED) as price
                    FROM housedeals h
                    WHERE h.apt_seq LIKE CONCAT(LEFT(d.dong_code, 5), '%')
                    AND h.deal_amount IS NOT NULL 
                    AND h.deal_amount != ''
                    AND h.deal_amount REGEXP '^[0-9,]+$'
                    AND h.deal_year >= 2023
                ) AS price_subquery
            ),
            d.apt_count = (
                SELECT COUNT(*) FROM (
                    SELECT h.id
                    FROM housedeals h
                    WHERE h.apt_seq LIKE CONCAT(LEFT(d.dong_code, 5), '%')
                    AND h.deal_amount IS NOT NULL 
                    AND h.deal_amount != ''
                    AND h.deal_amount REGEXP '^[0-9,]+$'
                    AND h.deal_year >= 2023
                ) AS count_subquery
            )
            WHERE LENGTH(d.dong_code) = 10
            """

            cursor.execute(sql)
            affected_rows = cursor.rowcount
            self.connection.commit()

            logger.info(f"✅ 동별 대안 방법 완료: {affected_rows}개 동")
            return affected_rows

        except Exception as e:
            logger.error(f"❌ 동별 대안 방법 실패: {e}")
            self.connection.rollback()
            raise
        finally:
            cursor.close()

    def update_gun_avg_prices(self):
        """✅ Derived Table을 사용한 군별 평균가격 업데이트"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            logger.info("📊 2단계: 군별 평균가격 업데이트 시작")

            # ✅ 이중 서브쿼리로 MySQL 제한사항 회피
            sql = """
            UPDATE dong_code_superman d
            JOIN (
                SELECT gun_prefix, avg_price, apt_count FROM (
                    SELECT
                        LEFT(dong_code, 5) as gun_prefix,
                        AVG(avg_price) as avg_price,
                        SUM(apt_count) as apt_count
                    FROM dong_code_superman
                    WHERE LENGTH(dong_code) = 10
                    AND avg_price IS NOT NULL
                    GROUP BY LEFT(dong_code, 5)
                ) AS gun_subquery
            ) t ON LEFT(d.dong_code, 5) = t.gun_prefix
            SET d.avg_price = t.avg_price,
                d.apt_count = t.apt_count
            WHERE LENGTH(d.dong_code) = 5
            """

            cursor.execute(sql)
            affected_rows = cursor.rowcount
            self.connection.commit()

            logger.info(f"✅ 군별 평균가격 업데이트 완료: {affected_rows}개 군")
            return affected_rows

        except Exception as e:
            logger.error(f"❌ 군별 평균가격 업데이트 실패: {e}")
            self.connection.rollback()
            raise
        finally:
            cursor.close()

    def update_sido_avg_prices(self):
        """✅ Derived Table을 사용한 시도별 평균가격 업데이트"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            logger.info("📊 3단계: 시도별 평균가격 업데이트 시작")

            # ✅ 이중 서브쿼리로 MySQL 제한사항 회피
            sql = """
            UPDATE dong_code_superman d
            JOIN (
                SELECT sido_prefix, avg_price, apt_count FROM (
                    SELECT
                        LEFT(dong_code, 2) as sido_prefix,
                        AVG(avg_price) as avg_price,
                        SUM(apt_count) as apt_count
                    FROM dong_code_superman
                    WHERE LENGTH(dong_code) = 5
                    AND avg_price IS NOT NULL
                    GROUP BY LEFT(dong_code, 2)
                ) AS sido_subquery
            ) t ON LEFT(d.dong_code, 2) = t.sido_prefix
            SET d.avg_price = t.avg_price,
                d.apt_count = t.apt_count
            WHERE LENGTH(d.dong_code) = 2
            """

            cursor.execute(sql)
            affected_rows = cursor.rowcount
            self.connection.commit()

            logger.info(f"✅ 시도별 평균가격 업데이트 완료: {affected_rows}개 시도")
            return affected_rows

        except Exception as e:
            logger.error(f"❌ 시도별 평균가격 업데이트 실패: {e}")
            self.connection.rollback()
            raise
        finally:
            cursor.close()

    def update_all_avg_prices(self):
        """전체 평균가격 업데이트 (MySQL UPDATE 제한 해결)"""
        logger.info("🚀 === MySQL UPDATE 제한 해결 평균가격 전체 업데이트 시작 ===")

        start_time = datetime.now()

        try:
            if not self.connection:
                self.connect()

            # 1단계: 동별 업데이트
            dong_updated = self.update_dong_avg_prices()

            # 2단계: 군별 업데이트
            gun_updated = self.update_gun_avg_prices()

            # 3단계: 시도별 업데이트
            sido_updated = self.update_sido_avg_prices()

            # 완료 시간 계산
            elapsed_time = datetime.now() - start_time
            total_updated = dong_updated + gun_updated + sido_updated

            logger.info("🎉 === MySQL UPDATE 제한 해결 평균가격 업데이트 완료 ===")
            logger.info(f"⏱️ 처리 시간: {elapsed_time}")
            logger.info(f"📊 총 업데이트: {total_updated}개 지역")
            logger.info(f"  - 동별: {dong_updated}개")
            logger.info(f"  - 군별: {gun_updated}개")
            logger.info(f"  - 시도별: {sido_updated}개")

            return {
                'total_updated': total_updated,
                'dong_updated': dong_updated,
                'gun_updated': gun_updated,
                'sido_updated': sido_updated,
                'elapsed_time': elapsed_time
            }

        except Exception as e:
            logger.error(f"❌ MySQL UPDATE 제한 해결 평균가격 업데이트 실패: {e}")
            raise

    def close(self):
        if self.connection:
            self.connection.close()
            logger.info("🔌 MySQL UPDATE 제한 해결 평균가격 업데이터 DB 연결 종료")
