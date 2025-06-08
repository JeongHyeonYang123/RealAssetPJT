package com.ssafy.home.dto;

import com.ssafy.home.domain.HouseDeal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PropertySearchResponse {
    private List<HouseDeal> properties;
    private Integer totalCount;
    private Integer totalPages;
    private Integer currentPage;
}