import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import * as api from '../api.js'
import { loadEvents, clearEvents } from '../utils/events.js'

const TOPIC = { PRODUCT_VIEWED: 'product-viewed', CART_ADDED: 'cart-added', ORDER_COMPLETED: 'order-completed' }
const TCLS = { PRODUCT_VIEWED: 'viewed', CART_ADDED: 'cart', ORDER_COMPLETED: 'order' }
const COLOR = { PRODUCT_VIEWED: '#3B82F6', CART_ADDED: '#F59E0B', ORDER_COMPLETED: '#10B981' }

/* 데모용 관리자 인증 — 실서비스에서는 백엔드 관리자 계정·권한으로 대체한다 */
const ADMIN_ID = 'admin'
const ADMIN_PW = 'zeropick5!'

export default function Admin() {
  const [authed, setAuthed] = useState(() => sessionStorage.getItem('zp_admin') === '1')
  if (!authed) return <Gate onPass={() => { sessionStorage.setItem('zp_admin', '1'); setAuthed(true) }} />
  return <AdminConsole onLogout={() => { sessionStorage.removeItem('zp_admin'); setAuthed(false) }} />
}

function Gate({ onPass }) {
  // 데모 편의상 미리 채워둔다 — 실서비스 전환 시 제거
  const [id, setId] = useState(ADMIN_ID)
  const [pw, setPw] = useState(ADMIN_PW)
  const [err, setErr] = useState('')
  const submit = () => {
    if (id === ADMIN_ID && pw === ADMIN_PW) onPass()
    else setErr('아이디 또는 비밀번호가 올바르지 않아요')
  }
  return (
    <div className="gate">
      <div className="gatecard">
        <div className="lg">🛠</div>
        <h2>ZeroPick 관리자</h2>
        <p>백엔드 공유 콘솔 — 팀원 전용</p>
        <div className="mh5">아이디</div>
        <input className="au" value={id} onChange={(e) => setId(e.target.value)} />
        <div className="mh5">비밀번호</div>
        <input className="au" type="password" value={pw} onChange={(e) => setPw(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && submit()} />
        <div className="err">{err}</div>
        <button className="primaryb" onClick={submit}>로그인</button>
        <div className="linkline"><Link to="/">← 쇼핑몰로 돌아가기</Link></div>
      </div>
    </div>
  )
}

function AdminConsole({ onLogout }) {
  const [tab, setTab] = useState('stats')
  const [tick, setTick] = useState(0)
  const [metrics, setMetrics] = useState(null)

  // 3초 폴링 + storage 이벤트(다른 탭의 고객 행동) → 실시간 갱신
  useEffect(() => {
    const iv = setInterval(() => setTick((t) => t + 1), 3000)
    const onStorage = () => setTick((t) => t + 1)
    window.addEventListener('storage', onStorage)
    return () => { clearInterval(iv); window.removeEventListener('storage', onStorage) }
  }, [])
  useEffect(() => { api.fetchMetrics().then(setMetrics) }, [tick])

  const ev = useMemo(() => loadEvents(), [tick])
  const n = (t) => ev.filter((e) => e.type === t).length
  const v = n('PRODUCT_VIEWED'), c = n('CART_ADDED'), o = n('ORDER_COMPLETED')

  return (
    <div className="adm-shell">
      <aside className="adm-side">
        <div className="adm-brand"><div className="lg">🧃</div><div><b>ZeroPick Admin</b><span>백엔드 공유 콘솔</span></div></div>
        <nav>
          <Link to="/">🏬 쇼핑몰 열기</Link>
          <div className="grp">모니터링</div>
          <button className={tab === 'stats' ? 'on' : ''} onClick={() => setTab('stats')}>📊 성과 대시보드</button>
          <button className={tab === 'log' ? 'on' : ''} onClick={() => setTab('log')}>📡 이벤트 로그</button>
          <div className="grp">계정</div>
          <button onClick={onLogout}>🚪 로그아웃</button>
        </nav>
        <div className="foot">고객 페이지의 행동이 실시간으로 흘러온다<br />실서비스: GET /recommendation-service/metrics</div>
      </aside>
      <main className="adm-main">
        <div className="adm-top">
          <h1>{tab === 'stats' ? '성과 대시보드' : '이벤트 로그'}</h1>
          <span className="live"><span className="dot" />LIVE</span>
          <div className="actions">
            {ev.some((e) => e.demo) && <button className="btn" onClick={() => { clearEvents(true); setTick((t) => t + 1) }}>예시 지우기</button>}
            <button className="btn" style={{ color: 'var(--red)' }} onClick={() => { clearEvents(); setTick((t) => t + 1) }}>초기화</button>
          </div>
        </div>
        <div className="adm-content">
          {tab === 'stats'
            ? <Stats ev={ev} v={v} c={c} o={o} metrics={metrics} />
            : <Log ev={ev} />}
        </div>
      </main>
    </div>
  )
}

function Spark({ vals, color }) {
  const mx = Math.max(...vals, 1), W = 180, H = 28
  const pts = vals.map((x, i) => `${(i / (vals.length - 1)) * W},${H - 3 - (x / mx) * (H - 6)}`).join(' ')
  return (
    <svg viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" style={{ width: '100%', height: 30, display: 'block', marginTop: 6 }}>
      <polyline points={pts} fill="none" stroke={color} strokeWidth="1.8" strokeLinejoin="round" strokeLinecap="round" />
    </svg>
  )
}

function Donut({ v, c, o }) {
  const tot = v + c + o || 1, R = 44, C = 2 * Math.PI * R
  let off = 0
  const seg = (nv, col, key) => {
    const len = (nv / tot) * C
    const el = <circle key={key} r={R} cx="60" cy="60" fill="none" stroke={col} strokeWidth="14"
      strokeDasharray={`${len} ${C - len}`} strokeDashoffset={-off} transform="rotate(-90 60 60)" />
    off += len
    return el
  }
  return (
    <svg width="120" height="120" viewBox="0 0 120 120">
      <circle r={R} cx="60" cy="60" fill="none" stroke="#F0F1F4" strokeWidth="14" />
      {seg(v, COLOR.PRODUCT_VIEWED, 'v')}{seg(c, COLOR.CART_ADDED, 'c')}{seg(o, COLOR.ORDER_COMPLETED, 'o')}
      <text x="60" y="56" textAnchor="middle" fontSize="20" fontWeight="800" fill="#111827">{v + c + o}</text>
      <text x="60" y="72" textAnchor="middle" fontSize="9.5" fill="#9CA3AF">이벤트</text>
    </svg>
  )
}

function Stats({ ev, v, c, o, metrics }) {
  const conv = v ? Math.round((o / v) * 1000) / 10 : 0
  const buckets = useMemo(() => {
    const now = Date.now(), out = []
    for (let i = 11; i >= 0; i--) {
      const s = now - (i + 1) * 5 * 60000, e = now - i * 5 * 60000
      const slice = ev.filter((x) => { const t = new Date(x.at).getTime(); return t >= s && t < e })
      out.push({
        v: slice.filter((x) => x.type === 'PRODUCT_VIEWED').length,
        c: slice.filter((x) => x.type === 'CART_ADDED').length,
        o: slice.filter((x) => x.type === 'ORDER_COMPLETED').length,
      })
    }
    return out
  }, [ev])
  const byCat = {}
  ev.forEach((e) => { byCat[e.cat] = (byCat[e.cat] || 0) + 1 })
  const cats = Object.entries(byCat).sort((a, b) => b[1] - a[1]).slice(0, 6)
  const cmx = cats.length ? cats[0][1] : 1
  const byP = {}
  ev.filter((e) => e.type === 'PRODUCT_VIEWED').forEach((e) => {
    byP[e.productId] = byP[e.productId] || { name: e.name, img: e.img, cnt: 0 }; byP[e.productId].cnt++
  })
  const tops = Object.values(byP).sort((a, b) => b.cnt - a.cnt).slice(0, 5)

  const kpi = (t, val, sub, vals, col) => (
    <div className="kpi">
      <div className="t"><span className="sw" style={{ background: col }} />{t}</div>
      <div className="v num">{val}</div>
      <div className="s">{sub}</div>
      <Spark vals={vals} color={col} />
    </div>
  )

  return (
    <>
      <div className="kpis">
        {kpi('상품 조회', v, 'PRODUCT_VIEWED · 가중치 +1', buckets.map((b) => b.v), COLOR.PRODUCT_VIEWED)}
        {kpi('장바구니 담기', c, 'CART_ADDED · 가중치 0', buckets.map((b) => b.c), COLOR.CART_ADDED)}
        {kpi('주문 완료', o, 'ORDER_COMPLETED · 가중치 +50', buckets.map((b) => b.o), COLOR.ORDER_COMPLETED)}
        {kpi('조회→주문 전환율', conv + '%', '주문 ÷ 조회', buckets.map((b) => b.v + b.c + b.o), '#6366F1')}
      </div>
      <div className="g2">
        <div className="acard">
          <div className="hd"><b>이벤트 구성</b><span>토픽별 비중</span></div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 18 }}>
            <Donut v={v} c={c} o={o} />
            <div style={{ fontSize: 12, display: 'flex', flexDirection: 'column', gap: 8, flex: 1 }}>
              {[['product-viewed', v, COLOR.PRODUCT_VIEWED], ['cart-added', c, COLOR.CART_ADDED], ['order-completed', o, COLOR.ORDER_COMPLETED]].map(([t, nv, col]) => (
                <div key={t} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <i style={{ width: 9, height: 9, borderRadius: 3, background: col }} />{t}
                  <b className="num" style={{ marginLeft: 'auto', color: 'var(--ink)' }}>{nv}</b>
                </div>
              ))}
              {metrics?.fallbackRate != null && (
                <div style={{ fontSize: 11, color: 'var(--faint)', marginTop: 4 }}>
                  챗봇 폴백률 <b className="num">{metrics.fallbackRate}%</b>
                </div>
              )}
            </div>
          </div>
        </div>
        <div className="acard">
          <div className="hd"><b>카테고리별 이벤트</b><span>전체 토픽 합산</span></div>
          {cats.length === 0 && <div className="empty">데이터 없음</div>}
          {cats.map(([k, cnt]) => (
            <div key={k} className="hbar">
              <span className="lbl">{k}</span>
              <div className="trk"><div className="fill" style={{ width: `${Math.round((cnt / cmx) * 100)}%` }} /></div>
              <b className="num">{cnt}</b>
            </div>
          ))}
        </div>
      </div>
      <div className="acard">
        <div className="hd"><b>조회 상위 상품</b><span>PRODUCT_VIEWED 기준</span></div>
        {tops.length === 0 && <div className="empty">조회 기록 없음</div>}
        {tops.map((t, i) => (
          <div key={t.name} className="toprow">
            <span className="rank" style={i === 0 ? { background: '#312E81', color: '#fff' } : undefined}>{i + 1}</span>
            {t.img && <img src={t.img} loading="lazy" alt="" onError={(e) => e.target.remove()} />}
            <span className="nm">{t.name}</span>
            <span className="cnt num">{t.cnt}<small style={{ fontWeight: 400, color: 'var(--faint)' }}> 회</small></span>
          </div>
        ))}
      </div>
      <div className="footnote">
        실서비스에서는 recommendation-service 가 behavior_log · reco_click 을 집계해 <code>GET /recommendation-service/metrics</code> 로 제공한다.
        추천 클릭률·챗봇 폴백률·추천 API 응답시간이 여기에 추가된다 — FR-12 · 핵심 10번 측정 문서화의 데이터 원천.
      </div>
    </>
  )
}

