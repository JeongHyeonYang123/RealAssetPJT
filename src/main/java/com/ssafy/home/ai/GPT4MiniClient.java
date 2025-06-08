package com.ssafy.home.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;

@Component
@RequiredArgsConstructor
public class GPT4MiniClient {
    @Value("${gpt4mini.api-key}")
    private String apiKey;

    @Value("${gpt4mini.api-url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String askLLM(String prompt) {
        try {
            // 1. 요청 바디를 Map/리스트로 구성 후 ObjectMapper로 직렬화
            Map<String, Object> body = new HashMap<>();
            body.put("model", "gpt-3.5-turbo");
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", prompt));
            body.put("messages", messages);
            String requestBody = objectMapper.writeValueAsString(body);

            // 2. 헤더 구성
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            String bodyStr = response.getBody();
            // OpenAI 응답에서 답변 텍스트 추출
            JsonNode root = objectMapper.readTree(bodyStr);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText();
                return content;
            }
            return "[GPT4MiniClient 오류] 답변을 파싱할 수 없습니다.";
        } catch (HttpClientErrorException e) {
            System.err.println("[GPT4MiniClient 400에러 본문] " + e.getResponseBodyAsString());
            return "[GPT4MiniClient 오류] " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "[GPT4MiniClient 오류] " + e.getMessage();
        }
    }
}