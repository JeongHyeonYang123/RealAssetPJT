package com.ssafy.home.auth.jwt;

import com.ssafy.home.auth.dto.TokenResponse;
import com.ssafy.home.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    public TokenResponse createToken(User user) {
        // Claims를 직접 수정하지 않고 Map으로 먼저 데이터 구성
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getEmail());
        claims.put("role", user.getRole());
        claims.put("name", user.getName());
        claims.put("mno", user.getMno()); // mno 추가
        claims.put("profileImage", user.getProfileImage()); // 프로필 이미지 추가

        Date now = new Date();
        Date accessTokenValidityDate = new Date(now.getTime() + this.accessTokenValidity);
        Date refreshTokenValidityDate = new Date(now.getTime() + this.refreshTokenValidity);

        // 적절한 크기의 키 생성
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

        String accessToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(accessTokenValidityDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String refreshToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(refreshTokenValidityDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .mno(user.getMno())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .profileImage(user.getProfileImage()) // 프로필 이미지 포함
                .build();
    }
}