function Log({ ev }) {
  return (
    <>
      <div className="acard" style={{ padding: 0 }}>
        <div style={{ maxHeight: 560, overflow: 'auto' }}>
          <table className="adm">
            <thead>
              <tr><th style={{ width: 86 }}>시간</th><th style={{ width: 176 }}>토픽</th><th style={{ width: 60 }}>회원</th><th>상품</th><th>payload</th></tr>
            </thead>
            <tbody>
              {ev.length === 0 && (
                <tr><td colSpan={5}><div className="empty">이벤트가 없어요 — 쇼핑몰에서 상품을 클릭·담기·주문하면 여기 쌓입니다</div></td></tr>
              )}
              {ev.slice().reverse().map((b, i) => (
                <tr key={i} style={b.demo ? { opacity: .55 } : undefined}>
                  <td className="num" style={{ color: 'var(--faint)', fontSize: 11.5 }}>{new Date(b.at).toLocaleTimeString()}</td>
                  <td>
                    <span className={`topic ${TCLS[b.type]}`}>{TOPIC[b.type]}</span>
                    {b.demo && <span style={{ fontSize: 9.5, fontWeight: 700, color: '#DB2777', background: '#FDF2F8', borderRadius: 5, padding: '1px 5px', marginLeft: 6, whiteSpace: 'nowrap' }}>예시</span>}
                  </td>
                  <td className="num">#{b.memberId}</td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0 }}>
                      {b.img && <img src={b.img} loading="lazy" alt="" onError={(e) => e.target.remove()}
                        style={{ width: 28, height: 28, objectFit: 'contain', borderRadius: 6, border: '1px solid var(--line)', background: '#fff', flex: 'none' }} />}
                      <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: 260, color: 'var(--ink)' }}>{b.name}</span>
                    </div>
                  </td>
                  <td className="payload">
                    {'{'} memberId: {b.memberId}, productId: {b.productId}, category: "{b.cat}"{b.qty ? `, qty: ${b.qty}` : ''}{b.payment ? `, payment: "${b.payment}"` : ''} {'}'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
      <div className="footnote">
        payload 는 <code>docs/avro/</code> 스키마 3종과 같은 구조 (키 memberId · 파티션 3 · 컨슈머 그룹 reco-service).
        회색 행은 형태 예시 — 우측 상단 버튼으로 지울 수 있다.
      </div>
    </>
  )
}
