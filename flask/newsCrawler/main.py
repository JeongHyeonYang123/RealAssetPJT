from flask import Flask, request, jsonify
import requests
from bs4 import BeautifulSoup
from flask_cors import CORS
import html
import re
from difflib import SequenceMatcher
from datetime import datetime

app = Flask(__name__)
CORS(app)

# 네이버 개발자센터에서 발급받은 정보 입력
CLIENT_ID = 'TewVSvULFBfAd9C9k0io'
CLIENT_SECRET = 'Qis8B3JTj0'


def get_thumbnail(news_url):
    """뉴스 기사의 썸네일 이미지를 추출하는 함수"""
    try:
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
        }
        res = requests.get(news_url, headers=headers, timeout=3)
        soup = BeautifulSoup(res.text, 'html.parser')
        og_image = soup.find('meta', property='og:image')
        if og_image and og_image.get('content'):
            return og_image['content']
    except Exception:
        pass
    return None


def get_naver_news(query, display=5):
    """네이버 뉴스 API를 통해 뉴스를 검색하는 함수"""
    # 따옴표를 사용한 정확한 구문 검색
    if ' ' in query and not query.startswith('"'):
        query = f'"{query}"'

    url = 'https://openapi.naver.com/v1/search/news.json'
    headers = {
        'X-Naver-Client-Id': CLIENT_ID,
        'X-Naver-Client-Secret': CLIENT_SECRET
    }
    params = {
        'query': query,
        'display': display,
        'start': 1,
        'sort': 'date'
    }
    response = requests.get(url, headers=headers, params=params)
    return response.json().get('items', [])


def filter_real_estate_news(news_items):
    """부동산 관련 뉴스만 필터링하는 함수"""
    # 부동산 관련 키워드
    relevant_keywords = [
        '아파트', '주택', '매매', '임대', '전세', '월세',
        '부동산', '분양', '재개발', '재건축', '오피스텔',
        '상가', '토지', '건물', '시세', '가격', '거래',
        '청약', '입주', '분양가', '임차', '투자'
    ]

    # 부동산과 관련없는 키워드 (제외할 키워드)
    exclude_keywords = [
        '게임', '영화', '드라마', '소설', '음악',
        '스포츠', '연예', '패션', '요리', '여행'
    ]

    filtered_items = []
    for item in news_items:
        title = item.get('title', '').lower()
        description = item.get('description', '').lower()
        text = title + " " + description

        # 관련 키워드가 포함되어 있는지 확인
        has_relevant = any(keyword in text for keyword in relevant_keywords)
        has_exclude = any(keyword in text for keyword in exclude_keywords)

        if has_relevant and not has_exclude:
            filtered_items.append(item)

    return filtered_items


def calculate_similarity(text1, text2):
    """두 텍스트 간의 유사도를 계산 (0~1)"""
    return SequenceMatcher(None, text1, text2).ratio()


def normalize_title(title):
    """제목 정규화 (HTML 태그 제거, 소문자 변환, 공백 정리)"""
    title = html.unescape(title)
    title = re.sub(r'<[^>]+>', '', title)  # HTML 태그 제거
    title = re.sub(r'[^\w\s가-힣]', '', title)  # 특수문자 제거
    title = re.sub(r'\s+', ' ', title).strip().lower()  # 공백 정리
    return title


def filter_similar_titles(news_items, threshold=0.7):
    """제목 유사도 기반으로 중복 기사 제거"""
    unique_news = []
    for item in news_items:
        title = normalize_title(item.get('title', ''))
        is_duplicate = False
        for existing_item in unique_news:
            existing_title = normalize_title(existing_item.get('title', ''))
            similarity = calculate_similarity(title, existing_title)
            if similarity >= threshold:
                is_duplicate = True
                break
        if not is_duplicate:
            unique_news.append(item)
    return unique_news


