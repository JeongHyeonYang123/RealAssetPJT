package com.ssafy.home.filter;

import com.ssafy.home.service.CustomUserDetailService;
import com.ssafy.home.util.JWTUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JWTVerificationFilter extends OncePerRequestFilter {
    private final JWTUtil jwtUtil;
    private final CustomUserDetailService userDetailsService;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        log.debug("JWTVerificationFilter.doFilterInternal() called for: {} {}", method, requestURI);

        // HTTP 메서드별로 세분화된 Public Path 체크
        if (isPublicPath(requestURI, method)) {
            log.debug("Public path detected, skipping JWT verification: {} {}", method, requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);
        if (token == null) {
            log.debug("No token found in request for: {} {}", method, requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            // 토큰 검증 및 사용자 정보 추출
            Claims claims = jwtUtil.getClaims(token);
            String email = claims.get("email") != null ? claims.get("email").toString() : claims.getSubject();
            if (email == null) {
                log.error("JWT 토큰에 email/sub 정보가 없습니다.");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            UserDetails details = userDetailsService.loadUserByUsername(email);

            // 인증 객체 생성 및 SecurityContext에 저장
            var authentication = new UsernamePasswordAuthenticationToken(
                    details,
                    null,
                    details.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT authentication successful for user: {}", email);
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWT verification failed for: {} {}", method, requestURI, e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    // HTTP 메서드별 Public Path 체크
    private boolean isPublicPath(String requestURI, String method) {
        // 기본 인증 관련 경로
        if (requestURI.startsWith("/api/auth/")
                || requestURI.equals("/api/v1/users")
                || requestURI.startsWith("/api/v1/users/email/")
                || requestURI.startsWith("/api/v1/users/verify")
                || requestURI.startsWith("/api/v1/users/find-password")
                || requestURI.startsWith("/api/v1/users/reset-password")
                || requestURI.startsWith("/api/v1/users/refresh")
                || requestURI.startsWith("/api/v1/adm/")
                || requestURI.startsWith("/api/v1/transactions/")
                || requestURI.startsWith("/api/v1/commercial-areas/")
                || requestURI.startsWith("/api/v1/public/")
                || requestURI.startsWith("/api/v1/bitcoin/")
                || requestURI.startsWith("/api/v1/house/")
                || requestURI.startsWith("/api/v1/chatbot/")
                || requestURI.startsWith("/v3/api-docs")
                || requestURI.startsWith("/v1/api-docs")
                || requestURI.startsWith("/swagger-ui")) {
            return true;
        }

        // 관심지역 API는 인증 필요
        if (requestURI.startsWith("/api/v1/interest-areas/")) {
            return false;
        }

        // 게시글 관련 - GET 요청만 허용
        if (requestURI.startsWith("/api/v1/posts")) {
            // 게시글 목록 조회: GET /api/v1/posts
            if (requestURI.equals("/api/v1/posts") && "GET".equals(method)) {
                return true;
            }

            // 게시글 상세 조회: GET /api/v1/posts/{id}
            if (requestURI.matches("/api/v1/posts/\\d+$") && "GET".equals(method)) {
                return true;
            }

            // 댓글 조회: GET /api/v1/posts/{id}/comments
            if (requestURI.matches("/api/v1/posts/\\d+/comments$") && "GET".equals(method)) {
                return true;
            }
        }

        return false;
    }
}
