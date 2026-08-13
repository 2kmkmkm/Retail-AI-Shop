import { useEffect } from 'react'
import { useStore } from '../context/StoreContext.jsx'

export default function DetailModal({ p, onClose, onBuy }) {
  const { addCart, emit, prefs } = useStore()
  const conflicts = p.sw.filter((s) => prefs.banSw.includes(s))

  // 상세 진입 = 조회 행동 이벤트 (추천 가중치 +1)
  useEffect(() => { emit('PRODUCT_VIEWED', p) }, [p, emit])

  return (
    <div className="modalbg show" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal">
        <button className="x" onClick={onClose}>✕</button>
        <div style={{ display: 'flex', gap: 18, alignItems: 'flex-start' }}>
          <div className="thumb" style={{ width: 150, height: 150, flex: 'none', fontSize: 56 }}>
            {p.e}
            {p.img && <img src={p.img} alt="" onError={(e) => e.target.remove()} />}
          </div>
          <div style={{ flex: 1 }}>
            <h3>{p.name}</h3>
            <p className="sub2">{p.brand} · {p.cat}</p>
            <div className="badges" style={{ marginBottom: 10 }}>
              {p.sw.length === 0
                ? <span className="bdg none">감미료 무첨가</span>
                : p.sw.map((s) => <span key={s} className="bdg">{s}</span>)}
            </div>
            {conflicts.length > 0 && (
              <div style={{ background: '#FEF2F2', border: '1px solid #FECACA', color: '#B91C1C',
                            borderRadius: 10, padding: '8px 12px', fontSize: 12.5, marginBottom: 10 }}>
                ⚠️ 프로필에서 제외한 감미료({conflicts.join(', ')})가 들어 있어요
              </div>
            )}
            <table className="cmp" style={{ minWidth: 0 }}>
              <tbody>
                <tr><td>칼로리</td><td><b className="num">{p.kcal} kcal</b></td></tr>
                <tr><td>당류</td><td><b className="num">{p.sugar} g</b></td></tr>
                <tr><td>탄수화물</td><td><b className="num">{p.carb} g</b></td></tr>
                <tr><td>가격</td><td><b className="num">{p.price.toLocaleString()} KRW</b></td></tr>
                <tr><td>재고</td><td><b className="num">{p.stock}</b></td></tr>
              </tbody>
            </table>
          </div>
        </div>
        <div style={{ display: 'flex', gap: 8, marginTop: 18 }}>
          <button className="pillb" style={{ flex: 1, padding: 12 }} onClick={() => { addCart(p); onClose() }}>
            🛒 장바구니 담기
          </button>
          <button className="primaryb" style={{ flex: 1 }} onClick={() => onBuy(p)}>바로 주문</button>
        </div>
      </div>
    </div>
  )
}
