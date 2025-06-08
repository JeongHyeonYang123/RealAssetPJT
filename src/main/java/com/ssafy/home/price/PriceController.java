package com.ssafy.home.price;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bitcoin")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    /**
     * GET  /api/v1/bitcoin/prices?start=YYYY-MM-DD&end=YYYY-MM-DD
     * 반환: { prices: [ { timestamp: 1640995200000, price: 47000.12 }, … ] }
     */
    @GetMapping("/prices")
    public ResponseEntity<?> getPrices(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String start,
            @RequestParam("end")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String end
    ) {
        try {
            // 문자열 → LocalDate
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate   = LocalDate.parse(end);

            // DB 조회
            List<PriceDTO> list = priceService.getPrices(startDate, endDate);

            // null 체크 후 epoch millis, double 로 변환
            List<Map<String, Object>> rows = list.stream()
                    .filter(p -> p.getClosingPrice() != null)
                    .map(p -> {
                        Map<String, Object> m = new HashMap<>();
                        long ts = p.getDate()
                                .atStartOfDay(ZoneOffset.UTC)
                                .toInstant()
                                .toEpochMilli();
                        m.put("timestamp", ts);
                        m.put("price", p.getClosingPrice().doubleValue());
                        return m;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Collections.singletonMap("prices", rows));

        } catch (DateTimeParseException dtpe) {
            return ResponseEntity
                    .badRequest()
                    .body(Collections.singletonMap("error", "Invalid date format: " + dtpe.getParsedString()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", ex.getMessage()));
        }
    }
}
