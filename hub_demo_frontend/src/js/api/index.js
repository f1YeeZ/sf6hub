const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
const CSRF_COOKIE = 'hub_csrf'
let csrfToken = ''

export function apiUrl(path) {
  return `${API_BASE_URL}${path}`
}

export function createApiEventSource(path) {
  return new EventSource(`${API_BASE_URL}${path}`, { withCredentials: true })
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  const method = options.method || 'GET'
  if (!isSafeMethod(method) || options.protect) {
    const token = await ensureCsrfToken()
    if (token) headers['X-CSRF-Token'] = token
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    credentials: 'include',
    body: options.body ? JSON.stringify(options.body) : undefined,
  })
  captureCsrfToken(response)

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: '请求失败' }))
    throw new Error(error.message || `HTTP ${response.status}`)
  }

  const text = await response.text()
  if (!text) return null
  return JSON.parse(text)
}

async function uploadRequest(path, file, extra = {}, options = {}) {
  const formData = new FormData()
  formData.append('file', file)
  Object.entries(extra).forEach(([key, value]) => formData.append(key, value))

  const headers = {}
  const token = await ensureCsrfToken()
  if (token) headers['X-CSRF-Token'] = token

  if (typeof options.onProgress === 'function' && typeof XMLHttpRequest !== 'undefined') {
    return uploadRequestWithProgress(path, formData, headers, options.onProgress)
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers,
    credentials: 'include',
    body: formData,
  })
  captureCsrfToken(response)

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: '上传失败' }))
    throw new Error(error.message || `HTTP ${response.status}`)
  }

  return response.json()
}

function uploadRequestWithProgress(path, formData, headers, onProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', `${API_BASE_URL}${path}`)
    xhr.withCredentials = true
    Object.entries(headers).forEach(([key, value]) => xhr.setRequestHeader(key, value))
    xhr.upload.onprogress = event => {
      if (!event.lengthComputable) return
      onProgress(Math.round((event.loaded / event.total) * 100))
    }
    xhr.onload = () => {
      const responseHeaders = xhr.getAllResponseHeaders()
      const tokenMatch = responseHeaders.match(/^x-csrf-token:\s*(.+)$/im)
      if (tokenMatch?.[1]) csrfToken = tokenMatch[1].trim()
      if (xhr.status < 200 || xhr.status >= 300) {
        let message = `HTTP ${xhr.status}`
        try {
          message = JSON.parse(xhr.responseText || '{}').message || message
        } catch (_) {}
        reject(new Error(message))
        return
      }
      try {
        resolve(JSON.parse(xhr.responseText || 'null'))
      } catch (_) {
        reject(new Error('上传响应解析失败'))
      }
    }
    xhr.onerror = () => reject(new Error('上传失败，请检查网络'))
    xhr.onabort = () => reject(new Error('上传已取消'))
    xhr.send(formData)
  })
}

function queryString(params = {}) {
  const query = new URLSearchParams(Object.entries(params).filter(([, value]) => value !== '' && value !== undefined && value !== null))
  return query.size ? `?${query}` : ''
}

function isSafeMethod(method) {
  return ['GET', 'HEAD', 'OPTIONS'].includes(String(method).toUpperCase())
}

async function ensureCsrfToken() {
  const token = csrfToken || getCookie(CSRF_COOKIE)
  if (token) return token
  const response = await fetch(`${API_BASE_URL}/health`, { credentials: 'include' })
  captureCsrfToken(response)
  return csrfToken || getCookie(CSRF_COOKIE)
}

function captureCsrfToken(response) {
  if (response.headers.has('X-CSRF-Token')) {
    csrfToken = response.headers.get('X-CSRF-Token') || ''
  }
}

function clearCsrfToken() {
  csrfToken = ''
}

function getCookie(name) {
  const prefix = `${name}=`
  return document.cookie
    .split(';')
    .map(item => item.trim())
    .find(item => item.startsWith(prefix))
    ?.slice(prefix.length) || ''
}

function normalizeUsername(username) {
  const value = String(username || '').trim()
  if (value.length < 3 || value.length > 20) throw new Error('用户名长度需为 3-20 个字符')
  if (!/^[A-Za-z0-9_-]+$/.test(value)) throw new Error('用户名只能包含字母、数字、下划线或短横线')
  return value
}

function normalizeEmail(email) {
  const value = String(email || '').trim().toLowerCase()
  if (!value) throw new Error('邮箱不能为空')
  return value
}

