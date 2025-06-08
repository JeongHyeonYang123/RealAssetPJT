package com.ssafy.home.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.home.ai.GPT4MiniClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntentClassificationService {

    private final GPT4MiniClient gptClient;
    private final ObjectMapper objectMapper;

    public String classifyIntent(String userQuery) {
        try {
            String prompt = buildIntentClassificationPrompt(userQuery);
            String response = gptClient.askLLM(prompt);

            // JSON 응답 파싱
            JsonNode result = objectMapper.readTree(response);
            String intent = result.path("intent").asText("INFO");

            log.info("질문 의도 분석 - 입력: '{}', 의도: '{}'", userQuery, intent);
            return intent;

        } catch (Exception e) {
            log.error("의도 분석 실패, 기본값(INFO) 반환: {}", e.getMessage());
            return "INFO"; // 기본값
        }
    }

    private String buildIntentClassificationPrompt(String userQuery) {
        return String.format("""
            다음 사용자 질문의 의도를 분석해서 정확히 분류해주세요.
            
            사용자 질문: "%s"
            
            분류 기준:
            1. INFO (정보 조회): 특정 아파트의 정보, 가격, 거래내역, 상세정보를 묻는 경우
               - 예시: "래미안 블레스티지 정보 알려줘", "현대인텔렉스 시세 궁금해", "이 아파트 언제 지어졌어?"
               - 키워드: 정보, 알려줘, 궁금해, 가격, 시세, 거래, 상세, 어떤, 언제, 얼마, 몇년
            
            2. RECOMMEND (추천 요청): 조건에 맞는 아파트를 찾거나 추천을 요청하는 경우
               - 예시: "분당 10억 이하 아파트 추천해줘", "강남 30평대 좋은 아파트 찾아줘"
               - 키워드: 추천, 찾아줘, 원해, 조건, 가격대, 평수, 예산, 범위, ~억, ~만원, ~평
            
            다음 JSON 형식으로만 응답하세요:
            {
                "intent": "INFO" 또는 "RECOMMEND",
                "confidence": 0.0~1.0 사이의 신뢰도,
                "reasoning": "분류 근거 간단 설명"
            }
            """, userQuery);
    }
}
