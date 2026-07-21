<template>
  <div class="feedback-page">
    <header class="feedback-head">
      <router-link to="/" class="back-link">
        <span class="material-symbols-outlined">arrow_back</span>
        返回角色
      </router-link>
      <span>FEEDBACK</span>
      <h1>提交反馈</h1>
      <p>发现数据错误、页面问题或想补充功能时，在这里提交。反馈会进入后台反馈模块。</p>
    </header>

    <form class="feedback-panel" @submit.prevent="submit">
      <label>
        反馈类型
        <select v-model="form.reason" required>
          <option value="wrong_data">数据错误</option>
          <option value="abuse">体验问题</option>
          <option value="duplicate">重复内容</option>
          <option value="other">其他建议</option>
        </select>
      </label>

      <label>
        反馈内容
        <textarea v-model.trim="form.detail" maxlength="500" required placeholder="请描述问题、页面位置或建议，最多 500 字"></textarea>
      </label>

      <p v-if="error" class="feedback-error">{{ error }}</p>

      <button class="feedback-submit" type="submit" :disabled="submitting || !form.detail">
        <span class="material-symbols-outlined">send</span>
        {{ submitting ? '提交中...' : '提交反馈' }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/js/api'
import { useAuthStore } from '@/js/stores/auth'
import { useUiStore } from '@/js/stores/ui'

const router = useRouter()
const auth = useAuthStore()
const ui = useUiStore()
const form = reactive({ reason: 'other', detail: '' })
const submitting = ref(false)
const error = ref('')

async function submit() {
  if (!form.detail || submitting.value) return
  if (!auth.isLoggedIn) {
    router.push('/login')
    return
  }
  submitting.value = true
  error.value = ''
  try {
    await api.submitFeedback({
      reason: form.reason,
      detail: form.detail,
    })
    form.reason = 'other'
    form.detail = ''
    await ui.alertDialog({
      title: '反馈已提交',
      message: '感谢反馈，管理员会在后台查看。',
      tone: 'success',
    })
  } catch (err) {
    error.value = err.message || '提交失败'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.feedback-page {
  display: grid;
  gap: 24px;
  max-width: 660px;
  margin-inline: auto;
}

.feedback-head {
  display: grid;
  gap: 8px;
  padding-left: 58px;
}

.feedback-head > span,
.back-link,
.feedback-panel label,
.feedback-error {
  font-family: 'JetBrains Mono';
}

.feedback-head > span {
  color: var(--accent-red);
  font-size: 12px;
  letter-spacing: 0;
}

.feedback-head h1 {
  margin: 0;
  color: var(--ink-text);
  font-family: inherit;
  font-size: 40px;
  font-weight: 900;
  line-height: 1.08;
}

.feedback-head p {
  max-width: 62ch;
  margin: 0;
  color: var(--ink-text-soft);
  line-height: 1.65;
}

.back-link {
  width: fit-content;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--ink-muted);
  font-size: 12px;
}

.back-link:hover {
  color: var(--ink-text);
}

.back-link .material-symbols-outlined {
  font-size: 17px;
}

.feedback-panel {
  display: grid;
  gap: 16px;
  padding: 28px;
  background: var(--ink-surface);
  border: 1px solid var(--ink-line);
  border-radius: 6px;
}

.feedback-panel label {
  display: grid;
  gap: 8px;
  color: var(--ink-text-soft);
  font-size: 12px;
}

.feedback-panel select,
.feedback-panel textarea {
  width: 100%;
  color: var(--ink-text);
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  outline: none;
}

.feedback-panel select {
  height: 46px;
  padding: 0 12px;
}

.feedback-panel textarea {
  min-height: 180px;
  padding: 12px;
  resize: vertical;
}

.feedback-panel select:focus,
.feedback-panel textarea:focus {
  border-color: var(--accent-red);
  box-shadow: 0 0 0 3px rgba(91, 214, 199, 0.14);
}

.feedback-error {
  padding: 10px 12px;
  color: var(--state-error);
  background: rgba(255, 86, 105, 0.11);
  border: 1px solid rgba(255, 118, 135, 0.28);
  border-radius: 4px;
  font-size: 12px;
}

.feedback-submit {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #190607;
  background: var(--accent-red);
  border-radius: 4px;
  font-family: inherit;
  font-weight: 900;
}

.feedback-submit span {
  transform: none;
}

.feedback-submit:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 560px) {
  .feedback-head {
    padding-left: 46px;
  }

  .feedback-head h1 {
    font-size: 32px;
  }

  .feedback-panel {
    padding: 20px;
    clip-path: none;
  }
}
</style>
