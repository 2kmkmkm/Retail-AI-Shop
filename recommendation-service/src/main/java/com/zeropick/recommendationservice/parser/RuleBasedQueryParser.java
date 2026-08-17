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

    // 2. 알레르기 마스터
    private static final List<String> ALLERGENS = List.of("우유", "대두", "밀", "땅콩");

    // 3. 카테고리 매핑 사전 (우선순위: 긴 복합어 및 세부 카테고리 -> 일반 포괄 카테고리)
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
        CATEGORY_MAP.put("아이스크림", "간식/디저트");
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

        // [3] 탄산 계열 — 상품 DB 카테고리는 "음료"뿐이므로 음료로 매핑한다
        //     ("탄산" 카테고리는 DB에 없어 exact match 필터에서 0건이 된다)
        CATEGORY_MAP.put("스파클링", "음료");
        CATEGORY_MAP.put("탄산수", "음료");
        CATEGORY_MAP.put("에이드", "음료");
        CATEGORY_MAP.put("콜라", "음료");
        CATEGORY_MAP.put("사이다", "음료");
        CATEGORY_MAP.put("탄산", "음료");

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

    // 상품명에 그대로 등장하지 않는 분류 일반어 — query(이름 검색)로 넘기지 않는다
    private static final Set<String> GENERIC_CATEGORY_WORDS =
            Set.of("음료", "디저트", "베이커리", "조미료", "건강기능식품", "영양제");

    // 4. 당류/표시유형 추출 정규식
    //    "0g"는 앞에 숫자가 없을 때만 매칭 ("10g"의 부분 문자열 오탐 방지),
    //    "당류 0"은 뒤에 숫자/소수점이 없을 때만 매칭 ("당류 0.5g" 오탐 방지)
    private static final Pattern SUGAR_ZERO_PATTERN = Pattern.compile("(무당|무당류|제로|(?<!\\d)0g|당류\\s*0(?![.\\d])|무가당|무설탕|슈가프리)");
    private static final Pattern SUGAR_LOW_PATTERN = Pattern.compile("(저당|저당류)");

    // 5. 칼로리 추출 정규식 — "이상"은 하한(kcalMin)으로 구분 처리한다
    private static final Pattern KCAL_PATTERN = Pattern.compile("(\\d+)\\s*(kcal|칼로리)\\s*(이하|미만|이상|넘는)?", Pattern.CASE_INSENSITIVE);

    // 6. 금액 추출 정규식 — "원"을 필수로 요구해 "500ml" 같은 용량 수치의 오추출을 막는다
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d+)\\s*([천만])?\\s*원\\s*(이하|미만|까지)?");

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
                remainingText = remainingText.replace(matcher.group(), " ");
                break;
            }
        }

        // 2. 알레르기 제외 성분(allergenExclude) 추출
        String allergenExclude = null;
        for (String allergen : ALLERGENS) {
            Pattern allergenPattern = Pattern.compile(Pattern.quote(allergen) + "[이가은는]?\\s*(없는|제외|빼고|안들어간|안 들어간|무)");
            Matcher matcher = allergenPattern.matcher(remainingText);
            if (matcher.find()) {
                allergenExclude = allergen;
                remainingText = remainingText.replace(matcher.group(), " ");
                break;
            }
        }

        // 3. 칼로리 상한/하한 추출 — "이상·넘는"은 하한(kcalMin), 그 외는 상한(kcalMax)
        BigDecimal kcalMax = null;
        BigDecimal kcalMin = null;
        Matcher kcalMatcher = KCAL_PATTERN.matcher(remainingText);
        if (kcalMatcher.find()) {
            try {
                BigDecimal value = new BigDecimal(kcalMatcher.group(1));
                String bound = kcalMatcher.group(3);
                if ("이상".equals(bound) || "넘는".equals(bound)) {
                    kcalMin = value;
                } else {
                    kcalMax = value;
                }
                remainingText = remainingText.replace(kcalMatcher.group(), " ");
            } catch (NumberFormatException ignored) {}
        }

        // 4. 가격 상한(maxPrice) 추출
        Integer maxPrice = extractPrice(remainingText);

        // 5. 당류 기준(sugarMax) 추출
        BigDecimal sugarMax = null;
        if (SUGAR_ZERO_PATTERN.matcher(remainingText).find()) {
            sugarMax = BigDecimal.ZERO; // 0.0g (무당)
        } else if (SUGAR_LOW_PATTERN.matcher(remainingText).find()) {
            sugarMax = BigDecimal.valueOf(5.0); // 5.0g 이하 (저당)
        }

        // 6. 카테고리 및 검색 키워드(query) 추출.
        //    "콜라"처럼 상품명에 실제로 들어가는 단어만 query 로 넘긴다.
        //    "음료" 같은 분류 일반어를 query 로 넘기면 이름 contains 필터가 되어
        //    카테고리 전체가 0건으로 걸러진다 (예: "음료 3천원 이하" → 0건).
        String category = null;
        String query = null;
        for (Map.Entry<String, String> entry : CATEGORY_MAP.entrySet()) {
            String keyword = entry.getKey();
            if (remainingText.contains(keyword)) {
                category = entry.getValue();
                if (!GENERIC_CATEGORY_WORDS.contains(keyword)) {
                    query = keyword;
                }
                break;
            }
        }

        return SearchCondition.builder()
                .category(category)
                .sweetenerExclude(sweetenerExclude)
                .allergenExclude(allergenExclude)
                .sugarMax(sugarMax)
                .kcalMin(kcalMin)
                .kcalMax(kcalMax)
                .maxPrice(maxPrice)
                .query(query)
                .build();
    }

    /**
     * 금액 단위("천원", "만원", "원") 및 "이하/미만" 패턴 분석.
     * 패턴이 "원"을 필수로 요구하므로 "500ml" 같은 용량 수치는 매칭되지 않는다.
     */
    private Integer extractPrice(String text) {
        Matcher m = PRICE_PATTERN.matcher(text.replace(",", ""));

        while (m.find()) {
            String numStr = m.group(1);
            String unit = m.group(2);

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
        return null;
    }
}