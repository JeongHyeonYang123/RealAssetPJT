/*
 * [프로젝트 구조/분석]
 * - 댓글(Comment) 관련 REST API 컨트롤러
 * - Swagger 문서화, 권한 체크, 예외처리 등 담당
 * [확장 포인트]
 * - JWT 인증/인가 실제 구현 필요 (extractUserMnoFromAuth, extractUserNameFromAuth)
 * - 대댓글 UI, 첨부파일, 신고 등 추가 가능
 * [주의사항]
 * - Soft Delete, 권한 체크, 입력값 검증 등 주의
 */
// getComments: 댓글 목록 조회(게시글별)
// createComment: 댓글 작성 (TODO: JWT 인증/인가 구현 필요)
// updateComment: 댓글 수정(본인만 가능)
// deleteComment: 댓글 삭제(본인만 가능)
// extractUserMnoFromAuth, extractUserNameFromAuth: JWT에서 정보 추출(임시값, 실제 구현 필요)

package com.ssafy.home.controller;

import com.ssafy.home.dto.CommentDto;
import com.ssafy.home.dto.CommentWithPostDto;
import com.ssafy.home.dto.CustomUserDetails;
import com.ssafy.home.service.PostService;
import com.ssafy.home.dto.Response;
import com.ssafy.home.util.JWTUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.List;

