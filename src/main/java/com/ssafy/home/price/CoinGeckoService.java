package com.ssafy.home.price;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CoinGecko Range API를 사용해 특정 기간의 일별 종가를 가져오는 서비스
 */
@Service
public class CoinGeckoService {
    private final RestTemplate rt = new RestTemplate();

    private static final String RANGE_URL =
            "https://api.coingecko.com/api/v3/coins/bitcoin/market_chart/range";

    /**
     * 지정된 start~end 기간(UTC 기준)의 일별 종가 맵
     */
    public Map<LocalDate, BigDecimal> fetchDailyClosingPrices(
            LocalDate start, LocalDate end) {
        long fromTs = start.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long toTs   = end.atStartOfDay(ZoneOffset.UTC).toEpochSecond();

        String uri = UriComponentsBuilder
                .fromHttpUrl(RANGE_URL)
                .queryParam("vs_currency", "usd")
                .queryParam("from", fromTs)
                .queryParam("to", toTs)
                .toUriString();

        MarketChartDto dto = rt.getForObject(uri, MarketChartDto.class);
        if (dto == null || dto.getPrices() == null) {
            throw new RestClientException("CoinGecko 응답 없음");
        }

        return dto.getPrices().stream()
                .map(arr -> {
                    Instant inst = Instant.ofEpochMilli(arr.get(0).longValue());
                    LocalDate date = inst.atZone(ZoneOffset.UTC).toLocalDate();
                    BigDecimal price = BigDecimal.valueOf(arr.get(1));
                    return Map.entry(date, price);
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, last) -> last,
                        LinkedHashMap::new
                ));
    }

    /**
     * 편의 메서드: today 포함 과거 days일치 데이터를 범위 API로 호출
     */
    public Map<LocalDate, BigDecimal> fetchDailyClosingPrices(long days) {
        LocalDate end   = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = end.minusDays(days - 1);
        return fetchDailyClosingPrices(start, end);
    }
}