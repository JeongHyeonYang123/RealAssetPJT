package com.ssafy.home.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CommentWithPostDto {
    // 댓글 정보
    private Long id;
    private String content;
    private Integer authorMno;
    private String authorName;
    private String createdAt;
    private String updatedAt;
    
    // 게시글 정보
    private Long postId;
    private String postTitle;
    private String postCategory;
    private Integer postAuthorMno;
    private String postAuthorName;
    private String postContent;
} 