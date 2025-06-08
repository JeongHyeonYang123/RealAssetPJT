package com.ssafy.home.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SecurityExceptionHandlingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            log.debug("SecurityExceptionHandlingFilter 시작: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            log.debug("SecurityExceptionHandlingFilter 종료: 정상 처리");
        } catch (Exception e) {
            log.error("보안 필터 체인 처리 중 예외 발생: {}", e.getMessage(), e);

            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("timestamp", System.currentTimeMillis());
            errorDetails.put("path", request.getRequestURI());
            errorDetails.put("method", request.getMethod());

            if (e instanceof JwtException) {
                setErrorResponse(response, HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", errorDetails);
            } else if (e instanceof BadCredentialsException) {
                setErrorResponse(response, HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.", errorDetails);
            } else if (e instanceof AuthenticationException) {
                setErrorResponse(response, HttpStatus.UNAUTHORIZED, "인증에 실패했습니다.", errorDetails);
            } else if (e instanceof AccessDeniedException) {
                setErrorResponse(response, HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", errorDetails);
            } else {
                errorDetails.put("error", e.getClass().getSimpleName());
                setErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", errorDetails);
            }
        }
    }

    private void setErrorResponse(HttpServletResponse response, HttpStatus status, String message, Map<String, Object> errorDetails) throws IOException {
        log.debug("에러 응답 생성 - 상태: {}, 메시지: {}", status, message);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        errorDetails.put("status", status.value());
        errorDetails.put("message", message);
        
        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
    }
}
