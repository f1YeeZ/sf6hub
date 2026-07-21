const baseUrl = normalizeBaseUrl(process.env.BETA_API_BASE_URL || process.argv[2] || 'http://localhost:8080/api')
const timeoutMs = Number(process.env.BETA_PREFLIGHT_TIMEOUT_MS || 8000)

const requiredSecurityHeaders = [
  'x-request-id',
  'x-content-type-options',
  'x-frame-options',
  'referrer-policy',
  'permissions-policy',
  'content-security-policy',
]

const checks = [
  ['health', checkHealth],
  ['security headers', checkSecurityHeaders],
  ['character catalog', checkCharacterCatalog],
  ['announcements', checkAnnouncements],
  ['public realtime SSE', checkPublicEvents],
  ['visit analytics', checkVisitAnalytics],
]

const results = []
for (const [name, check] of checks) {
  try {
    await check()
    results.push({ name, ok: true })
  } catch (error) {
    results.push({ name, ok: false, message: error.message })
  }
}

for (const result of results) {
  if (result.ok) {
    console.log(`OK ${result.name}`)
  } else {
    console.error(`FAIL ${result.name}: ${result.message}`)
  }
}

if (results.some(result => !result.ok)) {
  process.exitCode = 1
}

async function checkHealth() {
  const response = await request('/health')
  const body = await response.json()
  if (response.status !== 200) {
    throw new Error(`expected HTTP 200, got ${response.status}`)
  }
  if (body.status !== 'UP' || body.database !== 'UP') {
    throw new Error(`expected status/database UP, got ${JSON.stringify(body)}`)
  }
}

async function checkSecurityHeaders() {
  const response = await request('/health')
  const missing = requiredSecurityHeaders.filter(header => !response.headers.get(header))
  if (missing.length) {
    throw new Error(`missing ${missing.join(', ')}`)
  }
  if (response.headers.get('x-content-type-options') !== 'nosniff') {
    throw new Error('x-content-type-options must be nosniff')
  }
}

async function checkVisitAnalytics() {
  const healthResponse = await request('/health')
  const csrfToken = healthResponse.headers.get('x-csrf-token') || ''
  const csrfCookie = parseSetCookie(healthResponse.headers.get('set-cookie') || '', 'hub_csrf')
  if (!csrfToken || !csrfCookie) {
    throw new Error('missing CSRF token for write preflight')
  }

  const response = await request('/analytics/visit', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Cookie: `hub_csrf=${csrfCookie}`,
      'X-CSRF-Token': csrfToken,
    },
    body: JSON.stringify({
      path: '/__beta_preflight__',
      visitorId: `preflight_${Date.now().toString(36)}`,
      referrer: '',
    }),
  })
  if (response.status !== 200) {
    throw new Error(`expected HTTP 200, got ${response.status}`)
  }
  const body = await response.json()
  if (body.tracked !== true && body.deduped !== true && body.unavailable !== true) {
    throw new Error(`unexpected analytics response ${JSON.stringify(body)}`)
  }
}

async function checkCharacterCatalog() {
  const response = await request('/characters')
  if (response.status !== 200) {
    throw new Error(`expected HTTP 200, got ${response.status}`)
  }
  const body = await response.json()
  if (!Array.isArray(body)) {
    throw new Error('expected JSON array')
  }
}

async function checkAnnouncements() {
  const response = await request('/announcements')
  if (response.status !== 200) {
    throw new Error(`expected HTTP 200, got ${response.status}`)
  }
  const body = await response.json()
  if (!Array.isArray(body)) {
    throw new Error('expected JSON array')
  }
}

async function checkPublicEvents() {
  const response = await request('/events', {
    headers: { Accept: 'text/event-stream' },
  })
  if (response.status !== 200) {
    throw new Error(`expected HTTP 200, got ${response.status}`)
  }
  const contentType = response.headers.get('content-type') || ''
  if (!contentType.includes('text/event-stream')) {
    throw new Error(`expected text/event-stream, got ${contentType || 'missing content-type'}`)
  }
  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('response body is not readable')
  }
  try {
    const { value } = await reader.read()
    const chunk = new TextDecoder().decode(value || new Uint8Array())
    if (!chunk.includes('event:realtime-ready')) {
      throw new Error('missing realtime-ready SSE event')
    }
  } finally {
    await reader.cancel().catch(() => {})
  }
}

async function request(path, options = {}) {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeoutMs)
  try {
    return await fetch(`${baseUrl}${path}`, {
      ...options,
      signal: controller.signal,
    })
  } catch (error) {
    if (error.name === 'AbortError') {
      throw new Error(`timed out after ${timeoutMs}ms`)
    }
    throw error
  } finally {
    clearTimeout(timer)
  }
}

function normalizeBaseUrl(value) {
  return String(value || '').replace(/\/+$/, '')
}

function parseSetCookie(value, name) {
  const prefix = `${name}=`
  const cookie = value
    .split(/,(?=\s*[^;,=]+=[^;,]+)/)
    .map(item => item.trim())
    .find(item => item.startsWith(prefix))
  return cookie?.slice(prefix.length).split(';')[0] || ''
}
