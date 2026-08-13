import { useStore } from '../context/StoreContext.jsx'

export default function ProductCard({ p, reco, onDetail, onBuy }) {
  const { addCart, compare, toggleCompare, prefs } = useStore()
  const hasBan = p.sw.some((s) => prefs.banSw.includes(s))

  return (
    <div className="pcard" onClick={() => onDetail(p)}>
      <button
        className={`cmpb ${compare.includes(p.id) ? 'on' : ''}`}
        onClick={(e) => { e.stopPropagation(); toggleCompare(p.id) }}
      >
        {compare.includes(p.id) ? '✓ 비교' : '+ 비교'}
      </button>
      <div className="thumb">
        {p.e}
        {p.img && <img src={p.img} loading="lazy" alt="" onError={(e) => e.target.remove()} />}
      </div>
      <h4>{p.name}</h4>
      <div className="brand">{p.brand}</div>
      <div className="nut">칼로리 <b>{p.kcal}kcal</b> · 당류 <b>{p.sugar}g</b> · 탄수화물 <b>{p.carb}g</b></div>
      <div className="badges">
        {reco && <span className="bdg airec">✨ 추천</span>}
        {p.sugar === 0 && p.sw.length === 0 && <span className="bdg none">감미료 무첨가</span>}
        {p.sw.slice(0, 3).map((s) => <span key={s} className="bdg">{s}</span>)}
        {hasBan && <span className="bdg" style={{ background: '#FEF2F2', color: 'var(--red)' }}>제외 감미료 포함</span>}
      </div>
      <div className="prow">
        <span className="krw num">{p.price.toLocaleString()}<small> KRW</small></span>
        <span className={`stock num ${p.stock > 0 && p.stock < 10 ? 'low' : ''}`}>
          {p.stock > 0 ? `재고 ${p.stock}` : '품절'}
        </span>
      </div>
      <div className="cardacts">
        <button onClick={(e) => { e.stopPropagation(); addCart(p) }}>🛒 담기</button>
        <button className="buy" onClick={(e) => { e.stopPropagation(); onBuy(p) }}>바로 주문</button>
      </div>
    </div>
  )
}
