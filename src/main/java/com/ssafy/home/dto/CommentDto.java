/*
 * [프로젝트 구조/분석]
 * - 댓글(Comment) 정보를 담는 DTO로, 컨트롤러-서비스-매퍼 계층 간 데이터 전달에 사용
 * - 대댓글(parentId, depth) 구조 지원
 * - Soft Delete(isDeleted) 지원
 * [확장 포인트]
 * - 대댓글 UI, 첨부파일 등 확장 가능
 * - 권한/인증 정보 추가 가능
 * [주의사항]
 * - LocalDateTime 직렬화 포맷 일치 필요
 */
// id: 댓글 고유 ID
// postId: 소속 게시글 ID
// authorMno: 작성자 회원번호
// authorName: 작성자 이름
// content: 댓글 내용
// parentId: 부모 댓글 ID(대댓글용)
// depth: 댓글 깊이(0=일반, 1=대댓글)
// createdAt, updatedAt: 생성/수정 시각
// likes: 좋아요 수
// isDeleted: 삭제 여부(Soft Delete)

package com.ssafy.home.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {
    private Long id;
    private Long postId;
    private Integer authorMno;
    private String authorName;
    private String content;
    private Long parentId;

    @Builder.Default
    private Integer depth = 0;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @Builder.Default
    private Integer likes = 0;

    @Builder.Default
    private Boolean isDeleted = false;
}