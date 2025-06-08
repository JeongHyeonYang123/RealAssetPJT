package com.ssafy.home.service;

import com.ssafy.home.domain.HouseDeal;
import com.ssafy.home.mapper.DataUpdateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@EnableCaching
@RequiredArgsConstructor
public class DataUpdateService {
    private final DataUpdateMapper dataUpdateMapper;
    private final CacheManager cacheManager;

    // 특정 지역+월의 실거래 데이터 조회
    public List<HouseDeal> getDealsByRegionAndMonth(String lawdCd, String dealYmd) {
        return dataUpdateMapper.selectDealsByRegionAndMonth(lawdCd, dealYmd);
    }

    // 전체 지역 특정 월 실거래 데이터 조회
    public List<HouseDeal> getAllDealsByMonth(String dealYmd) {
        return dataUpdateMapper.selectAllDealsByMonth(dealYmd);
    }

    // 최근 3개월 실거래 데이터 조회
    public List<HouseDeal> getRecentDeals(int limit) {
        return dataUpdateMapper.selectRecentDeals(limit);
    }

    // 캐시 초기화
    public void clearCache() {
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
