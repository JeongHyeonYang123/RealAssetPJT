package com.ssafy.home.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.home.controller.RestControllerHelper;
import com.ssafy.home.domain.User;
import com.ssafy.home.dto.CustomUserDetails;
import com.ssafy.home.service.UserService;
import com.ssafy.home.util.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter implements RestControllerHelper {

    private final UserService userService;
    private final JWTUtil jwtUtil;

    public JWTAuthenticationFilter(AuthenticationManager authenticationManager, UserService userService, JWTUtil jwtUtil) {
        super(authenticationManager);
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.setFilterProcessesUrl("/api/auth/login");
        this.setUsernameParameter("email");
        this.setPasswordParameter("password");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        log.debug("로그인 시도: {}", request.getRequestURI());
        try {
            // JSON 요청 처리
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> loginRequest = mapper.readValue(request.getInputStream(), Map.class);
            
            String email = loginRequest.get("email");
            String password = loginRequest.get("password");
            
            log.debug("로그인 시도 - 이메일: {}", email);
            log.debug("로그인 시도 - 비밀번호 길이: {}", password != null ? password.length() : 0);
            
            if (email == null || password == null) {
                throw new BadCredentialsException("이메일과 비밀번호를 모두 입력해주세요.");
            }
            
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password);
            return this.getAuthenticationManager().authenticate(authToken);
        } catch (IOException e) {
            log.error("로그인 요청 처리 중 오류 발생", e);
            throw new BadCredentialsException("로그인 요청을 처리할 수 없습니다.");
        } catch (AuthenticationException e) {
            log.error("인증 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                       Authentication authentication) {
        log.debug("로그인 성공: {}", authentication.getName());
        CustomUserDetails details = (CustomUserDetails) authentication.getPrincipal();
        User user = details.getUser();

        String accessToken = jwtUtil.createAccessToken(user);
        String refreshToken = jwtUtil.createRefreshToken(user);
        
        // 리프레시 토큰 저장
        user.setRefresh(refreshToken);
        log.debug("user: {}", user);
        userService.update(user, user.getMno());

        Map<String, String> result = Map.of(
            "status", "SUCCESS",
            "accessToken", accessToken,
            "refreshToken", refreshToken,
            "mno", String.valueOf(user.getMno()),
            "email", user.getEmail(),
            "name", user.getName(),
            "role", user.getRole()
        );
        
        handleResult(response, result, HttpStatus.OK);
    }

    @Override
    public void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException failed) {
        log.error("로그인 실패: {}", failed.getClass().getSimpleName());
        log.error("로그인 실패 상세: {}", failed.getMessage());
        if (failed instanceof BadCredentialsException) {
            log.error("잘못된 자격 증명");
        } else if (failed instanceof UsernameNotFoundException) {
            log.error("사용자를 찾을 수 없음");
        }
        throw failed;
    }

    private void handleResult(HttpServletResponse response, Map<String, ?> data, HttpStatus status) {
        response.setContentType("application/json;charset=UTF-8");
        try {
            String jsonResponse = new ObjectMapper().writeValueAsString(data);
            response.setStatus(status.value());
            response.getWriter().write(jsonResponse);
        } catch (IOException e) {
            log.error("응답 작성 중 오류 발생", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}
