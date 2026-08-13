# API 명세서

기계가 읽는 원본은 `openapi/openapi.yaml` (OpenAPI 3.0.3, 스펙 검증 완료).
모든 외부 호출은 **API Gateway(:8000)** 를 경유한다. 아래는 요약표.

## 라우팅

Gateway 라우팅은 수업 관례와 동일한 서비스명 프리픽스 방식으로 확정 (2026-08-13, 인프라 세팅 기준).
예: `GET http://localhost:8000/product-service/products``

| 경로 접두 | 서비스 | 포트 |
|---|---|---|
| `/product-service/**` | product-service | 8081 |
| `/commerce-service/**` | commerce-service | 8082 |
| `/recommendation-service/**` | recommendation-service | 8083 |

## product-service

| Method | 경로 | 설명 |
|---|---|---|
| GET | `/product-service/products` | 목록 — category·sweetenerExclude·sugarMax·q·sort 필터 |
| GET | `/product-service/products/{id}` | 상세 |
| GET | `/product-service/products/compare?ids=1,2,3` | 영양성분 비교 (개수 제한 없음) |
| PUT | `/product-service/products/{id}/stock/deduct` | **내부 전용** — commerce 가 OpenFeign 으로 호출. Gateway 비노출. 재고 부족 시 409 |

## commerce-service

| Method | 경로 | 설명 |
|---|---|---|
| POST | `/commerce-service/members` | 회원가입 |
| POST | `/commerce-service/members/login` | 로그인 — memberId 반환 (JWT 는 MP1 코드 재사용 시 선택) |
| POST | `/commerce-service/behaviors` | 행동 이벤트 수신(조회) — Kafka 발행. 담기·주문 이벤트는 해당 API 처리 중 직접 발행 |
| GET | `/commerce-service/members/{id}` | 회원 조회 |
| GET | `/commerce-service/carts/{memberId}` | 장바구니 |
| POST | `/commerce-service/carts` | 담기 → `cart-added` 발행 (가중치 0, 기록용) |
| POST | `/commerce-service/orders` | 주문 생성 — 상태 **PENDING**, 재고 차감 없음 |
| POST | `/commerce-service/orders/{orderId}/pay` | 모의 결제 승인 → **PAID** + 재고 차감(OpenFeign) + `order-completed` 품목별 발행. 차감 실패 시 CANCELLED + 409 |
| GET | `/commerce-service/orders?memberId=` | 주문 내역 |

## recommendation-service

| Method | 경로 | 설명 | 채점 |
|---|---|---|---|
| POST | `/recommendation-service/preferences` | 온보딩 — 제외 감미료(하드필터)·카테고리·가격대 | |
| GET | `/recommendation-service/preferences/{memberId}` | 조건 조회 | |
| GET | `/recommendation-service/recommendations/{memberId}` | 추천 점수 목록 — FE 가 목록과 병합해 **추천순 정렬·✨배지** | 선택 8 부하 대상 |
| POST | `/recommendation-service/chat` | **상담 챗봇** — 조건 추출(LLM)→하드필터→가중치 랭킹→답변. LLM 실패 시 규칙 기반 Fallback(`usedFallback`) | **핵심 10** / 선택 4 |
| POST | `/recommendation-service/search` | 자연어 검색 — 챗봇 1단계와 동일 파이프라인 | 선택 10 |
| POST | `/recommendation-service/click` | 추천 배지 클릭 기록 | 선택 6 |
| GET | `/recommendation-service/metrics` | 노출·클릭률·폴백률 집계 | 선택 6 |

## 설계 메모

- 목록의 추천순 정렬은 **FE 병합** 방식: `GET /product-service/products` + `GET /recommendation-service/recommendations/{memberId}` 를 각각 받아 점수로 정렬.
  (Gateway 집계나 서비스 간 동기 호출보다 결합이 낮고, 추천 장애 시 목록은 기본 정렬로 동작)
- 오류 응답은 `{ code, message }` 공통 형식. 재고 부족 409, 없음 404, 검증 실패 400.
