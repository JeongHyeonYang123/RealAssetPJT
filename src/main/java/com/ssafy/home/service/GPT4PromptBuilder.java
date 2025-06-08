package com.ssafy.home.service;

import com.ssafy.home.domain.Apartment;
import java.util.List;

public class GPT4PromptBuilder {

    // 🔥 1. 기존 아파트 추천 프롬프트
    public static String buildRecommendPrompt(String userQuery, List<Apartment> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("아래 조건에 맞는 아파트를 추천해줘.\n");
        sb.append("사용자 조건: ").append(userQuery).append("\n");
        sb.append("아파트 후보 목록:\n");

        for (Apartment apt : candidates) {
            sb.append(String.format("- %s | 가격: %d만원 | 면적: %.1f㎡ | 주소: %s | 상권점수: %.1f\n",
                    apt.getName(), apt.getPrice(), apt.getArea(), apt.getAddress(), apt.getCommercialScore()));
        }

        sb.append("가장 적합한 아파트를 5개 추천하고, 이유도 간단히 설명해줘.");
        return sb.toString();
    }

    // 🔥 2. 기존 아파트 정보 프롬프트 (상권 정보 없음)
    public static String buildApartmentInfoPrompt(
            String userQuery, String aptName, String address, Integer buildYear,
            String latestDealAmount, Double latestExcluUseAr, Integer latestDealYear,
            Integer latestDealMonth, Integer latestDealDay) {

        StringBuilder sb = new StringBuilder();
        sb.append("아래 아파트에 대해 사용자의 질의에 맞춰 친절하게 설명해줘.\n");
        sb.append("사용자 질의: ").append(userQuery).append("\n");
        sb.append("아파트명: ").append(aptName).append("\n");
        sb.append("주소: ").append(address).append("\n");

        if (buildYear != null) {
            sb.append("준공연도: ").append(buildYear).append("\n");
        }
        if (latestDealAmount != null) {
            sb.append("최근 거래가: ").append(latestDealAmount).append("만원\n");
        }
        if (latestExcluUseAr != null) {
            sb.append("최근 거래 면적: ").append(String.format("%.1f㎡", latestExcluUseAr)).append("\n");
        }
        if (latestDealYear != null && latestDealMonth != null && latestDealDay != null) {
            sb.append("최근 거래일: ").append(latestDealYear).append("년 ")
                    .append(latestDealMonth).append("월 ").append(latestDealDay).append("일\n");
        }

        sb.append("아파트의 특징, 장점, 주변 환경 등을 요약해서 설명해줘.\n");
        return sb.toString();
    }

    // 🔥 3. 상권 정보 포함 아파트 정보 프롬프트
    public static String buildApartmentInfoWithCommercialPrompt(
            String userQuery, String aptName, String address, Integer buildYear,
            String latestDealAmount, Double latestExcluUseAr, Integer latestDealYear,
            Integer latestDealMonth, Integer latestDealDay, String commercialInfo) {

        StringBuilder sb = new StringBuilder();
        sb.append("아래 아파트에 대해 사용자의 질의에 맞춰 친절하게 설명해줘.\n");
        sb.append("사용자 질의: ").append(userQuery).append("\n");
        sb.append("아파트명: ").append(aptName).append("\n");
        sb.append("주소: ").append(address).append("\n");

        if (buildYear != null) {
            sb.append("준공연도: ").append(buildYear).append("\n");
        }
        if (latestDealAmount != null) {
            sb.append("최근 거래가: ").append(latestDealAmount).append("만원\n");
        }
        if (latestExcluUseAr != null) {
            sb.append("최근 거래 면적: ").append(String.format("%.1f㎡", latestExcluUseAr)).append("\n");
        }
        if (latestDealYear != null && latestDealMonth != null && latestDealDay != null) {
            sb.append("최근 거래일: ").append(latestDealYear).append("년 ")
                    .append(latestDealMonth).append("월 ").append(latestDealDay).append("일\n");
        }

        // 🔥 상권 정보 추가
        if (commercialInfo != null && !commercialInfo.isEmpty()) {
            sb.append(commercialInfo).append("\n");
        }

        sb.append("아파트의 특징, 장점, 주변 환경, 상권 현황 등을 종합해서 간략하게 설명해줘.\n");
        return sb.toString();
    }

