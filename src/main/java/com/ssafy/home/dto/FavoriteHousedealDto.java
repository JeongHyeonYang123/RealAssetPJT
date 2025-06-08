package com.ssafy.home.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관심매물(찜) DTO")
public class FavoriteHousedealDto {
    @Schema(description = "관심매물 PK", example = "1")
    private int id;
    @Schema(description = "사용자 mno", example = "10")
    private int userMno;
    @Schema(description = "아파트 no", example = "100")
    private String aptSeq;
    @Schema(description = "찜한 시각", example = "2024-05-30T12:34:56")
    private LocalDateTime createdAt;
    @Schema(description = "삭제 여부", example = "false")
    private boolean deleted;
}