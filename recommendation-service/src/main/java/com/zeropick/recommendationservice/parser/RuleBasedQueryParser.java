package com.zeropick.recommendationservice.parser;

import com.zeropick.recommendationservice.parser.dto.SearchCondition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 규칙 기반 자연어 조건 추출 파서
 * - 챗봇(POST /chat) LLM 호출 실패 시 Fallback 및 자연어 검색(POST /search)의 파이프라인으로 사용됨
 */
@Component
public class RuleBasedQueryParser {

    // 1. 감미료 마스터 15종 및 주요 파생어 (긴 명칭 우선 매칭하여 부분 문자열 충돌 방지)
    private static final List<String> SWEETENERS = List.of(
            "효소처리스테비아", "스테비올 배당체", "나한과추출분말", "아세설팜칼륨",
            "D-말티톨", "D-소비톨액", "아라비아검", "수크랄로스", "에리스리톨",
            "아스파탐", "알룰로스", "자일리톨", "이소말트", "스테비아",
            "말티톨", "나한과", "사카린", "소르비톨"
    );

    // 2. 카테고리 매핑 사전 (우선순위: 긴 복합어 및 세부 카테고리 -> 일반 포괄 카테고리)
    private static final Map<String, String> CATEGORY_MAP = new LinkedHashMap<>();
    static {
        // [1] 복합/특정 키워드 우선 매칭
        CATEGORY_MAP.put("프로틴바", "간식/디저트");
        CATEGORY_MAP.put("프로틴 드링크", "음료");
        CATEGORY_MAP.put("프로틴", "건강기능식품");
        CATEGORY_MAP.put("단백질", "건강기능식품");
        CATEGORY_MAP.put("유산균", "건강기능식품");
        CATEGORY_MAP.put("영양제", "건강기능식품");
        CATEGORY_MAP.put("건강기능식품", "건강기능식품");

        // [2] 간식/디저트
        CATEGORY_MAP.put("초콜릿", "간식/디저트");
        CATEGORY_MAP.put("초코", "간식/디저트");
        CATEGORY_MAP.put("팝콘", "간식/디저트");
        CATEGORY_MAP.put("웨하스", "간식/디저트");
        CATEGORY_MAP.put("캔디", "간식/디저트");
        CATEGORY_MAP.put("사탕", "간식/디저트");
        CATEGORY_MAP.put("쿠키", "간식/디저트");
        CATEGORY_MAP.put("과자", "간식/디저트");
        CATEGORY_MAP.put("젤리", "간식/디저트");
        CATEGORY_MAP.put("베이커리", "간식/디저트");
        CATEGORY_MAP.put("디저트", "간식/디저트");

        // [3] 탄산 (일반 '음료'보다 먼저 매칭되어 "탄산 음료" 질의 시 탄산으로 분류)
        CATEGORY_MAP.put("스파클링", "탄산");
        CATEGORY_MAP.put("탄산수", "탄산");
        CATEGORY_MAP.put("에이드", "탄산");
        CATEGORY_MAP.put("콜라", "탄산");
        CATEGORY_MAP.put("사이다", "탄산");
        CATEGORY_MAP.put("탄산", "탄산");

        // [4] 일반 음료
        CATEGORY_MAP.put("커피", "음료");
        CATEGORY_MAP.put("라떼", "음료");
        CATEGORY_MAP.put("주스", "음료");
        CATEGORY_MAP.put("차", "음료");
        CATEGORY_MAP.put("음료", "음료");

        // [5] 조미료/소스
        CATEGORY_MAP.put("시럽", "조미료/소스");
        CATEGORY_MAP.put("소스", "조미료/소스");
        CATEGORY_MAP.put("조미료", "조미료/소스");
    }

    // 3. 당류/표시유형 추출 정규식
    private static final Pattern SUGAR_ZERO_PATTERN = Pattern.compile("(무당|무당류|제로|0g|당류\\s*0|무가당|무설탕|슈가프리)");
    private static final Pattern SUGAR_LOW_PATTERN = Pattern.compile("(저당|저당류)");

    // 4. 금액 추출 정규식 ([천만] 단일 문자 대체 적용)
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d+)([천만])?\\s*원?\\s*(이하|미만|까지)?");

    /**
     * 입력된 자연어 질의를 파싱하여 조건 DTO로 반환
     */
    public SearchCondition parse(String text) {
        if (text == null || text.isBlank()) {
            return SearchCondition.builder().build();
        }

        String remainingText = text.trim();

        // 1. 제외 감미료 추출 (단일 조사 문자는 [이가은는]으로 최적화)
        String sweetenerExclude = null;
        for (String sweetener : SWEETENERS) {
            Pattern excludePattern = Pattern.compile(Pattern.quote(sweetener) + "[이가은는]?\\s*(없는|제외|빼고|안들어간|안 들어간|무)");
            Matcher matcher = excludePattern.matcher(remainingText);
            if (matcher.find()) {
                sweetenerExclude = sweetener;
                // 제외 감미료 표현을 제거하여 후속 키워드 매칭 시 오인식 방지
                remainingText = remainingText.replace(matcher.group(), " ");
                break;
            }
        }

        // 2. 가격 상한(maxPrice) 추출
        Integer maxPrice = extractPrice(remainingText);

        // 3. 당류 기준(sugarMax) 추출
        BigDecimal sugarMax = null;
        if (SUGAR_ZERO_PATTERN.matcher(remainingText).find()) {
            sugarMax = BigDecimal.ZERO; // 0.0g (무당)
        } else if (SUGAR_LOW_PATTERN.matcher(remainingText).find()) {
            sugarMax = BigDecimal.valueOf(5.0); // 5.0g 이하 (저당)
        }

        // 4. 카테고리 및 검색 키워드(query) 추출 (정리된 remainingText 기준으로 매칭)
        String category = null;
        String query = null;
        for (Map.Entry<String, String> entry : CATEGORY_MAP.entrySet()) {
            String keyword = entry.getKey();
            if (remainingText.contains(keyword)) {
                category = entry.getValue();
                query = keyword;
                break;
            }
        }

        return SearchCondition.builder()
                .category(category)
                .sweetenerExclude(sweetenerExclude)
                .sugarMax(sugarMax)
                .maxPrice(maxPrice)
                .query(query)
                .build();
    }

    /**
     * 금액 단위("천원", "만원", "원") 및 "이하/미만" 패턴 분석
     */
    private Integer extractPrice(String text) {
        Matcher m = PRICE_PATTERN.matcher(text.replace(",", ""));

        while (m.find()) {
            String numStr = m.group(1);
            String unit = m.group(2);
            String suffix = m.group(3);

            // "이하/미만/까지" 접미사가 있거나 "원" 단위가 붙은 경우 금액으로 파싱
            if (suffix != null || text.contains("원")) {
                try {
                    int price = Integer.parseInt(numStr);
                    if ("천".equals(unit)) {
                        price *= 1000;
                    } else if ("만".equals(unit)) {
                        price *= 10000;
                    }
                    if (price >= 100) { // 100원 이상의 값만 유효한 가격으로 채택
                        return price;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }
}