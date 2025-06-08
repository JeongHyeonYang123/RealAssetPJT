package com.ssafy.home.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface StatisticsMapper {

    @Select("SELECT CONCAT(dealYear, '-', LPAD(dealMonth, 2, '0')) as yearMonth, " +
            "AVG(CAST(dealAmount AS UNSIGNED)) as avgPrice, " +
            "COUNT(*) as dealCount " +
            "FROM housedeals " +
            "WHERE aptSeq = #{aptSeq} " +
            "AND ((dealYear > #{startYear}) OR " +
            "     (dealYear = #{startYear} AND dealMonth >= #{startMonth})) " +
            "AND ((dealYear < #{endYear}) OR " +
            "     (dealYear = #{endYear} AND dealMonth <= #{endMonth})) " +
            "GROUP BY yearMonth " +
            "ORDER BY yearMonth")
    List<Map<String, Object>> findMonthlyAveragePrices(
            @Param("aptSeq") String aptSeq,
            @Param("startYear") int startYear,
            @Param("startMonth") int startMonth,
            @Param("endYear") int endYear,
            @Param("endMonth") int endMonth);

    @Select("SELECT " +
            "AVG(CAST(dealAmount AS UNSIGNED)) as avgPrice, " +
            "MAX(CAST(dealAmount AS UNSIGNED)) as maxPrice, " +
            "MIN(CAST(dealAmount AS UNSIGNED)) as minPrice, " +
            "COUNT(*) as totalDeals, " +
            "STD(CAST(dealAmount AS UNSIGNED)) as priceStd " +
            "FROM housedeals hd " +
            "JOIN dongcodes d ON hd.dongCode = d.dongCode " +
            "WHERE d.dongCode LIKE CONCAT(#{areaCode}, '%') " +
            "AND ((hd.dealYear > #{startYear}) OR " +
            "     (hd.dealYear = #{startYear} AND hd.dealMonth >= #{startMonth})) " +
            "AND ((hd.dealYear < #{endYear}) OR " +
            "     (hd.dealYear = #{endYear} AND hd.dealMonth <= #{endMonth}))")
    Map<String, Object> findAreaStatistics(
            @Param("areaCode") String areaCode,
            @Param("startYear") int startYear,
            @Param("startMonth") int startMonth,
            @Param("endYear") int endYear,
            @Param("endMonth") int endMonth);

    @Select("SELECT CONCAT(dealYear, '-', LPAD(dealMonth, 2, '0')) as yearMonth, " +
            "COUNT(*) as dealCount " +
            "FROM housedeals hd " +
            "JOIN dongcodes d ON hd.dongCode = d.dongCode " +
            "WHERE d.dongCode LIKE CONCAT(#{areaCode}, '%') " +
            "AND ((hd.dealYear > #{startYear}) OR " +
            "     (hd.dealYear = #{startYear} AND hd.dealMonth >= #{startMonth})) " +
            "AND ((hd.dealYear < #{endYear}) OR " +
            "     (hd.dealYear = #{endYear} AND hd.dealMonth <= #{endMonth})) " +
            "GROUP BY yearMonth " +
            "ORDER BY yearMonth")
    List<Map<String, Object>> findMonthlyTransactionVolume(
            @Param("areaCode") String areaCode,
            @Param("startYear") int startYear,
            @Param("startMonth") int startMonth,
            @Param("endYear") int endYear,
            @Param("endMonth") int endMonth);

    @Select("SELECT " +
            "FLOOR(CAST(area AS DECIMAL(10,2)) / 3.3) as sizeRange, " +
            "AVG(CAST(dealAmount AS UNSIGNED)) as avgPrice, " +
            "COUNT(*) as dealCount " +
            "FROM housedeals hd " +
            "JOIN dongcodes d ON hd.dongCode = d.dongCode " +
            "WHERE d.dongCode LIKE CONCAT(#{areaCode}, '%') " +
            "AND FLOOR(CAST(area AS DECIMAL(10,2)) / 3.3) BETWEEN #{minSize} AND #{maxSize} " +
            "GROUP BY sizeRange " +
            "ORDER BY sizeRange")
    List<Map<String, Object>> findSizeStatisticsByRange(
            @Param("areaCode") String areaCode,
            @Param("minSize") int minSize,
            @Param("maxSize") int maxSize);

    @Select("SELECT " +
            "FLOOR(CAST(area AS DECIMAL(10,2)) / 3.3) as sizeRange, " +
            "AVG(CAST(dealAmount AS UNSIGNED)) as avgPrice, " +
            "COUNT(*) as dealCount " +
            "FROM housedeals hd " +
            "JOIN dongcodes d ON hd.dongCode = d.dongCode " +
            "WHERE d.dongCode LIKE CONCAT(#{areaCode}, '%') " +
            "GROUP BY sizeRange " +
            "ORDER BY sizeRange")
    List<Map<String, Object>> findAllSizeStatistics(@Param("areaCode") String areaCode);
}

