/*
 * [프로젝트 구조/분석]
 * - 게시글 목록 검색/필터/정렬/페이지네이션 요청 DTO
 * - 컨트롤러-서비스-매퍼 계층 간 데이터 전달에 사용
 * [확장 포인트]
 * - 태그 검색, 날짜 범위 등 추가 가능
 * [주의사항]
 * - sortBy, sortDirection 값 유효성 주의
 */
// category: 카테고리 필터
// searchKeyword: 통합 검색어(제목, 내용, 작성자)
// authorMno: 작성자 회원번호
// page, size: 페이지네이션
// sortBy: 정렬 기준(createdAt, views, likes, comments)
// sortDirection: 정렬 방향(ASC, DESC)

package com.ssafy.home.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostSearchDto {
    private String category;
    private String searchKeyword;
    private Integer authorMno;
    private Integer page;
    private Integer size;
    private Integer offset;

    @Builder.Default
    private String sortBy = "createdAt";

    @Builder.Default
    private String sortDirection = "DESC";

    // 🔥 null 안전 getter 메서드들 추가
    public String getCategory() {
        return category != null ? category : "";
    }

    public String getSearchKeyword() {
        return searchKeyword != null ? searchKeyword : "";
    }

    public String getSortBy() {
        return sortBy != null ? sortBy : "createdAt";
    }

    public String getSortDirection() {
        return sortDirection != null ? sortDirection : "DESC";
    }

    public Integer getPage() {
        return page != null ? page : 0;
    }

    public Integer getSize() {
        return size != null ? size : 10;
    }

    public Integer getOffset() {
        if (offset != null) return offset;
        if (page != null && size != null) {
            return page * size;
        }
        return 0;
    }
}