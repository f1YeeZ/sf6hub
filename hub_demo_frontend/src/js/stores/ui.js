import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUiStore = defineStore('ui', () => {
  const showNotifications = ref(false)
  const dialog = ref(null)
  const dialogInput = ref('')
  const dialogQueue = []
  let dialogResolve = null

  function toggleNotifications() { showNotifications.value = !showNotifications.value }
  function closeNotifications() { showNotifications.value = false }

  function enqueueDialog(config) {
    return new Promise(resolve => {
      dialogQueue.push({ config, resolve })
      if (!dialog.value) showNextDialog()
    })
  }

  function showNextDialog() {
    const next = dialogQueue.shift()
    if (!next) return
    dialog.value = {
      type: next.config.type || 'alert',
      tone: next.config.tone || 'default',
      title: next.config.title || (next.config.type === 'confirm' ? '确认操作' : '提示'),
      message: next.config.message || '',
      confirmText: next.config.confirmText || '确定',
      cancelText: next.config.cancelText || '取消',
      placeholder: next.config.placeholder || '',
    }
    dialogInput.value = next.config.defaultValue || ''
    dialogResolve = next.resolve
  }

  function finishDialog(result) {
    const resolve = dialogResolve
    dialog.value = null
    dialogInput.value = ''
    dialogResolve = null
    resolve?.(result)
    showNextDialog()
  }

  function resolveDialog() {
    if (!dialog.value) return
    if (dialog.value.type === 'prompt') {
      finishDialog(dialogInput.value)
      return
    }
    finishDialog(dialog.value.type === 'confirm' ? true : undefined)
  }

  function cancelDialog() {
    if (!dialog.value) return
    finishDialog(dialog.value.type === 'confirm' ? false : null)
  }

  function alertDialog(options = {}) {
    return enqueueDialog({ ...options, type: 'alert' })
  }

  function confirmDialog(options = {}) {
    return enqueueDialog({ ...options, type: 'confirm' })
  }

  function promptDialog(options = {}) {
    return enqueueDialog({ ...options, type: 'prompt' })
  }

  return {
    showNotifications,
    dialog,
    dialogInput,
    toggleNotifications,
    closeNotifications,
    alertDialog,
    confirmDialog,
    promptDialog,
    resolveDialog,
    cancelDialog,
  }
})
