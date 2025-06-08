package com.ssafy.home.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DongCode {
    private String code;
    private String sido;
    private String gugun;
    private String dong;
    private Double lat;
    private Double lng;
    private Long avgPrice;
    private Integer aptCount;
    private LocalDateTime updatedAt;
}
