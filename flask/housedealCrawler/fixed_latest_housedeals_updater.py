# fixed_latest_housedeals_updater.py
import pymysql
import logging
from datetime import datetime

logger = logging.getLogger(__name__)


class FixedLatestHouseDealsUpdater:
    """중복 키 문제 해결된 latest_housedeals 업데이터"""

    def __init__(self, db_config):
        self.db_config = db_config
        self.connection = None

    def connect(self):
        """데이터베이스 연결"""
        try:
            self.connection = pymysql.connect(**self.db_config, charset='utf8mb4', autocommit=False)
            logger.info("✅ 수정된 latest_housedeals 업데이터 DB 연결 성공")
        except Exception as e:
            logger.error(f"❌ DB 연결 실패: {e}")
            raise

    def check_table_structure(self):
        """테이블 구조 확인"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            logger.info("🔍 latest_housedeals 테이블 구조 확인 중...")

            # 테이블 구조 확인
            cursor.execute("SHOW CREATE TABLE latest_housedeals")
            table_structure = cursor.fetchone()
            if table_structure:
                logger.info(f"테이블 구조:\n{table_structure[1]}")

            # 인덱스 확인
            cursor.execute("SHOW INDEX FROM latest_housedeals")
            indexes = cursor.fetchall()
            logger.info("인덱스 정보:")
            for index in indexes:
                logger.info(f"  {index[2]} ({index[4]}): {index[10]}")

            # 현재 데이터 개수
            cursor.execute("SELECT COUNT(*) FROM latest_housedeals")
            count = cursor.fetchone()[0]
            logger.info(f"현재 데이터 개수: {count:,}건")

        except Exception as e:
            logger.error(f"❌ 테이블 구조 확인 실패: {e}")
        finally:
            cursor.close()

    def recreate_latest_housedeals_table(self):
        """latest_housedeals 테이블 재생성 (올바른 구조로)"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            logger.info("🔄 latest_housedeals 테이블 재생성 중...")

            # 기존 테이블 삭제
            cursor.execute("DROP TABLE IF EXISTS latest_housedeals")
            logger.info("🗑️ 기존 테이블 삭제 완료")

            # ✅ 올바른 구조로 테이블 재생성
            create_table_sql = """
            CREATE TABLE latest_housedeals (
                apt_seq VARCHAR(50) NOT NULL PRIMARY KEY,
                deal_year INT NOT NULL,
                deal_month INT NOT NULL,
                deal_day INT NOT NULL,
                deal_amount VARCHAR(50),
                exclu_use_ar DECIMAL(10,2),
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_deal_date (deal_year, deal_month, deal_day),
                INDEX idx_updated_at (updated_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """

            cursor.execute(create_table_sql)
            self.connection.commit()

            logger.info("✅ latest_housedeals 테이블 재생성 완료 (apt_seq를 PRIMARY KEY로)")

        except Exception as e:
            logger.error(f"❌ 테이블 재생성 실패: {e}")
            self.connection.rollback()
            raise
        finally:
            cursor.close()

    def update_latest_housedeals_safe(self):
        """안전한 방식으로 latest_housedeals 업데이트"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            logger.info("🔄 안전한 방식으로 latest_housedeals 업데이트 시작")

            # 테이블 재생성 (구조 문제 해결)
            self.recreate_latest_housedeals_table()

            # ✅ REPLACE INTO 사용 (중복 자동 처리)
            replace_sql = """
            REPLACE INTO latest_housedeals (apt_seq, deal_year, deal_month, deal_day, deal_amount, exclu_use_ar, updated_at)
            SELECT
                h1.apt_seq,
                h1.deal_year,
                h1.deal_month,
                h1.deal_day,
                h1.deal_amount,
                h1.exclu_use_ar,
                NOW()
            FROM housedeals h1
            INNER JOIN (
                SELECT 
                    apt_seq, 
                    MAX(CONCAT(
                        LPAD(deal_year, 4, '0'), 
                        LPAD(deal_month, 2, '0'), 
                        LPAD(deal_day, 2, '0')
                    )) AS max_date
                FROM housedeals
                WHERE deal_amount IS NOT NULL AND deal_amount != ''
                GROUP BY apt_seq
            ) h2 ON h1.apt_seq = h2.apt_seq
            AND CONCAT(
                LPAD(h1.deal_year, 4, '0'), 
                LPAD(h1.deal_month, 2, '0'), 
                LPAD(h1.deal_day, 2, '0')
            ) = h2.max_date
            """

            cursor.execute(replace_sql)
            affected_rows = cursor.rowcount
            self.connection.commit()

            logger.info(f"✅ latest_housedeals 안전 업데이트 완료: {affected_rows:,}건 처리")

            # 결과 확인
            self._check_update_result()

            return affected_rows

        except Exception as e:
            logger.error(f"❌ latest_housedeals 안전 업데이트 실패: {e}")
            self.connection.rollback()
            raise
        finally:
            cursor.close()

    def update_latest_housedeals_incremental(self):
        """증분 업데이트 (INSERT ... ON DUPLICATE KEY UPDATE)"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            logger.info("🔄 latest_housedeals 증분 업데이트 시작")

            # ✅ INSERT ... ON DUPLICATE KEY UPDATE 사용
            upsert_sql = """
            INSERT INTO latest_housedeals (apt_seq, deal_year, deal_month, deal_day, deal_amount, exclu_use_ar, updated_at)
            SELECT
                h1.apt_seq,
                h1.deal_year,
                h1.deal_month,
                h1.deal_day,
                h1.deal_amount,
                h1.exclu_use_ar,
                NOW()
            FROM housedeals h1
            INNER JOIN (
                SELECT 
                    apt_seq, 
                    MAX(CONCAT(
                        LPAD(deal_year, 4, '0'), 
                        LPAD(deal_month, 2, '0'), 
                        LPAD(deal_day, 2, '0')
                    )) AS max_date
                FROM housedeals
                WHERE deal_amount IS NOT NULL AND deal_amount != ''
                GROUP BY apt_seq
            ) h2 ON h1.apt_seq = h2.apt_seq
            AND CONCAT(
                LPAD(h1.deal_year, 4, '0'), 
                LPAD(h1.deal_month, 2, '0'), 
                LPAD(h1.deal_day, 2, '0')
            ) = h2.max_date
            ON DUPLICATE KEY UPDATE
                deal_year = VALUES(deal_year),
                deal_month = VALUES(deal_month),
                deal_day = VALUES(deal_day),
                deal_amount = VALUES(deal_amount),
                exclu_use_ar = VALUES(exclu_use_ar),
                updated_at = VALUES(updated_at)
            """

            cursor.execute(upsert_sql)
            affected_rows = cursor.rowcount
            self.connection.commit()

            logger.info(f"✅ latest_housedeals 증분 업데이트 완료: {affected_rows:,}건 처리")

            return affected_rows

        except Exception as e:
            logger.error(f"❌ latest_housedeals 증분 업데이트 실패: {e}")
            self.connection.rollback()
            raise
        finally:
            cursor.close()

    def clean_and_rebuild_latest_housedeals(self):
        """완전 정리 후 재구축"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            logger.info("🧹 latest_housedeals 완전 정리 후 재구축 시작")

            # 1단계: 테이블 백업 (선택적)
            cursor.execute("DROP TABLE IF EXISTS latest_housedeals_backup")
            cursor.execute("CREATE TABLE latest_housedeals_backup SELECT * FROM latest_housedeals")
            backup_count = cursor.rowcount
            logger.info(f"💾 기존 데이터 백업: {backup_count}건")

            # 2단계: 테이블 재생성
            self.recreate_latest_housedeals_table()

            # 3단계: 데이터 재삽입 (배치별 처리)
            logger.info("📊 데이터 재삽입 시작...")

            # apt_seq 목록 가져오기
            cursor.execute("SELECT DISTINCT apt_seq FROM housedeals ORDER BY apt_seq")
            apt_seqs = [row[0] for row in cursor.fetchall()]

            logger.info(f"📋 처리할 아파트: {len(apt_seqs)}개")

            batch_size = 1000
            total_inserted = 0

            for i in range(0, len(apt_seqs), batch_size):
                batch = apt_seqs[i:i + batch_size]

                # 각 배치별로 최신 거래 데이터 삽입
                placeholders = ','.join(['%s'] * len(batch))
                batch_sql = f"""
                INSERT INTO latest_housedeals (apt_seq, deal_year, deal_month, deal_day, deal_amount, exclu_use_ar, updated_at)
                SELECT
                    h1.apt_seq,
                    h1.deal_year,
                    h1.deal_month,
                    h1.deal_day,
                    h1.deal_amount,
                    h1.exclu_use_ar,
                    NOW()
                FROM housedeals h1
                INNER JOIN (
                    SELECT 
                        apt_seq, 
                        MAX(CONCAT(
                            LPAD(deal_year, 4, '0'), 
                            LPAD(deal_month, 2, '0'), 
                            LPAD(deal_day, 2, '0')
                        )) AS max_date
                    FROM housedeals
                    WHERE apt_seq IN ({placeholders})
                    AND deal_amount IS NOT NULL AND deal_amount != ''
                    GROUP BY apt_seq
                ) h2 ON h1.apt_seq = h2.apt_seq
                AND CONCAT(
                    LPAD(h1.deal_year, 4, '0'), 
                    LPAD(h1.deal_month, 2, '0'), 
                    LPAD(h1.deal_day, 2, '0')
                ) = h2.max_date
                """

                cursor.execute(batch_sql, batch)
                batch_inserted = cursor.rowcount
                total_inserted += batch_inserted

                if (i // batch_size + 1) % 10 == 0:
                    logger.info(f"📈 진행상황: {i + len(batch)}/{len(apt_seqs)} 처리됨 ({total_inserted}건 삽입)")

            self.connection.commit()

            logger.info(f"✅ latest_housedeals 완전 재구축 완료: {total_inserted:,}건 삽입")

            # 결과 확인
            self._check_update_result()

            return total_inserted

        except Exception as e:
            logger.error(f"❌ latest_housedeals 완전 재구축 실패: {e}")
            self.connection.rollback()
            raise
        finally:
            cursor.close()

    def _check_update_result(self):
        """업데이트 결과 확인"""
        cursor = self.connection.cursor()

        try:
            # 총 레코드 수
            cursor.execute("SELECT COUNT(*) FROM latest_housedeals")
            total_count = cursor.fetchone()[0]

            # 최근 거래 5건
            cursor.execute("""
                SELECT apt_seq, deal_amount, deal_year, deal_month, deal_day 
                FROM latest_housedeals 
                ORDER BY updated_at DESC 
                LIMIT 5
            """)
            recent_deals = cursor.fetchall()

            logger.info(f"📊 latest_housedeals 현황: 총 {total_count:,}건")
            logger.info("🕐 최근 업데이트된 거래 5건:")
            for deal in recent_deals:
                logger.info(f"  {deal[0]}: {deal[1]} ({deal[2]}.{deal[3]:02d}.{deal[4]:02d})")

        except Exception as e:
            logger.error(f"❌ 결과 확인 실패: {e}")
        finally:
            cursor.close()

    def close(self):
        """연결 종료"""
        if self.connection:
            self.connection.close()
            logger.info("🔌 수정된 latest_housedeals 업데이터 DB 연결 종료")
