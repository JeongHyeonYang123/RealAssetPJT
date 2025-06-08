package com.ssafy.home.controller;

import com.ssafy.home.domain.InterestArea;
import com.ssafy.home.dto.CustomUserDetails;
import com.ssafy.home.dto.Response;
import com.ssafy.home.dto.UserDetailsDTO;
import com.ssafy.home.service.InterestAreaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interest-areas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "관심지역 API", description = "관심지역(즐겨찾기) 관리 기능 제공")
public class InterestAreaController {
    private final InterestAreaService interestAreaService;

    @Operation(summary = "관심지역 등록", description = "관심지역을 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "관심지역 등록 성공"),
            @ApiResponse(responseCode = "400", description = "관심지역 등록 실패")
    })
    @PostMapping
    public ResponseEntity<Response<Void>> saveInterestArea(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "등록할 관심지역 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = InterestArea.class))
            )
            @RequestBody InterestArea interestArea,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // JWT에서 유저 정보 추출하여 userId 세팅
        interestArea.setMno(userDetails.getUser().getMno());
        interestArea.setDongCode(interestArea.getDongCode() + "00");
        boolean ok = interestAreaService.save(interestArea) == 1;

        if (!ok) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new Response<>(false, "관심지역 등록에 실패하였습니다.", null));
        }

        return ResponseEntity.ok(new Response<>(true, "관심지역 등록에 성공하였습니다.", null));
    }

    @Operation(summary = "나의 관심 지역 조회", description = "나의 관심 지역을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "관심지역 조회 성공"),
            @ApiResponse(responseCode = "400", description = "관심지역 조회 실패")
    })
    @GetMapping("/my")
    public ResponseEntity<Response<List<InterestArea>>> getMyInterestAreas(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<InterestArea> interestArea = interestAreaService.findByMno(userDetails.getUser().getMno());
        return ResponseEntity.ok(new Response<>(true, "관심지역 리스트", interestArea));
    }

    @Operation(summary = "관심지역 전체 조회", description = "등록된 모든 관심지역을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "관심지역 리스트 조회 성공")
    })
    @GetMapping
    public ResponseEntity<Response<List<InterestArea>>> getInterestAreas() {
        List<InterestArea> interestAreas = interestAreaService.findAll();
        return ResponseEntity.ok(new Response<>(true, "관심지역 리스트", interestAreas));
    }

    @Operation(summary = "관심지역 삭제", description = "관심지역을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "관심지역 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "관심지역 삭제 실패")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> deleteInterestArea(
            @Parameter(description = "관심지역 번호", example = "1")
            @PathVariable int id) {
        boolean ok = interestAreaService.delete(id) == 1;

        if (!ok) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new Response<>(false, "관심지역 삭제에 실패하였습니다.", null));
        }

        return ResponseEntity.ok(new Response<>(true, "관심지역 삭제에 성공하였습니다.", null));
    }
}
