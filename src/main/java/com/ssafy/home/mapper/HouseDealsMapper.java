package com.ssafy.home.mapper;

import com.ssafy.home.domain.HouseDeal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface HouseDealsMapper {
    public void insertHouseDeal(HouseDeal housedeal);

    public List<HouseDeal> getAllHouseDeals();

    public HouseDeal getHouseDealByCode(int code);

    public HouseDeal updateHouseDeal(HouseDeal houseDeal);

    public void deleteHouseDealByCode(int code);

    public List<HouseDeal> getHouseDealsByConditions(Map<String, String> conditions);

    public int deleteAll();

    List<HouseDeal> findDealsWithFilter(@Param("params") Map<String, Object> params);

    Integer countDeals(@Param("params") Map<String, Object> params);
}
