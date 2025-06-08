package com.ssafy.home.service;

import com.ssafy.home.mapper.StatisticsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {

    private final StatisticsMapper statisticsMapper;

    @Autowired
    public StatisticsService(StatisticsMapper statisticsMapper) {
        this.statisticsMapper = statisticsMapper;
    }

    // 아파트 가격 추세 분석
    public Map<String, Object> getApartmentPriceTrend(String aptSeq, String period) {
        // 기간에 따른 시작일 계산
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        switch (period) {
            case "1y":
                startDate = endDate.minusYears(1);
                break;
            case "3y":
                startDate = endDate.minusYears(3);
                break;
            case "5y":
                startDate = endDate.minusYears(5);
                break;
            default:
                startDate = endDate.minusYears(1);
        }

        // 월별 평균 가격 조회
        List<Map<String, Object>> monthlyPrices = statisticsMapper.findMonthlyAveragePrices(
                aptSeq,
                startDate.getYear(),
                startDate.getMonthValue(),
                endDate.getYear(),
                endDate.getMonthValue()
        );

        // 결과 맵 생성
        Map<String, Object> result = new HashMap<>();
        result.put("aptSeq", aptSeq);
        result.put("period", period);
        result.put("data", monthlyPrices);

        return result;
    }

    // 지역별 평균 가격 통계
    public Map<String, Object> getAreaStatistics(String areaCode, String period) {
        // 기간에 따른 시작일 계산
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        switch (period) {
            case "1y":
                startDate = endDate.minusYears(1);
                break;
            case "3y":
                startDate = endDate.minusYears(3);
                break;
            case "5y":
                startDate = endDate.minusYears(5);
                break;
            default:
                startDate = endDate.minusYears(1);
        }

        // 지역별 통계 조회
        Map<String, Object> areaStats = statisticsMapper.findAreaStatistics(
                areaCode,
                startDate.getYear(),
                startDate.getMonthValue(),
                endDate.getYear(),
                endDate.getMonthValue()
        );

        // 월별 거래량 조회
        List<Map<String, Object>> monthlyVolume = statisticsMapper.findMonthlyTransactionVolume(
                areaCode,
                startDate.getYear(),
                startDate.getMonthValue(),
                endDate.getYear(),
                endDate.getMonthValue()
        );

        // 결과 맵 생성
        Map<String, Object> result = new HashMap<>();
        result.put("areaCode", areaCode);
        result.put("period", period);
        result.put("statistics", areaStats);
        result.put("monthlyVolume", monthlyVolume);

        return result;
    }

    // 평수별 가격 통계
    public Map<String, Object> getSizeStatistics(String areaCode, String sizeRange) {
        List<Map<String, Object>> sizeStats;

        if (sizeRange != null && !sizeRange.isEmpty()) {
            // 특정 평수 범위의 통계 조회
            String[] range = sizeRange.split("-");
            int minSize = Integer.parseInt(range[0]);
            int maxSize = Integer.parseInt(range[1]);
            sizeStats = statisticsMapper.findSizeStatisticsByRange(areaCode, minSize, maxSize);
        } else {
            // 전체 평수 범위의 통계 조회
            sizeStats = statisticsMapper.findAllSizeStatistics(areaCode);
        }

        // 결과 맵 생성
        Map<String, Object> result = new HashMap<>();
        result.put("areaCode", areaCode);
        result.put("sizeRange", sizeRange);
        result.put("statistics", sizeStats);

        return result;
    }
}
