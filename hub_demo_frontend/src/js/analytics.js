import { api } from '@/js/api'

const VISITOR_ID_KEY = 'hub_visitor_id'

export function installVisitTracking(router) {
  router.afterEach(to => {
    if (to.path.startsWith('/admin')) return
    window.setTimeout(() => {
      api.trackVisit({
      path: to.path || '/',
        visitorId: visitorId(),
        referrer: document.referrer || '',
      }).catch(() => {})
    }, 0)
  })
}

function visitorId() {
  const existing = localStorage.getItem(VISITOR_ID_KEY)
  if (existing) return existing
  const id = createVisitorId()
  localStorage.setItem(VISITOR_ID_KEY, id)
  return id
}

function createVisitorId() {
  const bytes = new Uint8Array(16)
  if (window.crypto?.getRandomValues) {
    window.crypto.getRandomValues(bytes)
  } else {
    for (let i = 0; i < bytes.length; i += 1) {
      bytes[i] = Math.floor(Math.random() * 256)
    }
  }
  return Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('')
}
