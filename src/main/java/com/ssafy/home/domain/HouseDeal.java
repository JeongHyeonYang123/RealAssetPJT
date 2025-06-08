package com.ssafy.home.domain;

import lombok.*;
import org.springframework.stereotype.Component;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class HouseDeal {
    private int no;
    private String aptSeq;
    private String aptDong;
    private String aptNm;
    private String floor;
    private int dealYear;
    private int dealMonth;
    private int dealDay;
    private double excluUseAr;
    private String dealAmount;
}
