package com.ssafy.home.mapper;

import com.ssafy.home.domain.DongCode;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DongMapper {
    public void insertDong(DongCode dongCode);

    public List<DongCode> getAllDongs();

    public DongCode getDongByCode(String code);

    public List<DongCode> getDongsByCodes(List<String> dongCodes);

    public String getCodeByDongCode(DongCode dongCode);

    public DongCode updateDong(DongCode dongCode);

    public void deleteDong(String code);

    public List<DongCode> getSidoList();

    public List<DongCode> getGugunListBySido(String sido);

    public List<DongCode> getDongListBySidoAndGugun(String sido, String gugun);

    public int deleteAll();
}