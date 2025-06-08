package com.ssafy.home.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
    private int mno;
    private String password;
    private String email;
    private String name;
    private String address;
    private String phone;
    private String role;
    private String refresh;
    private boolean isVerified = false;
    private String verificationToken;
    private LocalDateTime tokenExpiryDate;
    private LocalDateTime createdAt;
    private String profileImage;
}