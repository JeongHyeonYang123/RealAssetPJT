package com.ssafy.home.controller;

import com.ssafy.home.dto.ChatResponse;
import com.ssafy.home.service.ApartmentInfoChatbotService;
import com.ssafy.home.service.ApartmentRecommendService;
import com.ssafy.home.service.IntentClassificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chatbot")
@Tag(name = "Chatbot", description = "아파트 정보/추천 챗봇 API")
public class ChatbotController {
    private final ApartmentInfoChatbotService chatbotService;
    private final ApartmentRecommendService recommendService;
    private final IntentClassificationService intentService; // 🔥 새로 추가

    @Operation(summary = "통합 AI 챗봇", description = "AI가 질문 의도를 분석하여 적절한 서비스로 자동 라우팅")
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody String userQuery) {
        try {
            // 🔥 AI로 질문 의도 분석
            String intent = intentService.classifyIntent(userQuery);

            String answer;
            String apiUsed;

            if ("RECOMMEND".equals(intent)) {
                answer = recommendService.recommendApartment(userQuery);
                apiUsed = "apartment-recommend";
            } else {
                answer = chatbotService.answerApartmentInfo(userQuery);
                apiUsed = "apartment-info";
            }

            ChatResponse response = ChatResponse.builder()
                    .answer(answer)
                    .intent(intent)
                    .apiUsed(apiUsed)
                    .timestamp(LocalDateTime.now())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ChatResponse.builder()
                            .answer("죄송합니다. 일시적인 오류가 발생했습니다.")
                            .intent("ERROR")
                            .apiUsed("none")
                            .timestamp(LocalDateTime.now())
                            .build());
        }
    }

    // 기존 개별 엔드포인트들은 유지 (하위 호환성)
    @PostMapping("/apartment-info")
    public ResponseEntity<String> getApartmentInfo(@RequestBody String userQuery) {
        String answer = chatbotService.answerApartmentInfo(userQuery);
        return ResponseEntity.ok(answer);
    }

    @PostMapping("/apartment-recommend")
    public ResponseEntity<String> recommendApartment(@RequestBody String userQuery) {
        String answer = recommendService.recommendApartment(userQuery);
        return ResponseEntity.ok(answer);
    }
}
