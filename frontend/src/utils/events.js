// 행동 이벤트 로컬 기록 — Kafka 로 발행될 payload 와 같은 모양을 유지한다.
// 백엔드 가동 시에는 api.postBehavior 가 함께 전송하고, 이 기록은 관리자 화면 폴백 데이터가 된다.
const KEY = 'zp_events'

export function loadEvents() {
  try { return JSON.parse(localStorage.getItem(KEY) || '[]') } catch { return [] }
}

export function recordEvent(type, product, extra = {}, memberId = 1) {
  const occurredAt = Date.now()
  const commonPayload = {
    memberId,
    productId: product.id,
    category: product.cat,
  }
  const payload = type === 'CART_ADDED'
    ? { ...commonPayload, qty: extra.qty, occurredAt }
    : type === 'ORDER_COMPLETED'
      ? {
          ...commonPayload,
          qty: extra.qty,
          unitPrice: extra.unitPrice,
          orderNo: extra.orderNo,
          paymentMethod: extra.paymentMethod,
          occurredAt,
        }
      : { ...commonPayload, occurredAt }

  const ev = {
    type,
    memberId,
    productId: product.id,
    name: product.name,
    img: product.img || '',
    cat: product.cat,
    at: new Date(occurredAt).toISOString(),
    payload,
    ...extra,
  }
  const all = loadEvents()
  all.push(ev)
  localStorage.setItem(KEY, JSON.stringify(all.slice(-500)))
  return ev
}

export function clearEvents(demoOnly = false) {
  if (demoOnly) {
    localStorage.setItem(KEY, JSON.stringify(loadEvents().filter((e) => !e.demo)))
  } else {
    localStorage.setItem(KEY, '[]')
  }
}
