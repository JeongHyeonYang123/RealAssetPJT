package com.ssafy.home.price;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class PriceService {

    private static final int RECENT_DAYS = 50;

    private final PriceMapper priceMapper;
    private final CoinGeckoService coinGeckoService;

    public PriceService(PriceMapper priceMapper,
                        CoinGeckoService coinGeckoService) {
        this.priceMapper = priceMapper;
        this.coinGeckoService = coinGeckoService;
    }

    /**
     * 1) 호출 시점 기준 최근 50일치 가격을 API에서 가져와 DB에 누락분을 채우고,
     * 2) 실제 요청된 start~end 구간만 DB에서 읽어 반환
     */
    @Cacheable(value = "bitcoinPrices", key = "#start.toString() + '-' + #end.toString()")
    public List<PriceDTO> getPrices(LocalDate start, LocalDate end) {
        // DB에서만 데이터를 조회
        return priceMapper.findByDateBetween(start, end);
    }

    /**
     * 스케줄러에서 호출: 마지막 저장일 이후로 누락된 날짜를 모두 채워넣는다.
     */
    @Transactional
    public void updateMissingPrices() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate lastDate = priceMapper.findMaxDate();

        // 시작일 계산: DB 비어있으면 오늘-49, 있으면 마지막 저장일 다음날
        LocalDate from = (lastDate == null)
                ? today.minusDays(RECENT_DAYS - 1)
                : lastDate.plusDays(1);

        if (from.isAfter(today)) {
            // 이미 최신까지 채워져 있으면 할 것 없음
            return;
        }

        long daysBetween = ChronoUnit.DAYS.between(from, today) + 1;
        Map<LocalDate, BigDecimal> missing =
                // 범위 기반 API 호출 (from ~ today)
                coinGeckoService.fetchDailyClosingPrices(from, today);

        missing.forEach((date, price) -> {
            if (priceMapper.countByDate(date) == 0) {
                priceMapper.insert(date, price);
            }
        });
    }
}
