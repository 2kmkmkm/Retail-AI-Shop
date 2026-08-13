import { CATEGORIES, SWEETENERS, ALLERGENS } from '../data/seed.js'
import { useStore } from '../context/StoreContext.jsx'

export default function Profile() {
  const { member, prefs, setPrefs, showToast } = useStore()

  const toggle = (key, v) => {
    const list = prefs[key].includes(v) ? prefs[key].filter((x) => x !== v) : [...prefs[key], v]
    setPrefs({ ...prefs, [key]: list })
  }

  return (
    <div className="pagewrap">
      <h2>내 프로필 · 선호 조건</h2>
      <p className="sub">{member ? `${member.name}님의` : '게스트의'} 조건 — 저장 즉시 목록·추천·챗봇 전체에 반영돼요</p>

      <div className="lrow" style={{ display: 'block' }}>
        <div className="mh5">🚫 제외할 감미료 <small>— 이 감미료가 든 상품은 어디에도 나오지 않아요</small></div>
        <div className="chips">
          {SWEETENERS.map((s) => (
            <button key={s} className={`chip ban ${prefs.banSw.includes(s) ? 'on' : ''}`} onClick={() => toggle('banSw', s)}>{s}</button>
          ))}
        </div>
        <div className="mh5">⚠️ 알레르기</div>
        <div className="chips">
          {ALLERGENS.map((a) => (
            <button key={a} className={`chip ban ${prefs.banAllergen.includes(a) ? 'on' : ''}`} onClick={() => toggle('banAllergen', a)}>{a}</button>
          ))}
        </div>
        <div className="mh5">💜 선호 카테고리 <small>— 추천 점수에 가산돼요</small></div>
        <div className="chips">
          {CATEGORIES.map((c) => (
            <button key={c} className={`chip ${prefs.cats.includes(c) ? 'on' : ''}`} onClick={() => toggle('cats', c)}>{c}</button>
          ))}
        </div>
        <div className="mh5">💰 가격대</div>
        <div className="rangein">
          <input type="number" value={prefs.priceMin} onChange={(e) => setPrefs({ ...prefs, priceMin: Number(e.target.value) })} />
          ~
          <input type="number" value={prefs.priceMax} onChange={(e) => setPrefs({ ...prefs, priceMax: Number(e.target.value) })} />
          원
        </div>
        <button className="primaryb" style={{ marginTop: 16 }} onClick={() => showToast('✅ 선호 조건을 저장했어요')}>
          저장
        </button>
      </div>
    </div>
  )
}
