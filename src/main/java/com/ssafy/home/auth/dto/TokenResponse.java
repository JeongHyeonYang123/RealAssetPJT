package com.ssafy.home.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private int mno;
    private String name;
    private String email;
    private String role;
    private String profileImage; // 프로필 이미지 추가
}