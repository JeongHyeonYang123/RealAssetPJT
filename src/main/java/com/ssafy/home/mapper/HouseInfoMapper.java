package com.ssafy.home.mapper;

import com.ssafy.home.domain.HouseInfo;
import com.ssafy.home.dto.HouseDongGroupDTO;
import com.ssafy.home.dto.HouseDongSearchDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface HouseInfoMapper {
    public void insertHouseInfo(HouseInfo houseInfo);

    Optional<HouseInfo> selectByAptSeq(@Param("aptSeq") String aptSeq);

    public List<HouseInfo> getAllHouseInfo();

    public HouseInfo getHouseInfoByAptCode(String aptCode);

    public boolean updateHouseInfoByAptCode(String aptCode, HouseInfo houseInfo);

    public List<HouseDongSearchDTO> getHouseInfosByDongCode(String code);

    public List<HouseDongSearchDTO> getHouseInfosBySigugunCode(String code);

    public List<HouseDongGroupDTO> getGroupedHouseInfoByDongCode(String code);

    public boolean deleteHouseInfoByAptCode(String aptCode);

    public int deleteAll();

    List<HouseInfo> findHouseInfosByName(String dongCode, String aptName);

    List<String> findAptSeqsByName(@Param("aptName") String aptName);

    List<HouseInfo> findHouseInfosByNameAndRegion(
            @Param("aptName") String aptName,
            @Param("dongCode") String dongCode);
}
