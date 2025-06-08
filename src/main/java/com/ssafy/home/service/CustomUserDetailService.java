package com.ssafy.home.service;

import com.ssafy.home.domain.User;
import com.ssafy.home.dto.CustomUserDetails;
import com.ssafy.home.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("사용자 조회 시도: {}", username);
        
        Optional<User> user = userMapper.findByEmail(username);
        
        if (user.isEmpty()) {
            log.error("사용자를 찾을 수 없음: {}", username);
            throw new UsernameNotFoundException(username);
        }
        
        log.debug("사용자 조회 성공: {}", username);
        log.debug("사용자 권한: {}", user.get().getRole());
        
        return new CustomUserDetails(user.get());
    }
}
