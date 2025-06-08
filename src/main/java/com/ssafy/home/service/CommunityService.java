package com.ssafy.home.service;

import com.ssafy.home.dto.PostDto;
import com.ssafy.home.dto.PostSearchDto;
import com.ssafy.home.dto.CommentDto;
import com.ssafy.home.ai.GPT4MiniClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityService {

    private final PostService postService;
    private final GPT4MiniClient gptClient;

    /**
     * 아파트별 커뮤니티 정보 조회 및 요약
     */
    public String getApartmentCommunityInfo(String aptSeq) {
        try {
            // 🔥 수정: 올바른 PostSearchDto 빌더 패턴 사용
            PostSearchDto searchDto = PostSearchDto.builder()
                    .searchKeyword(aptSeq != null && !aptSeq.trim().isEmpty() ? aptSeq.trim() : "") // 🔥 null 체크 강화
                    .page(0)
                    .size(10)
                    .offset(0)                    // 🔥 offset 명시적 설정
                    .sortBy("createdAt")          // 🔥 필드명 수정
                    .sortDirection("DESC")        // 🔥 정렬 방향 수정
                    .build();

            List<PostDto> apartmentPosts = postService.getPosts(searchDto);

            if (apartmentPosts.isEmpty()) {
                return "커뮤니티 정보: 등록된 후기가 없습니다.";
            }

            // 2. 각 게시글의 댓글 조회 및 종합
            List<CommentDto> allComments = apartmentPosts.stream()
                    .flatMap(post -> postService.getComments(post.getId()).stream())
                    .collect(Collectors.toList());

            // 3. 간략한 요약 생성 (AI 호출 최소화)
            return generateBriefSummary(apartmentPosts, allComments);

        } catch (Exception e) {
            log.error("아파트 커뮤니티 정보 조회 실패 - aptSeq: {}", aptSeq, e);
            return "커뮤니티 정보: 조회 중 오류가 발생했습니다.";
        }
    }

    /**
     * 아파트 관련 최신 게시글 조회 (제목에 아파트명 포함)
     */
    public String getApartmentCommunityInfoByName(String aptName) {
        try {
            // 🔥 수정: 올바른 PostSearchDto 빌더 패턴 사용
            PostSearchDto searchDto = PostSearchDto.builder()
                    .searchKeyword(aptName != null && !aptName.trim().isEmpty() ? aptName.trim() : "") // 🔥 null 체크 강화
                    .page(0)
                    .size(5)
                    .offset(0)                    // 🔥 offset 명시적 설정
                    .sortBy("createdAt")          // 🔥 필드명 수정
                    .sortDirection("DESC")        // 🔥 정렬 방향 수정
                    .build();

            List<PostDto> apartmentPosts = postService.getPosts(searchDto);

            if (apartmentPosts.isEmpty()) {
                return "커뮤니티 정보: 해당 아파트 관련 후기가 없습니다.";
            }

            // 댓글 정보도 함께 수집
            List<CommentDto> allComments = apartmentPosts.stream()
                    .flatMap(post -> postService.getComments(post.getId()).stream())
                    .limit(10) // 댓글은 최대 10개만
                    .collect(Collectors.toList());

            // 간략한 요약 생성
            return generateBriefSummary(apartmentPosts, allComments);

        } catch (Exception e) {
            log.error("아파트 커뮤니티 정보 조회 실패 - aptName: {}", aptName, e);
            return "커뮤니티 정보: 조회 중 오류가 발생했습니다.";
        }
    }

    /**
     * 간단한 통계 기반 요약 (AI 호출 최소화)
     */
    private String generateBriefSummary(List<PostDto> posts, List<CommentDto> comments) {
        StringBuilder sb = new StringBuilder();
        sb.append("커뮤니티 정보:\n");

        // 게시글 통계
        sb.append("- 최근 게시글: ").append(posts.size()).append("개");

        if (!posts.isEmpty()) {
            // 평균 좋아요 수 계산
            double avgLikes = posts.stream()
                    .mapToInt(PostDto::getLikes)
                    .average()
                    .orElse(0.0);

            if (avgLikes > 0) {
                sb.append(", 평균 좋아요: ").append(String.format("%.1f개", avgLikes));
            }
        }

        // 댓글 통계
        if (!comments.isEmpty()) {
            sb.append("\n- 댓글: ").append(comments.size()).append("개");
        }

        // 대표 게시글 제목
        if (!posts.isEmpty()) {
            PostDto topPost = posts.stream()
                    .max((p1, p2) -> Integer.compare(p1.getLikes() + p1.getViews(), p2.getLikes() + p2.getViews()))
                    .orElse(posts.get(0));

            sb.append("\n- 인기 글: ").append(topPost.getTitle().length() > 30 ?
                    topPost.getTitle().substring(0, 27) + "..." : topPost.getTitle());
        }

        return sb.toString();
    }
}
