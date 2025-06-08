# simple_disable_foreign_key.py
import pymysql
import logging
from datetime import datetime

logger = logging.getLogger(__name__)


class SimpleDisableForeignKeySaver:
    """외래 키 제약 조건을 임시 비활성화하여 저장 (Type Hint 없음)"""

    def __init__(self, db_config):
        self.db_config = db_config
        self.connection = None

    def connect(self):
        """데이터베이스 연결"""
        try:
            self.connection = pymysql.connect(**self.db_config, charset='utf8mb4', autocommit=False)
            logger.info("✅ 데이터베이스 연결 성공")
        except Exception as e:
            logger.error(f"❌ 데이터베이스 연결 실패: {e}")
            raise

    def save_house_deals_no_fk(self, deals_data, target_month, region_code):
        """외래 키 제약 조건 비활성화하여 저장"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            self.connection.begin()

            # 외래 키 제약 조건 임시 비활성화
            logger.info("🔓 외래 키 제약 조건 임시 비활성화")
            cursor.execute("SET FOREIGN_KEY_CHECKS = 0")

            if not deals_data:
                logger.warning(f"⚠️ {region_code} {target_month}: 저장할 데이터 없음")
                cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
                self.connection.commit()
                return 0

            valid_deals = []
            skipped_count = 0

            for deal in deals_data:
                try:
                    apt_nm = deal.get('apt_nm', '').strip()
                    if not apt_nm:
                        skipped_count += 1
                        continue

                    # apt_seq 생성
                    apt_seq = f"SEQ_{region_code}_{apt_nm.replace(' ', '')[:10]}"

                    # 날짜 변환
                    deal_year = int(deal.get('deal_year', 0)) if deal.get('deal_year') else None
                    deal_month = int(deal.get('deal_month', 0)) if deal.get('deal_month') else None
                    deal_day = int(deal.get('deal_day', 0)) if deal.get('deal_day') else None

                    if not all([deal_year, deal_month, deal_day]):
                        skipped_count += 1
                        continue

                    # 면적 변환
                    exclu_use_ar = None
                    if deal.get('exclu_use_ar'):
                        try:
                            exclu_use_ar = float(deal.get('exclu_use_ar', 0))
                        except:
                            exclu_use_ar = None

                    deal_tuple = (
                        apt_seq,  # apt_seq
                        apt_nm[:30],  # apt_dong
                        deal.get('floor', '').strip()[:3] or None,  # floor
                        deal_year, deal_month, deal_day,  # 날짜들
                        exclu_use_ar,  # exclu_use_ar
                        deal.get('deal_amount', '').strip()[:10]  # deal_amount
                    )

                    valid_deals.append(deal_tuple)

                except Exception as e:
                    skipped_count += 1
                    continue

            if not valid_deals:
                logger.warning(f"⚠️ {region_code} {target_month}: 유효한 데이터 없음")
                cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
                self.connection.commit()
                return 0

            # 데이터 삽입
            insert_sql = '''
            INSERT INTO housedeals (
                apt_seq, apt_dong, floor, deal_year, deal_month, deal_day,
                exclu_use_ar, deal_amount
            ) VALUES (
                %s, %s, %s, %s, %s, %s, %s, %s
            )
            '''

            cursor.executemany(insert_sql, valid_deals)
            inserted_count = cursor.rowcount

            # 외래 키 제약 조건 다시 활성화
            logger.info("🔒 외래 키 제약 조건 다시 활성화")
            cursor.execute("SET FOREIGN_KEY_CHECKS = 1")

            self.connection.commit()

            logger.info(f"✅ {region_code} {target_month}: {inserted_count}건 저장 완료")
            logger.info(f"📊 처리 결과: 유효 {len(valid_deals)}건, 건너뜀 {skipped_count}건")

            return inserted_count

        except Exception as e:
            logger.error(f"❌ {region_code} {target_month} 저장 실패: {e}")
            try:
                cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
                self.connection.rollback()
            except:
                pass
            raise
        finally:
            cursor.close()

    def check_table_structure(self):
        """테이블 구조 확인"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            cursor.execute("SHOW COLUMNS FROM housedeals")
            columns = cursor.fetchall()

            logger.info("=== housedeals 테이블 구조 ===")
            for col in columns:
                logger.info(f"  {col[0]} - {col[1]}")

            return [col[0] for col in columns]

        except Exception as e:
            logger.error(f"❌ 테이블 구조 확인 실패: {e}")
            return []
        finally:
            cursor.close()

    def get_data_count(self):
        """저장된 데이터 개수 확인"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            cursor.execute("SELECT COUNT(*) FROM housedeals")
            count = cursor.fetchone()[0]
            logger.info(f"📊 현재 저장된 거래 건수: {count:,}건")
            return count

        except Exception as e:
            logger.error(f"❌ 데이터 개수 확인 실패: {e}")
            return 0
        finally:
            cursor.close()

    def get_recent_deals(self, limit=5):
        """최근 거래 데이터 조회"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            query = """
            SELECT apt_dong, deal_amount, deal_year, deal_month, deal_day, floor
            FROM housedeals 
            ORDER BY deal_year DESC, deal_month DESC, deal_day DESC 
            LIMIT %s
            """
            cursor.execute(query, (limit,))
            deals = cursor.fetchall()

            logger.info(f"📋 최근 거래 {len(deals)}건:")
            for deal in deals:
                logger.info(f"  {deal[0]}: {deal[1]} ({deal[2]}.{deal[3]:02d}.{deal[4]:02d}) {deal[5]}층")

            return deals

        except Exception as e:
            logger.error(f"❌ 최근 거래 조회 실패: {e}")
            return []
        finally:
            cursor.close()

    def close(self):
        """연결 종료"""
        if self.connection:
            try:
                # 종료 전 외래 키 제약 조건 다시 활성화 (안전장치)
                cursor = self.connection.cursor()
                cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
                cursor.close()

                self.connection.close()
                logger.info("🔌 데이터베이스 연결 종료")
            except:
                pass


# 테스트용 함수
def test_simple_saver():
    """SimpleDisableForeignKeySaver 테스트"""

    DB_CONFIG = {
        'host': 'localhost',
        'database': 'ssafyhome',
        'user': 'ssafy',
        'password': 'ssafy'
    }

    saver = SimpleDisableForeignKeySaver(DB_CONFIG)

    try:
        # 연결 및 테이블 구조 확인
        saver.connect()
        saver.check_table_structure()

        # 현재 데이터 개수 확인
        saver.get_data_count()

        # 최근 거래 조회
        saver.get_recent_deals(3)

        logger.info("✅ 테스트 완료")

    except Exception as e:
        logger.error(f"❌ 테스트 실패: {e}")
    finally:
        saver.close()


if __name__ == "__main__":
    # 로깅 설정
    logging.basicConfig(level=logging.INFO)

    # 테스트 실행
    test_simple_saver()
