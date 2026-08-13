import { useEffect, useState } from 'react'
import { useStore } from '../context/StoreContext.jsx'
import * as api from '../api.js'

export default function Orders() {
  const { member } = useStore()
  const [orders, setOrders] = useState([])

  useEffect(() => {
    api.fetchOrders(member?.memberId || 1).then((data) => setOrders(Array.isArray(data) ? data : []))
  }, [member])

  return (
    <div className="pagewrap">
      <h2>주문내역</h2>
      <p className="sub num">{orders.length}건</p>
      {orders.length === 0 && <div className="empty">주문 내역이 없어요</div>}
      {orders.map((o) => (
        <div key={o.orderNo} className="lrow">
          <div className="nm">
            <b className="num">{o.orderNo}</b>
            <span>
              {(o.items || []).map((i) => `${i.name} × ${i.qty}`).join(', ')}
              {o.paymentMethod && ` · ${o.paymentMethod}`}
              {o.orderedAt && ` · ${new Date(o.orderedAt).toLocaleString()}`}
            </span>
          </div>
          <span className={`status ${o.status}`}>{o.status}</span>
          <div className="rp num">{(o.totalPrice || 0).toLocaleString()}원</div>
        </div>
      ))}
    </div>
  )
}
