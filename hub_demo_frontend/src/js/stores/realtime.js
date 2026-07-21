import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '@/js/api'

export const useRealtimeStore = defineStore('realtime', () => {
  const lastEvent = ref(null)
  const connected = ref(false)
  let eventSource = null

  function connect() {
    if (eventSource || typeof EventSource === 'undefined') return
    eventSource = api.openEvents()
    eventSource.addEventListener('realtime-ready', () => {
      connected.value = true
    })
    eventSource.addEventListener('realtime-update', event => {
      connected.value = true
      try {
        lastEvent.value = JSON.parse(event.data || '{}')
      } catch (_) {
        lastEvent.value = { action: 'unknown', areas: [] }
      }
    })
    eventSource.onerror = () => {
      connected.value = false
    }
  }

  function disconnect() {
    if (!eventSource) return
    eventSource.close()
    eventSource = null
    connected.value = false
  }

  return {
    lastEvent,
    connected,
    connect,
    disconnect,
  }
})
