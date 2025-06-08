package com.ssafy.home.price;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 스케줄러로 누락분 자동 업데이트
 */
@Component
public class PriceScheduler {
    private final PriceService priceService;

    public PriceScheduler(PriceService priceService) {
        this.priceService = priceService;
    }

    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    public void fetchMissing() {
        priceService.updateMissingPrices();
    }
}