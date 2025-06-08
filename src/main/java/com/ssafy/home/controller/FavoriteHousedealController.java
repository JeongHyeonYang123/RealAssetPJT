package com.ssafy.home.controller;

import com.ssafy.home.dto.FavoriteHousedealDto;
import com.ssafy.home.service.FavoriteHousedealService;
import com.ssafy.home.dto.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/favorites")
@Tag(name = "관심매물 API", description = "찜한 매물 관리 API")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class FavoriteHousedealController {
    private final FavoriteHousedealService service;

    public FavoriteHousedealController(FavoriteHousedealService service) {
        this.service = service;
    }

    @Operation(summary = "관심매물 등록", description = "관심매물(찜) 정보를 등록합니다. (userMno는 JWT에서 추출)")
    @PostMapping
    public int create(@RequestBody FavoriteHousedealDto dto, @AuthenticationPrincipal CustomUserDetails userDetails) {
        dto.setUserMno(userDetails.getMno());
        return service.create(dto);
    }

    @Operation(summary = "관심매물 전체 조회", description = "내 관심매물(찜) 정보를 모두 조회합니다.")
    @GetMapping
    public List<FavoriteHousedealDto> getAll(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return service.getAllByUserMno(userDetails.getMno());
    }

    @Operation(summary = "관심매물 상세 조회", description = "내 특정 관심매물(찜) 정보를 조회합니다.")
    @GetMapping("/{id}")
    public FavoriteHousedealDto getById(@Parameter(description = "관심매물 PK", example = "1") @PathVariable int id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return service.getByIdAndUserMno(id, userDetails.getMno());
    }

    @Operation(summary = "관심매물 삭제", description = "내 특정 관심매물(찜) 정보를 삭제(소프트 삭제)합니다.")
    @DeleteMapping("/{id}")
    public int deleteById(@Parameter(description = "관심매물 PK", example = "1") @PathVariable int id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return service.deleteByIdAndUserMno(id, userDetails.getMno());
    }

    @Operation(summary = "관심매물 전체 개수 반환", description = "내 관심매물(찜) 개수를 반환합니다.")
    @GetMapping("/count")
    public int countAll(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return service.countAllByUserMno(userDetails.getMno());
    }
}