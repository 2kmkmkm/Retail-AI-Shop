import { useState } from 'react'
import { useStore } from '../context/StoreContext.jsx'
import { getProducts } from '../utils/productStore.js'
import * as api from '../api.js'

export default function CompareTray() {
  const { compare, toggleCompare, setCompare, addCart, showToast } = useStore()
  const [rows, setRows] = useState(null)
  const items = compare.map((id) => getProducts().find((p) => p.id === id)).filter(Boolean)

  const openCompare = async () => {
    const data = await api.compareProducts(compare)
    setRows(Array.isArray(data) ? data : items)
  }

  if (compare.length === 0) return null

  const best = (key, lower = true) => {
    if (!rows) return null
    const vals = rows.map((p) => p[key])
    return lower ? Math.min(...vals) : Math.max(...vals)
  }

  return (
    <>
      <div className={`tray ${compare.length ? 'show' : ''}`}>
        <div className="tray-in">
          <b style={{ fontSize: 13 }}>비교함</b>
          {items.map((p) => (
            <span key={p.id} className="trayitem">
              {p.name.slice(0, 14)}
              <button onClick={() => toggleCompare(p.id)}>✕</button>
            </span>
          ))}
          <button className="traygo" disabled={compare.length < 2} onClick={openCompare}>
            비교하기 ({compare.length})
          </button>
        </div>
      </div>
      {rows && (
        <div className="modalbg show" onClick={(e) => e.target === e.currentTarget && setRows(null)}>
          <div className="modal" style={{ maxWidth: Math.min(220 + rows.length * 180, 1100) }}>
            <button className="x" onClick={() => setRows(null)}>✕</button>
            <h3>영양성분 비교</h3>
            <p className="sub2">초록색이 항목별 우위 — 비교 개수 제한은 없어요</p>
            <div className="cmp-table-wrap">
              <table className="cmp">
                <thead>
                  <tr>
                    <th></th>
                    {rows.map((p) => (
                      <th key={p.id}>{p.name}<br /><span style={{ fontWeight: 400, color: 'var(--faint)' }}>{p.brand}</span></th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {[['칼로리', 'kcal', ' kcal', true], ['당류', 'sugar', ' g', true], ['탄수화물', 'carb', ' g', true], ['가격', 'price', ' KRW', true]].map(([label, key, unit, lower]) => (
                    <tr key={key}>
                      <td>{label}</td>
                      {rows.map((p) => (
                        <td key={p.id} className={`num ${p[key] === best(key, lower) ? 'best' : ''}`}>
                          {key === 'price' ? p[key].toLocaleString() : p[key]}{unit}
                        </td>
                      ))}
                    </tr>
                  ))}
                  <tr>
                    <td>감미료</td>
                    {rows.map((p) => <td key={p.id} style={{ fontSize: 11.5 }}>{p.sw.join(', ') || '무첨가'}</td>)}
                  </tr>
                  <tr>
                    <td></td>
                    {rows.map((p) => (
                      <td key={p.id}>
                        <button className="pillb" style={{ fontSize: 12 }}
                          onClick={() => { addCart(p); showToast(`🛒 ${p.name.slice(0, 14)} 담았어요`) }}>담기</button>
                      </td>
                    ))}
                  </tr>
                </tbody>
              </table>
            </div>
            <button className="resetb" style={{ marginTop: 14 }} onClick={() => { setCompare([]); setRows(null) }}>
              비교함 비우기
            </button>
          </div>
        </div>
      )}
    </>
  )
}
