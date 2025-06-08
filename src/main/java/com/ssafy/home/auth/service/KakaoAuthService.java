package com.ssafy.home.auth.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ssafy.home.auth.dto.KakaoUserInfo;
import com.ssafy.home.auth.dto.TokenResponse;
import com.ssafy.home.auth.jwt.JwtTokenProvider;
import com.ssafy.home.domain.User;
import com.ssafy.home.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KakaoAuthService {
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${kakao.client.id}")
    private String clientId;

    @Value("${kakao.client.secret}")
    private String clientSecret;

    @Value("${kakao.client.redirect-uri}")
    private String redirectUri;

    public TokenResponse kakaoLogin(String code) {
        // 1. 카카오 토큰 받기
        String accessToken = getKakaoAccessToken(code);

        // 2. 카카오 사용자 정보 받기
        KakaoUserInfo kakaoUserInfo = getKakaoUserInfo(accessToken);

        // 3. 회원가입 또는 로그인 처리
        User user = processKakaoUser(kakaoUserInfo);

        // 4. JWT 토큰 발급
        TokenResponse tokenResponse = jwtTokenProvider.createToken(user);

        // 5. refreshToken DB 저장 (추가)
        user.setRefresh(tokenResponse.getRefreshToken());
        userMapper.update(user, user.getMno());

        return tokenResponse;
    }

    private String getKakaoAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class);

        JsonElement element = JsonParser.parseString(response.getBody());
        return element.getAsJsonObject().get("access_token").getAsString();
    }

    private KakaoUserInfo getKakaoUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<String> kakaoUserInfoRequest = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                kakaoUserInfoRequest,
                String.class);

        // 디버깅용 로그 추가
        System.out.println("=== 카카오 API 전체 응답 ===");
        System.out.println(response.getBody());
        System.out.println("===========================");

        JsonElement element = JsonParser.parseString(response.getBody());
        JsonObject jsonObject = element.getAsJsonObject();

        String id = jsonObject.get("id").getAsString();

        // 닉네임 추출 로직 강화
        String nickname = extractNickname(jsonObject);

        JsonObject kakaoAccount = jsonObject.getAsJsonObject("kakao_account");
        String email = kakaoAccount != null && kakaoAccount.has("email") ?
                kakaoAccount.get("email").getAsString() : id + "@kakao.com";

        // 프로필 이미지 추출
        String profileImageUrl = null;
        if (kakaoAccount != null && kakaoAccount.has("profile") && !kakaoAccount.get("profile").isJsonNull()) {
            JsonObject profile = kakaoAccount.getAsJsonObject("profile");
            if (profile.has("profile_image_url") && !profile.get("profile_image_url").isJsonNull()) {
                profileImageUrl = profile.get("profile_image_url").getAsString();
            }
        }

        System.out.println("추출된 정보 - ID: " + id + ", 닉네임: " + nickname + ", 이메일: " + email + ", 프로필: " + profileImageUrl);

        return new KakaoUserInfo(id, nickname, email, profileImageUrl);
    }

    private String extractNickname(JsonObject jsonObject) {
        String nickname = "카카오사용자"; // 기본값

        try {
            // 1. properties.nickname에서 시도
            if (jsonObject.has("properties") && !jsonObject.get("properties").isJsonNull()) {
                JsonObject properties = jsonObject.getAsJsonObject("properties");
                if (properties.has("nickname") && !properties.get("nickname").isJsonNull()) {
                    String extracted = properties.get("nickname").getAsString();
                    if (extracted != null && !extracted.trim().isEmpty()) {
                        nickname = extracted.trim();
                        System.out.println("properties.nickname에서 추출: " + nickname);
                        return nickname;
                    }
                }
            }

            // 2. kakao_account.profile.nickname에서 시도
            if (jsonObject.has("kakao_account") && !jsonObject.get("kakao_account").isJsonNull()) {
                JsonObject kakaoAccount = jsonObject.getAsJsonObject("kakao_account");
                if (kakaoAccount.has("profile") && !kakaoAccount.get("profile").isJsonNull()) {
                    JsonObject profile = kakaoAccount.getAsJsonObject("profile");
                    if (profile.has("nickname") && !profile.get("nickname").isJsonNull()) {
                        String extracted = profile.get("nickname").getAsString();
                        if (extracted != null && !extracted.trim().isEmpty()) {
                            nickname = extracted.trim();
                            System.out.println("kakao_account.profile.nickname에서 추출: " + nickname);
                            return nickname;
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("닉네임 추출 중 오류: " + e.getMessage());
        }

        System.out.println("닉네임을 찾을 수 없어 기본값 사용: " + nickname);
        return nickname;
    }

    private User processKakaoUser(KakaoUserInfo kakaoUserInfo) {
        Optional<User> existingUser = userMapper.findByEmail(kakaoUserInfo.getEmail());

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            // 기존 사용자의 닉네임/프로필 이미지 업데이트
            boolean updated = false;
            if (!user.getName().equals(kakaoUserInfo.getNickname())) {
                user.setName(kakaoUserInfo.getNickname());
                updated = true;
            }
            if (kakaoUserInfo.getProfileImageUrl() != null && !kakaoUserInfo.getProfileImageUrl().equals(user.getProfileImage())) {
                user.setProfileImage(kakaoUserInfo.getProfileImageUrl());
                updated = true;
            }
            // UserMapper에 update 메서드가 있다면 주석 해제
            // if (updated) userMapper.update(user);
            if (updated) System.out.println("기존 사용자 정보 업데이트: 닉네임/프로필");
            return user;
        }

        // 새로운 회원 생성
        User newUser = User.builder()
                .email(kakaoUserInfo.getEmail())
                .name(kakaoUserInfo.getNickname())
                .password("KAKAO_" + kakaoUserInfo.getId())
                .role("USER")
                .createdAt(LocalDateTime.now())
                .profileImage(kakaoUserInfo.getProfileImageUrl())
                .build();

        System.out.println("새로운 카카오 사용자 생성: " + newUser.getName() + " (" + newUser.getEmail() + ")");
        userMapper.insert(newUser);
        return newUser;
    }
}
