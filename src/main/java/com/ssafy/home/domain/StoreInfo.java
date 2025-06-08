package com.ssafy.home.domain;

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
public class StoreInfo {
    String storeId;
    String storeName;
    String branchName;
    String categoryMajorName;
    String categoryMidName;
    String categoryMinorName;
    String city;
    String district;
    String legalDongCode;
    String roadAddress;
    String buildingName;
}
