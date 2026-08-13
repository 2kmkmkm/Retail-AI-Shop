import { Routes, Route } from 'react-router-dom'
import { useStore } from './context/StoreContext.jsx'
import NavBar from './components/NavBar.jsx'
import Toast from './components/Toast.jsx'
import ChatWidget from './components/ChatWidget.jsx'
import Shop from './pages/Shop.jsx'
import Cart from './pages/Cart.jsx'
import Orders from './pages/Orders.jsx'
import Profile from './pages/Profile.jsx'
import Admin from './pages/Admin.jsx'

export default function App() {
  const { mock } = useStore()
  return (
    <Routes>
      <Route path="/admin/*" element={<Admin />} />
      <Route
        path="*"
        element={
          <>
            {mock && (
              <div className="mockbar">
                백엔드 미연결 — 시드 데이터로 동작 중 (게이트웨이 :8000 이 올라오면 자동으로 실제 API를 사용합니다)
              </div>
            )}
            <NavBar />
            <Routes>
              <Route path="/" element={<Shop />} />
              <Route path="/cart" element={<Cart />} />
              <Route path="/orders" element={<Orders />} />
              <Route path="/profile" element={<Profile />} />
            </Routes>
            <ChatWidget />
            <Toast />
          </>
        }
      />
    </Routes>
  )
}
