package com.ssafy.home.mapper;

import com.ssafy.home.domain.InterestArea;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface InterestAreaMapper {
    public int insert(InterestArea interestArea);

    public List<InterestArea> findByMno(int mno);

    public List<InterestArea> selectAll();

    public int delete(int id);

    public int deleteAll();
}
