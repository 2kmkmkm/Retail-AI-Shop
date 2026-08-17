package com.zeropick.recommendationservice.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeropick.recommendationservice.llm.dto.LlmParseResult;
import com.zeropick.recommendationservice.parser.RuleBasedQueryParser;
import com.zeropick.recommendationservice.parser.dto.SearchCondition;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * LLM 기반 질의 조건 추출 서비스 + Resilience4j Circuit Breaker
 * - Spring Boot 4.0.7 / Spring 7 표준 RestClient 적용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmQueryService {

    private final RuleBasedQueryParser ruleBasedQueryParser;
    private final ObjectMapper objectMapper;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model:gpt-4o-mini}")
    private String model;

    @Value("${llm.api-url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    /**
     * 사용자 질의로부터 검색 조건 추출
     * - Circuit Breaker 'llmService' 적용: API 실패/타임아웃 시 fallbackParseQuery 자동 실행
     */
    @CircuitBreaker(name = "llmService", fallbackMethod = "fallbackParseQuery")
    public LlmParseResult extractCondition(String query) {
        log.info("[LLM 질의 파싱 요청] query='{}'", query);

        // 키 미설정 또는 더미키 상태면 즉시 예외를 발생시켜 Fallback으로 안전 전환
        if ("dummy-key".equals(apiKey) || "dummy-key-for-test".equals(apiKey) || apiKey.isBlank()) {
            throw new IllegalStateException("LLM API Key가 설정되지 않았습니다. Fallback 파서로 전환합니다.");
        }

        // 2초 타임아웃이 적용된 HTTP 클라이언트 구성
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(2));

        RestClient restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String systemPrompt = """
            당신은 저당/제로 식품 쇼핑몰의 질의 파서입니다. 사용자의 질문에서 검색 조건을 JSON 형식으로만 추출하세요.
            반드시 아래 JSON 구조로만 응답하세요:
            {
              "category": "간식/디저트" | "음료" | "탄산" | "조미료/소스" | "건강기능식품" | null,
              "sweetenerExclude": "제외할 감미료명" | null,
              "allergenExclude": "우유" | "대두" | "밀" | "땅콩" | null,
              "sugarMax": 최대 당류(숫자) | null,
              "kcalMin": 최소 칼로리(숫자) | null,
              "kcalMax": 최대 칼로리(숫자) | null,
              "maxPrice": 최대 가격(정수 원단위) | null,
              "query": "검색 키워드" | null
            }
            """;

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", query)
                ),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.0
        );

        String responseBody = restClient.post()
                .body(requestBody)
                .retrieve()
                .body(String.class);

        SearchCondition condition = parseJsonToCondition(responseBody);
        return LlmParseResult.of(condition, false);
    }

    /**
     * Circuit Breaker Fallback 메서드 (LLM 호출 실패 또는 서킷 OPEN 시 호출)
     */
    public LlmParseResult fallbackParseQuery(String query, Throwable throwable) {
        log.warn("[LLM Fallback 발동] 원인: {}. 규칙 기반 파서(RuleBasedQueryParser)로 전환합니다.", throwable.getMessage());
        SearchCondition fallbackCondition = ruleBasedQueryParser.parse(query);
        return LlmParseResult.of(fallbackCondition, true); // usedFallback = true
    }

    private SearchCondition parseJsonToCondition(String rawJson) {
        try {
            JsonNode rootNode = objectMapper.readTree(rawJson);
            String content = rootNode.path("choices").get(0).path("message").path("content").asText();
            JsonNode conditionJson = objectMapper.readTree(content);

            String category = conditionJson.path("category").isNull() ? null : conditionJson.path("category").asText(null);
            String sweetenerExclude = conditionJson.path("sweetenerExclude").isNull() ? null : conditionJson.path("sweetenerExclude").asText(null);
            String allergenExclude = conditionJson.path("allergenExclude").isNull() ? null : conditionJson.path("allergenExclude").asText(null);
            BigDecimal sugarMax = conditionJson.path("sugarMax").isNull() ? null : BigDecimal.valueOf(conditionJson.path("sugarMax").asDouble());
            BigDecimal kcalMin = conditionJson.path("kcalMin").isNull() ? null : BigDecimal.valueOf(conditionJson.path("kcalMin").asDouble());
            BigDecimal kcalMax = conditionJson.path("kcalMax").isNull() ? null : BigDecimal.valueOf(conditionJson.path("kcalMax").asDouble());
            Integer maxPrice = conditionJson.path("maxPrice").isNull() ? null : conditionJson.path("maxPrice").asInt();
            String q = conditionJson.path("query").isNull() ? null : conditionJson.path("query").asText(null);

            return SearchCondition.builder()
                    .category(category)
                    .sweetenerExclude(sweetenerExclude)
                    .allergenExclude(allergenExclude)
                    .sugarMax(sugarMax)
                    .kcalMin(kcalMin)
                    .kcalMax(kcalMax)
                    .maxPrice(maxPrice)
                    .query(q)
                    .build();
        } catch (Exception e) {
            log.error("[LLM 응답 JSON 파싱 실패] rawJson={}", rawJson, e);
            throw new RuntimeException("LLM JSON 응답 변환 실패", e);
        }
    }
}