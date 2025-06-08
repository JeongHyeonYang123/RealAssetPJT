package com.ssafy.home.service;

import com.ssafy.home.ai.GPT4MiniClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class ApartmentSearchCondition {
    private Integer minPrice;
    private Integer maxPrice;
    private Double minArea;
    private Double maxArea;
    private String addressKeyword;
    private String commercialPreference;

    private static final Logger log = LoggerFactory.getLogger(ApartmentSearchCondition.class);
    private static final int PRICE_THRESHOLD = 200000; // 200억원 (현실적인 최대 가격)

    public static ApartmentSearchCondition fromUserQuery(String query, GPT4MiniClient gptClient) {
        try {
            String prompt = String.format("""
                    다음 사용자 질문에서 아파트 검색 조건을 추출해주세요.

                    사용자 질문: "%s"

                    다음 JSON 형식으로 응답해주세요:
                    {
                        "minPrice": 최소 가격(만원 단위, 숫자만),
                        "maxPrice": 최대 가격(만원 단위, 숫자만),
                        "minArea": 최소 면적(평 단위, 숫자만),
                        "maxArea": 최대 면적(평 단위, 숫자만),
                        "addressKeyword": 동 이름(예: 청운동)
                    }

                    주의사항:
                    1. 가격은 만원 단위로 변환 (예: 5억 -> 50000)
                    2. 면적은 평 단위로 유지 (예: 30평 -> 30)
                    3. XX평대는 XX~XX+9 평 범위로 설정 (예: 30평대 -> 30~39)
                    4. 숫자가 아닌 값은 null로 설정
                    5. 동 이름만 추출 (예: "서울시 종로구 청운동" -> "청운동")
                    """, query);

            String response = gptClient.askLLM(prompt);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            ApartmentSearchCondition condition = mapper.readValue(response, ApartmentSearchCondition.class);

            // 평을 제곱미터로 변환 (1평 = 3.30578㎡)
            if (condition.getMinArea() != null) {
                condition.setMinArea(condition.getMinArea() * 3.30578);
            }
            if (condition.getMaxArea() != null) {
                condition.setMaxArea(condition.getMaxArea() * 3.30578);
            }

            condition.setCommercialPreference(query);

            log.info("LLM 파싱 결과 (제곱미터 변환 후): {}", condition);
            return condition;

        } catch (Exception e) {
            log.error("LLM을 통한 검색 조건 파싱 중 오류 발생", e);
            return new ApartmentSearchCondition();
        }
    }

    private static void validateAndCorrectPrices(ApartmentSearchCondition condition, String query) {
        // 가격이 비현실적으로 큰 경우 수동 파싱으로 재시도
        if ((condition.getMinPrice() != null && condition.getMinPrice() > PRICE_THRESHOLD) ||
                (condition.getMaxPrice() != null && condition.getMaxPrice() > PRICE_THRESHOLD)) {
            log.warn("LLM이 추출한 가격이 비현실적으로 큽니다. 수동 파싱으로 재시도합니다.");
            ApartmentSearchCondition manualCondition = parseManually(query);
            condition.setMinPrice(manualCondition.getMinPrice());
            condition.setMaxPrice(manualCondition.getMaxPrice());
        }
    }

    private static ApartmentSearchCondition parseManually(String query) {
        ApartmentSearchCondition condition = new ApartmentSearchCondition();
        condition.setCommercialPreference(query);

        try {
            // 가격 범위 추출
            if (query.contains("억")) {
                String[] parts = query.split("억");
                for (String part : parts) {
                    if (part.contains("~")) {
                        String[] range = part.split("~");
                        if (range.length == 2) {
                            String minStr = range[0].trim();
                            String maxStr = range[1].trim();
                            if (!minStr.isEmpty() && !maxStr.isEmpty()) {
                                // "억" 단위 처리
                                int minPrice = Integer.parseInt(minStr.replaceAll("[^0-9]", "")) * 10000;
                                int maxPrice = Integer.parseInt(maxStr.replaceAll("[^0-9]", "")) * 10000;

                                // 추가로 "천" 단위가 있는지 확인
                                if (minStr.contains("천")) {
                                    String[] minParts = minStr.split("천");
                                    if (minParts.length > 1) {
                                        minPrice += Integer.parseInt(minParts[1].replaceAll("[^0-9]", "")) * 1000;
                                    }
                                }
                                if (maxStr.contains("천")) {
                                    String[] maxParts = maxStr.split("천");
                                    if (maxParts.length > 1) {
                                        maxPrice += Integer.parseInt(maxParts[1].replaceAll("[^0-9]", "")) * 1000;
                                    }
                                }

                                condition.setMinPrice(minPrice);
                                condition.setMaxPrice(maxPrice);
                            }
                        }
                    }
                }
            } else if (query.contains("만원")) {
                // "만원" 단위 처리
                String[] parts = query.split("만원");
                for (String part : parts) {
                    if (part.contains("~")) {
                        String[] range = part.split("~");
                        if (range.length == 2) {
                            String minStr = range[0].trim();
                            String maxStr = range[1].trim();
                            if (!minStr.isEmpty() && !maxStr.isEmpty()) {
                                condition.setMinPrice(Integer.parseInt(minStr.replaceAll("[^0-9]", "")));
                                condition.setMaxPrice(Integer.parseInt(maxStr.replaceAll("[^0-9]", "")));
                            }
                        }
                    }
                }
            }

            // 면적 추출
            if (query.contains("평")) {
                String[] parts = query.split("평");
                for (String part : parts) {
                    if (part.matches(".*\\d+.*")) {
                        String numStr = part.replaceAll("[^0-9.]", "");
                        if (!numStr.isEmpty()) {
                            try {
                                double areaInPyeong = Double.parseDouble(numStr);
                                double areaInSqm = areaInPyeong * 3.30578;
                                condition.setMinArea(areaInSqm);
                                condition.setMaxArea(areaInSqm);
                            } catch (NumberFormatException nfe) {
                                log.warn("면적 숫자 변환 실패: {}", numStr);
                            }
                        }
                    }
                }
            }

            // 주소 키워드 추출
            String[] regions = { "구", "동", "읍", "면", "시" };
            for (String region : regions) {
                if (query.contains(region)) {
                    String[] parts = query.split(region);
                    if (parts.length > 0) {
                        String keywordCandidate = parts[0].trim();
                        String[] keywordParts = keywordCandidate.split("\\s+");
                        if (keywordParts.length > 0) {
                            condition.setAddressKeyword(keywordParts[keywordParts.length - 1] + region);
                        } else {
                            condition.setAddressKeyword(keywordCandidate + region);
                        }
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("기본값(수동 파싱) 설정 중 오류 발생: {}", ex.getMessage());
        }

        log.info(
                "수동 파싱된 검색 조건: minPrice={}, maxPrice={}, minArea={}, maxArea={}, addressKeyword={}, commercialPreference={}",
                condition.getMinPrice(), condition.getMaxPrice(),
                condition.getMinArea(), condition.getMaxArea(),
                condition.getAddressKeyword(), condition.getCommercialPreference());
        return condition;
    }
}
