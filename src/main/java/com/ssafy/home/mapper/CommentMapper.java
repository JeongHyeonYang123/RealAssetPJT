/*
 * [프로젝트 구조/분석]
 * - 댓글(Comment) 관련 DB 접근(MyBatis Mapper)
 * - CRUD, 대댓글, 댓글수 등 지원
 * [확장 포인트]
 * - 대댓글, 첨부파일, 신고 등 추가 가능
 * [주의사항]
 * - Soft Delete(논리 삭제) 방식, 실제 삭제 아님
 */
// 댓글 생성
// 댓글 단건 조회
// 게시글별 댓글 목록 조회(대댓글 포함)
// 작성자별 댓글 목록 조회
// 댓글 수정
// 댓글 삭제(Soft Delete)
// 댓글 수 조회(게시글별/작성자별)

package com.ssafy.home.mapper;

import com.ssafy.home.dto.CommentDto;
import com.ssafy.home.dto.CommentWithPostDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CommentMapper {

    // 댓글 CRUD
    void insertComment(CommentDto comment);

    CommentDto selectCommentById(@Param("id") Long id);

    List<CommentDto> selectCommentsByPostId(@Param("postId") Long postId);

    List<CommentDto> selectCommentsByAuthor(@Param("authorMno") Integer authorMno,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit);

    void updateComment(CommentDto comment);

    void deleteComment(@Param("id") Long id);

    // 댓글 수 조회
    Integer countCommentsByPostId(@Param("postId") Long postId);

    Integer countCommentsByAuthor(@Param("authorMno") Integer authorMno);

    // 사용자가 작성한 댓글을 게시글 정보와 함께 조회
    List<CommentWithPostDto> selectUserCommentsWithPost(
        @Param("authorMno") Integer authorMno,
        @Param("offset") Integer offset,
        @Param("size") Integer size
    );
}