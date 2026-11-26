import { DemoPage } from '../pages/demo'
import { StatusPage } from '../pages/status'

export default function App() {
  return window.location.pathname === '/status' ? <StatusPage /> : <DemoPage />
}
