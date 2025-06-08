package com.ssafy.home.price;

import java.util.List;

/**
 * CoinGecko API 응답을 매핑하기 위한 DTO
 * prices: [[timestamp(ms), price], ...]
 */
public class MarketChartDto {
    private List<List<Double>> prices;

    public List<List<Double>> getPrices() {
        return prices;
    }

    public void setPrices(List<List<Double>> prices) {
        this.prices = prices;
    }
}