    // 🔥 4. 커뮤니티 정보까지 포함한 아파트 정보 프롬프트 (새로 추가!)
    public static String buildApartmentInfoWithCommunityPrompt(
            String userQuery, String aptName, String address, Integer buildYear,
            String latestDealAmount, Double latestExcluUseAr, Integer latestDealYear,
            Integer latestDealMonth, Integer latestDealDay,
            String commercialInfo, String communityInfo) {

        StringBuilder sb = new StringBuilder();
        sb.append("아래 아파트에 대해 사용자의 질의에 맞춰 친절하고 객관적으로 설명해줘.\n");
        sb.append("사용자 질의: ").append(userQuery).append("\n");
        sb.append("아파트명: ").append(aptName).append("\n");
        sb.append("주소: ").append(address).append("\n");

        if (buildYear != null) {
            sb.append("준공연도: ").append(buildYear).append("\n");
        }
        if (latestDealAmount != null) {
            sb.append("최근 거래가: ").append(latestDealAmount).append("만원\n");
        }
        if (latestExcluUseAr != null) {
            sb.append("최근 거래 면적: ").append(String.format("%.1f㎡", latestExcluUseAr)).append("\n");
        }
        if (latestDealYear != null && latestDealMonth != null && latestDealDay != null) {
            sb.append("최근 거래일: ").append(latestDealYear).append("년 ")
                    .append(latestDealMonth).append("월 ").append(latestDealDay).append("일\n");
        }

        // 🔥 상권 정보 추가
        if (commercialInfo != null && !commercialInfo.isEmpty()) {
            sb.append("\n").append(commercialInfo).append("\n");
        }

        // 🔥 커뮤니티 정보 추가
        if (communityInfo != null && !communityInfo.isEmpty()) {
            sb.append("\n").append(communityInfo).append("\n");
        }

        sb.append("\n위 정보를 바탕으로 아파트의 특징, 장점, 주변 환경, 상권 현황, 주민 후기 등을 종합하여 ");
        sb.append("객관적이고 균형잡힌 시각으로 설명해줘. ");
        sb.append("긍정적인 면과 고려사항을 모두 포함하여 실질적인 정보를 제공해줘.\n");

        return sb.toString();
    }

    // 🔥 5. 아파트 검색 조건 추출 프롬프트 (ApartmentSearchCondition용)
    public static String buildSearchConditionPrompt(String userQuery) {
        return String.format("""
                다음 사용자 입력에서 아파트 검색 조건을 추출해주세요.

                사용자 입력: %s

                다음 JSON 형식으로 응답해주세요:
                {
                    "minPrice": 최소가격(만원, 숫자만),
                    "maxPrice": 최대가격(만원, 숫자만),
                    "minArea": 최소면적(㎡, 숫자만),
                    "maxArea": 최대면적(㎡, 숫자만),
                    "addressKeyword": "지역명",
                    "commercialPreference": "상권 선호도 설명"
                }

                규칙:
                1. 가격은 만원 단위로 변환 (예: 5억 → 50000)
                2. 면적은 평을 ㎡로 변환 (1평 ≈ 3.3㎡)
                3. 정보가 없으면 null로 설정
                4. JSON 형식만 응답하고 다른 설명은 하지 않음
                """, userQuery);
    }

    // 🔥 6. 상권 가중치 추출 프롬프트 (CommercialAreaScorer용)
    public static String buildCommercialWeightPrompt(String userQuery) {
        return String.format("""
                다음 사용자 입력에서 상권 선호도를 분석해주세요.

                사용자 입력: %s

                다음 형식의 JSON으로 응답해주세요:
                {
                    "weights": {
                        "음식점": 2.0,
                        "카페": 1.5,
                        "편의점": 1.0,
                        "병원": 2.0,
                        "약국": 1.5,
                        "마트": 2.0,
                        "백화점": 2.5,
                        "학교": 2.0,
                        "지하철": 2.5,
                        "버스정류장": 1.5
                    }
                }

                규칙:
                1. 사용자가 언급한 상권 유형의 가중치는 기본값의 1.5배로 설정
                2. 사용자가 특별히 강조한 상권 유형은 2배로 설정
                3. 사용자가 부정적으로 언급한 상권 유형은 0.5배로 설정
                4. 언급되지 않은 상권 유형은 기본값 유지
                5. JSON 형식만 응답하고 다른 설명은 하지 않음
                """, userQuery);
    }
}
