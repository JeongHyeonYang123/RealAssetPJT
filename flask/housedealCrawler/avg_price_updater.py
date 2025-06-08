# avg_price_updater.py
import pymysql
import logging
from datetime import datetime

logger = logging.getLogger(__name__)


class AvgPriceUpdater:
    """dong_code_superman 테이블의 평균가격 업데이트 클래스"""

    def __init__(self, db_config):
        self.db_config = db_config
        self.connection = None

    def connect(self):
        """데이터베이스 연결"""
        try:
            self.connection = pymysql.connect(**self.db_config, charset='utf8mb4', autocommit=False)
            logger.info("✅ 평균가격 업데이터 DB 연결 성공")
        except Exception as e:
            logger.error(f"❌ 평균가격 업데이터 DB 연결 실패: {e}")
            raise

    def update_dong_avg_prices(self):
        """1단계: 동별 평균가격 업데이트"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            logger.info("📊 1단계: 동별 평균가격 업데이트 시작")

            sql = """
            UPDATE dong_code_superman d
            JOIN (
                SELECT
                    CONCAT(hi.sgg_cd, hi.umd_cd) AS dong_code,
                    AVG(CAST(REPLACE(lh.deal_amount, ',', '') AS UNSIGNED)) AS avg_price,
                    COUNT(*) AS apt_count
                FROM houseinfos hi
                JOIN latest_housedeals lh ON hi.apt_seq = lh.apt_seq
                WHERE lh.deal_amount IS NOT NULL AND lh.deal_amount != ''
                GROUP BY CONCAT(hi.sgg_cd, hi.umd_cd)
            ) t ON d.dong_code = t.dong_code
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
            self.connection.rollback()
            raise
        finally:
            cursor.close()

    def update_gun_avg_prices(self):
        """2단계: 군별 평균가격 업데이트"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            logger.info("📊 2단계: 군별 평균가격 업데이트 시작")

            sql = """
            UPDATE dong_code_superman d
            JOIN (
                SELECT
                    CONCAT(gun_prefix, '00000') AS gun_dong_code,
                    AVG(avg_price) AS avg_price,
                    SUM(apt_count) AS apt_count
                FROM (
                    SELECT
                        SUBSTR(CONCAT(hi.sgg_cd, hi.umd_cd), 1, 5) AS gun_prefix,
                        AVG(CAST(REPLACE(lh.deal_amount, ',', '') AS UNSIGNED)) AS avg_price,
                        COUNT(*) AS apt_count
                    FROM houseinfos hi
                    JOIN latest_housedeals lh ON hi.apt_seq = lh.apt_seq
                    WHERE lh.deal_amount IS NOT NULL AND lh.deal_amount != ''
                    GROUP BY SUBSTR(CONCAT(hi.sgg_cd, hi.umd_cd), 1, 5)
                ) sub
                GROUP BY gun_prefix
            ) t ON d.dong_code = t.gun_dong_code
            SET d.avg_price = t.avg_price,
                d.apt_count = t.apt_count
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

    def update_sido_avg_prices(self):
        """3단계: 광역시도별 평균가격 업데이트"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            logger.info("📊 3단계: 광역시도별 평균가격 업데이트 시작")

            sql = """
            UPDATE dong_code_superman d
            JOIN (
                SELECT
                    CONCAT(sido_prefix, '00000000') AS sido_dong_code,
                    AVG(avg_price) AS avg_price,
                    SUM(apt_count) AS apt_count
                FROM (
                    SELECT
                        SUBSTR(CONCAT(hi.sgg_cd, hi.umd_cd), 1, 2) AS sido_prefix,
                        AVG(CAST(REPLACE(lh.deal_amount, ',', '') AS UNSIGNED)) AS avg_price,
                        COUNT(*) AS apt_count
                    FROM houseinfos hi
                    JOIN latest_housedeals lh ON hi.apt_seq = lh.apt_seq
                    WHERE lh.deal_amount IS NOT NULL AND lh.deal_amount != ''
                    GROUP BY SUBSTR(CONCAT(hi.sgg_cd, hi.umd_cd), 1, 2)
                ) sub
                GROUP BY sido_prefix
            ) t ON d.dong_code = t.sido_dong_code
            SET d.avg_price = t.avg_price,
                d.apt_count = t.apt_count
            """

            cursor.execute(sql)
            affected_rows = cursor.rowcount
            self.connection.commit()

            logger.info(f"✅ 광역시도별 평균가격 업데이트 완료: {affected_rows}개 시도 업데이트")
            return affected_rows

        except Exception as e:
            logger.error(f"❌ 광역시도별 평균가격 업데이트 실패: {e}")
            self.connection.rollback()
            raise
        finally:
            cursor.close()

    def update_all_avg_prices(self):
        """전체 평균가격 업데이트 (3단계 모두 실행)"""
        logger.info("🚀 === dong_code_superman 평균가격 전체 업데이트 시작 ===")

        start_time = datetime.now()
        total_updated = 0

        try:
            if not self.connection:
                self.connect()

            # 1단계: 동별 업데이트
            dong_updated = self.update_dong_avg_prices()
            total_updated += dong_updated

            # 2단계: 군별 업데이트
            gun_updated = self.update_gun_avg_prices()
            total_updated += gun_updated

            # 3단계: 시도별 업데이트
            sido_updated = self.update_sido_avg_prices()
            total_updated += sido_updated

            # 완료 시간 계산
            elapsed_time = datetime.now() - start_time

            logger.info("🎉 === 평균가격 전체 업데이트 완료 ===")
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
            logger.error(f"❌ 평균가격 전체 업데이트 실패: {e}")
            raise

    def check_update_status(self):
        """업데이트 상태 확인"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            # 평균가격이 있는 레코드 수 확인
            cursor.execute("SELECT COUNT(*) FROM dong_code_superman WHERE avg_price IS NOT NULL AND avg_price > 0")
            updated_count = cursor.fetchone()[0]

            # 전체 레코드 수 확인
            cursor.execute("SELECT COUNT(*) FROM dong_code_superman")
            total_count = cursor.fetchone()[0]

            # 최근 업데이트된 지역들 확인
            cursor.execute("""
                SELECT dong_code, avg_price, apt_count 
                FROM dong_code_superman 
                WHERE avg_price IS NOT NULL AND avg_price > 0 
                ORDER BY avg_price DESC 
                LIMIT 5
            """)
            top_regions = cursor.fetchall()

            logger.info(f"📊 업데이트 상태: {updated_count}/{total_count} 지역")
            logger.info("💰 평균가격 상위 5개 지역:")
            for region in top_regions:
                logger.info(f"  {region[0]}: {region[1]:,.0f}원 ({region[2]}건)")

            return {
                'updated_count': updated_count,
                'total_count': total_count,
                'update_rate': (updated_count / total_count * 100) if total_count > 0 else 0
            }

        except Exception as e:
            logger.error(f"❌ 업데이트 상태 확인 실패: {e}")
            return None
        finally:
            cursor.close()

    def close(self):
        """연결 종료"""
        if self.connection:
            self.connection.close()
            logger.info("🔌 평균가격 업데이터 DB 연결 종료")
