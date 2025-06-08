package com.ssafy.home.controller;

import com.ssafy.home.domain.StoreInfo;
import com.ssafy.home.dto.Response;
import com.ssafy.home.service.StoreInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/commercial-areas")
@Tag(name = "상권정보 API", description = "상권 정보 제공")
public class CommercialAreaController {
    private final StoreInfoService storeInfoService;

    @Operation(
            summary = "상권 정보 조회",
            description = "동 코드(dongCode)를 이용해 해당 지역의 상권(StoreInfo) 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상권 정보 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 동 코드의 상권 정보 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{dongCode}")
    public ResponseEntity<Response<List<StoreInfo>>> getBusinessArea(
            @Parameter(description = "법정동 코드", example = "1111010100", required = true)
            @PathVariable String dongCode) {
        List<StoreInfo> businessArea = storeInfoService.getStoreInfos(dongCode);
        return ResponseEntity.ok(new Response<>(true, "success", businessArea));
    }
}
