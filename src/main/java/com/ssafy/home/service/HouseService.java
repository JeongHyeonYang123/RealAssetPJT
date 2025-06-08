package com.ssafy.home.service;

import com.ssafy.home.domain.DongCode;
import com.ssafy.home.domain.HouseDeal;
import com.ssafy.home.domain.HouseInfo;
import com.ssafy.home.dto.ApartmentWithLatestDeal;
import com.ssafy.home.mapper.HouseInfoMapper;
import com.ssafy.home.mapper.HouseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class HouseService {

    private final HouseMapper houseMapper;
    private final HouseInfoMapper houseInfoMapper;

    // 지도 영역 내 아파트 + 최신 거래 정보 반환
    public List<ApartmentWithLatestDeal> getApartmentsInBounds(double swLat, double swLng, double neLat, double neLng) {
        List<HouseInfo> apartments = houseMapper.findApartmentsInBounds(swLat, swLng, neLat, neLng);
        List<ApartmentWithLatestDeal> result = new java.util.ArrayList<>();
        for (HouseInfo apt : apartments) {
            List<HouseDeal> deals = houseMapper.findDealsByAptSeq(apt.getAptSeq());
            HouseDeal latestDeal = deals.isEmpty() ? null : deals.get(0);
            ApartmentWithLatestDeal dto = ApartmentWithLatestDeal.from(apt, latestDeal);
            result.add(dto);
        }
        return result;
    }

    public List<ApartmentWithLatestDeal> getApartmentsWithLatestDeal() {
        return houseMapper.findApartmentsWithLatestDeal(
                null, // addressKeyword
                null, // minPrice
                null, // maxPrice
                null, // minArea
                null // maxArea
        );
    }

    // 동별 평균 가격 조회
    public Map<String, Object> getDistrictAveragePrice(double swLat, double swLng, double neLat, double neLng) {
        List<Map<String, Object>> districtPrices = houseMapper.findDistrictAveragePrice(swLat, swLng, neLat, neLng);

        Map<String, Object> result = new HashMap<>();
        result.put("type", "district");
        result.put("data", districtPrices);

        return result;
    }

    // 상세 아파트 정보 조회
    public Map<String, Object> getDetailedApartmentInfo(double swLat, double swLng, double neLat, double neLng) {
        List<Map<String, Object>> apartmentInfos = houseMapper.findDetailedApartmentInfo(swLat, swLng, neLat, neLng);

        Map<String, Object> result = new HashMap<>();
        result.put("type", "apartment");
        result.put("data", apartmentInfos);

        return result;
    }

    // 법정동 코드 조회
    public DongCode getDongCodeByName(String dongName) {
        return houseMapper.findDongCodeByName(dongName);
    }

    /**
     * 위도/경도 기반으로 주변 아파트 검색
     * 
     * @param lat    위도
     * @param lng    경도
     * @param radius 반경(미터)
     * @return 검색된 아파트 정보와 개수
     */
    public Map<String, Object> getNearbyApartments(double lat, double lng, int radius) {
        // 위도/경도 기반으로 주변 아파트 검색
        List<HouseInfo> apartments = houseMapper.findApartmentsByLocation(lat, lng, radius);

        // 결과를 담을 맵 생성
        Map<String, Object> result = new HashMap<>();
        result.put("count", apartments.size());
        result.put("apartments", apartments);

        return result;
    }

    /**
     * 특정 아파트의 거래 내역 조회
     * 
     * @param aptSeq 아파트 일련번호
     * @return 해당 아파트의 거래 내역 목록
     */
    public List<HouseDeal> getApartmentDeals(String aptSeq) {
        return houseMapper.findDealsByAptSeq(aptSeq);
    }

    public Map<String, Object> getLatestDealPricesByRegion(
            double swLat, double swLng, double neLat, double neLng, String regionType) {

        String regionColumn = "sgg_cd";
        if ("dong".equals(regionType))
            regionColumn = "umd_cd";

        List<Map<String, Object>> regionPrices = houseMapper.findLatestDealPriceByRegion(swLat, swLng, neLat, neLng,
                regionColumn);

        // 전체 평균 계산
        double sum = 0;
        int cnt = 0;
        for (Map<String, Object> region : regionPrices) {
            if (region.get("avg_latest_deal_price") != null) {
                sum += ((Number) region.get("avg_latest_deal_price")).doubleValue();
                cnt++;
            }
        }
        double overallAvg = cnt > 0 ? sum / cnt : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("averagePrice", overallAvg);
        result.put("regions", regionPrices);
        return result;
    }

    public List<Map<String, Object>> getDongLatestDealAvgInBounds(
            double swLat, double swLng, double neLat, double neLng) {
        return houseMapper.findDongAvgPriceInBounds(swLat, swLng, neLat, neLng);
    }

    // 시도별 평균가격 조회
    public List<Map<String, Object>> getSidoAvgPriceInBounds(
            double swLat, double swLng, double neLat, double neLng) {
        return houseMapper.findSidoAvgPriceInBounds(swLat, swLng, neLat, neLng);
    }

    // 구군별 평균가격 조회
    public List<Map<String, Object>> getGugunAvgPriceInBounds(
            double swLat, double swLng, double neLat, double neLng) {
        return houseMapper.findGugunAvgPriceInBounds(swLat, swLng, neLat, neLng);
    }

    // 동별 평균가격 조회 (기존 메서드명 변경)
    public List<Map<String, Object>> getDongAvgPriceInBounds(
            double swLat, double swLng, double neLat, double neLng) {
        return houseMapper.findDongAvgPriceInBounds(swLat, swLng, neLat, neLng);
    }

    public Optional<HouseInfo> getHouseInfoByAptSeq(String aptSeq) {
        try {
            log.info("아파트 정보 조회 - aptSeq: {}", aptSeq);
            return houseInfoMapper.selectByAptSeq(aptSeq);
        } catch (Exception e) {
            log.error("아파트 정보 조회 실패 - aptSeq: {}", aptSeq, e);
            return Optional.empty();
        }
    }

    // 동코드별 평균가격 조회
    public Map<String, Object> getAvgPriceByDongCode(String dongCode) {
        log.info("동코드별 평균가격 조회 - dongCode: {}", dongCode);

        Map<String, Object> priceData = houseMapper.findAvgPriceByDongCode(dongCode);

        Map<String, Object> result = new HashMap<>();
        if (priceData != null && !priceData.isEmpty()) {
            result.put("success", true);
            result.put("message", "success");
            result.put("data", priceData);
        } else {
            result.put("success", false);
            result.put("message", "해당 동코드의 데이터를 찾을 수 없습니다.");
        }

        return result;
    }

    public List<HouseInfo> getHouseInfosByName(String aptName) {
        // 동코드 없이 아파트명만으로 검색 (여러 동에 같은 이름이 있을 수 있음)
        return houseInfoMapper.findHouseInfosByName(null, aptName);
    }

    // 🔥 누락된 메서드 추가
    public List<HouseInfo> getHouseInfosByNameAndRegion(String aptName, String dongCode) {
        // 지역 정보가 있으면 동코드 조건 포함, 없으면 아파트명만으로 검색
        return houseInfoMapper.findHouseInfosByNameAndRegion(aptName, dongCode);
    }

    // 평균가격만 간단히 조회
    public Long getSimpleAvgPrice(String dongCode) {
        log.info("간단 평균가격 조회 - dongCode: {}", dongCode);
        return houseMapper.findSimpleAvgPrice(dongCode);
    }

}
