package com.ssafy.home.mapper;

import com.ssafy.home.domain.StoreInfo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StoreInfoMapper {
    public List<StoreInfo> selectByDongCode(String dongCode);

    public int deleteAll();
}
