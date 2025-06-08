package com.ssafy.home.service;

import com.ssafy.home.dto.FavoriteHousedealDto;
import com.ssafy.home.mapper.FavoriteHousedealMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteHousedealService {
    private final FavoriteHousedealMapper mapper;

    public int create(FavoriteHousedealDto dto) {
        try {
            return mapper.insert(dto);
        } catch (DuplicateKeyException e) {
            // 이미 soft delete된 row가 있다면 복구(undelete)
            return mapper.undelete(dto.getUserMno(), dto.getAptSeq());
        }
    }

    public List<FavoriteHousedealDto> getAll() {
        return mapper.selectAll();
    }

    public FavoriteHousedealDto getById(int id) {
        return mapper.selectById(id);
    }

    public int deleteById(int id) {
        return mapper.deleteById(id);
    }

    public int countAll() {
        return mapper.countAll();
    }

    // === 유저 mno 기반 메서드 ===
    public List<FavoriteHousedealDto> getAllByUserMno(int userMno) {
        return mapper.selectAllByUserMno(userMno);
    }

    public FavoriteHousedealDto getByIdAndUserMno(int id, int userMno) {
        return mapper.selectByIdAndUserMno(id, userMno);
    }

    public int deleteByIdAndUserMno(int id, int userMno) {
        return mapper.deleteByIdAndUserMno(id, userMno);
    }

    public int countAllByUserMno(int userMno) {
        return mapper.countAllByUserMno(userMno);
    }
}