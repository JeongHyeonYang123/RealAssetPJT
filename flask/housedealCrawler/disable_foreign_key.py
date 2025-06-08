# disable_foreign_key.py
import pymysql
import logging
from typing import List, Dict, Any  # ✅ typing import 추가
logger = logging.getLogger(__name__)


class DisableForeignKeySaver:
    """외래 키 제약 조건을 임시 비활성화하여 저장"""

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

    def save_house_deals_no_fk(self, deals_data: List[Dict], target_month: str, region_code: str) -> int:
        """외래 키 제약 조건 비활성화하여 저장"""
        if not self.connection:
            self.connect()

        cursor = self.connection.cursor()

        try:
            self.connection.begin()

            # ✅ 외래 키 제약 조건 임시 비활성화
            logger.info("🔓 외래 키 제약 조건 임시 비활성화")
            cursor.execute("SET FOREIGN_KEY_CHECKS = 0")

            if not deals_data:
                logger.warning(f"⚠️ {region_code} {target_month}: 저장할 데이터 없음")
                return 0

            valid_deals = []
            skipped_count = 0

            for deal in deals_data:
                try:
                    apt_nm = deal.get('apt_nm', '').strip()
                    if not apt_nm:
                        skipped_count += 1
                        continue

                    # 간단한 apt_seq 생성
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
                        apt_seq,
                        apt_nm[:30],
                        deal.get('floor', '').strip()[:3] or None,
                        deal_year, deal_month, deal_day,
                        exclu_use_ar,
                        deal.get('deal_amount', '').strip()[:10]
                    )

                    valid_deals.append(deal_tuple)

                except Exception as e:
                    skipped_count += 1
                    continue

            if not valid_deals:
                logger.warning(f"⚠️ {region_code} {target_month}: 유효한 데이터 없음")
                cursor.execute("SET FOREIGN_KEY_CHECKS = 1")  # 다시 활성화
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

            # ✅ 외래 키 제약 조건 다시 활성화
            logger.info("🔒 외래 키 제약 조건 다시 활성화")
            cursor.execute("SET FOREIGN_KEY_CHECKS = 1")

            self.connection.commit()

            logger.info(f"✅ {region_code} {target_month}: {inserted_count}건 저장 완료")
            return inserted_count

        except Exception as e:
            logger.error(f"❌ {region_code} {target_month} 저장 실패: {e}")
            # 오류 시에도 외래 키 제약 조건 다시 활성화
            cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
            self.connection.rollback()
            raise
        finally:
            cursor.close()

    def close(self):
        """연결 종료"""
        if self.connection:
            self.connection.close()
