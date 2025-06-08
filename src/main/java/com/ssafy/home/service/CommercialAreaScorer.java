package com.ssafy.home.service;

import com.ssafy.home.domain.CommercialArea;
import com.ssafy.home.ai.GPT4MiniClient;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommercialAreaScorer {
    private final GPT4MiniClient gptClient;
    private final ObjectMapper objectMapper;

    // 기본 가중치 설정
    private static final Map<String, Double> DEFAULT_WEIGHTS = Map.of(
            "음식점", 2.0,
            "카페", 1.5,
            "편의점", 1.0,
            "병원", 2.0,
            "약국", 1.5,
            "마트", 2.0,
            "백화점", 2.5,
            "학교", 2.0,
            "지하철", 2.5,
            "버스정류장", 1.5
    );

    // 🔥 기존 메서드 - LLM 호출 없이 가중치만 사용 (최적화된 메서드)
    public double scoreWithWeights(List<CommercialArea> areas, Map<String, Double> weights) {
        double score = 0;
        for (CommercialArea area : areas) {
            String type = area.getType();
            double weight = weights.getOrDefault(type, DEFAULT_WEIGHTS.getOrDefault(type, 1.0));
            score += weight;
        }
        return score;
    }

    // 🔥 public으로 변경하여 외부에서 호출 가능 (한 번만 호출용)
    public Map<String, Double> extractWeightsFromQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new HashMap<>(DEFAULT_WEIGHTS);
        }

        try {
            // LLM에 전달할 프롬프트 생성
            String prompt = String.format("""
                다음 사용자 입력에서 상권 선호도를 분석해주세요.
                입력: %s
                
                다음 형식의 JSON으로 응답해주세요:
                {
                    "weights": {
                        "음식점": 2.0,  // 기본값
                        "카페": 1.5,    // 기본값
                        "편의점": 1.0,   // 기본값
                        "병원": 2.0,     // 기본값
                        "약국": 1.5,     // 기본값
                        "마트": 2.0,     // 기본값
                        "백화점": 2.5,   // 기본값
                        "학교": 2.0,     // 기본값
                        "지하철": 2.5,   // 기본값
                        "버스정류장": 1.5 // 기본값
                    }
                }
                
                규칙:
                1. 사용자가 언급한 상권 유형의 가중치는 기본값의 1.5배로 설정
                2. 사용자가 특별히 강조한 상권 유형(예: "매우 중요", "꼭 필요")은 2배로 설정
                3. 사용자가 부정적으로 언급한 상권 유형(예: "불필요", "싫어")은 0.5배로 설정
                4. 언급되지 않은 상권 유형은 기본값 유지
                5. JSON 형식만 응답하고 다른 설명은 하지 않음
                """, query);

            // LLM에 요청하여 가중치 조정 정보 받기 (한 번만!)
            String response = gptClient.askLLM(prompt);

            // JSON 응답 파싱
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(response, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Double> weights = (Map<String, Double>) result.get("weights");

            return weights != null ? weights : new HashMap<>(DEFAULT_WEIGHTS);
        } catch (Exception e) {
            // LLM 처리 실패 시 기본 가중치 반환
            return new HashMap<>(DEFAULT_WEIGHTS);
        }
    }

    // 🔥 기존 메서드는 하위 호환성을 위해 유지 (레거시 지원)
    public double score(List<CommercialArea> areas, String userQuery) {
        Map<String, Double> adjustedWeights = extractWeightsFromQuery(userQuery);
        return scoreWithWeights(areas, adjustedWeights);
    }

    // 기존 메서드는 하위 호환성을 위해 유지
    public double score(List<CommercialArea> areas) {
        return scoreWithWeights(areas, DEFAULT_WEIGHTS);
    }
}
