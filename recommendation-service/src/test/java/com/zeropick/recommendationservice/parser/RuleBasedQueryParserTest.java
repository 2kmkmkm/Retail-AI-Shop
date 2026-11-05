package com.zeropick.recommendationservice.parser;

import com.zeropick.recommendationservice.parser.dto.SearchCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 규칙 기반 파서 단위 테스트.
 * 스프링을 띄우지 않고 new 로 바로 생성 → 실행 1초 이내.
 * 기대값은 상품 DB의 정식 감미료명 (예: 소르비톨 → D-소비톨액).
 */
class RuleBasedQueryParserTest {

    private final RuleBasedQueryParser parser = new RuleBasedQueryParser();

    /** "a|b" 형태 기대값을 목록으로 */
    private static List<String> list(String pipe) {
        return Arrays.asList(pipe.split("\\|"));
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @DisplayName("제외 감미료 — 정형 표현")
    @CsvSource({
            "아스파탐 빼고 콜라 추천해줘,      아스파탐",
            "수크랄로스가 안 들어간 음료,      수크랄로스",
            "에리스리톨 제외한 젤리,          에리스리톨",
            "말티톨 없는 제로 초콜릿,         말티톨|D-말티톨",   // DB에 두 이름이 따로 있음
            "효소처리스테비아 없는 음료,       효소처리스테비아"    // 안쪽의 '스테비아'를 다시 잡지 않음
    })
    void excludesSweetener(String query, String expected) {
        assertThat(parser.parse(query).getSweetenerExcludes()).containsExactlyInAnyOrderElementsOf(list(expected));
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @DisplayName("제외 감미료 — 구어체 부정 표현")
    @CsvSource({
            "수크랄로스는 싫어,                   수크랄로스",
            "에리스리톨 안든거 젤리,               에리스리톨",
            "말티톨은 절대 안됨,                  말티톨|D-말티톨",
            "자일리톨 든 껌말고 사탕,              자일리톨",
            "아스파탐 뇌에 안좋다면서요 그거 없는 요거트, 아스파탐"
    })
    void excludesSweetenerInCasualSpeech(String query, String expected) {
        assertThat(parser.parse(query).getSweetenerExcludes()).containsExactlyInAnyOrderElementsOf(list(expected));
    }

    @ParameterizedTest(name = "[{index}] 오타·약칭 {0} → {1}")
    @DisplayName("제외 감미료 — 오타·약칭을 DB 정식명으로")
    @CsvSource({
            "아쓰파탐 빼고 콜라,     아스파탐",
            "수쿠랄로스 없는 음료,   수크랄로스",
            "소르비톨 없는 껌,       D-소비톨액",
            "나한과 안 넣은 초콜릿,  나한과추출분말"
    })
    void normalizesTypoAndAlias(String query, String expected) {
        assertThat(parser.parse(query).getSweetenerExcludes()).containsExactlyInAnyOrderElementsOf(list(expected));
    }

    @Test
    @DisplayName("제외 감미료 — 여러 개를 모두 추출")
    void excludesMultipleSweeteners() {
        assertThat(parser.parse("말티톨이랑 아스파탐 둘 다 없는 과자").getSweetenerExcludes())
                .containsExactlyInAnyOrder("말티톨", "D-말티톨", "아스파탐");
    }

    @Test
    @DisplayName("제외 감미료 — '괜찮다'고 한 감미료는 제외하지 않음")
    void keepsAcceptedSweetener() {
        assertThat(parser.parse("스테비아나 알룰로스는 괜찮은데 아스파탐이랑 수크랄로스는 피하고싶어").getSweetenerExcludes())
                .containsExactlyInAnyOrder("아스파탐", "수크랄로스");
    }

    @ParameterizedTest(name = "[{index}] {0} → 제외 없음")
    @DisplayName("제외 감미료 — 부정 표현이 없으면 제외하지 않음")
    @ValueSource(strings = {
            "알룰로스 들어간 음료 추천해줘",
            "자일리톨 껌 추천해줘",
            "아스파탐 들어간 건 상관없어요 콜라 추천"
    })
    void noExclusionWithoutNegation(String query) {
        assertThat(parser.parse(query).getSweetenerExcludes()).isNull();
    }

    @ParameterizedTest(name = "[{index}] {0} → 최대 {1}원")
    @DisplayName("가격 상한 추출")
    @CsvSource({
            "5천원 이하 초콜릿,     5000",
            "1만원 미만 프로틴바,   10000",
            "'3,000원 이하 음료',   3000"   // 값 안의 쉼표는 작은따옴표로 감싸야 CSV 칸이 안 나뉨
    })
    void extractsMaxPrice(String query, int expected) {
        assertThat(parser.parse(query).getMaxPrice()).isEqualTo(expected);
    }

    @Test
    @DisplayName("PR #37 회귀 — 500ml 같은 용량은 가격으로 읽지 않는다")
    void volumeIsNotPrice() {
        assertThat(parser.parse("500ml 제로콜라").getMaxPrice()).isNull();
    }

    @Test
    @DisplayName("PR #37 회귀 — 'kcal 이상'은 하한으로 읽는다")
    void kcalOverIsMin() {
        SearchCondition c = parser.parse("200kcal 이상 프로틴바");
        assertThat(c.getKcalMin()).isEqualByComparingTo("200");
        assertThat(c.getKcalMax()).isNull();
    }

    @Test
    @DisplayName("'제로'는 당류 0g 조건")
    void zeroMeansNoSugar() {
        assertThat(parser.parse("제로 초콜릿").getSugarMax()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("상품 필터용 — 제외 감미료가 하나라도 든 상품을 걸러낸다")
    void detectsExcludedSweetenerInProduct() {
        SearchCondition c = parser.parse("소르비톨 없는 껌");
        assertThat(c.containsExcludedSweetener(List.of("D-소비톨액", "자일리톨"))).isTrue();
        assertThat(c.containsExcludedSweetener(List.of("자일리톨"))).isFalse();
        assertThat(c.sweetenerExcludeParam()).isEqualTo("D-소비톨액");
    }
}
