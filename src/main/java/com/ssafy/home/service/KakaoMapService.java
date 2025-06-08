package com.ssafy.home.service;

import com.ssafy.home.domain.CommercialArea;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KakaoMapService {
    private final RestTemplate restTemplate;
    private final String kakaoApiKey;
    private static final String KAKAO_API_URL = "https://dapi.kakao.com/v2/local/search/category.json";
    private static final int RADIUS = 1000; // 1km

    public KakaoMapService(
            @Value("${kakao.api.key}") String kakaoApiKey,
            RestTemplate restTemplate) {
        this.kakaoApiKey = kakaoApiKey;
        this.restTemplate = restTemplate;
    }

    public List<CommercialArea> getNearbyCommercialAreas(double lat, double lng) {
        List<CommercialArea> areas = new ArrayList<>();

        // 음식점, 카페, 편의점 카테고리 코드
        String[] categories = {"FD6", "CE7", "CS2"}; // 음식점, 카페, 편의점
        String[] categoryNames = {"음식점", "카페", "편의점"};

        for (int i = 0; i < categories.length; i++) {
            String category = categories[i];
            String categoryName = categoryNames[i];

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);

            String url = UriComponentsBuilder.fromHttpUrl(KAKAO_API_URL)
                    .queryParam("category_group_code", category)
                    .queryParam("x", String.valueOf(lng))
                    .queryParam("y", String.valueOf(lat))
                    .queryParam("radius", RADIUS)
                    .queryParam("sort", "distance")
                    .build()
                    .toUriString();

            try {
                HttpEntity<?> entity = new HttpEntity<>(headers);
                Map<String, Object> response = restTemplate.exchange(
                        url, HttpMethod.GET, entity, Map.class).getBody();

                if (response != null && response.containsKey("documents")) {
                    List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");

                    for (Map<String, Object> doc : documents) {
                        CommercialArea area = new CommercialArea();
                        area.setName((String) doc.get("place_name"));
                        area.setType(categoryName);
                        area.setLat(Double.parseDouble((String) doc.get("y")));
                        area.setLng(Double.parseDouble((String) doc.get("x")));
                        areas.add(area);
                    }
                }
            } catch (Exception e) {
                // TODO: 로깅 추가
                System.err.println("Error fetching commercial areas for category " + category + ": " + e.getMessage());
            }
        }

        return areas;
    }
}