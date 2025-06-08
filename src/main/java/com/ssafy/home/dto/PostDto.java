/*
 * [프로젝트 구조/분석]
 * - 게시글(Post) 정보를 담는 DTO로, 컨트롤러-서비스-매퍼 계층 간 데이터 전달에 사용
 * - Lombok(@Data, @Builder 등)으로 코드 간결화
 * - 태그(tags)는 JSON 배열로 DB에 저장/조회
 * - Soft Delete(isDeleted) 지원
 * - 통계(views, likes, dislikes, commentsCount) 필드 포함
 * [확장 포인트]
 * - 태그 검색, 첨부파일, 이미지 등 확장 가능
 * - 권한/인증 정보 추가 가능
 * [주의사항]
 * - LocalDateTime 직렬화 포맷 일치 필요
 * - 통계 필드 동기화 주의
 */
// id: 게시글 고유 ID
// authorMno: 작성자 회원번호
// authorName: 작성자 이름
// category: 게시글 카테고리
// title: 제목
// content: 내용
// tags: 태그 목록(JSON 배열)
// createdAt, updatedAt: 생성/수정 시각
// views: 조회수
// likes: 좋아요 수
// dislikes: 싫어요 수
// commentsCount: 댓글 수
// isDeleted: 삭제 여부(Soft Delete)

package com.ssafy.home.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDto {
    private Long id;
    private Integer authorMno;
    private String authorName;
    private String category;
    private String title;
    private String content;
    private List<String> tags;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @Builder.Default
    private Integer views = 0;

    @Builder.Default
    private Integer likes = 0;

    @Builder.Default
    private Integer dislikes = 0;

    @Builder.Default
    private Integer commentsCount = 0;

    @Builder.Default
    private Boolean isDeleted = false;
}