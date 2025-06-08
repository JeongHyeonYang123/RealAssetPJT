/*
 * [프로젝트 구조/분석]
 * - 게시글/댓글 비즈니스 로직 담당 서비스 계층
 * - 트랜잭션 관리, 통계 동기화, 권한 체크 등
 * [확장 포인트]
 * - 조회수/좋아요/싫어요 중복 방지(유저별 처리) 필요
 * - 태그 검색, 대댓글, 첨부파일 등 확장 가능
 * [주의사항]
 * - Soft Delete, 통계 동기화 주의
 */
// ... existing code ...
// 각 메서드별 역할/TODO 주석 추가
// createPost: 게시글 생성
// getPost: 게시글 상세 조회(조회수 증가)
// getPostForUpdate: 수정용 게시글 조회(조회수 증가X)
// getPosts: 게시글 목록 조회
// getPostsByAuthor: 사용자 본인 게시글 조회
// updatePost: 게시글 수정
// deletePost: 게시글 삭제(Soft Delete)
// likePost: 게시글 좋아요/싫어요 (TODO: 중복 방지 필요)
// getPostCount: 게시글 수 조회
// getPostCountByAuthor: 사용자별 게시글 수 조회
// createComment: 댓글 생성(댓글수 동기화)
// getComments: 댓글 목록 조회
// getCommentsByAuthor: 사용자별 댓글 조회
// getCommentForUpdate: 수정용 댓글 조회
// updateComment: 댓글 수정
// deleteComment: 댓글 삭제(댓글수 동기화)
// getCommentCountByAuthor: 사용자별 댓글 수 조회
// ... existing code ...

package com.ssafy.home.service;

import com.ssafy.home.dto.PostDto;
import com.ssafy.home.dto.PostSearchDto;
import com.ssafy.home.dto.CommentDto;
import com.ssafy.home.dto.CommentWithPostDto;
import com.ssafy.home.mapper.PostMapper;
import com.ssafy.home.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    // 게시글 생성
    public Long createPost(PostDto post) {
        postMapper.insertPost(post);
        return post.getId();
    }

    // 게시글 조회 (조회수 증가)
    @Transactional
    public PostDto getPost(Long id) {
        PostDto post = postMapper.selectPostById(id);
        if (post != null) {
            postMapper.incrementViews(id);
            post.setViews(post.getViews() + 1);
        }
        return post;
    }

    // 수정용 게시글 조회 (조회수 증가하지 않음)
    public PostDto getPostForUpdate(Long id) {
        return postMapper.selectPostById(id);
    }

    // 게시글 목록 조회
    public List<PostDto> getPosts(PostSearchDto searchDto) {
        return postMapper.selectPosts(searchDto);
    }

    // 사용자 본인 게시글 조회
    public List<PostDto> getPostsByAuthor(Integer authorMno, Integer page, Integer size) {
        Integer offset = page * size;
        return postMapper.selectPostsByAuthor(authorMno, offset, size);
    }

    // 게시글 수정
    public void updatePost(PostDto post) {
        postMapper.updatePost(post);
    }

    // 게시글 삭제
    public void deletePost(Long id) {
        postMapper.deletePost(id);
    }

    // 게시글 좋아요/싫어요
    public void likePost(Long id, boolean isLike) {
        PostDto post = postMapper.selectPostById(id);
        if (post != null) {
            if (isLike) {
                postMapper.updateLikes(id, post.getLikes() + 1);
            } else {
                postMapper.updateDislikes(id, post.getDislikes() + 1);
            }
        }
    }

    // 게시글 수 조회
    public Integer getPostCount(PostSearchDto searchDto) {
        return postMapper.countPosts(searchDto);
    }

    // 사용자별 게시글 수 조회
    public Integer getPostCountByAuthor(Integer authorMno) {
        return postMapper.countPostsByAuthor(authorMno);
    }

    // 댓글 생성
    public Long createComment(CommentDto comment) {
        commentMapper.insertComment(comment);

        // 게시글 댓글 수 업데이트
        Integer commentCount = commentMapper.countCommentsByPostId(comment.getPostId());
        postMapper.updateCommentsCount(comment.getPostId(), commentCount);

        return comment.getId();
    }

    // 댓글 목록 조회
    public List<CommentDto> getComments(Long postId) {
        return commentMapper.selectCommentsByPostId(postId);
    }

    // 사용자별 댓글 조회
    public List<CommentDto> getCommentsByAuthor(Integer authorMno, Integer page, Integer size) {
        Integer offset = page * size;
        return commentMapper.selectCommentsByAuthor(authorMno, offset, size);
    }

    // 수정용 댓글 조회
    public CommentDto getCommentForUpdate(Long id) {
        return commentMapper.selectCommentById(id);
    }

    // 댓글 수정
    public void updateComment(CommentDto comment) {
        commentMapper.updateComment(comment);
    }

    // 댓글 삭제
    public void deleteComment(Long id) {
        CommentDto comment = commentMapper.selectCommentById(id);
        if (comment != null) {
            commentMapper.deleteComment(id);

            // 게시글 댓글 수 업데이트
            Integer commentCount = commentMapper.countCommentsByPostId(comment.getPostId());
            postMapper.updateCommentsCount(comment.getPostId(), commentCount);
        }
    }

    // 사용자별 댓글 수 조회
    public Integer getCommentCountByAuthor(Integer authorMno) {
        return commentMapper.countCommentsByAuthor(authorMno);
    }

    public List<CommentWithPostDto> getMyCommentsWithPost(Integer authorMno, Integer page, Integer size) {
        return commentMapper.selectUserCommentsWithPost(authorMno, page * size, size);
    }
}