@Tag(name = "댓글 API", description = "게시글 댓글 관리 API")
@RestController
@RequestMapping("/api/v1/posts")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final PostService postService;
    private final JWTUtil jwtUtil;

    @Operation(summary = "댓글 목록 조회", description = "게시글의 모든 댓글을 조회합니다.")
    @GetMapping("/{id}/comments")
    public ResponseEntity<Response<List<CommentDto>>> getComments(
            @Parameter(description = "게시글 ID") @PathVariable Long id) {
        try {
            List<CommentDto> comments = postService.getComments(id);
            return ResponseEntity.ok(new Response<>(true, "댓글 목록 조회 성공", comments));
        } catch (Exception e) {
            log.error("댓글 목록 조회 실패: ", e);
            return ResponseEntity.badRequest()
                    .body(new Response<>(false, "댓글 목록 조회 실패: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "댓글 작성", description = "게시글에 새 댓글을 작성합니다.")
    @PostMapping("/{id}/comments")
    public ResponseEntity<Response<Long>> createComment(
            @Parameter(description = "게시글 ID") @PathVariable Long id,
            @RequestBody CommentDto comment,
            Authentication auth) {
        try {
            Integer authorMno = extractUserMnoFromAuth(auth);
            String authorName = extractUserNameFromAuth(auth);

            comment.setPostId(id);
            comment.setAuthorMno(authorMno);
            comment.setAuthorName(authorName);

            Long commentId = postService.createComment(comment);
            return ResponseEntity.ok(new Response<>(true, "댓글 작성 성공", commentId));
        } catch (Exception e) {
            log.error("댓글 작성 실패: ", e);
            return ResponseEntity.badRequest()
                    .body(new Response<>(false, "댓글 작성 실패: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "댓글 수정", description = "댓글을 수정합니다. 작성자만 수정 가능합니다.")
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<Response<Void>> updateComment(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @RequestBody CommentDto comment,
            Authentication auth) {
        try {
            Integer authorMno = extractUserMnoFromAuth(auth);

            // 작성자 확인
            CommentDto existingComment = postService.getCommentForUpdate(commentId);
            if (!existingComment.getAuthorMno().equals(authorMno)) {
                return ResponseEntity.status(403)
                        .body(new Response<>(false, "수정 권한이 없습니다.", null));
            }

            comment.setId(commentId);
            postService.updateComment(comment);
            return ResponseEntity.ok(new Response<>(true, "댓글 수정 성공", null));
        } catch (Exception e) {
            log.error("댓글 수정 실패: ", e);
            return ResponseEntity.badRequest()
                    .body(new Response<>(false, "댓글 수정 실패: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다. 작성자만 삭제 가능합니다.")
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Response<Void>> deleteComment(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            Authentication auth) {
        try {
            Integer authorMno = extractUserMnoFromAuth(auth);

            // 작성자 확인
            CommentDto existingComment = postService.getCommentForUpdate(commentId);
            if (!existingComment.getAuthorMno().equals(authorMno)) {
                return ResponseEntity.status(403)
                        .body(new Response<>(false, "삭제 권한이 없습니다.", null));
            }

            postService.deleteComment(commentId);
            return ResponseEntity.ok(new Response<>(true, "댓글 삭제 성공", null));
        } catch (Exception e) {
            log.error("댓글 삭제 실패: ", e);
            return ResponseEntity.badRequest()
                    .body(new Response<>(false, "댓글 삭제 실패: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "내 댓글 목록 조회", description = "사용자가 작성한 모든 댓글을 게시글 정보와 함께 조회합니다.")
    @GetMapping("/comments/my")
    public ResponseEntity<Response<List<CommentWithPostDto>>> getMyComments(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") Integer size,
            Authentication auth) {
        try {
            Integer authorMno = extractUserMnoFromAuth(auth);
            if (authorMno == null) {
                return ResponseEntity.status(401)
                        .body(new Response<>(false, "인증이 필요합니다.", null));
            }

            List<CommentWithPostDto> comments = postService.getMyCommentsWithPost(authorMno, page, size);
            return ResponseEntity.ok(new Response<>(true, "내 댓글 목록 조회 성공", comments));
        } catch (Exception e) {
            log.error("내 댓글 목록 조회 실패: ", e);
            return ResponseEntity.badRequest()
                    .body(new Response<>(false, "내 댓글 목록 조회 실패: " + e.getMessage(), null));
        }
    }

    // JWT에서 사용자 정보 추출 헬퍼 메서드들
    private Integer extractUserMnoFromAuth(Authentication auth) {
        if (auth == null) return null;
        try {
            Object details = auth.getPrincipal();
            // CustomUserDetails에서 mno 추출
            if (details instanceof com.ssafy.home.dto.CustomUserDetails customUserDetails) {
                log.debug("[extractUserMnoFromAuth] CustomUserDetails mno: {}", customUserDetails.getMno());
                return customUserDetails.getMno();
            }
            // UserDetails에서 mno 추출 (UserDetailsDTO 등)
            if (details instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                try {
                    Method getMnoMethod = userDetails.getClass().getMethod("getMno");
                    Object mnoObj = getMnoMethod.invoke(userDetails);
                    if (mnoObj != null) {
                        log.debug("[extractUserMnoFromAuth] UserDetails.getMno(): {}", mnoObj);
                        return Integer.parseInt(mnoObj.toString());
                    }
                } catch (NoSuchMethodException ignored) {}
            }
            // JWT Claims에서 직접 추출
            if (auth.getCredentials() instanceof String token) {
                Claims claims = jwtUtil.getClaims(token);
                Object mnoObj = claims.get("mno");
                log.debug("[extractUserMnoFromAuth] claims mno: {}", mnoObj);
                if (mnoObj != null) {
                    return Integer.parseInt(mnoObj.toString());
                }
            }
        } catch (Exception e) {
            log.warn("extractUserMnoFromAuth 실패", e);
        }
        log.warn("[extractUserMnoFromAuth] mno 추출 실패: principal={}, credentials={}", auth.getPrincipal(), auth.getCredentials());
        return null;
    }

    private String extractUserNameFromAuth(Authentication auth) {
        if (auth == null) return null;
        try {
            Object details = auth.getPrincipal();
            // CustomUserDetails에서 name 추출
            if (details instanceof com.ssafy.home.dto.CustomUserDetails customUserDetails) {
                log.debug("[extractUserNameFromAuth] CustomUserDetails name: {}", customUserDetails.getName());
                return customUserDetails.getName();
            }
            // UserDetails에서 name 추출 (UserDetailsDTO 등)
            if (details instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                try {
                    java.lang.reflect.Method getNameMethod = userDetails.getClass().getMethod("getName");
                    Object nameObj = getNameMethod.invoke(userDetails);
                    if (nameObj != null) {
                        log.debug("[extractUserNameFromAuth] UserDetails.getName(): {}", nameObj);
                        return nameObj.toString();
                    }
                } catch (NoSuchMethodException ignored) {}
            }
            // JWT Claims에서 직접 추출
            if (auth.getCredentials() instanceof String token) {
                Claims claims = jwtUtil.getClaims(token);
                Object nameObj = claims.get("name");
                log.debug("[extractUserNameFromAuth] claims name: {}", nameObj);
                if (nameObj != null) {
                    return nameObj.toString();
                }
            }
        } catch (Exception e) {
            log.warn("extractUserNameFromAuth 실패", e);
        }
        log.warn("[extractUserNameFromAuth] name 추출 실패: principal={}, credentials={}", auth.getPrincipal(), auth.getCredentials());
        return null;
    }
}