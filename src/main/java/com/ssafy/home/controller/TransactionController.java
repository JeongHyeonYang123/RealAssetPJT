package com.ssafy.home.controller;

import com.ssafy.home.domain.DongCode;
import com.ssafy.home.dto.*;
import com.ssafy.home.service.BasicService;
import com.ssafy.home.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transactions")
@Slf4j
@Tag(name = "거래 관련 API", description = "주택 거래 정보 조회 기능 제공")
public class TransactionController {
    private final BasicService basicService;
    private final PropertyService propertyService;

    @Operation(
            summary = "동코드로 주택 거래 정보 조회",
            description = "동코드를 이용해 주택 거래 정보를 페이지 단위로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = HouseSearchResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "조회 실패"
            )
    })
    @GetMapping("/{dongCode}")
    public ResponseEntity<Response<HouseSearchResponse>> getHouseDeals(
            @Parameter(description = "조회할 동코드", example = "12345678")
            @PathVariable String dongCode,
            @Parameter(description = "현재 페이지 번호", example = "1")
            @RequestParam(value = "currentPage", defaultValue = "1") int currentPage) {

        String normalizedDongCode = normalizeDongCode(dongCode);

        DongCode address = basicService.findDongByCode(normalizedDongCode);
        int pageSize = 10;

        PageDTO<HouseDongSearchDTO> pageResult = basicService.findPagedHousesByDongCode(normalizedDongCode, currentPage, pageSize);

        String baseAddress = buildBaseAddress(address);

        // 도로명 주소 가공
        pageResult.getList().forEach(house ->
                house.setRoadAddress(buildRoadAddress(baseAddress, house))
        );

        HouseSearchResponse response = new HouseSearchResponse(
                normalizedDongCode,
                pageResult.getList(),
                currentPage,
                pageResult.getTotalPages(),
                pageResult.getTotalRecords()
        );

        return ResponseEntity.ok(new Response<>(true, "success", response));
    }

    // --- 아래는 private helper 메소드들 ---

    private String normalizeDongCode(String dongCode) {
        return dongCode.length() < 10 ? dongCode + "00" : dongCode;
    }

    private String buildBaseAddress(DongCode address) {
        StringBuilder sb = new StringBuilder();
        sb.append(address.getSido()).append(" ").append(address.getGugun()).append(" ");
        String dong = address.getDong();
        if (!dong.endsWith("동")) {
            sb.append(dong).append(" ");
        }
        return sb.toString();
    }

    private String buildRoadAddress(String baseAddress, HouseDongSearchDTO house) {
        return baseAddress
                + house.getRoadNm() + " "
                + house.getRoadNmBonBun() + "-"
                + house.getRoadNmBuBun();
    }

    @GetMapping("/search")
    @Operation(
            summary = "아파트 거래내역 검색",
            description = """
            아파트명으로 거래내역을 검색하고 가격대, 면적, 정렬 조건으로 필터링합니다.
            
            **가격 필터:**
            - all: 전체
            - under1: 1억 이하
            - 1to3: 1억~3억
            - 3to5: 3억~5억
            - 5to10: 5억~10억
            - over10: 10억 이상
            
            **면적 필터:**
            - all: 전체
            - small: 60㎡ 이하
            - medium: 60㎡~85㎡
            - large: 85㎡ 초과
            
            **정렬 옵션:**
            - price-asc: 가격 낮은순
            - price-desc: 가격 높은순
            - date-desc: 최신순
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "검색 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Response.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 파라미터 누락, 잘못된 값 등)"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public Response<PropertySearchResponse> searchProperties(
            @Parameter(
                    description = "동코드",
                    required = true,
                    example = "11680103"
            )
            @RequestParam String dongCode,
            @Parameter(
                    description = "아파트명 (부분 검색 가능)",
                    required = true,
                    example = "청운현대"
            )
            @RequestParam String aptName,

            @Parameter(
                    description = "가격대 필터",
                    example = "all",
                    schema = @Schema(allowableValues = {"all", "under1", "1to3", "3to5", "5to10", "over10"})
            )
            @RequestParam(defaultValue = "all") String priceFilter,

            @Parameter(
                    description = "면적 필터",
                    example = "all",
                    schema = @Schema(allowableValues = {"all", "small", "medium", "large"})
            )
            @RequestParam(defaultValue = "all") String areaFilter,

            @Parameter(
                    description = "정렬 조건",
                    example = "date-desc",
                    schema = @Schema(allowableValues = {"price-asc", "price-desc", "date-desc"})
            )
            @RequestParam(defaultValue = "date-desc") String sortOrder,

            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "페이지 크기 (1~100)", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {

        try {
            // 입력값 검증
            if (dongCode == null || dongCode.trim().isEmpty()) {
                return new Response<>(false, "동코드을 입력해주세요.", null);
            }

            if (aptName == null || aptName.trim().isEmpty()) {
                return new Response<>(false, "아파트명을 입력해주세요.", null);
            }

            if (page < 1) {
                return new Response<>(false, "페이지 번호는 1 이상이어야 합니다.", null);
            }

            if (size < 1 || size > 100) {
                return new Response<>(false, "페이지 크기는 1~100 사이여야 합니다.", null);
            }

            PropertySearchRequest request = new PropertySearchRequest();
            request.setDongCode(dongCode.trim() + "00");
            request.setAptName(aptName.trim());
            request.setPriceFilter(priceFilter);
            request.setAreaFilter(areaFilter);
            request.setSortOrder(sortOrder);
            request.setPage(page);
            request.setSize(size);

            PropertySearchResponse result = propertyService.searchProperties(request);

            if (result.getProperties().isEmpty()) {
                return new Response<>(true, "검색 결과가 없습니다.", result);
            }

            return new Response<>(true,
                    String.format("총 %d건의 거래내역을 찾았습니다.", result.getTotalCount()),
                    result);

        } catch (IllegalArgumentException e) {
            return new Response<>(false, "잘못된 요청입니다: " + e.getMessage(), null);
        } catch (Exception e) {
            log.error("부동산 검색 중 오류 발생", e);
            return new Response<>(false, "검색 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", null);
        }
    }

    @GetMapping("/filters")
    @Operation(
            summary = "검색 필터 옵션 조회",
            description = """
            프론트엔드에서 사용할 수 있는 모든 필터 옵션들을 반환합니다.
            
            반환되는 옵션들:
            - priceOptions: 가격대 필터 옵션
            - areaOptions: 면적 필터 옵션
            - sortOptions: 정렬 옵션
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "옵션 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Response.class)
                    )
            )
    })
    public Response<Map<String, Object>> getFilterOptions() {
        Map<String, Object> filters = new HashMap<>();

        // 가격대 필터 옵션
        List<Map<String, String>> priceOptions = Arrays.asList(
                Map.of("value", "all", "label", "전체"),
                Map.of("value", "under1", "label", "1억 이하"),
                Map.of("value", "1to3", "label", "1억~3억"),
                Map.of("value", "3to5", "label", "3억~5억"),
                Map.of("value", "5to10", "label", "5억~10억"),
                Map.of("value", "over10", "label", "10억 이상")
        );

        // 면적 필터 옵션
        List<Map<String, String>> areaOptions = Arrays.asList(
                Map.of("value", "all", "label", "전체"),
                Map.of("value", "small", "label", "60㎡ 이하"),
                Map.of("value", "medium", "label", "60㎡~85㎡"),
                Map.of("value", "large", "label", "85㎡ 초과")
        );

        // 정렬 옵션
        List<Map<String, String>> sortOptions = Arrays.asList(
                Map.of("value", "price-asc", "label", "가격 낮은순"),
                Map.of("value", "price-desc", "label", "가격 높은순"),
                Map.of("value", "date-desc", "label", "최신순")
        );

        filters.put("priceOptions", priceOptions);
        filters.put("areaOptions", areaOptions);
        filters.put("sortOptions", sortOptions);

        return new Response<>(true, "필터 옵션 조회 성공", filters);
    }
}
