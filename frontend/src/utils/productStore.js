// 상품 CRUD 로컬 오버레이 — 백엔드 미가동 시 관리자 화면의 등록·수정·삭제를 반영한다.
// 시드는 정적이므로 변경분(추가/수정/삭제)만 localStorage에 겹쳐 둔다.
// 백엔드가 올라오면 api.js 가 실제 CRUD API를 쓰고 이 오버레이는 호출되지 않는다.
import { seedProducts } from '../data/seed.js'

const KEY = 'zp_prod_overlay'

function overlay() {
  try { return JSON.parse(localStorage.getItem(KEY) || '{"added":[],"updated":{},"deleted":[]}') }
  catch { return { added: [], updated: {}, deleted: [] } }
}
function save(o) { localStorage.setItem(KEY, JSON.stringify(o)) }

export function getProducts() {
  const o = overlay()
  const base = seedProducts
    .filter((p) => !o.deleted.includes(p.id))
    .map((p) => (o.updated[p.id] ? { ...p, ...o.updated[p.id] } : p))
  return [...base, ...o.added.filter((p) => !o.deleted.includes(p.id))]
}

export function addProduct(body) {
  const o = overlay()
  const maxId = Math.max(...seedProducts.map((p) => p.id), ...o.added.map((p) => p.id), 0)
  const p = {
    id: maxId + 1,
    name: body.name, brand: body.brand || '', cat: body.category || '기타', e: '🛒',
    kcal: Number(body.kcal) || 0, sugar: Number(body.sugarG) || 0, carb: Number(body.carbG) || 0,
    sw: body.sweeteners || [], price: Number(body.price) || 0, stock: Number(body.stock) || 0,
    img: body.imageUrl || '',
  }
  o.added.push(p); save(o)
  return p
}

export function updateProduct(id, patch) {
  const o = overlay()
  const added = o.added.find((p) => p.id === Number(id))
  if (added) Object.assign(added, patch)
  else o.updated[id] = { ...(o.updated[id] || {}), ...patch }
  save(o)
  return getProducts().find((p) => p.id === Number(id))
}

export function deleteProduct(id) {
  const o = overlay()
  o.deleted.push(Number(id)); save(o)
}
