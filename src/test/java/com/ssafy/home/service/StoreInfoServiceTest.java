package com.ssafy.home.service;

import com.ssafy.home.domain.StoreInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class StoreInfoServiceTest {
    @Autowired
    private StoreInfoService storeInfoService;

    @Test
    void getStoreInfos() {
        // given
        String dongCode = "1117013000";

        // when
        List<StoreInfo> storeInfos = storeInfoService.getStoreInfos(dongCode);

        // then
        assertThat(storeInfos.size()).isGreaterThan(0);
    }
}