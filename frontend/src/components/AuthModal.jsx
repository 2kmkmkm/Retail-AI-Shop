import { useState } from 'react'
import { useStore } from '../context/StoreContext.jsx'
import * as api from '../api.js'

export default function AuthModal({ mode, setMode, onClose }) {
  const { setMember, showToast } = useStore()
  const join = mode === 'join'
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [err, setErr] = useState('')

  const submit = async () => {
    setErr('')
    if ((join && !form.name) || !form.email || !form.password) {
      setErr('모든 항목을 입력해 주세요')
      return
    }
    try {
      const res = join
        ? await api.join(form)
        : await api.login({ email: form.email, password: form.password })
      setMember({ memberId: res.memberId, name: res.name })
      showToast(join ? `✅ 가입 완료 — ${res.name}님 환영해요` : `✅ ${res.name}님 로그인`)
      onClose()
    } catch (e) {
      const status = e.status || e.response?.status
      if (status === 409) setErr('이미 가입된 이메일이에요')
      else if (status === 401) setErr('이메일 또는 비밀번호가 올바르지 않아요')
      else setErr('요청에 실패했어요 — 잠시 후 다시 시도해 주세요')
    }
  }

  return (
    <div className="modalbg show" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal sm">
        <button className="x" onClick={onClose}>✕</button>
        <h3>{join ? '회원가입' : '로그인'}</h3>
        <p className="sub2">{join ? '이메일로 간단히 가입해요' : '다시 만나서 반가워요'}</p>
        {join && (
          <>
            <div className="mh5">이름</div>
            <input className="au" value={form.name} placeholder="이름"
              onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </>
        )}
        <div className="mh5">이메일</div>
        <input className="au" value={form.email} placeholder="이메일"
          onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <div className="mh5">비밀번호</div>
        <input className="au" type="password" value={form.password} placeholder="비밀번호"
          onKeyDown={(e) => e.key === 'Enter' && submit()}
          onChange={(e) => setForm({ ...form, password: e.target.value })} />
        <div className="err">{err}</div>
        <button className="primaryb" onClick={submit}>{join ? '가입하기' : '로그인'}</button>
        <div className="linkline">
          {join ? '이미 계정이 있나요? ' : '계정이 없나요? '}
          <a onClick={() => setMode(join ? 'login' : 'join')}>{join ? '로그인' : '회원가입'}</a>
        </div>
      </div>
    </div>
  )
}
