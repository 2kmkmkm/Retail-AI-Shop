# Retail-AI-Shop — 제로픽 (ZeroPick)

저당·제로 식품 전문 커머스 + 조건 기반 개인화 추천 + 상담 챗봇

LG CNS AM Inspire Camp 5기 · 2차 미니 프로젝트 · 과제 **[RTL-M]** (리테일 · 난이도 중) — 난이도 상 핵심기술인 CDC 실시간 재고 반영까지 구현

---

## 1. 서비스 개요

못 먹는 감미료·알레르기·가격대를 등록하면, 조회·구매 행동을 반영해 조건에 맞는 상품을
추천 이유와 함께 제시한다. 상품 목록의 기본 정렬이 추천순이며, 상담 챗봇이 자연어 질의
("말티톨 없는 제로 초콜릿 5천원 이하")에 상품과 근거로 답한다.

| 항목 | 내용 |
|---|---|
| 기간 | 2026-08-13 ~ (40시간) |
| 목표 | 기능 100점 (핵심 10개 70 + 선택 10개 30) |
| 핵심 차별화 기술 (핵심 10번) | LLM 상담 챗봇 — 실패 시 규칙 기반 폴백 |

## 2. 기술 스택

팀 표준: **Spring Boot 4.0.7** (2026-08-13 확정 — start.spring.io 가 현재 4.x 만 제공하고, 인프라 3종이 4.0.7 로 구축·검증됨).
수업·강사 레포(`joneconsulting/new-toy-msa`)는 3.5 기준이므로 수업 코드 복붙 시 아래 주의 참고.

| 구분 | 값 |
|---|---|
| 빌드 | **Maven** (Gradle 아님) |
| Java / Spring Boot / Cloud | 17 / **4.0.7** / **2025.1.2** |
| DB | H2 (개발) / MariaDB (통합) — 서비스별 분리 |
| 메시징 | Kafka + Schema Registry (Avro, BACKWARD) |
| 이미지 | 멀티스테이지 (`maven:3.9.11-eclipse-temurin-17` → `eclipse-temurin:17-jre`) |

> **수업 코드(3.5) 복붙 주의** — Gateway 의존성은 `spring-cloud-starter-gateway-server-webflux`
> (yml 키는 `spring.cloud.gateway.server.webflux.routes` — 강사 레포와 동일). Swagger 는 Boot 4 용 springdoc **3.x**.
> web·data-jpa·validation·kafka·openfeign·config 스타터는 이름 동일 — Boot 4 호환 존재 확인 완료
> (OpenFeign 5.0.2 · Config client 5.0.4 · springdoc 3.1.0).

> **플랫폼 주의** — Mac(Apple Silicon)에서 만든 arm64 이미지는 Windows·EC2에서
> `exec format error`로 즉시 종료된다. 공유 이미지는 `docker buildx build --platform linux/amd64`.

## 3. 서비스 구성 · 포트

| 서비스 | 포트 | 책임 |
|---|---|---|
| `product-service` | 8081 | 상품·영양정보·감미료·재고 |
| `commerce-service` | 8082 | 회원·로그인·장바구니·주문·모의결제(PENDING→PAID)·행동 이벤트 발행 |
| `recommendation-service` | 8083 | 선호 조건·행동 로그·추천 점수·상담 챗봇(LLM+폴백) |
| API Gateway | 8000 | 단일 진입점 |
| Eureka / Config | 8761 / 8888 | |
| Kafka / Schema Registry | 9092 / **8085** | 8081 충돌로 변경 |
| Prometheus / Grafana / Zipkin | 9090 / 3000 / 9411 | |
| 가상 POS DB (MySQL) | 3307 | 오프라인 매장 판매 로그 · binlog(ROW) 활성화 |
| Kafka Connect (Debezium) | 8084 | POS 판매 로그를 캡처해 Kafka 로 발행 (CDC) |

## 4. 개발 계약 문서 (`docs/`)

**코드 작성 전에 이 세 개부터.** 전부 자동 검증 통과 상태다 (`docs/verify/verify_docs.py`, 38 검사).

| 문서 | 내용 | 검증 |
|---|---|---|
| [제로픽_ERD.dbml](docs/제로픽_ERD.dbml) · [sql/](docs/sql) | 스키마 3개 + 시드 18개. dbml 은 dbdiagram.io 붙여넣기용 | H2(MariaDB 모드) 실행 |
| [API명세서.md](docs/API명세서.md) · [openapi/openapi.yaml](docs/openapi/openapi.yaml) | 경로 22개 (회원·주문취소·behaviors·상품 CRUD·재고 차감/복구 포함) | OpenAPI 3.0.3 검증 |
| [이벤트스키마.md](docs/이벤트스키마.md) · [avro/](docs/avro) | 토픽 3개, 파티션·컨슈머 그룹, 가중치(조회+1/담기0/주문+50) | fastavro 왕복 |

시드: [docs/시드데이터_zerofinder.csv](docs/시드데이터_zerofinder.csv) — 크롤링 515건 (영양·감미료 전 건, 이미지 URL 포함).
수기 입력(1인 25개) 계획은 폐기. 가격 없는 항목은 로딩 시 임의값, 카테고리는 원문 유지 — 5분류로 걸러 쓸지 회의에서 결정.

## 5. 로컬 실행

```bash
docker compose up -d
```

한 번의 명령으로 인프라와 서비스가 모두 뜬다. 초기화 컨테이너가 순서를 보장하므로
스키마 등록이나 커넥터 등록을 손으로 할 필요가 없다.

