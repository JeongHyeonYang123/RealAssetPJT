package com.ssafy.home.service;

import com.ssafy.home.domain.DongCode;
import com.ssafy.home.dto.HouseDongSearchDTO;
import com.ssafy.home.dto.PageDTO;
import com.ssafy.home.mapper.DongMapper;
import com.ssafy.home.mapper.HouseInfoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BasicServiceTest {
    @Autowired
    private BasicService basicService;

    @Autowired
    private HouseInfoMapper houseInfoMapper;
    @Autowired
    private DongMapper dongMapper;

    @Test
    void findHousesByDongCode() {
        // given
        String dongCode = "1111010100";

        // when
        List<HouseDongSearchDTO> result = basicService.findHousesByDongCode(dongCode);

        // then
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(h -> "청운동".equals(h.getUmdName())));
    }

    @Test
    void findDongByCode() {
        // given
        String dongCode = "1111010100";

        // when
        DongCode result = basicService.findDongByCode(dongCode);

        // then
        assertNotNull(result);
        assertEquals("1111010100", result.getCode());
        assertEquals("서울특별시", result.getSido());
        assertEquals("종로구", result.getGugun());
        assertEquals("청운동", result.getDong());
    }

    @Test
    void findPagedHousesByDongCode() {
        // given
        String dongCode = "1168010600";

        int page = 2;
        int pageSize = 5;

        // when
        PageDTO<HouseDongSearchDTO> pageResult = basicService.findPagedHousesByDongCode(dongCode, page, pageSize);

        // then
        assertEquals(page, pageResult.getCurrentPage());
        assertEquals(pageSize, pageResult.getPageSize());
    }

    @Test
    void findDongsByCodes() {
        // given
        List<String> codes = List.of("1111010100", "1111010200");

        // when
        List<DongCode> result = basicService.findDongsByCodes(codes);

        // then
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(d -> "청운동".equals(d.getDong())));
        assertTrue(result.stream().anyMatch(d -> "신교동".equals(d.getDong())));
    }
}
