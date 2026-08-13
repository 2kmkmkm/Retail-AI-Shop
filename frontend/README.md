# ZeroPick 프론트엔드

React(Vite) 기반 실서비스 프론트. MENUPICK FE 구조를 따른다.

## 실행

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

- 개발 서버가 `/product-service`, `/commerce-service`, `/recommendation-service` 요청을
  **API Gateway(:8000)** 로 프록시한다 (`vite.config.js`) — CORS 설정 불필요.
- **백엔드가 아직 안 떠 있어도 동작한다** — API 호출이 실패하면 시드 데이터(515건)로 폴백하고
  상단에 안내 배너가 뜬다. 게이트웨이가 올라오면 자동으로 실제 API를 쓴다.

## 화면

| 경로 | 내용 |
|---|---|
| `/` | 상품 목록 — 필터(카테고리·감미료 제외·칼로리·검색) · 추천순 정렬 + ✨배지 · 비교함 |
| `/cart` | 장바구니 → 주문·모의결제 (결제 시점 재고 차감) |
| `/orders` | 주문내역 (상태·결제수단) |
| `/profile` | 선호 조건 — 제외 감미료 · 알레르기 · 선호 카테고리 · 가격대 |
| `/admin` | 관리자 콘솔 (성과 대시보드 · 이벤트 로그) — **ID/PW 게이트**, 접속 정보는 팀 채널 공유 |
| 전역 | 상담 챗봇 플로팅 (LLM 폴백 시 usedFallback 표시) |

## 구조

```
src/
  api.js          게이트웨이 호출 + 시드 폴백 (계약: docs/API명세서.md)
  data/seed.js    시드 515건 — docs/시드데이터_zerofinder.csv 와 동일
  context/        전역 상태 (회원·장바구니·선호·행동 이벤트)
  components/     NavBar · ProductCard · DetailModal · CompareTray · CheckoutModal · ChatWidget …
  pages/          Shop · Cart · Orders · Profile · Admin
  utils/          mock(폴백 구현) · events(행동 이벤트 기록)
```

## 행동 이벤트

상세 조회 / 담기 / 결제 완료 시 `docs/avro/` 스키마와 같은 payload 를
`POST /commerce-service/behaviors` 로 전송한다 (백엔드 미가동 시 로컬 기록 → 관리자 콘솔에 표시).
관리자 콘솔 이벤트 로그에서 payload 형태를 그대로 볼 수 있다.
