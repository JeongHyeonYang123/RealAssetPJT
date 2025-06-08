package com.ssafy.home.mapper;

import com.ssafy.home.dto.FavoriteHousedealDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface FavoriteHousedealMapper {
    int insert(FavoriteHousedealDto dto);

    List<FavoriteHousedealDto> selectAll();

    FavoriteHousedealDto selectById(int id);

    int undelete(int userMno, String aptSeq);

    int deleteById(int id);

    int countAll();

    List<FavoriteHousedealDto> selectAllByUserMno(@Param("userMno") int userMno);

    FavoriteHousedealDto selectByIdAndUserMno(@Param("id") int id, @Param("userMno") int userMno);

    int deleteByIdAndUserMno(@Param("id") int id, @Param("userMno") int userMno);

    int countAllByUserMno(@Param("userMno") int userMno);
}