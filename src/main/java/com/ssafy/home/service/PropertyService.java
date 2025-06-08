package com.ssafy.home.service;

import com.ssafy.home.domain.HouseDeal;
import com.ssafy.home.domain.HouseInfo;
import com.ssafy.home.dto.PropertySearchRequest;
import com.ssafy.home.dto.PropertySearchResponse;
import com.ssafy.home.mapper.HouseDealsMapper;
import com.ssafy.home.mapper.HouseInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final HouseInfoMapper houseInfoMapper;
    private final HouseDealsMapper houseDealMapper;

    public PropertySearchResponse searchProperties(PropertySearchRequest request) {
        // 1. 아파트명으로 apt_seq와 실제 아파트명만 조회
        List<HouseInfo> houseInfos = houseInfoMapper.findHouseInfosByName(request.getDongCode(), request.getAptName());

        if (houseInfos.isEmpty()) {
            return new PropertySearchResponse(new ArrayList<>(), 0, 0, request.getPage());
        }

        // 2. apt_seq 목록 추출
        List<String> aptSeqs = houseInfos.stream()
                .map(HouseInfo::getAptSeq)
                .collect(Collectors.toList());

        // 3. apt_seq별 실제 아파트명 매핑만 생성
        Map<String, String> aptNameMap = houseInfos.stream()
                .collect(Collectors.toMap(HouseInfo::getAptSeq, HouseInfo::getAptNm));

        // 4. 거래내역 조회 조건 설정
        Map<String, Object> params = buildSearchParams(request, aptSeqs);

        // 5. 전체 개수 조회
        Integer totalCount = houseDealMapper.countDeals(params);

        // 6. 페이징 처리된 거래내역 조회
        Integer offset = (request.getPage() - 1) * request.getSize();
        params.put("offset", offset);
        params.put("limit", request.getSize());

        List<HouseDeal> deals = houseDealMapper.findDealsWithFilter(params);

        // 7. 거래내역에 실제 아파트명만 설정 (기존 구조 유지)
        deals.forEach(deal -> {
            String realAptName = aptNameMap.get(deal.getAptSeq());
            deal.setAptNm(realAptName != null ? realAptName : request.getAptName());
        });

        Integer totalPages = (int) Math.ceil((double) totalCount / request.getSize());

        return new PropertySearchResponse(deals, totalCount, totalPages, request.getPage());
    }

    // 빠진 메서드 추가!
    private Map<String, Object> buildSearchParams(PropertySearchRequest request, List<String> aptSeqs) {
        Map<String, Object> params = new HashMap<>();
        params.put("aptSeqs", aptSeqs);

        // 가격 필터 (만원 단위)
        switch (request.getPriceFilter()) {
            case "under1":
                params.put("maxPrice", 10000); // 1억 = 10,000만원
                break;
            case "1to3":
                params.put("minPrice", 10000);
                params.put("maxPrice", 30000);
                break;
            case "3to5":
                params.put("minPrice", 30000);
                params.put("maxPrice", 50000);
                break;
            case "5to10":
                params.put("minPrice", 50000);
                params.put("maxPrice", 100000);
                break;
            case "over10":
                params.put("minPrice", 100000);
                break;
        }

        // 면적 필터
        switch (request.getAreaFilter()) {
            case "small":
                params.put("maxArea", 60.0);
                break;
            case "medium":
                params.put("minArea", 60.0);
                params.put("maxArea", 85.0);
                break;
            case "large":
                params.put("minArea", 85.0);
                break;
        }

        // 정렬
        params.put("sortOrder", request.getSortOrder());

        return params;
    }
}

