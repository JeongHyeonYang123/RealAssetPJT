package com.ssafy.home.service;

import com.ssafy.home.ai.GPT4MiniClient;
import com.ssafy.home.domain.Apartment;
import com.ssafy.home.domain.CommercialArea;
import com.ssafy.home.dto.ApartmentWithLatestDeal;
import com.ssafy.home.domain.HouseInfo;
import com.ssafy.home.mapper.HouseMapper;
import com.ssafy.home.domain.DongCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApartmentRecommendService {
    private final HouseMapper houseMapper;
    private final KakaoMapService kakaoMapService;
    private final GPT4MiniClient gptClient;
    private final CommercialAreaScorer commercialAreaScorer;

    public String recommendApartment(String userQuery) {
        try {
            log.info("아파트 추천 시작 - 사용자 쿼리: {}", userQuery);

            // 1. 사용자 쿼리에서 검색 조건 추출
            ApartmentSearchCondition condition = ApartmentSearchCondition.fromUserQuery(userQuery, gptClient);
            log.info("검색 조건 추출 완료: {}", condition);

            // 2. 조건에 맞는 아파트+최신 거래 정보 검색
            List<ApartmentWithLatestDeal> aptDeals = searchApartments(condition);
            log.info("검색된 아파트 수: {}", aptDeals.size());

            if (aptDeals.isEmpty()) {
                return "죄송합니다. 조건에 맞는 아파트를 찾을 수 없습니다. 다른 조건으로 다시 시도해주세요.";
            }

            // 3. ApartmentWithLatestDeal을 Apartment로 변환
            List<Apartment> candidates = convertToApartments(aptDeals);
            log.info("변환된 아파트 수: {}", candidates.size());

            // 4. 상권 가중치 추출
            Map<String, Double> commercialWeights = commercialAreaScorer.extractWeightsFromQuery(userQuery);
            log.info("상권 가중치 추출 완료: {}", commercialWeights);

            // 5. 각 아파트의 상권 정보 조회 및 점수 계산
            for (Apartment apt : candidates) {
                try {
                    List<CommercialArea> commercialAreas = kakaoMapService.getNearbyCommercialAreas(
                            apt.getLat(), apt.getLng());
                    apt.setCommercialScore(commercialAreaScorer.scoreWithWeights(commercialAreas, commercialWeights));
                } catch (Exception e) {
                    log.warn("상권 정보 조회 실패 - 아파트: {}, 에러: {}", apt.getName(), e.getMessage());
                    apt.setCommercialScore(0.0);
                }
            }

            // 6. 상권 점수 기준으로 정렬
            candidates.sort((a, b) -> Double.compare(b.getCommercialScore(), a.getCommercialScore()));

            // 7. GPT를 통한 최종 추천
            String prompt = GPT4PromptBuilder.buildRecommendPrompt(userQuery, candidates);
            String recommendation = gptClient.askLLM(prompt);
            log.info("GPT 추천 완료");

            return recommendation;

        } catch (Exception e) {
            log.error("아파트 추천 중 오류 발생", e);
            return "죄송합니다. 아파트 추천 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }
    }

    private List<ApartmentWithLatestDeal> searchApartments(ApartmentSearchCondition condition) {
        try {
            List<ApartmentWithLatestDeal> all = houseMapper.findApartmentsWithLatestDeal(
                    condition.getAddressKeyword(),
                    condition.getMinPrice(),
                    condition.getMaxPrice(),
                    condition.getMinArea(),
                    condition.getMaxArea());

            return all.stream()
                    .filter(a -> {
                        try {
                            boolean ok = true;
                            if (condition.getMinPrice() != null && a.getLatestDealAmount() != null) {
                                int price = Integer.parseInt(a.getLatestDealAmount().replace(",", ""));
                                ok &= price >= condition.getMinPrice();
                            }
                            if (condition.getMaxPrice() != null && a.getLatestDealAmount() != null) {
                                int price = Integer.parseInt(a.getLatestDealAmount().replace(",", ""));
                                ok &= price <= condition.getMaxPrice();
                            }
                            if (condition.getMinArea() != null && a.getLatestExcluUseAr() != null) {
                                ok &= a.getLatestExcluUseAr() >= condition.getMinArea();
                            }
                            if (condition.getMaxArea() != null && a.getLatestExcluUseAr() != null) {
                                ok &= a.getLatestExcluUseAr() <= condition.getMaxArea();
                            }
                            return ok;
                        } catch (Exception e) {
                            log.warn("아파트 필터링 중 오류 발생 - 아파트: {}, 에러: {}", a.getAptNm(), e.getMessage());
                            return false;
                        }
                    })
                    .limit(20)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("아파트 검색 중 오류 발생", e);
            return new ArrayList<>();
        }
    }

    private List<Apartment> convertToApartments(List<ApartmentWithLatestDeal> aptDeals) {
        return aptDeals.stream().map(info -> {
            try {
                Apartment apt = new Apartment();
                apt.setName(info.getAptNm());
                apt.setAddress(String.format("%s %s %s",
                        info.getSggCd(), info.getUmdNm(), info.getJibun()));

                try {
                    apt.setLat(Double.parseDouble(info.getLatitude()));
                    apt.setLng(Double.parseDouble(info.getLongitude()));
                } catch (Exception e) {
                    log.warn("위도/경도 변환 실패 - 아파트: {}, 에러: {}", info.getAptNm(), e.getMessage());
                    apt.setLat(0);
                    apt.setLng(0);
                }

                try {
                    apt.setPrice(info.getLatestDealAmount() != null
                            ? Integer.parseInt(info.getLatestDealAmount().replace(",", ""))
                            : 0);
                } catch (Exception e) {
                    log.warn("가격 변환 실패 - 아파트: {}, 에러: {}", info.getAptNm(), e.getMessage());
                    apt.setPrice(0);
                }

                apt.setArea(info.getLatestExcluUseAr() != null ? info.getLatestExcluUseAr() : 0.0);
                return apt;
            } catch (Exception e) {
                log.error("아파트 변환 중 오류 발생 - 아파트 정보: {}, 에러: {}", info, e.getMessage());
                return null;
            }
        })
                .filter(apt -> apt != null)
                .collect(Collectors.toList());
    }
}