def extract_keywords(text, min_length=2):
    """텍스트에서 주요 키워드 추출"""
    words = re.findall(r'[가-힣]{2,}|[a-zA-Z]{3,}', text)
    return set(word.lower() for word in words if len(word) >= min_length)


def filter_duplicate_by_keywords(news_items, threshold=0.6):
    """키워드 유사도 기반 중복 제거"""
    unique_news = []
    for item in news_items:
        title = item.get('title', '')
        description = item.get('description', '')
        keywords = extract_keywords(title + ' ' + description)
        is_duplicate = False
        for existing_item in unique_news:
            existing_title = existing_item.get('title', '')
            existing_desc = existing_item.get('description', '')
            existing_keywords = extract_keywords(existing_title + ' ' + existing_desc)
            if keywords and existing_keywords:
                intersection = len(keywords & existing_keywords)
                union = len(keywords | existing_keywords)
                jaccard_similarity = intersection / union if union > 0 else 0
                if jaccard_similarity >= threshold:
                    is_duplicate = True
                    break
        if not is_duplicate:
            unique_news.append(item)
    return unique_news


def parse_pub_date(date_str):
    """네이버 뉴스 날짜 형식 파싱"""
    try:
        return datetime.strptime(date_str, "%a, %d %b %Y %H:%M:%S %z")
    except:
        return None


def filter_by_date_and_title(news_items, hours_threshold=6):
    """같은 시간대의 유사한 제목 기사 제거"""
    unique_news = []
    for item in news_items:
        title = normalize_title(item.get('title', ''))
        pub_date = parse_pub_date(item.get('pubDate', ''))
        is_duplicate = False
        for existing_item in unique_news:
            existing_title = normalize_title(existing_item.get('title', ''))
            existing_date = parse_pub_date(existing_item.get('pubDate', ''))
            if pub_date and existing_date:
                time_diff = abs((pub_date - existing_date).total_seconds() / 3600)
                title_similarity = calculate_similarity(title, existing_title)
                if time_diff <= hours_threshold and title_similarity >= 0.6:
                    is_duplicate = True
                    break
        if not is_duplicate:
            unique_news.append(item)
    return unique_news


def comprehensive_duplicate_filter(news_items):
    """여러 방법을 조합한 종합적인 중복 제거"""
    # 1단계: 유사한 제목 제거
    filtered_news = filter_similar_titles(news_items, threshold=0.8)
    # 2단계: 키워드 기반 중복 제거
    filtered_news = filter_duplicate_by_keywords(filtered_news, threshold=0.7)
    # 3단계: 발행일과 제목 조합 필터링
    filtered_news = filter_by_date_and_title(filtered_news, hours_threshold=12)
    return filtered_news


@app.route('/news', methods=['GET'])
def news():
    """뉴스 검색 API 엔드포인트"""
    query = request.args.get('query', '부동산')
    region = request.args.get('region', '')  # 지역 파라미터 추가
    display = int(request.args.get('display', 5))

    # 지역이 지정된 경우 지역명 포함
    if region:
        query = f"{region} {query}"
    elif query == '부동산':
        query = "부동산 시장"  # 더 구체적인 검색어로 변경

    # 더 많은 뉴스를 가져와서 중복 제거 후 필요한 개수만 반환
    news_items = get_naver_news(query, display * 3)

    # 부동산 관련 필터링
    filtered_items = filter_real_estate_news(news_items)

    # 중복 제거
    unique_items = comprehensive_duplicate_filter(filtered_items)

    results = []
    for item in unique_items[:display]:
        link = item.get('link') or item.get('originallink') or ''
        thumbnail = get_thumbnail(link)
        results.append({
            'title': html.unescape(item.get('title', '')),
            'link': link,
            'pubDate': item.get('pubDate'),
            'description': html.unescape(item.get('description', '')),
            'thumbnail': thumbnail
        })

    return jsonify(results)


if __name__ == '__main__':
    app.run(debug=True)
