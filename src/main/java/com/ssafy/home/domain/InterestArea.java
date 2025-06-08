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
public class InterestArea {
    private int id;
    private int mno;
    private String dongCode;
    private LocalDateTime createdAt;
}
