<template>
  <Teleport to="body">
    <transition name="app-dialog">
      <div
        v-if="ui.dialog"
        class="app-dialog-backdrop"
        role="presentation"
        @click.self="ui.cancelDialog()"
      >
        <form
          class="app-dialog-panel"
          role="dialog"
          aria-modal="true"
          :aria-labelledby="titleId"
          @submit.prevent="ui.resolveDialog()"
          @keydown.esc.prevent="ui.cancelDialog()"
        >
          <div class="app-dialog-icon" :class="ui.dialog.tone">
            <span class="material-symbols-outlined">{{ iconName }}</span>
          </div>
          <div class="app-dialog-copy">
            <h2 :id="titleId">{{ ui.dialog.title }}</h2>
            <p v-if="ui.dialog.message">{{ ui.dialog.message }}</p>
          </div>
          <input
            v-if="ui.dialog.type === 'prompt'"
            ref="inputRef"
            v-model="ui.dialogInput"
            class="app-dialog-input"
            :placeholder="ui.dialog.placeholder"
          />
          <div class="app-dialog-actions">
            <button
              v-if="ui.dialog.type !== 'alert'"
              type="button"
              class="app-dialog-button ghost"
              @click="ui.cancelDialog()"
            >
              {{ ui.dialog.cancelText }}
            </button>
            <button type="submit" class="app-dialog-button" :class="ui.dialog.tone">
              {{ ui.dialog.confirmText }}
            </button>
          </div>
        </form>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useUiStore } from '@/js/stores/ui'

const ui = useUiStore()
const inputRef = ref(null)
const titleId = `app-dialog-title-${Math.random().toString(36).slice(2)}`
const iconName = computed(() => {
  if (ui.dialog?.type === 'prompt') return 'edit_note'
  if (ui.dialog?.tone === 'danger') return 'warning'
  if (ui.dialog?.tone === 'success') return 'check_circle'
  return 'info'
})

watch(() => ui.dialog?.type, async type => {
  if (type !== 'prompt') return
  await nextTick()
  inputRef.value?.focus()
})
</script>

<style scoped>
.app-dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 220;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(9, 10, 14, 0.78);
  backdrop-filter: blur(12px);
}

.app-dialog-panel {
  width: min(440px, 100%);
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 16px;
  padding: 22px;
  color: var(--ink-text);
  background: var(--ink-surface);
  border: 1px solid var(--ink-line-strong);
  border-radius: 8px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.48);
}

.app-dialog-icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  color: var(--accent-red);
  background: rgba(91, 214, 199, 0.12);
  border: 1px solid rgba(91, 214, 199, 0.32);
  border-radius: 6px;
}

.app-dialog-icon.success {
  color: var(--accent-red);
  background: rgba(91, 214, 199, 0.12);
  border-color: rgba(91, 214, 199, 0.32);
}

.app-dialog-icon.danger {
  color: var(--state-error);
  background: rgba(255, 86, 105, 0.11);
  border-color: rgba(255, 118, 135, 0.28);
}

.app-dialog-copy {
  min-width: 0;
}

.app-dialog-copy h2 {
  margin: 0;
  color: var(--ink-text);
  font-family: inherit;
  font-size: 24px;
  font-weight: 900;
  line-height: 1.12;
}

.app-dialog-copy p {
  margin: 8px 0 0;
  color: var(--ink-text-soft);
  font-size: 14px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.app-dialog-input {
  grid-column: 1 / -1;
  width: 100%;
  height: 44px;
  padding: 0 14px;
  color: var(--ink-text);
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  outline: none;
}

.app-dialog-input:focus {
  border-color: var(--accent-red);
  box-shadow: 0 0 0 3px rgba(91, 214, 199, 0.14);
}

.app-dialog-actions {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 2px;
}

.app-dialog-button {
  min-width: 86px;
  min-height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  color: #190607;
  background: var(--accent-red);
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  font-weight: 900;
  transition: background-color 0.16s ease, color 0.16s ease, opacity 0.16s ease;
}

.app-dialog-button:hover {
  background: var(--accent-red-hot);
}

.app-dialog-button.danger {
  color: #fff;
  background: #d32f3f;
}

.app-dialog-button.danger:hover {
  background: #e04b58;
}

.app-dialog-button.ghost {
  color: var(--ink-text-soft);
  background: var(--ink-surface-high);
}

.app-dialog-button.ghost:hover {
  color: var(--ink-text);
  background: var(--ink-surface-high);
}

.app-dialog-enter-active,
.app-dialog-leave-active {
  transition: opacity 0.16s ease;
}

.app-dialog-enter-from,
.app-dialog-leave-to {
  opacity: 0;
}

@media (max-width: 520px) {
  .app-dialog-panel {
    grid-template-columns: 1fr;
    padding: 18px;
  }

  .app-dialog-actions {
    flex-direction: column-reverse;
  }

  .app-dialog-button {
    width: 100%;
  }
}
</style>
