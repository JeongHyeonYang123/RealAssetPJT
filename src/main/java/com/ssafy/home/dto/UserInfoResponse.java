package com.ssafy.home.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private String email;
    private String name;
    private String role;
    private String address;
    private int mno;
    private String profileImage;
    private int postCount;
    private int commentCount;
    private int interestCount;
}
