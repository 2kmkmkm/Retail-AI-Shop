import { CATEGORIES, SWEETENERS } from '../data/seed.js'
import { useStore } from '../context/StoreContext.jsx'

export default function FilterSidebar({ filters, setFilters }) {
  const { prefs, setPrefs } = useStore()

  const toggleCat = (c) => setFilters((f) => ({ ...f, category: f.category === c ? null : c }))
  const toggleBan = (s) => {
    const banSw = prefs.banSw.includes(s) ? prefs.banSw.filter((x) => x !== s) : [...prefs.banSw, s]
    setPrefs({ ...prefs, banSw })
  }

  return (
    <aside className="side">
      <section>
        <h5>카테고리</h5>
        <div className="catlist">
          {CATEGORIES.map((c) => (
            <button key={c} className={`cat ${filters.category === c ? 'on' : ''}`} onClick={() => toggleCat(c)}>
              {c}
            </button>
          ))}
        </div>
      </section>
      <section>
        <h5>감미료 <span style={{ textTransform: 'none', fontWeight: 400 }}>(체크 = 제외)</span></h5>
        <div className="swlist">
          {SWEETENERS.map((s) => (
            <label key={s} className="swrow">
              <input type="checkbox" checked={prefs.banSw.includes(s)} onChange={() => toggleBan(s)} />
              {s}
              {prefs.banSw.includes(s) && <span className="ban">제외</span>}
            </label>
          ))}
        </div>
      </section>
      <section>
        <h5>칼로리 (kcal)</h5>
        <div className="rangein">
          <input type="number" value={filters.minKcal} onChange={(e) => setFilters((f) => ({ ...f, minKcal: e.target.value }))} />
          ~
          <input type="number" value={filters.maxKcal} onChange={(e) => setFilters((f) => ({ ...f, maxKcal: e.target.value }))} />
        </div>
      </section>
      <button className="resetb" onClick={() => { setFilters({ category: null, minKcal: 0, maxKcal: 999 }); setPrefs({ ...prefs, banSw: [] }) }}>
        필터 초기화
      </button>
    </aside>
  )
}
