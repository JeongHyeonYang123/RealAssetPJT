# corrected_xml_crawler.py
import requests
import xml.etree.ElementTree as ET
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class CorrectedXMLCrawler:
    """올바른 XML 태그명으로 수정된 크롤러"""

    def __init__(self, api_key: str):
        self.api_key = api_key
        self.base_url = 'http://apis.data.go.kr/1613000/RTMSDataSvcAptTrade'
        self.session = requests.Session()

        # ✅ 실제 API의 영어 태그명 매핑
        self.tag_mapping = {
            # 한글 태그명 → 영어 태그명 (실제 API 응답)
            '아파트': 'aptNm',
            '층': 'floor',
            '년': 'dealYear',
            '월': 'dealMonth',
            '일': 'dealDay',
            '전용면적': 'excluUseAr',
            '거래금액': 'dealAmount',
            '지번': 'jibun',
            '건축년도': 'buildYear',
            '도로명': 'roadNm',
            '도로명건물본번호코드': 'roadNmBonbun',
            '도로명건물부번호코드': 'roadNmBubun',
            '법정동': 'umdNm'
        }

    def _get_xml_text_corrected(self, element, korean_tag: str) -> str:
        """수정된 XML 텍스트 추출 - 영어 태그명 사용"""
        # 한글 태그명을 영어 태그명으로 변환
        english_tag = self.tag_mapping.get(korean_tag, korean_tag)

        # 영어 태그명으로 검색
        elem = element.find(english_tag)
        if elem is not None and elem.text:
            return elem.text.strip()

        # 영어 태그명이 안되면 한글 태그명도 시도
        elem = element.find(korean_tag)
        if elem is not None and elem.text:
            return elem.text.strip()

        return ''

    def _get_xml_text_all_methods(self, element, korean_tag: str) -> str:
        """모든 방법으로 XML 텍스트 추출 시도"""
        # 방법 1: 영어 태그명
        english_tag = self.tag_mapping.get(korean_tag, korean_tag)
        elem = element.find(english_tag)
        if elem is not None and elem.text:
            return elem.text.strip()

        # 방법 2: 한글 태그명
        elem = element.find(korean_tag)
        if elem is not None and elem.text:
            return elem.text.strip()

        # 방법 3: 대소문자 변형 시도
        for tag_variation in [english_tag.lower(), english_tag.upper(), english_tag]:
            elem = element.find(tag_variation)
            if elem is not None and elem.text:
                return elem.text.strip()

        return ''

    def crawl_region_data_corrected(self, region_code: str, target_month: str):
        """수정된 지역 데이터 크롤링"""
        url = f"{self.base_url}/getRTMSDataSvcAptTrade"

        params = {
            'serviceKey': self.api_key,
            'pageNo': 1,
            'numOfRows': 10,  # 테스트용으로 10건만
            'LAWD_CD': region_code,
            'DEAL_YMD': target_month
        }

        try:
            response = self.session.get(url, params=params, timeout=30)
            response.raise_for_status()

            # ✅ 디버깅: 전체 응답 출력 (처음 1000자)
            logger.info(f"📋 {region_code} XML 응답 샘플:")
            logger.info(response.text[:1000] + "..." if len(response.text) > 1000 else response.text)

            root = ET.fromstring(response.content)

            # 결과 코드 확인
            result_code = root.find('.//resultCode')
            if result_code is not None:
                code = result_code.text.strip()
                if code not in ['00', '000']:
                    logger.error(f"❌ {region_code} API 오류: {code}")
                    return []

            # item 요소 찾기
            items = root.findall('.//item')

            if not items:
                logger.info(f"📭 {region_code}: item 요소 없음")
                return []

            logger.info(f"📊 {region_code}: {len(items)}개 item 발견")

            # ✅ 첫 번째 item의 모든 태그 출력 (디버깅)
            if items:
                first_item = items[0]
                logger.info(f"🔍 첫 번째 item의 모든 태그:")
                for child in first_item:
                    logger.info(f"  {child.tag}: '{child.text}'")

            # 데이터 추출 (수정된 방법)
            deals = []
            for item in items:
                deal = {
                    'apt_nm': self._get_xml_text_all_methods(item, '아파트'),
                    'floor': self._get_xml_text_all_methods(item, '층'),
                    'deal_year': self._get_xml_text_all_methods(item, '년'),
                    'deal_month': self._get_xml_text_all_methods(item, '월'),
                    'deal_day': self._get_xml_text_all_methods(item, '일'),
                    'exclu_use_ar': self._get_xml_text_all_methods(item, '전용면적'),
                    'deal_amount': self._get_xml_text_all_methods(item, '거래금액'),
                    'jibun': self._get_xml_text_all_methods(item, '지번'),
                    'build_year': self._get_xml_text_all_methods(item, '건축년도'),
                }
                deals.append(deal)

                # ✅ 첫 번째 deal 데이터 출력 (디버깅)
                if len(deals) == 1:
                    logger.info(f"🔍 첫 번째 변환된 데이터:")
                    for key, value in deal.items():
                        logger.info(f"  {key}: '{value}'")

            logger.info(f"✅ {region_code}: {len(deals)}건 추출 완료")
            return deals

        except Exception as e:
            logger.error(f"❌ {region_code} 크롤링 실패: {e}")
            return []


# 테스트 실행
def test_corrected_crawler():
    """수정된 크롤러 테스트"""
    API_KEY = "Tr5R4h7B1ooVneI7RvqRT7e/gww2bCjIovGfpJvwiEC3MgNNDCdPOwkfrx9UbXLZZtJedgWAl4mXCQNcD0tVAA=="

    crawler = CorrectedXMLCrawler(API_KEY)

    # 데이터가 있을 가능성이 높은 조건으로 테스트
    test_cases = [
        ('11680', '202408'),  # 강남구 2024년 8월
        ('11650', '202407'),  # 서초구 2024년 7월
    ]

    for region_code, month in test_cases:
        logger.info(f"\n{'=' * 50}")
        logger.info(f"🧪 테스트: {region_code} {month}")
        logger.info('=' * 50)

        data = crawler.crawl_region_data_corrected(region_code, month)

        if data:
            logger.info(f"🎉 성공: {len(data)}건 수집")

            # 데이터 품질 확인
            non_empty_data = [d for d in data if any(v.strip() for v in d.values())]
            logger.info(f"📊 비어있지 않은 데이터: {len(non_empty_data)}건")

            if non_empty_data:
                logger.info("✅ 수정된 크롤러 성공!")
                return True
        else:
            logger.warning(f"⚠️ {region_code} {month}: 데이터 없음")

    logger.error("❌ 모든 테스트 실패")
    return False


if __name__ == "__main__":
    test_corrected_crawler()
