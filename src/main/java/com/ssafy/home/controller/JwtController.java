package com.ssafy.home.controller;

import com.ssafy.home.domain.User;
import com.ssafy.home.service.UserService;
import com.ssafy.home.util.JWTUtil;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class JwtController implements RestControllerHelper {
    private final JWTUtil jwtUtil;
    private final UserService userService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestHeader("Refresh-Token") String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Refresh token is required"));
        }
        // 1. Refresh Token 유효성 검증 (JWT 자체) 및 이메일/서브 추출
        Map<String, Object> claims = jwtUtil.getClaims(refreshToken);
        String email = claims.get("email") != null ? claims.get("email").toString() : (claims.get("sub") != null ? claims.get("sub").toString() : null); // email 없으면 sub도 Map에서 추출

        if (email == null) {
            throw new JwtException("Invalid refresh token: email/sub claim missing");
        }

        // 2. DB에서 사용자 조회 및 Refresh Token 일치 여부 확인
        User user = userService.findByEmail(email).orElseGet(User::new);

        if (user.getEmail() == null || user.getRefresh() == null || !user.getRefresh().equals(refreshToken)) {
            log.warn("Invalid or mismatched refresh token for user: {}", email);
            // 보안: DB의 토큰과 불일치 시, 해당 사용자의 DB 토큰을 무효화(null 처리)하는 것도 고려
            // memberService.invalidateRefreshToken(email);
            return handleFail(new JwtException("Invalid refresh token"), HttpStatus.UNAUTHORIZED);
        }

        // 3. 새 Access Token 생성
        String newAccessToken = jwtUtil.createAccessToken(user);

        // 4. Refresh Token Rotation: 새 Refresh Token 생성 및 DB 업데이트 - 보안 상 권장
        String newRefreshToken = jwtUtil.createRefreshToken(user);
        user.setRefresh(newRefreshToken); // Member 객체에 새 Refresh Token 설정
        userService.update(user, user.getMno()); // DB에 새 Refresh Token 저장

        // 5. 새 토큰들을 응답으로 반환
        return handleSuccess(Map.of("accessToken", newAccessToken, "refreshToken", newRefreshToken));

    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Refresh-Token") String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Refresh token is required"));
        }

        // 1. Refresh Token 유효성 검증 (JWT 자체) 및 이메일/서브 추출
        Map<String, Object> claims = jwtUtil.getClaims(refreshToken);
        String email = claims.get("email") != null ? claims.get("email").toString() : (claims.get("sub") != null ? claims.get("sub").toString() : null); // email 없으면 sub도 Map에서 추출

        if (email == null) {
            throw new JwtException("Invalid refresh token: email/sub claim missing");
        }

        // 2. DB에서 사용자 조회 및 Refresh Token 일치 여부 확인
        User user = userService.findByEmail(email).orElseGet(User::new);
        user.setRefresh(null);
        userService.update(user, user.getMno());

        return handleSuccess(Map.of("accessToken", "", "refreshToken", ""));
    }
}
