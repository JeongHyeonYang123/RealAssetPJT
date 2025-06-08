package com.ssafy.home.auth.controller;

import com.ssafy.home.auth.dto.TokenResponse;
import com.ssafy.home.auth.service.KakaoAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final KakaoAuthService kakaoAuthService;

    @PostMapping("/kakao")
    public ResponseEntity<TokenResponse> kakaoLogin(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        TokenResponse tokenResponse = kakaoAuthService.kakaoLogin(code);
        return ResponseEntity.ok(tokenResponse);
    }
}