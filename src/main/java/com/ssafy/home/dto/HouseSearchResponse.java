package com.ssafy.home.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HouseSearchResponse {
    private String dong;
    private List<HouseDongSearchDTO> houseList;
    private int currentPage;
    private int totalPages;
    private int totalRecords;
}
