package com.ssafy.home.service;

import com.ssafy.home.domain.HouseInfo;
import com.ssafy.home.domain.HouseDeal;
import com.ssafy.home.domain.CommercialArea;
import com.ssafy.home.domain.DongCode;
import com.ssafy.home.ai.GPT4MiniClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApartmentInfoChatbotService {
    private final HouseService houseService;
    private final GPT4MiniClient gptClient;
    private final ObjectMapper objectMapper;
    private final KakaoMapService kakaoMapService;
    private final CommercialAreaScorer commercialAreaScorer;
    private final CommunityService communityService; // 🔥 PostService 기반 커뮤니티 서비스

    public String answerApartmentInfo(String userQuery) {
        // 1. LLM으로부터 아파트명과 지역 정보 함께 추출
        SearchInfo searchInfo = extractSearchInfoFromQuery(userQuery);
        if (searchInfo.aptName == null || searchInfo.aptName.isEmpty()) {
            return "아파트명을 입력하거나 명확히 말씀해 주세요.";
        }

        // 2. 지역 정보로 동코드 조회
        String dongCode = null;
        if (searchInfo.region != null && !searchInfo.region.isEmpty()) {
            DongCode dong = houseService.getDongCodeByName(searchInfo.region);
            if (dong != null) {
                dongCode = dong.getCode();
            }
        }

        // 3. 아파트 정보 조회 (지역 정보 포함)
        List<HouseInfo> infos = houseService.getHouseInfosByNameAndRegion(searchInfo.aptName, dongCode);
        if (infos == null || infos.isEmpty()) {
            return String.format("'%s' 아파트 정보를 찾을 수 없습니다. 아파트명과 지역을 정확히 입력해 주세요.", searchInfo.aptName);
        }

        if (infos.size() > 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("여러 아파트가 검색되었습니다. 더 구체적으로 입력해 주세요.\n");
            for (HouseInfo info : infos) {
                sb.append("- ").append(info.getAptNm()).append(" (주소: ")
                        .append(info.getSggCd()).append(" ")
                        .append(info.getUmdNm()).append(" ")
                        .append(info.getJibun()).append(")\n");
            }
            return sb.toString();
        }

        HouseInfo info = infos.get(0);

        // 4. 최신 거래 정보 조회
        List<HouseDeal> deals = houseService.getApartmentDeals(info.getAptSeq());
        HouseDeal latestDeal = (deals != null && !deals.isEmpty()) ? deals.get(0) : null;

        // 5. 상권 정보 조회
        String commercialInfo = "";
        try {
            double lat = Double.parseDouble(info.getLatitude());
            double lng = Double.parseDouble(info.getLongitude());
            List<CommercialArea> commercialAreas = kakaoMapService.getNearbyCommercialAreas(lat, lng);
            double commercialScore = commercialAreaScorer.score(commercialAreas);
            commercialInfo = generateBriefCommercialInfo(commercialAreas, commercialScore);
        } catch (Exception e) {
            commercialInfo = "상권 정보: 조회 불가";
        }

        // 🔥 6. 커뮤니티 정보 조회 (PostService 기반)
        String communityInfo = "";
        try {
            // 아파트 시퀀스와 아파트명 둘 다 시도
            communityInfo = communityService.getApartmentCommunityInfo(info.getAptSeq());

            // 첫 번째 시도에서 결과가 없으면 아파트명으로 재시도
            if (communityInfo.contains("등록된 후기가 없습니다")) {
                communityInfo = communityService.getApartmentCommunityInfoByName(info.getAptNm());
            }
        } catch (Exception e) {
            communityInfo = "커뮤니티 정보: 조회 불가";
        }

        // 7. 프롬프트 빌드 (커뮤니티 정보 포함)
        String address = String.format("%s %s %s", info.getSggCd(), info.getUmdNm(), info.getJibun());
        String latestDealAmount = (latestDeal != null) ? latestDeal.getDealAmount() : null;
        Double latestExcluUseAr = (latestDeal != null) ? latestDeal.getExcluUseAr() : null;
        Integer latestDealYear = (latestDeal != null) ? latestDeal.getDealYear() : null;
        Integer latestDealMonth = (latestDeal != null) ? latestDeal.getDealMonth() : null;
        Integer latestDealDay = (latestDeal != null) ? latestDeal.getDealDay() : null;

        String prompt = GPT4PromptBuilder.buildApartmentInfoWithCommunityPrompt(
                userQuery, info.getAptNm(), address, info.getBuildYear(),
                latestDealAmount, latestExcluUseAr, latestDealYear, latestDealMonth, latestDealDay,
                commercialInfo, communityInfo);

        return gptClient.askLLM(prompt);
    }

    // 기존 메서드들...
    private String generateBriefCommercialInfo(List<CommercialArea> areas, double score) {
        if (areas == null || areas.isEmpty()) {
            return "상권 정보: 주변 상권 시설 없음";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("상권 점수: ").append(String.format("%.1f점", score)).append("\n");
        sb.append("주변 주요 시설: ");

        int count = Math.min(3, areas.size());
        for (int i = 0; i < count; i++) {
            CommercialArea area = areas.get(i);
            sb.append(area.getName()).append("(").append(area.getType()).append(")");
            if (i < count - 1) sb.append(", ");
        }

        if (areas.size() > 3) {
            sb.append(" 외 ").append(areas.size() - 3).append("개");
        }

        return sb.toString();
    }

    private SearchInfo extractSearchInfoFromQuery(String query) {
        String prompt = String.format("""
            다음 문장에서 아파트명과 지역 정보를 추출해서 JSON으로 응답해줘.
            
            추출 규칙:
            1. 아파트명: 브랜드명(래미안, 힐스테이트, 자이 등) + 단지명
            2. 지역: 시/구/군/동 등의 행정구역명
            3. 띄어쓰기는 제거하고 한글만 추출
            
            예시: {"aptName":"래미안블레스티지", "region":"분당구"}
            
            문장: %s
            """, query);

        String response = gptClient.askLLM(prompt);
        try {
            JsonNode node = objectMapper.readTree(response);
            String aptName = node.path("aptName").asText("");
            String region = node.path("region").asText("");

            aptName = aptName.replaceAll("\\s+", "");

            return new SearchInfo(aptName, region);
        } catch (Exception e) {
            // fallback 로직
            String[] tokens = query.split("\\s+");
            String aptName = "";
            String region = "";

            for (String token : tokens) {
                if (token.matches(".*[시구군동]$")) {
                    region = token;
                } else if (token.matches("[가-힣A-Za-z0-9]+") && token.length() > aptName.length()) {
                    aptName = token;
                }
            }

            return new SearchInfo(aptName, region);
        }
    }

    private static class SearchInfo {
        String aptName;
        String region;

        SearchInfo(String aptName, String region) {
            this.aptName = aptName;
            this.region = region;
        }
    }
}
