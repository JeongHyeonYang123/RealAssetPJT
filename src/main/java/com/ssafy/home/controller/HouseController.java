package com.ssafy.home.controller;

import com.ssafy.home.domain.DongCode;
import com.ssafy.home.domain.HouseDeal;
import com.ssafy.home.domain.HouseInfo;
import com.ssafy.home.dto.ApartmentWithLatestDeal;
import com.ssafy.home.dto.Response;
import com.ssafy.home.service.DataUpdateService;
import com.ssafy.home.service.HouseService;
import com.ssafy.home.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/house")
public class HouseController {

    private final HouseService houseService;
    private final StatisticsService statisticsService;
    private final DataUpdateService dataUpdateService;

    // 기존에 구현한 API
    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyApartments(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1000") int radius) {
        try {
            Map<String, Object> result = houseService.getNearbyApartments(lat, lng, radius);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "아파트 정보 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/deals/{aptSeq}")
    public ResponseEntity<?> getApartmentDeals(@PathVariable String aptSeq) {
        try {
            List<HouseDeal> deals = houseService.getApartmentDeals(aptSeq);
            return ResponseEntity.ok(deals);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "거래 내역 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // 지도 영역 내 아파트 정보 조회 API
    // 현재 위도 경도 외의 정보가 null로 반환됨..
    @GetMapping("/apartments-in-bounds")
    public ResponseEntity<?> getApartmentsInBounds(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng) {
        try {
            List<ApartmentWithLatestDeal> apartments = houseService.getApartmentsInBounds(swLat, swLng, neLat, neLng);
            return ResponseEntity.ok(apartments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "영역 내 아파트 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 지도 영역 내 아파트 + 각 아파트별 최신 거래 1건 반환
     */
    @GetMapping("/apartments")
    public List<ApartmentWithLatestDeal> getApartmentsWithLatestDeal() {
        return houseService.getApartmentsWithLatestDeal();
    }

    @Operation(summary = "아파트 정보 조회", description = "apt_seq로 아파트의 상세 정보와 좌표를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "아파트 정보 없음"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/apartment/{aptSeq}")
    public ResponseEntity<Response<HouseInfo>> getApartmentInfo(
            @Parameter(description = "아파트 시퀀스", example = "11680-578") @PathVariable String aptSeq) {

        try {
            log.info("아파트 정보 조회 요청 - aptSeq: {}", aptSeq);

            Optional<HouseInfo> houseInfo = houseService.getHouseInfoByAptSeq(aptSeq);

            if (houseInfo.isPresent()) {
                return ResponseEntity.ok(new Response<>(true, "아파트 정보 조회 성공", houseInfo.get()));
            } else {
                return ResponseEntity.status(404)
                        .body(new Response<>(false, "해당 아파트 정보를 찾을 수 없습니다.", null));
            }

        } catch (Exception e) {
            log.error("아파트 정보 조회 실패 - aptSeq: {}", aptSeq, e);
            return ResponseEntity.badRequest()
                    .body(new Response<>(false, "아파트 정보 조회 중 오류가 발생했습니다.", null));
        }
    }

    // 법정동 코드 조회 API
    @GetMapping("/dongcode")
    public ResponseEntity<?> getDongCode(@RequestParam String dongName) {
        try {
            DongCode dongCode = houseService.getDongCodeByName(dongName);
            return ResponseEntity.ok(dongCode);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "법정동 코드 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/areas/latest-deal-prices")
    public ResponseEntity<?> getLatestDealPricesByRegion(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng,
            @RequestParam(defaultValue = "gu") String regionType) {
        try {
            Map<String, Object> result = houseService.getLatestDealPricesByRegion(
                    swLat, swLng, neLat, neLng, regionType);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "최신 거래 평균가 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // 시도별 평균가격 조회 API (뒤 8자리가 0)
    @GetMapping("/areas/sido-avg-price")
    public ResponseEntity<?> getSidoAvgPriceInBounds(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng) {
        try {
            List<Map<String, Object>> result = houseService.getSidoAvgPriceInBounds(swLat, swLng, neLat, neLng);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "시도별 평균가 조회 중 오류: " + e.getMessage()));
        }
    }

    // 구군별 평균가격 조회 API (뒤 5자리가 0, 뒤 8자리는 0이 아님)
    @GetMapping("/areas/gugun-avg-price")
    public ResponseEntity<?> getGugunAvgPriceInBounds(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng) {
        try {
            List<Map<String, Object>> result = houseService.getGugunAvgPriceInBounds(swLat, swLng, neLat, neLng);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "구군별 평균가 조회 중 오류: " + e.getMessage()));
        }
    }

    // 동별 평균가격 조회 API (뒤 5자리가 0이 아님)
    @GetMapping("/areas/dong-avg-price")
    public ResponseEntity<?> getDongAvgPriceInBounds(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng) {
        try {
            List<Map<String, Object>> result = houseService.getDongAvgPriceInBounds(swLat, swLng, neLat, neLng);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "동별 평균가 조회 중 오류: " + e.getMessage()));
        }
    }

    @Operation(summary = "동코드 별 평균 가격 조회", description = "dong_code로 해당 지역의 아파트 평균 가격을 조회")
    @GetMapping("/avg-price")
    public ResponseEntity<Map<String, Object>> getAvgPriceByDongCode(
            @RequestParam String dongCode) {

        log.info("동코드별 평균가격 조회 API 호출: {}", dongCode);
        Map<String, Object> result = houseService.getAvgPriceByDongCode(dongCode);

        boolean success = (Boolean) result.get("success");
        return success ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    // 평균가격만 간단히 조회
    @GetMapping("/simple-price/{dongCode}")
    public ResponseEntity<?> getSimpleAvgPrice(@PathVariable String dongCode) {

        log.info("간단 평균가격 조회 API 호출: {}", dongCode);
        Long avgPrice = houseService.getSimpleAvgPrice(dongCode);

        return avgPrice != null ? ResponseEntity.ok(avgPrice)
                : ResponseEntity.badRequest().body("해당 동코드의 데이터를 찾을 수 없습니다.");
    }

}
