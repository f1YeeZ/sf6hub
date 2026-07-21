import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/js/api'

export const useAdminAuthStore = defineStore('adminAuth', () => {
  const isLoggedIn = ref(false)
  const user = ref(null)
  const hydrated = ref(false)

  async function hydrate() {
    try {
      const me = await api.getAdminMe()
      login(me)
    } catch (_) {
      clearSession()
    } finally {
      hydrated.value = true
    }
  }

  function login(payload) {
    user.value = {
      id: payload.id || null,
      username: payload.username,
      avatar: payload.avatar || null,
      email: payload.email || '',
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
      await api.adminLogout()
    } catch (_) {
      // Local admin session still needs to be cleared even if the API call fails.
    } finally {
      clearSession()
    }
  }

  return { isLoggedIn, user, hydrated, hydrate, login, logout, clearSession }
})