export const api = {
  getMe() {
    return request('/me')
  },

  getAdminMe() {
    return request('/admin/me')
  },

  refreshAuth() {
    return request('/auth/refresh', { method: 'POST' })
  },

  logout() {
    return request('/auth/logout', { method: 'POST' }).finally(clearCsrfToken)
  },

  adminLogout() {
    return request('/auth/admin/logout', { method: 'POST' }).finally(clearCsrfToken)
  },

  getCharacters() {
    return request('/characters')
  },

  getCharacter(id) {
    return request(`/characters/${id}`)
  },

  getFrames(characterId) {
    return request(`/characters/${characterId}/frames`)
  },

  getCombos(characterId, params = {}) {
    return request(`/characters/${characterId}/combos${queryString(params)}`, { protect: true })
  },

  getComboParentOptions(characterId, params = {}) {
    return request(`/characters/${characterId}/combo-parent-options${queryString(params)}`, { protect: true })
  },

  getWorldTourCombos(params = {}) {
    return request(`/world-tour/combos${queryString(params)}`, { protect: true })
  },

  getWorldTourComboFilterOptions() {
    return request('/world-tour/combo-filter-options', { protect: true })
  },

  getWeeklyComboContributors() {
    return request('/combos/weekly-contributors')
  },

  getComboFilterOptions(characterId, params = {}) {
    return request(`/characters/${characterId}/combo-filter-options${queryString(params)}`, { protect: true })
  },

  getCombo(id) {
    return request(`/combos/${id}`, { protect: true })
  },

  getComboFollowups(id) {
    return request(`/combos/${id}/followups`, { protect: true })
  },

  createCombo(payload) {
    return request('/combos', { method: 'POST', body: payload })
  },

  checkComboDuplicates(payload) {
    return request('/combos/duplicate-check', { method: 'POST', body: payload })
  },

  updateCombo(id, payload) {
    return request(`/combos/${id}`, { method: 'PUT', body: payload })
  },

  deleteCombo(id) {
    return request(`/combos/${id}`, { method: 'DELETE' })
  },

  approveCombo(id) {
    return request(`/admin/combos/${id}/approve`, { method: 'POST' })
  },

  reviewAdminCombo(id, payload) {
    return request(`/admin/combos/${id}/review`, { method: 'PUT', body: payload })
  },

  getAdminComboDuplicates(id) {
    return request(`/admin/combos/${id}/duplicates`)
  },

  updateAdminComboVideoReview(id, payload) {
    return request(`/admin/combos/${id}/video-review`, { method: 'PUT', body: payload })
  },

  banUser(userId) {
    return request(`/admin/users/${userId}/ban`, { method: 'POST' })
  },

  getAnnouncements(params = {}) {
    return request(`/announcements${queryString(params)}`)
  },

  getAdminDashboard() {
    return request('/admin/dashboard')
  },

  getAdminUsers(params = {}) {
    return request(`/admin/users${queryString(params)}`)
  },

  getAdminCharacters() {
    return request('/admin/characters')
  },

  updateAdminUser(id, payload) {
    return request(`/admin/users/${id}`, { method: 'PUT', body: payload })
  },

  unbanUser(userId) {
    return request(`/admin/users/${userId}/unban`, { method: 'POST' })
  },

  getAdminFrames(params = {}) {
    return request(`/admin/frames${queryString(params)}`)
  },

  createAdminFrame(payload) {
    return request('/admin/frames', { method: 'POST', body: payload })
  },

  updateAdminFrame(id, payload) {
    return request(`/admin/frames/${id}`, { method: 'PUT', body: payload })
  },

  deleteAdminFrame(id) {
    return request(`/admin/frames/${id}`, { method: 'DELETE' })
  },

  syncOfficialFrames() {
    return request('/admin/frames/sync-official', { method: 'POST' })
  },

  getAdminFrameSyncLogs() {
    return request('/admin/frames/sync-logs')
  },

  getAdminFrameChangeHistory() {
    return request('/admin/frames/change-history')
  },

  getAdminReports(params = {}) {
    return request(`/admin/reports${queryString(params)}`)
  },

  updateAdminReport(id, payload) {
    return request(`/admin/reports/${id}`, { method: 'PUT', body: payload })
  },

  updateAdminReportsBatch(payload) {
    return request('/admin/reports/batch', { method: 'PUT', body: payload })
  },

  getAdminFeedbacks(params = {}) {
    return request(`/admin/feedbacks${queryString(params)}`)
  },

  updateAdminFeedback(id, payload) {
    return request(`/admin/feedbacks/${id}`, { method: 'PUT', body: payload })
  },

  updateAdminFeedbacksBatch(payload) {
    return request('/admin/feedbacks/batch', { method: 'PUT', body: payload })
  },

  getAdminCombos(params = {}) {
    return request(`/admin/combos${queryString(params)}`)
  },

  createAdminCharacter(payload) {
    return request('/admin/characters', { method: 'POST', body: payload })
  },

  updateAdminCharacter(id, payload) {
    return request(`/admin/characters/${id}`, { method: 'PUT', body: payload })
  },

  reorderAdminCharacters(ids) {
    return request('/admin/characters/reorder', { method: 'PUT', body: { ids } })
  },

  deleteAdminCharacter(id) {
    return request(`/admin/characters/${id}`, { method: 'DELETE' })
  },

  createAdminCombo(payload) {
    return request('/admin/combos', { method: 'POST', body: payload })
  },

  updateAdminCombo(id, payload) {
    return request(`/admin/combos/${id}`, { method: 'PUT', body: payload })
  },

  deleteAdminCombo(id) {
    return request(`/admin/combos/${id}`, { method: 'DELETE' })
  },

  getAdminAnnouncements() {
    return request('/admin/announcements')
  },

  createAdminAnnouncement(payload) {
    return request('/admin/announcements', { method: 'POST', body: payload })
  },

  updateAdminAnnouncement(id, payload) {
    return request(`/admin/announcements/${id}`, { method: 'PUT', body: payload })
  },

  deleteAdminAnnouncement(id) {
    return request(`/admin/announcements/${id}`, { method: 'DELETE' })
  },

  broadcastAdminNotification(payload) {
    return request('/admin/notifications/broadcast', { method: 'POST', body: payload })
  },

  getAdminAuditLogs(params = {}) {
    return request(`/admin/audit-logs${queryString(params)}`)
  },

  openAdminEvents() {
    return createApiEventSource('/admin/events')
  },

  openEvents() {
    return createApiEventSource('/events')
  },

  uploadFile(file, usage = 'avatar', options = {}) {
    return uploadRequest('/files', file, { usage }, options)
  },

  getNotifications(params = {}) {
    return request(`/notifications${queryString(params)}`)
  },

  markNotificationRead(notificationId) {
    return request(`/notifications/${notificationId}/read`, { method: 'POST' })
  },

  markNotificationsReadAll(params = {}) {
    return request(`/notifications/read-all${queryString(params)}`, { method: 'POST' })
  },

  getUserCombos(userId, params = {}) {
    return request(`/users/${userId}/combos${queryString(params)}`, { protect: true })
  },

  getUserFavoriteCombos(userId, params = {}) {
    return request(`/users/${userId}/favorite-combos${queryString(params)}`, { protect: true })
  },

  reportTarget(payload) {
    return request('/reports', { method: 'POST', body: payload })
  },

  submitFeedback(payload) {
    return request('/feedback', { method: 'POST', body: payload })
  },

  trackVisit(payload) {
    return request('/analytics/visit', { method: 'POST', body: payload })
  },

  likeCombo(id) {
    return request(`/combos/${id}/like`, { method: 'POST' })
  },

  favoriteCombo(id) {
    return request(`/combos/${id}/favorite`, { method: 'POST' })
  },

  login(email, password) {
    return request('/auth/login', { method: 'POST', body: { email: normalizeEmail(email), password } })
  },

  adminLogin(email, password) {
    return request('/auth/admin/login', { method: 'POST', body: { email: normalizeEmail(email), password } })
  },

  sendPasswordResetCode(email) {
    return request('/auth/password-reset/send-code', { method: 'POST', body: { email } })
  },

  verifyPasswordResetCode(payload) {
    return request('/auth/password-reset/verify-code', { method: 'POST', body: payload })
  },

  resetPassword(payload) {
    return request('/auth/password-reset/reset', { method: 'POST', body: payload })
  },

  updateProfileUsername(username) {
    return request('/profile/username', { method: 'PUT', body: { username: normalizeUsername(username) } })
  },

  sendProfileCurrentEmailCode() {
    return request('/profile/email/current-code', { method: 'POST' })
  },

  sendProfileNewEmailCode(email) {
    return request('/profile/email/new-code', { method: 'POST', body: { email: normalizeEmail(email) } })
  },

  updateProfileEmail(payload) {
    return request('/profile/email', {
      method: 'PUT',
      body: {
        newEmail: normalizeEmail(payload.newEmail),
        currentEmailCode: String(payload.currentEmailCode || '').trim(),
        newEmailCode: String(payload.newEmailCode || '').trim(),
      },
    })
  },

  sendProfilePasswordCode() {
    return request('/profile/password/code', { method: 'POST' })
  },

  updateProfilePassword(payload) {
    return request('/profile/password', {
      method: 'PUT',
      body: {
        emailCode: String(payload.emailCode || '').trim(),
        password: payload.password,
      },
    })
  },

  register(payload) {
    return request('/auth/register', { method: 'POST', body: { ...payload, username: normalizeUsername(payload.username) } })
  },
}
