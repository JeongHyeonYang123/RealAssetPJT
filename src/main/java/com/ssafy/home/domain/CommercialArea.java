package com.ssafy.home.domain;

import lombok.Data;

@Data
public class CommercialArea {
    private String name;
    private String type; // 음식점, 카페 등
    private double lat;
    private double lng;
}