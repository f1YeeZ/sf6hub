import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/js/api'

export const useAuthStore = defineStore('auth', () => {
  const isLoggedIn = ref(false)
  const user = ref(null)
  const showLogin = ref(false)

  async function hydrate() {
    try {
      const me = await api.getMe()
      if (me) {
        user.value = me
        isLoggedIn.value = true
      }
    } catch (_) {
      clearSession()
    }
  }

  function login(payload) {
    const username = typeof payload === 'string' ? payload : payload.username
    user.value = {
      id: payload.id || null,
      username,
      avatar: payload.avatar || null,
      email: payload.email || '',
      bio: payload.bio || '',
      role: payload.role || 'user',
      adminPermissions: payload.adminPermissions || '',
    }
    isLoggedIn.value = true
  }

  function clearSession() {
    user.value = null
    isLoggedIn.value = false
  }

  async function logout() {
    try {
      await api.logout()
    } catch (_) {
      // Logging out must still clear the local session even if cleanup fails.
    } finally {
      clearSession()
    }
  }

  function updateUser(payload) {
    user.value = { ...(user.value || {}), ...payload }
  }

  function openLogin() { showLogin.value = true }
  function closeLogin() { showLogin.value = false }

  return { isLoggedIn, user, showLogin, hydrate, login, logout, updateUser, openLogin, closeLogin }
})
