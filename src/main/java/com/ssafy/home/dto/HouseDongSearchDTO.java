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
public class HouseDongSearchDTO {
    private String umdName;
    private String buildYear;
    private String area;
    private String floor;
    private String dealAmount;
    private String roadNm;
    private String roadNmBonBun;
    private String roadNmBuBun;
    private String roadAddress;
    private String aptNm;
    private int id;
    private double distance = 0;
}
