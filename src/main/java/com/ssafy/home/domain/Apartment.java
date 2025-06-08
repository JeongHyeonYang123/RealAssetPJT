package com.ssafy.home.domain;

import lombok.Data;

@Data
public class Apartment {
    private String name;
    private int price;
    private double area;
    private String address;
    private double lat;
    private double lng;
    private double commercialScore;
}