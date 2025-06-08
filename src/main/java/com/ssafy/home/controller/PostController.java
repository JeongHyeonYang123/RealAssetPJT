package com.ssafy.home.controller;

import com.ssafy.home.dto.PostDto;
import com.ssafy.home.dto.PostSearchDto;
import com.ssafy.home.service.PostService;
import com.ssafy.home.dto.Response;
import com.ssafy.home.util.JWTUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * [프로젝트 구조/분석]
 * - 게시글(Post) 관련 REST API 컨트롤러
 * - Swagger 문서화, 권한 체크, 예외처리 등 담당
 * [확장 포인트]
 * - JWT 인증/인가 실제 구현 필요 (extractUserMnoFromAuth, extractUserNameFromAuth)
 * - 태그 검색, 첨부파일, 신고 등 추가 가능
 * [주의사항]
 * - Soft Delete, 권한 체크, 입력값 검증 등 주의
 */
@Tag(name = "게시글 API", description = "커뮤니티 게시글 관리 API")
@RestController
@RequestMapping("/api/v1/posts")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")  
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;
    private final JWTUtil jwtUtil;

    @Operation(
            summary = "게시글 목록 조회",
            description = "카테고리별, 검색어별, 작성자별 게시글 목록을 페이징하여 조회합니다. (로그인 불필요)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping
    public ResponseEntity<Response<Map<String, Object>>> getPosts(
            @Parameter(description = "카테고리 필터") @RequestParam(required = false) String category,
            @Parameter(description = "검색어 (제목, 내용, 작성자)") @RequestParam(required = false) String searchKeyword,
            @Parameter(description = "작성자 회원번호") @RequestParam(required = false) Integer authorMno,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "12") Integer size,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "DESC") String sortDirection) {

        try {
            int safePage = (page == null) ? 0 : page;
            int safeSize = (size == null) ? 12 : size;
            int offset = safePage * safeSize;

            PostSearchDto searchDto = PostSearchDto.builder()
                    .category(category)
                    .searchKeyword(searchKeyword)
                    .authorMno(authorMno)
                    .page(safePage)
                    .size(safeSize)
                    .offset(offset)
                    .sortBy(sortBy)
                    .sortDirection(sortDirection)
                    .build();

            List<PostDto> posts = postService.getPosts(searchDto);
            Integer totalCount = postService.getPostCount(searchDto);

            Map<String, Object> result = Map.of(
                    "posts", posts,
                    "totalCount", totalCount,
                    "currentPage", safePage,
                    "totalPages", (totalCount + safeSize - 1) / safeSize
            );

            return ResponseEntity.ok(new Response<>(true, "게시글 목록 조회 성공", result));
        } catch (Exception e) {
            log.error("게시글 목록 조회 실패: ", e);
            return ResponseEntity.badRequest()
                    .body(new Response<>(false, "게시글 목록 조회 실패: " + e.getMessage(), null));
        }
    }

    @Operation(
            summary = "게시글 상세 조회",
            description = "게시글 ID로 상세 정보를 조회합니다. 조회 시 조회수가 1 증가합니다. (로그인 불필요)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PostDto.class))
            ),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Response<PostDto>> getPost(
            @Parameter(description = "게시글 ID") @PathVariable Long id) {
        try {
            PostDto post = postService.getPost(id);
            if (post != null) {
                return ResponseEntity.ok(new Response<>(true, "게시글 조회 성공", post));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("게시글 조회 실패: ", e);
            return ResponseEntity.badRequest()
                    .body(new Response<>(false, "게시글 조회 실패: " + e.getMessage(), null));
        }
    }

    @Operation(
            summary = "사용자 본인 게시글 목록 조회",
            description = "로그인한 사용자가 작성한 게시글 목록을 조회합니다. (로그인 필요)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/my")
    public ResponseEntity<Response<Map<String, Object>>> getMyPosts(
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") Integer size,
            Authentication auth) {
        try {
            Integer authorMno = extractUserMnoFromAuth(auth);

            List<PostDto> posts = postService.getPostsByAuthor(authorMno, page, size);
            Integer totalCount = postService.getPostCountByAuthor(authorMno);

            Map<String, Object> result = Map.of(
                    "posts", posts,
                    "totalCount", totalCount,
                    "currentPage", page,
                    "totalPages", (totalCount + size - 1) / size
            );

            return ResponseEntity.ok(new Response<>(true, "내 게시글 조회 성공", result));
        } catch (Exception e) {
            log.error("내 게시글 조회 실패: ", e);
            return ResponseEntity.badRequest()
                    .body(new Response<>(false, "내 게시글 조회 실패: " + e.getMessage(), null));
        }
    }

    @Operation(
            summary = "게시글 작성",
            description = "새 게시글을 작성합니다. (로그인 필요)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "작성 성공"),
            @ApiResponse(responseCode = "400", description = "작성 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping
    public ResponseEntity<Response<Long>> createPost(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "게시글 작성 정보",
                    content = @Content(schema = @Schema(implementation = PostDto.class))
            )
            @RequestBody PostDto post,
            Authentication auth) {
        try {
            // 입력값 검증
            if (post.getTitle() == null || post.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new Response<>(false, "제목은 필수입니다.", null));
            }
            if (post.getContent() == null || post.getContent().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new Response<>(false, "내용은 필수입니다.", null));
            }
            if (post.getCategory() == null || post.getCategory().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new Response<>(false, "카테고리는 필수입니다.", null));
            }

            // JWT에서 사용자 정보 추출
            Integer authorMno = extractUserMnoFromAuth(auth);
            String authorName = extractUserNameFromAuth(auth);

            post.setAuthorMno(authorMno);
            post.setAuthorName(authorName);

            log.debug("authorMno: {}, authorName: {}", authorMno, authorName);

            Long postId = postService.createPost(post);
            return ResponseEntity.ok(new Response<>(true, "게시글 작성 성공", postId));
        } catch (Exception e) {
            log.error("게시글 작성 실패: ", e);
            return ResponseEntity.badRequest()
                    .body(new Response<>(false, "게시글 작성 실패: " + e.getMessage(), null));
        }
    }

    @Operation(
            summary = "게시글 수정",
            description = "게시글을 수정합니다. 작성자만 수정 가능합니다. (로그인 필요)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "수정 실패"),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Response<Void>> updatePost(
            @Parameter(description = "게시글 ID") @PathVariable Long id,
            @RequestBody PostDto post,
            Authentication auth) {
        try {
            Integer authorMno = extractUserMnoFromAuth(auth);

            // 작성자 확인
            PostDto existingPost = postService.getPostForUpdate(id);
            if (existingPost == null) {
                return ResponseEntity.notFound().build();
            }

            if (!existingPost.getAuthorMno().equals(authorMno)) {
                return ResponseEntity.status(403)
                        .body(new Response<>(false, "수정 권한이 없습니다.", null));
            }

            post.setId(id);
            postService.updatePost(post);
            return ResponseEntity.ok(new Response<>(true, "게시글 수정 성공", null));
        } catch (Exception e) {
            log.error("게시글 수정 실패: ", e);
            return ResponseEntity.badRequest()
                    .body(new Response<>(false, "게시글 수정 실패: " + e.getMessage(), null));
        }
    }

    @Operation(
            summary = "게시글 삭제",
            description = "게시글을 삭제합니다. 작성자만 삭제 가능합니다. (로그인 필요)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "삭제 실패"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> deletePost(
            @Parameter(description = "게시글 ID") @PathVariable Long id,
            Authentication auth) {
        try {
            Integer authorMno = extractUserMnoFromAuth(auth);

            // 작성자 확인
            PostDto existingPost = postService.getPostForUpdate(id);
            if (existingPost == null) {
                return ResponseEntity.notFound().build();
            }

            if (!existingPost.getAuthorMno().equals(authorMno)) {
                return ResponseEntity.status(403)
                        .body(new Response<>(false, "삭제 권한이 없습니다.", null));
            }

            postService.deletePost(id);
            return ResponseEntity.ok(new Response<>(true, "게시글 삭제 성공", null));
        } catch (Exception e) {
            log.error("게시글 삭제 실패: ", e);
            return ResponseEntity.badRequest()
                    .body(new Response<>(false, "게시글 삭제 실패: " + e.getMessage(), null));
        }
    }

    @Operation(
            summary = "게시글 좋아요/싫어요",
            description = "게시글에 좋아요 또는 싫어요를 표시합니다. (로그인 필요)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "반응 처리 성공"),
            @ApiResponse(responseCode = "400", description = "반응 처리 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/{id}/reaction")
    public ResponseEntity<Response<Void>> reactToPost(
            @Parameter(description = "게시글 ID") @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            // 로그인 사용자만 좋아요/싫어요 가능
            Integer userMno = extractUserMnoFromAuth(auth);
            Boolean isLike = (Boolean) request.get("isLike");

            if (isLike == null) {
                return ResponseEntity.badRequest()
                        .body(new Response<>(false, "isLike 파라미터가 필요합니다.", null));
            }

            // TODO: 중복 방지 로직 추가 (user_mno + post_id 체크)
            postService.likePost(id, isLike);
            return ResponseEntity.ok(new Response<>(true, "반응 처리 성공", null));
        } catch (Exception e) {
            log.error("반응 처리 실패: ", e);
            return ResponseEntity.badRequest()
                    .body(new Response<>(false, "반응 처리 실패: " + e.getMessage(), null));
        }
    }

    @Operation(
            summary = "게시글 검색",
            description = "제목, 내용, 태그로 게시글을 검색합니다. (로그인 불필요)"
    )
    @GetMapping("/search")
    public ResponseEntity<Response<Map<String, Object>>> searchPosts(
            @Parameter(description = "검색어") @RequestParam String keyword,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "12") Integer size) {

        try {
            int safePage = (page == null) ? 0 : page;
            int safeSize = (size == null) ? 12 : size;
            int offset = safePage * safeSize;

            PostSearchDto searchDto = PostSearchDto.builder()
                    .searchKeyword(keyword)
                    .page(safePage)
                    .size(safeSize)
                    .offset(offset)
                    .sortBy("createdAt")
                    .sortDirection("DESC")
                    .build();

            List<PostDto> posts = postService.getPosts(searchDto);
            Integer totalCount = postService.getPostCount(searchDto);

            Map<String, Object> result = Map.of(
                    "posts", posts,
                    "totalCount", totalCount,
                    "currentPage", safePage,
                    "totalPages", (totalCount + safeSize - 1) / safeSize,
                    "keyword", keyword
            );

            return ResponseEntity.ok(new Response<>(true, "검색 성공", result));
        } catch (Exception e) {
            log.error("게시글 검색 실패: ", e);
            return ResponseEntity.badRequest()
                    .body(new Response<>(false, "검색 실패: " + e.getMessage(), null));
        }
    }

    // JWT에서 사용자 정보 추출 헬퍼 메서드들
    private Integer extractUserMnoFromAuth(Authentication auth) {
        if (auth == null) return null;
        try {
            log.debug("auth.getPrincipal(): {}", auth.getPrincipal());
            log.debug("auth.getCredentials(): {}", auth.getCredentials());

            Object details = auth.getPrincipal();
            if (details instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                if (userDetails instanceof com.ssafy.home.domain.User user) {
                    return user.getMno();
                }
                if (userDetails instanceof com.ssafy.home.dto.CustomUserDetails customUserDetails) {
                    return customUserDetails.getMno();
                }
            }
            // JWT Claims에서 직접 추출
            if (auth.getCredentials() instanceof String token) {
                Claims claims = jwtUtil.getClaims(token);
                Object mnoObj = claims.get("mno");
                if (mnoObj != null) {
                    return Integer.parseInt(mnoObj.toString());
                }
            }
        } catch (Exception e) {
            log.warn("extractUserMnoFromAuth 실패", e);
        }
        return null;
    }

    private String extractUserNameFromAuth(Authentication auth) {
        if (auth == null) return null;
        try {
            Object details = auth.getPrincipal();
            if (details instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                if (userDetails instanceof com.ssafy.home.domain.User user) {
                    return user.getName();
                }
                if (userDetails instanceof com.ssafy.home.dto.CustomUserDetails customUserDetails) {
                    return customUserDetails.getName();
                }
            }
            // JWT Claims에서 직접 추출
            if (auth.getCredentials() instanceof String token) {
                Claims claims = jwtUtil.getClaims(token);
                Object nameObj = claims.get("name");
                if (nameObj != null) {
                    return nameObj.toString();
                }
            }
        } catch (Exception e) {
            log.warn("extractUserNameFromAuth 실패", e);
        }
        return null;
    }
}