| 초기화 컨테이너 | 하는 일 |
|---|---|
| `kafka-topic-init` | 행동 이벤트 토픽 생성 |
| `schema-registry-init` | Avro 스키마 3종을 Schema Registry 에 등록 (BACKWARD) |
| `cdc-connector-init` | Debezium MySQL 소스 커넥터를 등록하고 RUNNING 까지 확인 |

`pos_stock_logs` 테이블은 pos-db 최초 기동 때 `scripts/pos-db-init.sql` 로 만들어진다.
이미 떠 있던 pos-db 가 있다면 `docker compose down` 후 다시 올려야 반영된다.

로그 중앙화(EFK)는 오버레이로 따로 켠다.

```bash
docker compose -f docker-compose-efk.yml up -d
```

동작 확인

```bash
# 매장 판매를 한 건 기록하면 CDC 를 타고 재고에 반영된다
docker exec -i pos-db mysql --default-character-set=utf8mb4 -uroot -proot pos_db   -e "INSERT INTO pos_stock_logs (store_id, member_id, product_id, category, changed_qty, event_type)
      VALUES ('STORE_GANGNAM', 1, 1, '간식/디저트', 2, 'OFFLINE_PURCHASE');"

curl http://localhost:8084/connectors/pos-inventory-connector/status
```

## 요구사항 구현 위치

채점 항목이 코드 어디에 있는지 정리한다.

### 핵심 요구사항

| # | 요구사항 | 구현 위치 |
|---|---|---|
| 1 | 마이크로서비스 분리 | `product-service` · `commerce-service` · `recommendation-service` |
| 2 | 서비스 디스커버리 | `service-discovery` (Eureka :8761) |
| 3 | API Gateway | `apigateway-service` (:8000, `lb://` 라우팅) |
| 4 | 설정 중앙화 | `config-service` (:8888 native) — 시크릿은 환경변수로만 주입 |
| 5 | 동기 통신 | commerce → product 재고 확인·차감 (OpenFeign) |
| 6 | 비동기 통신 | Kafka 행동 이벤트 3토픽 (Avro) — `commerce-service/event`, `recommendation-service/consumer` |
| 7 | 주문·결제·재고 | `commerce-service` 주문 생성 → 모의결제(PENDING→PAID), 재고 부족 시 409 |
| 8 | DB 분리 | 서비스별 스키마 — `docs/제로픽_ERD.dbml`, `docs/sql/` |
| 9 | 프론트엔드 | `frontend/` (React · Vite) — 상품·장바구니·주문·프로필·관리자 |
| 10 | AI 기능 | `recommendation-service/chat` 상담 챗봇 — LLM 실패 시 규칙 기반 폴백 |

### 선택 요구사항

| 항목 | 구현 위치 |
|---|---|
| Schema Registry (Avro) | `schema-registry` + `scripts/register-avro-schemas.py`, `docs/avro/` |
| 모니터링 | `monitoring/prometheus.yml` + Grafana |
| 분산 추적 | Zipkin (:9411) |
| 로그 중앙화 | `docker-compose-efk.yml` + `efk/fluent-bit.conf` |
| 장애 대응 | `recommendation-service/llm/LlmQueryService` — LLM 장애 시 폴백 |
| 관리자 화면 | `frontend/src/pages/Admin.jsx` — 이벤트 로그·성과 지표 |

### 난이도 상 — CDC 실시간 재고 반영

오프라인 매장(POS)에서 팔린 수량을 온라인 재고에 실시간 반영한다.

```
POS DB(pos_stock_logs, binlog)
  → Debezium MySQL 소스 커넥터 (Kafka Connect :8084)
  → 토픽 pos.pos_db.pos_stock_logs
  → product-service (재고 차감) · recommendation-service (오프라인 구매를 추천에 반영)
```

| 구성요소 | 위치 |
|---|---|
| 커넥터 설정 | `docs/cdc/pos-inventory-connector.json` |
| 커넥터 자동 등록 | `scripts/register-cdc-connector.py` |
| POS 테이블 | `scripts/pos-db-init.sql` |
| 소비 | `product-service/consumer/PosCdcStockConsumer.java` · `recommendation-service/consumer/PosCdcEventConsumer.java` |

## 6. 협업 규칙

- 브랜치 전략: `feature/*` → **`develop`** 으로 PR 머지(1인 승인, 셀프 머지 금지).
  `main` 은 시연·제출 시점에 develop 을 머지하는 안정 브랜치 — 직접 push 금지.
- 브랜치: `<type>/<서비스>-<기능>` — 예: `feat/reco-kafka-consumer`, `chore/product-init`
- 커밋: `<type>: <설명>` — feat / fix / refactor / chore / docs / test
- 금지: `.env`·LLM API 키 커밋 (키는 Config Server), `--force`

## 7. 팀 (A안 — 1인 1서비스)

| 역할 | 담당 |
|---|---|
| PM · 프론트 · 산출물 | 백준하 |
| 부팀장 · 인프라 + `product-service` | 김지현 |
| `commerce-service` (회원·주문·결제·이벤트 발행) | 김도현 |
| `recommendation-service` (추천·챗봇/AI) | 이경민 |

인프라(Eureka·Gateway·Config·compose)는 김지현 님이 D1~D2 선행 구축, 이후 product-service 담당.

기획서·프로토타입(동작 데모)·산출물은 팀 공유 폴더 참고.
