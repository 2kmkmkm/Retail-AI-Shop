import { useStore } from '../context/StoreContext.jsx'

export default function Toast() {
  const { toast } = useStore()
  if (!toast) return null
  return <div className="toastbox">{toast}</div>
}
