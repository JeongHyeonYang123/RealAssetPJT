package com.ssafy.home.service;

import com.ssafy.home.domain.DongCode;
import com.ssafy.home.domain.InterestArea;
import com.ssafy.home.mapper.DongMapper;
import com.ssafy.home.mapper.InterestAreaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterestAreaService {
    private final InterestAreaMapper interestAreaMapper;
    private final DongMapper dongMapper;

    public int save(InterestArea interestAreaDTO) {
        return interestAreaMapper.insert(interestAreaDTO);
    }

    public  List<InterestArea> findByMno(int mno) {
        return interestAreaMapper.findByMno(mno);
    }

    public List<InterestArea> findAll() {
        return interestAreaMapper.selectAll();
    }

    public int delete(int id) {
        return interestAreaMapper.delete(id);
    }

    public DongCode getAddress(String code) {
        return dongMapper.getDongByCode(code);
    }
}
