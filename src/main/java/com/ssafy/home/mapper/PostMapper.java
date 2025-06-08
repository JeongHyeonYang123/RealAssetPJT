/*
 * [프로젝트 구조/분석]
 * - 게시글(Post) 관련 DB 접근(MyBatis Mapper)
 * - CRUD, 통계, 검색/정렬/필터/페이지네이션 등 지원
 * [확장 포인트]
 * - 태그 검색, 첨부파일, 신고 등 추가 가능
 * [주의사항]
 * - Soft Delete(논리 삭제) 방식, 실제 삭제 아님
 */
// 게시글 생성
// 게시글 단건 조회
// 게시글 목록 조회(검색/필터/정렬/페이지네이션)
// 작성자별 게시글 목록 조회
// 게시글 수정
// 게시글 삭제(Soft Delete)
// 조회수 증가
// 좋아요/싫어요/댓글수 업데이트
// 게시글 수 조회(전체/작성자별)

package com.ssafy.home.mapper;

import com.ssafy.home.dto.PostDto;
import com.ssafy.home.dto.PostSearchDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PostMapper {

    // 게시글 CRUD
    void insertPost(PostDto post);

    PostDto selectPostById(@Param("id") Long id);

    List<PostDto> selectPosts(PostSearchDto searchDto);

    List<PostDto> selectPostsByAuthor(@Param("authorMno") Integer authorMno,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit);

    void updatePost(PostDto post);

    void deletePost(@Param("id") Long id);

    // 통계 업데이트
    void incrementViews(@Param("id") Long id);

    void updateLikes(@Param("id") Long id, @Param("likes") Integer likes);

    void updateDislikes(@Param("id") Long id, @Param("dislikes") Integer dislikes);

    void updateCommentsCount(@Param("id") Long id, @Param("count") Integer count);

    // 게시글 수 조회
    Integer countPosts(PostSearchDto searchDto);

    Integer countPostsByAuthor(@Param("authorMno") Integer authorMno);
}