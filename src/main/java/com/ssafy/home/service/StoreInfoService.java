package com.ssafy.home.service;

import com.ssafy.home.domain.StoreInfo;
import com.ssafy.home.mapper.StoreInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreInfoService {
    private final StoreInfoMapper storeInfoMapper;

    public List<StoreInfo> getStoreInfos(String dongCode) {
        return storeInfoMapper.selectByDongCode(dongCode);
    }
}
