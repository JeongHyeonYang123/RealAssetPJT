package com.ssafy.home.price;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 애플리케이션 시작 시 최근 30일치만 백필하는 Runner
 */
@Component
public class PriceBackfillRunner implements ApplicationRunner {
    private final CoinGeckoService cg;
    private final PriceMapper mapper;

    public PriceBackfillRunner(CoinGeckoService cg, PriceMapper mapper) {
        this.cg     = cg;
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 과거 30일치만 백필
        Map<LocalDate, BigDecimal> recentPrices =
                cg.fetchDailyClosingPrices(30);

        // DTO 리스트로 변환
        List<PriceDTO> list = recentPrices.entrySet().stream()
                .map(e -> new PriceDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // 한 번에 insert (MyBatis 매퍼에 insertAll 메서드 구현 필요)
        mapper.insertAll(list);
    }
}
