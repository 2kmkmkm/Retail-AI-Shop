package com.zeropick.recommendationservice.parser;

import java.util.*;

/**
 * 감미료 표현 사전 — 사용자가 쓰는 이름(정식명·약칭·오타)을 상품 DB의 정식 감미료명으로 바꾼다.
 *
 * product-service 는 감미료 이름이 정확히 같을 때만 제외하므로(IN 조건),
 * "소르비톨"처럼 DB에 없는 이름을 넘기면 D-소비톨액 상품이 그대로 추천된다.
 * 규칙 파서와 LLM 응답 모두 이 사전을 거쳐 DB 이름으로 맞춘다.
 */
public final class SweetenerDictionary {

    /** 표현 → DB 정식명 목록. 긴 표현부터 찾도록 길이 내림차순으로 정렬해 둔다. */
    private static final Map<String, List<String>> ALIASES = new LinkedHashMap<>();

    static {
        Map<String, List<String>> m = new HashMap<>();
        // 말티톨: DB에 '말티톨'과 'D-말티톨'이 따로 있으므로 둘 다 제외
        put(m, List.of("말티톨", "D-말티톨"), "D-말티톨", "말티톨액", "말티톨", "말티톨시럽", "말티올");
        // 소르비톨: DB 이름은 'D-소비톨액'
        put(m, List.of("D-소비톨액"), "D-소비톨액", "D-소비톨", "D-소르비톨", "소비톨액", "소비톨", "소르비톨", "솔비톨");
        // 스테비아 계열: '스테비아'라고 하면 효소처리스테비아·스테비올 배당체까지 제외
        put(m, List.of("스테비아", "효소처리스테비아", "스테비올 배당체"), "스테비아", "스테비야");
        put(m, List.of("효소처리스테비아"), "효소처리스테비아", "효소처리 스테비아");
        put(m, List.of("스테비올 배당체"), "스테비올 배당체", "스테비올배당체", "스테비올");
        put(m, List.of("나한과추출분말"), "나한과추출분말", "나한과 추출분말", "나한과");
        put(m, List.of("아세설팜칼륨"), "아세설팜칼륨", "아세설팜 칼륨", "아세설팜K", "아세설팜");
        put(m, List.of("아스파탐"), "아스파탐", "아쓰파탐", "아스파땀", "아스바탐");
        put(m, List.of("수크랄로스"), "수크랄로스", "수쿠랄로스", "슈크랄로스", "수크랄로즈", "수크라로스");
        put(m, List.of("에리스리톨"), "에리스리톨", "에리트리톨", "에리쓰리톨", "에리스리톨");
        put(m, List.of("알룰로스"), "알룰로스", "알롤로스", "알루로스");
        put(m, List.of("자일리톨"), "자일리톨", "자이리톨", "자일리톨");
        put(m, List.of("이소말트"), "이소말트");
        put(m, List.of("아라비아검"), "아라비아검", "아라비아 검");
        put(m, List.of("사카린"), "사카린", "삭카린");

        m.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()))
                .forEach(e -> ALIASES.put(e.getKey(), e.getValue()));
    }

    private static void put(Map<String, List<String>> m, List<String> canonical, String... aliases) {
        for (String alias : aliases) m.put(alias, canonical);
    }

    private SweetenerDictionary() {}

    /** 길이 내림차순 표현 목록 (문장 속 언급 찾기용) */
    public static Set<String> aliases() {
        return ALIASES.keySet();
    }

    /** 표현 하나를 DB 정식명 목록으로. 사전에 없으면 입력 그대로 1개 */
    public static List<String> toCanonical(String term) {
        if (term == null || term.isBlank()) return List.of();
        String t = term.trim();
        List<String> found = ALIASES.get(t);
        if (found == null) found = ALIASES.get(t.replace(" ", ""));
        return found != null ? found : List.of(t);
    }

    /** 여러 표현을 중복 없이 DB 정식명 목록으로. 비면 null (응답에 빈 배열이 찍히지 않도록) */
    public static List<String> toCanonical(Collection<String> terms) {
        if (terms == null) return null;
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String term : terms) out.addAll(toCanonical(term));
        return out.isEmpty() ? null : new ArrayList<>(out);
    }
}
