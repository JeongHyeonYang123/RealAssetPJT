package com.ssafy.home.mapper;

import com.ssafy.home.domain.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {
    public int insert(User user);

    public Optional<User> findByMno(int mno);

    public Optional<User> findByEmailAndPassword(String email, String password);

    public Optional<String> findEmailByMno(int mno);

    public Optional<String> findPasswordByEmail(String email);

    public User findByVerificationToken(String token);

    public List<User> searchByKeyword(String keyword);

    public int update(User user, int mno);

    public Optional<User> findByEmail(String email);

    public int deleteByMno(int mno);

    public int deleteByEmail(String email);

    public int insertDeletedUserByEmail(String email);

    public int updateDeletedUserByEmail(String email);

    public boolean isUserDeleted(String email);

    public List<User> selectAll();

    public Optional<User> findByNameAndEmail(String name, String email);

    public int updateTemporaryPasswordByEmail(String email, String newPassword);

    public int deleteAll();
}
