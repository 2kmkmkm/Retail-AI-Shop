// 백엔드 응답(openapi 계약: category·sugarG·imageUrl…)을 화면 모델(cat·sugar·img…)로 정규화한다.
// 폴백(시드) 데이터는 이미 화면 모델이므로 그대로 통과시킨다.
const CAT_EMOJI = {
  '음료': '🥤', '간식/디저트': '🍫', '육가공품': '🍖', '조미료/소스': '🧂', '유제품': '🥛',
  '주식/면류': '🍜', '즉석식품': '🍱', '건강기능식품': '💊', '수산가공품': '🐟', '기타': '🛒',
}

export function toViewProduct(raw) {
  if (!raw) return raw
  if (raw.cat !== undefined) return raw
  return {
    id: raw.id,
    name: raw.name,
    brand: raw.brand || '',
    cat: raw.category || '기타',
    e: CAT_EMOJI[raw.category] || '🛒',
    kcal: raw.kcal ?? 0,
    sugar: raw.sugarG ?? 0,
    carb: raw.carbG ?? 0,
    protein: raw.proteinG ?? 0,
    fat: raw.fatG ?? 0,
    sodium: raw.sodiumMg ?? 0,
    serving: raw.servingSize ?? 0,
    servingUnit: raw.servingUnit || 'g',
    sw: raw.sweeteners || [],
    swd: (raw.sweetenerAmounts || []).map((a) => ({ n: a.name, g: a.amountG })),
    price: raw.price ?? 0,
    stock: raw.stock ?? 0,
    img: raw.imageUrl || '',
    nutriImg: raw.nutritionFactsUrl || '',
  }
}

export const toViewProducts = (list) => (Array.isArray(list) ? list.map(toViewProduct) : list)

// 화면 모델 → 상품 등록/수정 요청(openapi ProductRequest). 관리자 CRUD 화면이 사용한다.
export function toApiProduct(p) {
  return {
    name: p.name,
    brand: p.brand || '기타',
    category: p.cat || p.category || '기타',
    price: Number(p.price) || 0,
    stock: Number(p.stock) || 0,
    kcal: Number(p.kcal) || 0,
    sugarG: Number(p.sugar ?? p.sugarG) || 0,
    carbG: Number(p.carb ?? p.carbG) || 0,
    imageUrl: p.img || p.imageUrl || undefined,
    sweeteners: p.sw || p.sweeteners || [],
  }
}

// 화면 선호조건(banSw·banAllergen·cats) → 계약 Preference(excludedSweeteners·allergens·categories).
// 필드명이 다른 채로 보내면 201 로 성공하면서 조건만 빈 배열로 저장된다.
export function toApiPreference(memberId, prefs) {
  return {
    memberId,
    priceMin: Number(prefs.priceMin) || 0,
    priceMax: Number(prefs.priceMax) || 100000,
    categories: prefs.cats || [],
    excludedSweeteners: prefs.banSw || [],
    allergens: prefs.banAllergen || [],
  }
}

// 계약 Preference → 화면 선호조건 (온보딩 시 서버 저장분 복원)
export function toViewPreference(raw) {
  if (!raw) return null
  return {
    banSw: raw.excludedSweeteners || [],
    banAllergen: raw.allergens || [],
    cats: raw.categories || [],
    priceMin: raw.priceMin ?? 0,
    priceMax: raw.priceMax ?? 100000,
  }
}
