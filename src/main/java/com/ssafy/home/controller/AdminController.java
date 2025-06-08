package com.ssafy.home.controller;

import com.ssafy.home.domain.HouseDeal;
import com.ssafy.home.domain.User;
import com.ssafy.home.dto.Response;
import com.ssafy.home.service.DataUpdateService;
import com.ssafy.home.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
@Tag(name = "관리자 API", description = "관리자 기능 제공")
public class AdminController {
    private final UserService userService;
    private final DataUpdateService dataUpdateService;

    // 회원 관리 API
    // ===============================

    // READ (전체 회원 조회)
    @Operation(summary = "회원 리스트 조회", description = "전체 회원 목록을 조회합니다. 키워드로 검색도 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/users")
    public ResponseEntity<Response<List<User>>> getAllUsers(
            @Parameter(description = "검색 키워드(선택)", required = false)
            @RequestParam(required = false) String keyword) {
        List<User> users = (keyword != null && !keyword.isBlank())
                ? userService.searchByKeyword(keyword)
                : userService.findAll();

        return ResponseEntity.ok(new Response<>(true, "회원 리스트", users));
    }

    // DELETE
    @Operation(summary = "회원 삭제", description = "회원 번호로 회원을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "삭제 실패")
    })
    @DeleteMapping("/users/{mno}")
    public ResponseEntity<Response<Void>> deleteUser(
            @Parameter(description = "회원 번호", example = "1")
            @PathVariable int mno) {
        boolean ok = userService.deleteByMno(mno) == 1;

        if (!ok) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new Response<>(false, "회원 삭제 실패", null));
        }

        return ResponseEntity.ok(new Response<>(true, "회원 삭제", null));
    }

    // 부동산 데이터 관리 API
    // ===============================

    @Operation(summary = "지역별 월별 거래 조회", description = "법정동코드와 거래연월로 데이터 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/deals")
    public ResponseEntity<Response<List<HouseDeal>>> getDeals(
            @Parameter(description = "법정동코드 (예: 11680)", example = "11680")
            @RequestParam String lawdCd,
            @Parameter(description = "거래연월 (YYYYMM)", example = "202405")
            @RequestParam String dealYmd) {
        List<HouseDeal> deals = dataUpdateService.getDealsByRegionAndMonth(lawdCd, dealYmd);
        return ResponseEntity.ok(new Response<>(true, "거래 조회 성공", deals));
    }

    @Operation(summary = "전체 지역 월별 거래 조회", description = "특정 월의 전체 거래 데이터 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/deals/month/{dealYmd}")
    public ResponseEntity<Response<List<HouseDeal>>> getDealsByMonth(
            @Parameter(description = "거래연월 (YYYYMM)", example = "202405")
            @PathVariable String dealYmd) {
        List<HouseDeal> deals = dataUpdateService.getAllDealsByMonth(dealYmd);
        return ResponseEntity.ok(new Response<>(true, "월별 전체 거래 조회", deals));
    }

    @Operation(summary = "최근 거래 조회", description = "최근 N건의 거래 데이터 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/deals/recent")
    public ResponseEntity<Response<List<HouseDeal>>> getRecentDeals(
            @Parameter(description = "조회 건수", example = "100")
            @RequestParam(defaultValue = "100") int limit) {
        List<HouseDeal> deals = dataUpdateService.getRecentDeals(limit);
        return ResponseEntity.ok(new Response<>(true, "최근 거래 조회", deals));
    }
}
