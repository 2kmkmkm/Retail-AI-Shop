import axios from 'axios'
import { seedProducts } from './data/seed.js'
import * as store from './utils/productStore.js'
import * as mock from './utils/mock.js'

// 게이트웨이 라우팅 규칙(/<service-name>/**) 그대로 호출한다.
// 개발 서버에서는 vite 프록시가 :8000 으로 넘긴다 (vite.config.js).
const api = axios.create({
  headers: { 'Content-Type': 'application/json' },
  timeout: 4000,
})

// 백엔드가 아직 안 떠 있으면 시드 데이터로 폴백한다.
// 어떤 호출이든 한 번 실패하면 mockMode 로 표시해 화면에 배너를 띄운다.
let mockMode = false
const mockListeners = new Set()
export const isMock = () => mockMode
export const onMockChange = (fn) => { mockListeners.add(fn); return () => mockListeners.delete(fn) }
function setMock(v = true) {
  if (mockMode !== v) { mockMode = v; mockListeners.forEach((fn) => fn(v)) }
}

// vite 프록시는 게이트웨이가 죽어 있으면 에러 대신 index.html 을 200 으로 돌려준다.
// JSON 이 아닌 응답은 "백엔드 없음"으로 간주해 폴백한다.
function isJsonResponse(res) {
  const ct = String(res.headers?.['content-type'] || '')
  return ct.includes('application/json') || (typeof res.data === 'object' && res.data !== null)
}

async function tryApi(call, fallback) {
  try {
    const res = await call()
    if (!isJsonResponse(res)) { setMock(true); return fallback() }
    setMock(false)   // 진짜 백엔드 JSON 응답 — 폴백 모드 해제
    return res.data
  } catch (e) {
    if (!e.response || e.response.status >= 500) {
      setMock(true)  // 네트워크 단절·프록시/서버 5xx 는 폴백
      return fallback()
    }
    throw e          // 4xx(401·409 등)는 실제 업무 응답 — 화면 로직이 처리
  }
}

/* ── product-service ── */
export const fetchProducts = (params) =>
  tryApi(() => api.get('/product-service/products', { params }),
    () => mock.filterProducts(store.getProducts(), params))

export const fetchProduct = (id) =>
  tryApi(() => api.get(`/product-service/products/${id}`),
    () => store.getProducts().find((p) => p.id === Number(id)))

export const compareProducts = (ids) =>
  tryApi(() => api.get('/product-service/products/compare', { params: { ids: ids.join(',') } }),
    () => store.getProducts().filter((p) => ids.includes(p.id)))

export const createProduct = (body) =>
  tryApi(() => api.post('/product-service/products', body), () => store.addProduct(body))

export const updateProduct = (id, body) =>
  tryApi(() => api.put(`/product-service/products/${id}`, body), () => store.updateProduct(id, body))

export const deleteProduct = (id) =>
  tryApi(() => api.delete(`/product-service/products/${id}`), () => store.deleteProduct(id))

/* ── commerce-service ── */
export const join = (body) =>
  tryApi(() => api.post('/commerce-service/members', body), () => mock.join(body))

export const login = (body) =>
  tryApi(() => api.post('/commerce-service/members/login', body), () => mock.login(body))

export const createOrder = (body) =>
  tryApi(() => api.post('/commerce-service/orders', body), () => mock.createOrder(body))

export const payOrder = (orderId, body) =>
  tryApi(() => api.post(`/commerce-service/orders/${orderId}/pay`, body), () => mock.payOrder(orderId, body))

export const cancelOrder = (orderId) =>
  tryApi(() => api.post(`/commerce-service/orders/${orderId}/cancel`), () => mock.cancelOrder(orderId))

export const fetchOrders = (memberId) =>
  tryApi(() => api.get('/commerce-service/orders', { params: { memberId } }), () => mock.fetchOrders())

// 조회 행동 이벤트 전송 (POST /commerce-service/behaviors — PR #12 계약).
// 담기·주문 이벤트는 장바구니·결제 API 처리 중 백엔드가 직접 발행하므로 보내지 않는다.
export const postBehavior = (ev) =>
  api.post('/commerce-service/behaviors', {
    memberId: ev.memberId,
    productId: ev.productId,
    eventType: ev.type,
    category: ev.cat,
    occurredAt: ev.at,
  }).catch(() => {})

/* ── recommendation-service ── */
export const savePreferences = (body) =>
  tryApi(() => api.post('/recommendation-service/preferences', body), () => body)

export const fetchRecommendations = (memberId) =>
  tryApi(() => api.get(`/recommendation-service/recommendations/${memberId}`),
    () => mock.recommend(store.getProducts()))

export const chat = (body) =>
  tryApi(() => api.post('/recommendation-service/chat', body),
    () => mock.chat(store.getProducts(), body.message))

export const fetchMetrics = () =>
  tryApi(() => api.get('/recommendation-service/metrics'), () => mock.metrics())

// 추천 상품 클릭 — 클릭률(선택 6) 집계용. 실패 시 로컬 기록으로 폴백.
export const postRecoClick = (body) =>
  api.post('/recommendation-service/click', body).catch(() => {
    try {
      const all = JSON.parse(localStorage.getItem('zp_reco_clicks') || '[]')
      all.push({ ...body, at: new Date().toISOString() })
      localStorage.setItem('zp_reco_clicks', JSON.stringify(all.slice(-500)))
    } catch (e) { /* ignore */ }
  })

// 자연어 상품 검색 (선택 10) — 조건 추출 후 상품 반환. 폴백은 챗봇과 같은 규칙 파서.
export const nlSearch = (message) =>
  tryApi(() => api.post('/recommendation-service/search', { message }),
    () => mock.chat(store.getProducts(), message))

// 장바구니 서버 동기화 — 백엔드가 있으면 cart_item 저장 + cart-added 발행 트리거.
// 실패해도 화면은 로컬 장바구니로 동작한다 (베스트에포트).
export const syncCartAdd = (memberId, productId, qty) =>
  api.post('/commerce-service/carts', { memberId, productId, qty }).catch(() => {})
export const syncCartUpdate = (cartItemId, qty) =>
  api.put(`/commerce-service/carts/${cartItemId}`, { qty }).catch(() => {})
export const syncCartRemove = (cartItemId) =>
  api.delete(`/commerce-service/carts/${cartItemId}`).catch(() => {})
