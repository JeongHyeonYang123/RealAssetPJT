package com.ssafy.home.controller;

import com.ssafy.home.domain.DongCode;
import com.ssafy.home.dto.Response;
import com.ssafy.home.mapper.DongMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/adm")
@Tag(name = "시구군동 조회 API", description = "시구군동 조회 기능 제공")
public class AdmController {
    private final RestTemplate restTemplate;
    private final DongMapper dongMapper;

    @Value("${api.key_vworld}")
    private String vworldKey;
    private final String DOMAIN = "localhost";
    private final int NUM_OF_ROWS = 100;

    // 1. 시도 리스트
    @GetMapping("/sido")
    public ResponseEntity<Response<Map<String, ?>>> getSidoList() {
        String url = "https://api.vworld.kr/ned/data/admCodeList";
        URI uri = UriComponentsBuilder.fromUriString(url)
                .queryParam("format", "json")
                .queryParam("numOfRows", NUM_OF_ROWS)
                .queryParam("key", vworldKey)
                .queryParam("domain", DOMAIN)
                .build()
                .encode()
                .toUri();

        Map<String, ?> result = restTemplate.getForObject(uri, Map.class);
        return ResponseEntity.ok(new Response<>(true, "success", result));
    }

    // 2. 구군 리스트 (시도코드 필요)
    @GetMapping("/gugun")
    public ResponseEntity<Response<Map<String, ?>>> getGugunList(@RequestParam String sidoCode) {
        String url = "https://api.vworld.kr/ned/data/admSiList";
        URI uri = UriComponentsBuilder.fromUriString(url)
                .queryParam("admCode", sidoCode)
                .queryParam("format", "json")
                .queryParam("numOfRows", NUM_OF_ROWS)
                .queryParam("key", vworldKey)
                .queryParam("domain", DOMAIN)
                .build()
                .encode()
                .toUri();

        Map<String, ?> result = restTemplate.getForObject(uri, Map.class);
        return ResponseEntity.ok(new Response<>(true, "success", result));
    }

    // 3. 읍면동 리스트 (구군코드 필요)
    @GetMapping("/dong")
    public ResponseEntity<Response<Map<String, ?>>> getDongList(@RequestParam String gugunCode) {
        String url = "https://api.vworld.kr/ned/data/admDongList";
        URI uri = UriComponentsBuilder.fromUriString(url)
                .queryParam("admCode", gugunCode)
                .queryParam("format", "json")
                .queryParam("numOfRows", NUM_OF_ROWS)
                .queryParam("key", vworldKey)
                .queryParam("domain", DOMAIN)
                .build()
                .encode()
                .toUri();

        Map<String, ?> result = restTemplate.getForObject(uri, Map.class);
        return ResponseEntity.ok(new Response<>(true, "success", result));
    }

    @GetMapping("/dongcode")
    public ResponseEntity<Response<DongCode>> getDongCode(@RequestParam String dongCode) {
        DongCode result = dongMapper.getDongByCode(dongCode);
        return ResponseEntity.ok(new Response<>(true, "success", result));
    }
}
