package com.ssafy.home.util;

import com.ssafy.home.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

@Component
@Slf4j
public class JWTUtil {
    private final SecretKey key;

    public JWTUtil(@Value("${jwt.secret}") String secret) {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.debug("jwt secret key: {}", Arrays.toString(key.getEncoded()));
    }

    @Value("${jwt.access-expmin}")
    private long accessExpMin;

    @Value("${jwt.refresh-expmin}")
    private long refreshExpMin;

    public String createAccessToken(User user) {
        if (user.getRole() == null) {
            user.setRole("USER");
        }
        // mno도 claims에 포함
        return create("accessToken", accessExpMin,
                Map.of(
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "role", user.getRole(),
                    "mno", user.getMno()
                )
        );
    }

    public String createRefreshToken(User user) {
        // mno도 claims에 포함
        return create("refreshToken", refreshExpMin, Map.of(
            "email", user.getEmail(),
            "mno", user.getMno()
        ));
    }

    public String create(String subject, long expireMin, Map<String, Object> claims) {
        Date expireDate = new Date(System.currentTimeMillis() + 1000 * 60 * expireMin);
        String token = Jwts.builder().subject(subject).claims(claims).expiration(expireDate).signWith(key).compact();
        log.debug("token 생성: {}", token);
        return token;
    }

    public Claims getClaims(String jwt) {
        try {
            JwtParser parser = Jwts.parser().verifyWith(key).build();
            Jws<Claims> jws = parser.parseSignedClaims(jwt);
            log.debug("claim: {}", jws.getPayload());
            return jws.getPayload();
        } catch (Exception e) {
            log.error("토큰 검증 실패", e);
            throw e;
        }
    }
}