# collation_safe_avg_price_updater.py
import pymysql
import logging
from datetime import datetime

logger = logging.getLogger(__name__)


class CollationSafeAvgPriceUpdater:
    """Collation 충돌을 회피하는 평균가격 업데이터"""

    def __init__(self, db_config):
        self.db_config = db_config
        self.connection = None

    def connect(self):
        try:
            self.connection = pymysql.connect(**self.db_config, charset='utf8mb4', autocommit=False)
            logger.info("✅ Collation Safe 평균가격 업데이터 DB 연결 성공")
        except Exception as e:
            logger.error(f"❌ DB 연결 실패: {e}")
            raise

    def update_dong_avg_prices_safe(self):
        """Collation 충돌을 회피한 동별 평균가격 업데이트"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            logger.info("📊 1단계: 동별 평균가격 업데이트 시작 (Collation Safe)")

            # ✅ COLLATE 절을 명시적으로 추가하여 충돌 회피
            sql = """
            UPDATE dong_code_superman d
            JOIN (
                SELECT
                    CONCAT(hi.sgg_cd COLLATE utf8mb4_unicode_ci, hi.umd_cd COLLATE utf8mb4_unicode_ci) AS dong_code,
                    AVG(CAST(REPLACE(lh.deal_amount, ',', '') AS UNSIGNED)) AS avg_price,
                    COUNT(*) AS apt_count
                FROM houseinfos hi
                JOIN latest_housedeals lh ON hi.apt_seq COLLATE utf8mb4_unicode_ci = lh.apt_seq COLLATE utf8mb4_unicode_ci
                WHERE lh.deal_amount IS NOT NULL AND lh.deal_amount != ''
                GROUP BY CONCAT(hi.sgg_cd COLLATE utf8mb4_unicode_ci, hi.umd_cd COLLATE utf8mb4_unicode_ci)
            ) t ON d.dong_code COLLATE utf8mb4_unicode_ci = t.dong_code COLLATE utf8mb4_unicode_ci
            SET d.avg_price = t.avg_price,
                d.apt_count = t.apt_count
            """

            cursor.execute(sql)
            affected_rows = cursor.rowcount
            self.connection.commit()

            logger.info(f"✅ 동별 평균가격 업데이트 완료: {affected_rows}개 동 업데이트")
            return affected_rows

        except Exception as e:
            logger.error(f"❌ 동별 평균가격 업데이트 실패: {e}")
            # ✅ 실패 시 대안 방법 시도
            logger.info("🔄 대안 방법으로 재시도...")
            return self._update_dong_avg_prices_alternative()
        finally:
            cursor.close()

    def _update_dong_avg_prices_alternative(self):
        """대안 방법: housedeals 테이블만 사용"""
        cursor = self.connection.cursor()

        try:
            logger.info("🔄 대안 방법: housedeals 테이블만 사용한 동별 업데이트")

            # housedeals 테이블만 사용하여 지역코드 기반 업데이트
            sql = """
            UPDATE dong_code_superman d
            SET d.avg_price = (
                SELECT AVG(CAST(REPLACE(h.deal_amount, ',', '') AS UNSIGNED))
                FROM housedeals h
                WHERE h.apt_seq COLLATE utf8mb4_unicode_ci LIKE CONCAT(LEFT(d.dong_code COLLATE utf8mb4_unicode_ci, 5), '%')
                AND h.deal_amount IS NOT NULL 
                AND h.deal_amount != ''
                AND h.deal_amount REGEXP '^[0-9,]+$'
                AND h.deal_year >= YEAR(CURDATE()) - 1
            ),
            d.apt_count = (
                SELECT COUNT(*)
                FROM housedeals h
                WHERE h.apt_seq COLLATE utf8mb4_unicode_ci LIKE CONCAT(LEFT(d.dong_code COLLATE utf8mb4_unicode_ci, 5), '%')
                AND h.deal_amount IS NOT NULL 
                AND h.deal_amount != ''
                AND h.deal_amount REGEXP '^[0-9,]+$'
                AND h.deal_year >= YEAR(CURDATE()) - 1
            )
            WHERE LENGTH(d.dong_code) = 10
            """

            cursor.execute(sql)
            affected_rows = cursor.rowcount
            self.connection.commit()

            logger.info(f"✅ 대안 방법 완료: {affected_rows}개 동 업데이트")
            return affected_rows

        except Exception as e:
            logger.error(f"❌ 대안 방법도 실패: {e}")
            self.connection.rollback()
            raise
        finally:
            cursor.close()

    def update_gun_avg_prices_safe(self):
        """Collation Safe 군별 평균가격 업데이트"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            logger.info("📊 2단계: 군별 평균가격 업데이트 시작 (Collation Safe)")

            sql = """
            UPDATE dong_code_superman d
            SET d.avg_price = (
                SELECT AVG(avg_price)
                FROM dong_code_superman sub
                WHERE LEFT(sub.dong_code COLLATE utf8mb4_unicode_ci, 5) = LEFT(d.dong_code COLLATE utf8mb4_unicode_ci, 5)
                AND LENGTH(sub.dong_code) = 10
                AND sub.avg_price IS NOT NULL
            ),
            d.apt_count = (
                SELECT SUM(apt_count)
                FROM dong_code_superman sub
                WHERE LEFT(sub.dong_code COLLATE utf8mb4_unicode_ci, 5) = LEFT(d.dong_code COLLATE utf8mb4_unicode_ci, 5)
                AND LENGTH(sub.dong_code) = 10
                AND sub.apt_count IS NOT NULL
            )
            WHERE LENGTH(d.dong_code) = 5
            """

            cursor.execute(sql)
            affected_rows = cursor.rowcount
            self.connection.commit()

            logger.info(f"✅ 군별 평균가격 업데이트 완료: {affected_rows}개 군 업데이트")
            return affected_rows

        except Exception as e:
            logger.error(f"❌ 군별 평균가격 업데이트 실패: {e}")
            self.connection.rollback()
            raise
        finally:
            cursor.close()

    def update_sido_avg_prices_safe(self):
        """Collation Safe 시도별 평균가격 업데이트"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            logger.info("📊 3단계: 시도별 평균가격 업데이트 시작 (Collation Safe)")

            sql = """
            UPDATE dong_code_superman d
            SET d.avg_price = (
                SELECT AVG(avg_price)
                FROM dong_code_superman sub
                WHERE LEFT(sub.dong_code COLLATE utf8mb4_unicode_ci, 2) = LEFT(d.dong_code COLLATE utf8mb4_unicode_ci, 2)
                AND LENGTH(sub.dong_code) = 5
                AND sub.avg_price IS NOT NULL
            ),
            d.apt_count = (
                SELECT SUM(apt_count)
                FROM dong_code_superman sub
                WHERE LEFT(sub.dong_code COLLATE utf8mb4_unicode_ci, 2) = LEFT(d.dong_code COLLATE utf8mb4_unicode_ci, 2)
                AND LENGTH(sub.dong_code) = 5
                AND sub.apt_count IS NOT NULL
            )
            WHERE LENGTH(d.dong_code) = 2
            """

            cursor.execute(sql)
            affected_rows = cursor.rowcount
            self.connection.commit()

            logger.info(f"✅ 시도별 평균가격 업데이트 완료: {affected_rows}개 시도 업데이트")
            return affected_rows

        except Exception as e:
            logger.error(f"❌ 시도별 평균가격 업데이트 실패: {e}")
            self.connection.rollback()
            raise
        finally:
            cursor.close()

    def update_all_avg_prices_safe(self):
        """Collation Safe 전체 평균가격 업데이트"""
        logger.info("🚀 === Collation Safe 평균가격 전체 업데이트 시작 ===")

        start_time = datetime.now()

        try:
            if not self.connection:
                self.connect()

            # 1단계: 동별 업데이트
            dong_updated = self.update_dong_avg_prices_safe()

            # 2단계: 군별 업데이트
            gun_updated = self.update_gun_avg_prices_safe()

            # 3단계: 시도별 업데이트
            sido_updated = self.update_sido_avg_prices_safe()

            # 완료 시간 계산
            elapsed_time = datetime.now() - start_time
            total_updated = dong_updated + gun_updated + sido_updated

            logger.info("🎉 === Collation Safe 평균가격 업데이트 완료 ===")
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
            logger.error(f"❌ Collation Safe 평균가격 업데이트 실패: {e}")
            raise

    def close(self):
        if self.connection:
            self.connection.close()
            logger.info("🔌 Collation Safe 평균가격 업데이터 DB 연결 종료")
