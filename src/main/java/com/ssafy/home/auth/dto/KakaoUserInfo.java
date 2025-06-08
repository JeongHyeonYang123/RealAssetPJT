package com.ssafy.home.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KakaoUserInfo {
    private String id;
    private String nickname;
    private String email;
    private String profileImageUrl; // 카카오 프로필 이미지 URL 추가
}