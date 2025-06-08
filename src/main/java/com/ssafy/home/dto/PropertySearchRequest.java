package com.ssafy.home.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PropertySearchRequest {
    private String dongCode;          // 동코드
    private String aptName;           // 아파트명
    private String priceFilter = "all";       // all, under1, 1to3, 3to5, 5to10, over10
    private String areaFilter = "all";        // all, small, medium, large
    private String sortOrder = "date-desc";         // price-asc, price-desc, date-desc
    private Integer page = 1;         // 페이지 번호
    private Integer size = 10;        // 페이지 크기
}
