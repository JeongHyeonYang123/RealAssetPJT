# test_foreign_key_fix.py
from foreign_key_handler import ForeignKeyHandler
from disable_foreign_key import DisableForeignKeySaver
from corrected_xml_crawler import CorrectedXMLCrawler
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def test_foreign_key_solutions():
    """외래 키 문제 해결 방법들 테스트"""

    API_KEY = "Tr5R4h7B1ooVneI7RvqRT7e/gww2bCjIovGfpJvwiEC3MgNNDCdPOwkfrx9UbXLZZtJedgWAl4mXCQNcD0tVAA=="
    DB_CONFIG = {
        'host': 'localhost',
        'database': 'ssafyhome',
        'user': 'ssafy',
        'password': 'ssafy'
    }

    # 테스트할 방법 선택
    print("🔧 외래 키 문제 해결 방법 선택:")
    print("1. houseinfos에 먼저 데이터 삽입 (권장)")
    print("2. 외래 키 제약 조건 임시 비활성화")

    choice = input("선택하세요 (1-2) [기본값: 2]: ").strip() or "2"

    try:
        crawler = CorrectedXMLCrawler(API_KEY)

        if choice == "1":
            db = ForeignKeyHandler(DB_CONFIG)
            save_method = db.save_house_deals_with_fk
            method_name = "houseinfos 연동 방식"
        else:
            db = DisableForeignKeySaver(DB_CONFIG)
            save_method = db.save_house_deals_no_fk
            method_name = "외래 키 비활성화 방식"

        logger.info(f"🧪 {method_name}으로 테스트 시작")

        # DB 연결
        db.connect()

        # 테스트 데이터 수집
        region_code = '11680'
        target_month = '202408'

        deals_data = crawler.crawl_region_data_corrected(region_code, target_month)

        if deals_data:
            logger.info(f"✅ 수집 성공: {len(deals_data)}건")

            # 선택한 방식으로 저장
            saved_count = save_method(deals_data, target_month, region_code)

            if saved_count > 0:
                logger.info(f"🎉 {method_name} 성공: {saved_count}건 저장")
            else:
                logger.warning("⚠️ 저장된 데이터 없음")
        else:
            logger.warning("⚠️ 수집된 데이터 없음")

    except Exception as e:
        logger.error(f"💥 테스트 실패: {e}")
    finally:
        if 'db' in locals():
            db.close()


if __name__ == "__main__":
    test_foreign_key_solutions()
