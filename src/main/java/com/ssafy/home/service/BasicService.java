package com.ssafy.home.service;

import com.ssafy.home.domain.DongCode;
import com.ssafy.home.dto.HouseDongGroupDTO;
import com.ssafy.home.dto.HouseDongSearchDTO;
import com.ssafy.home.dto.PageDTO;
import com.ssafy.home.mapper.DongMapper;
import com.ssafy.home.mapper.HouseInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BasicService {
    private final HouseInfoMapper houseInfoMapper;
    private final DongMapper dongMapper;

    // 1. 동 코드로 아파트 정보 리스트 조회
    public List<HouseDongSearchDTO> findHousesByDongCode(String dongCode) {
        return houseInfoMapper.getHouseInfosByDongCode(dongCode);
    }

    // 2. 동 코드로 동 주소 정보 조회
    public DongCode findDongByCode(String dongCode) {
        return dongMapper.getDongByCode(dongCode);
    }

    // 3. 동 코드로 아파트 그룹 정보 리스트 조회
    public List<HouseDongGroupDTO> findGroupedHousesByDongCode(String dongCode) {
        return houseInfoMapper.getGroupedHouseInfoByDongCode(dongCode);
    }

    // 4. 동 코드로 아파트 정보 페이지네이션 조회
    public PageDTO<HouseDongSearchDTO> findPagedHousesByDongCode(String dongCode, int page, int pageSize) {
        List<HouseDongSearchDTO> houseList = houseInfoMapper.getHouseInfosByDongCode(dongCode);
        int totalRecords = houseList.size();
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalRecords);
        List<HouseDongSearchDTO> pagedList = houseList.subList(startIndex, endIndex);
        return new PageDTO<>(pagedList, page, totalRecords, pageSize);
    }

    // 5. 여러 동 코드로 동 정보 리스트 조회
    public List<DongCode> findDongsByCodes(List<String> dongCodes) {
        return dongMapper.getDongsByCodes(dongCodes);
    }
}

