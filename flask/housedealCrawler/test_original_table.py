# test_original_table.py
from original_table_saver import OriginalTableSaver
from corrected_xml_crawler import CorrectedXMLCrawler
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def test_original_table_save():
    """원본 테이블 구조로 저장 테스트"""

    API_KEY = "Tr5R4h7B1ooVneI7RvqRT7e/gww2bCjIovGfpJvwiEC3MgNNDCdPOwkfrx9UbXLZZtJedgWAl4mXCQNcD0tVAA=="
    DB_CONFIG = {
        'host': 'localhost',
        'database': 'ssafyhome',
        'user': 'ssafy',
        'password': 'ssafy'
    }

    try:
        # 크롤러와 DB 초기화
        crawler = CorrectedXMLCrawler(API_KEY)
        db = OriginalTableSaver(DB_CONFIG)

        # DB 연결
        db.connect()

        # 테스트 데이터 수집
        region_code = '11680'  # 강남구
        target_month = '202408'  # 2024년 8월

        logger.info(f"🧪 {region_code} {target_month} 테스트 시작")

        # 데이터 수집
        deals_data = crawler.crawl_region_data_corrected(region_code, target_month)

        if deals_data:
            logger.info(f"✅ 수집 성공: {len(deals_data)}건")

            # ✅ 원본 테이블 구조로 저장
            saved_count = db.save_house_deals_original(deals_data, target_month, region_code)

            if saved_count > 0:
                logger.info(f"🎉 저장 성공: {saved_count}건")
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
    test_original_table_save()
