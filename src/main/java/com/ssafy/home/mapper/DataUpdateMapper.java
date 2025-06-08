package com.ssafy.home.mapper;

import com.ssafy.home.domain.HouseDeal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DataUpdateMapper {
    // 법정동코드 + 거래연월로 조회
    @Select("SELECT * FROM housedeals " +
            "WHERE dongCode LIKE CONCAT(#{lawdCd}, '%') " +
            "AND CONCAT(dealYear, LPAD(dealMonth, 2, '0')) = #{dealYmd}")
    List<HouseDeal> selectDealsByRegionAndMonth(
            @Param("lawdCd") String lawdCd,
            @Param("dealYmd") String dealYmd);

    // 전체 지역 특정 월 조회
    @Select("SELECT * FROM housedeals " +
            "WHERE CONCAT(dealYear, LPAD(dealMonth, 2, '0')) = #{dealYmd}")
    List<HouseDeal> selectAllDealsByMonth(@Param("dealYmd") String dealYmd);

    // 최근 N건 조회
    @Select("SELECT * FROM housedeals ORDER BY dealYear DESC, dealMonth DESC, dealDay DESC LIMIT #{limit}")
    List<HouseDeal> selectRecentDeals(@Param("limit") int limit);
}
