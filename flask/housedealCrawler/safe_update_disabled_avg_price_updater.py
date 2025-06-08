# safe_update_disabled_avg_price_updater.py
import pymysql
import logging
from datetime import datetime

logger = logging.getLogger(__name__)


class SafeUpdateDisabledAvgPriceUpdater:
    """Safe Update Mode를 비활성화한 평균가격 업데이터"""

    def __init__(self, db_config):
        self.db_config = db_config
        self.connection = None

    def connect(self):
        try:
            self.connection = pymysql.connect(**self.db_config, charset='utf8mb4', autocommit=False)
            logger.info("✅ Safe Update 비활성화 평균가격 업데이터 DB 연결 성공")
        except Exception as e:
            logger.error(f"❌ DB 연결 실패: {e}")
            raise

    def _disable_safe_updates(self):
        """Safe Update Mode 비활성화"""
        cursor = self.connection.cursor()
        try:
            cursor.execute("SET SQL_SAFE_UPDATES = 0")
            logger.info("🔓 Safe Update Mode 비활성화")
        except Exception as e:
            logger.error(f"❌ Safe Update Mode 비활성화 실패: {e}")
            raise
        finally:
            cursor.close()

    def _enable_safe_updates(self):
        """Safe Update Mode 재활성화"""
        cursor = self.connection.cursor()
        try:
            cursor.execute("SET SQL_SAFE_UPDATES = 1")
            logger.info("🔒 Safe Update Mode 재활성화")
        except Exception as e:
            logger.error(f"❌ Safe Update Mode 재활성화 실패: {e}")
        finally:
            cursor.close()

    def update_dong_avg_prices(self):
        """동별 평균가격 업데이트"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            logger.info("📊 1단계: 동별 평균가격 업데이트 시작")

            # Safe Update Mode 비활성화
            self._disable_safe_updates()

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
            WHERE LENGTH(d.dong_code) = 10
            """

            cursor.execute(sql)
            affected_rows = cursor.rowcount
            self.connection.commit()

            # Safe Update Mode 재활성화
            self._enable_safe_updates()

            logger.info(f"✅ 동별 평균가격 업데이트 완료: {affected_rows}개 동")
            return affected_rows

        except Exception as e:
            logger.error(f"❌ 동별 평균가격 업데이트 실패: {e}")
            self.connection.rollback()
            # 오류 시에도 Safe Update Mode 재활성화
            self._enable_safe_updates()
            raise
        finally:
            cursor.close()

    def update_gun_avg_prices(self):
        """군별 평균가격 업데이트"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            logger.info("📊 2단계: 군별 평균가격 업데이트 시작")

            # Safe Update Mode 비활성화
            self._disable_safe_updates()

            sql = """
            UPDATE dong_code_superman d
            JOIN (
                SELECT gun_prefix, avg_price, apt_count FROM (
                    SELECT
                        LEFT(CONCAT(hi.sgg_cd COLLATE utf8mb4_unicode_ci, hi.umd_cd COLLATE utf8mb4_unicode_ci), 5) as gun_prefix,
                        AVG(CAST(REPLACE(lh.deal_amount, ',', '') AS UNSIGNED)) as avg_price,
                        COUNT(*) as apt_count
                    FROM houseinfos hi
                    JOIN latest_housedeals lh ON hi.apt_seq COLLATE utf8mb4_unicode_ci = lh.apt_seq COLLATE utf8mb4_unicode_ci
                    WHERE lh.deal_amount IS NOT NULL AND lh.deal_amount != ''
                    GROUP BY LEFT(CONCAT(hi.sgg_cd COLLATE utf8mb4_unicode_ci, hi.umd_cd COLLATE utf8mb4_unicode_ci), 5)
                ) AS gun_subquery
            ) t ON LEFT(d.dong_code COLLATE utf8mb4_unicode_ci, 5) = t.gun_prefix
            SET d.avg_price = t.avg_price,
                d.apt_count = t.apt_count
            WHERE LENGTH(d.dong_code) = 5
            """

            cursor.execute(sql)
            affected_rows = cursor.rowcount
            self.connection.commit()

            # Safe Update Mode 재활성화
            self._enable_safe_updates()

            logger.info(f"✅ 군별 평균가격 업데이트 완료: {affected_rows}개 군")
            return affected_rows

        except Exception as e:
            logger.error(f"❌ 군별 평균가격 업데이트 실패: {e}")
            self.connection.rollback()
            self._enable_safe_updates()
            raise
        finally:
            cursor.close()

    def update_sido_avg_prices(self):
        """시도별 평균가격 업데이트"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            logger.info("📊 3단계: 시도별 평균가격 업데이트 시작")

            # Safe Update Mode 비활성화
            self._disable_safe_updates()

            sql = """
            UPDATE dong_code_superman d
            JOIN (
                SELECT sido_prefix, avg_price, apt_count FROM (
                    SELECT
                        LEFT(CONCAT(hi.sgg_cd COLLATE utf8mb4_unicode_ci, hi.umd_cd COLLATE utf8mb4_unicode_ci), 2) as sido_prefix,
                        AVG(CAST(REPLACE(lh.deal_amount, ',', '') AS UNSIGNED)) as avg_price,
                        COUNT(*) as apt_count
                    FROM houseinfos hi
                    JOIN latest_housedeals lh ON hi.apt_seq COLLATE utf8mb4_unicode_ci = lh.apt_seq COLLATE utf8mb4_unicode_ci
                    WHERE lh.deal_amount IS NOT NULL AND lh.deal_amount != ''
                    GROUP BY LEFT(CONCAT(hi.sgg_cd COLLATE utf8mb4_unicode_ci, hi.umd_cd COLLATE utf8mb4_unicode_ci), 2)
                ) AS sido_subquery
            ) t ON LEFT(d.dong_code COLLATE utf8mb4_unicode_ci, 2) = t.sido_prefix
            SET d.avg_price = t.avg_price,
                d.apt_count = t.apt_count
            WHERE LENGTH(d.dong_code) = 2
            """

            cursor.execute(sql)
            affected_rows = cursor.rowcount
            self.connection.commit()

            # Safe Update Mode 재활성화
            self._enable_safe_updates()

            logger.info(f"✅ 시도별 평균가격 업데이트 완료: {affected_rows}개 시도")
            return affected_rows

        except Exception as e:
            logger.error(f"❌ 시도별 평균가격 업데이트 실패: {e}")
            self.connection.rollback()
            self._enable_safe_updates()
            raise
        finally:
            cursor.close()

    def update_all_avg_prices(self):
        """전체 평균가격 업데이트 (Safe Update Mode 처리)"""
        logger.info("🚀 === Safe Update Mode 비활성화 평균가격 업데이트 시작 ===")

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

            logger.info("🎉 === Safe Update Mode 처리 평균가격 업데이트 완료 ===")
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
            logger.error(f"❌ Safe Update Mode 처리 평균가격 업데이트 실패: {e}")
            raise

    def close(self):
        if self.connection:
            # 종료 전 Safe Update Mode 재활성화 확인
            try:
                self._enable_safe_updates()
            except:
                pass
            self.connection.close()
            logger.info("🔌 Safe Update 비활성화 평균가격 업데이터 DB 연결 종료")
