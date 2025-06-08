package com.ssafy.home.price;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface PriceMapper {
    @Select("SELECT COUNT(*) FROM daily_price WHERE `date` = #{date}")
    int countByDate(@Param("date") LocalDate date);

    @Insert("INSERT INTO daily_price (`date`, closing_price) VALUES (#{date}, #{price})")
    void insert(@Param("date") LocalDate date,
                @Param("price") BigDecimal price);

    @Insert({
            "<script>",
            "INSERT INTO daily_price (`date`, closing_price) VALUES",
            "<foreach collection='list' item='item' separator=','>",
            "  (#{item.date}, #{item.closingPrice})",
            "</foreach>",
            "ON DUPLICATE KEY UPDATE closing_price = VALUES(closing_price)",
            "</script>"
    })
    void insertAll(@Param("list") List<PriceDTO> list);

    @Select("SELECT `date` AS date, closing_price AS closingPrice " +
            "FROM daily_price " +
            "WHERE `date` BETWEEN #{start} AND #{end} " +
            "ORDER BY `date`")
    List<PriceDTO> findByDateBetween(@Param("start") LocalDate start,
                                     @Param("end")   LocalDate end);

    @Select("SELECT MAX(`date`) FROM daily_price")
    LocalDate findMaxDate();
}

