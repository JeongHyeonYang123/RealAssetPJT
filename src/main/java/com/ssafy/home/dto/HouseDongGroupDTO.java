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
public class HouseDongGroupDTO {
    private String umdName;
    private String buildYear;
    private String minArea;
    private String maxArea;
    private String minDealAmount;
    private String maxDealAmount;
    private String roadNm;
    private String roadNmBonBun;
    private String roadNmBuBun;
    private String aptNm;
    private String address;
}
