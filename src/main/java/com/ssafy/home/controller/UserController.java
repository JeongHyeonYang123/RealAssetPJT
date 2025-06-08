package com.ssafy.home.controller;

import com.ssafy.home.domain.User;
import com.ssafy.home.dto.ResetPasswordRequest;
import com.ssafy.home.dto.Response;
import com.ssafy.home.dto.UserInfoResponse;
import com.ssafy.home.service.FavoriteHousedealService;
import com.ssafy.home.service.PostService;
import com.ssafy.home.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User API", description = "회원 관련 API")
public class UserController {
    private final UserService userService;
    private final PostService postService;
    private final FavoriteHousedealService favoriteHousedealService;

    // CREATE
    @Operation(summary = "회원 가입", description = "새로운 회원을 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "회원가입 실패")
    })
    @PostMapping
    public ResponseEntity<Response<Void>> createUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "회원 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = User.class))
            )
            @RequestBody User user) {

        try {
            userService.register(user);
            return ResponseEntity.ok(new Response<>(true, "회원가입 신청이 완료되었습니다. 이메일을 확인해주세요.", null));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new Response<>(false, "회원가입에 실패하였습니다.", null));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<Response<Void>> verifyUser(@RequestParam String token) {
        boolean isVerified = userService.verifyUser(token);

        if (isVerified) {
            return ResponseEntity.ok(new Response<>(true, "이메일 인증이 완료되었습니다!", null));
        } else {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new Response<>(false, "유효하지 않거나 만료된 토큰입니다.", null));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestParam String email) {
        try {
            User user = userService.findByEmail(email).orElseGet(null);
            if (user != null && !user.isVerified()) {
                userService.resendVerificationEmail(user);
                return ResponseEntity.ok(new Response<>(true, "인증 메일을 재발송했습니다.", null));
            }
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new Response<>(false, "유효하지 않은 요청입니다.", null));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new Response<>(false, "재발송 실패: " + e.getMessage(), null));
        }
    }

    // READ (이메일 중복 확인)
    @Operation(summary = "이메일 사용 가능 여부 조회", description = "이메일이 이미 사용 중인지 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/email/{email}")
    public ResponseEntity<Response<Map<String, Boolean>>> checkEmail(
            @Parameter(description = "확인할 이메일", example = "test@example.com")
            @PathVariable String email) {
        boolean exist = userService.existsByEmail(email);
        Map<String, Boolean> result = Collections.singletonMap("canUse", !exist);
        return ResponseEntity.ok(new Response<>(true, "이메일 사용 가능 여부 조회", result));
    }

    // READ (단일 회원 조회)
    @Operation(summary = "회원 정보 조회", description = "회원 번호로 회원 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "회원 정보 조회 실패")
    })
    @GetMapping("/{mno}")
    public ResponseEntity<Response<UserInfoResponse>> getUser(
            @Parameter(description = "회원 번호", example = "1")
            @PathVariable int mno) {
        User user = userService.findByMno(mno).orElse(null);

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new Response<>(false, "회원 정보 조회 실패", null));
        }

        if (!user.isVerified()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new Response<>(false, "이메일 인증을 완료해주세요.", null));
        }

        UserInfoResponse userInfoResponse = new UserInfoResponse();
        userInfoResponse.setEmail(user.getEmail());
        userInfoResponse.setName(user.getName());
        userInfoResponse.setRole(user.getRole());
        userInfoResponse.setAddress(user.getAddress());
        userInfoResponse.setMno(user.getMno());
        userInfoResponse.setPostCount(postService.getPostCountByAuthor(mno));
        userInfoResponse.setCommentCount(postService.getCommentCountByAuthor(mno));
        userInfoResponse.setInterestCount(favoriteHousedealService.countAllByUserMno(mno));   // TODO
        userInfoResponse.setProfileImage(user.getProfileImage());

        return ResponseEntity.ok(new Response<>(true, "회원 정보 조회", userInfoResponse));
    }

    // UPDATE
    @Operation(summary = "회원 정보 수정", description = "회원 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "수정 실패")
    })
    @PutMapping("/{mno}")
    public ResponseEntity<Response<Void>> updateUser(
            @Parameter(description = "회원 번호", example = "1")
            @PathVariable int mno,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "수정할 회원 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = User.class))
            )
            @RequestBody User user) {

        boolean ok = userService.update(user, mno) == 1;

        if (!ok) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new Response<>(false, "회원 정보 수정 실패", null));
        }

        return ResponseEntity.ok(new Response<>(true, "회원 정보 수정", null));
    }

    @Operation(summary = "회원 프로필 이미지 수정", description = "회원 프로필 이미지를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "수정 실패")
    })
    @PutMapping("/{mno}/profile-image")
    public ResponseEntity<Response<Void>> updateProfileImage(
            @Parameter(description = "회원 번호", example = "1")
            @PathVariable int mno,
            @RequestParam("profileImage") String profileImageUrl) {
        // URL 또는 일반 이미지 업로드 모두 허용: URL이면 그대로 저장, 파일이면 파일 서버에 저장 후 URL 반환
        // (여기서는 URL만 처리, 파일 업로드는 별도 API 필요)
        User user = userService.findByMno(mno).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Response<>(false, "회원 정보 조회 실패", null));
        }
        user.setProfileImage(profileImageUrl);
        userService.update(user, mno);
        return ResponseEntity.ok(new Response<>(true, "프로필 이미지가 변경되었습니다.", null));
    }

    // DELETE
    @Operation(summary = "회원 삭제", description = "회원 번호로 회원을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "삭제 실패")
    })
    @DeleteMapping("/{mno}")
    public ResponseEntity<Response<Void>> withdraw(
            @Parameter(description = "회원 번호", example = "1")
            @PathVariable int mno) {

        boolean ok = userService.deleteByMno(mno) == 1;

        if (!ok) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new Response<>(false, "회원 삭제 실패", null));
        }

        return ResponseEntity.ok(new Response<>(true, "회원 삭제", null));
    }

    // 비밀번호 초기화 (임시 비밀번호 발급)
    @Operation(summary = "비밀번호 초기화", description = "이름, 이메일이 모두 일치하는 경우 임시 비밀번호를 발급하고 이메일로 전송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 초기화 성공"),
            @ApiResponse(responseCode = "400", description = "비밀번호 초기화 실패")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Response<Void>> resetPassword(@RequestBody ResetPasswordRequest request) {
        boolean ok = userService.resetPasswordAndSendEmail(request.getName(), request.getEmail());

        if (!ok) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Response<>(false, "비밀번호 초기화 실패 (이름, 이메일, 휴대폰 번호 확인 필요)", null));
        }
        return ResponseEntity.ok(new Response<>(true, "임시 비밀번호가 이메일로 발송되었습니다.", null));
    }

    // 비밀번호 변경
    @PutMapping("/{mno}/password")
    public ResponseEntity<Response<Void>> changePassword(
            @PathVariable int mno,
            @RequestBody Map<String, String> body) {
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");
        boolean ok = userService.changePassword(mno, currentPassword, newPassword);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Response<>(false, "현재 비밀번호가 일치하지 않습니다.", null));
        }
        return ResponseEntity.ok(new Response<>(true, "비밀번호 변경 성공", null));
    }
}
