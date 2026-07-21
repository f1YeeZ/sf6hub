<template>
  <Teleport to="body">
    <transition name="report-dialog">
      <div v-if="modelValue" class="report-backdrop" @click.self="close">
        <form class="report-panel" @submit.prevent="submit">
          <button type="button" class="report-close" @click="close">
            <span class="material-symbols-outlined">close</span>
          </button>

          <span class="report-kicker">REPORT</span>
          <h2>举报{{ targetLabel }}</h2>
          <p v-if="targetName" class="report-target">{{ targetName }}</p>

          <label>
            举报理由
            <select v-model="form.reason" required>
              <option value="" disabled>请选择理由</option>
              <option v-for="item in reasonOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
          </label>

          <label>
            补充说明
            <textarea
              v-model.trim="form.detail"
              maxlength="500"
              placeholder="可填写具体问题、证据或希望管理员注意的地方"
            ></textarea>
          </label>

          <p v-if="error" class="report-error">{{ error }}</p>

          <div class="report-actions">
            <button type="button" class="ghost" :disabled="submitting" @click="close">取消</button>
            <button type="submit" class="danger" :disabled="submitting || !form.reason">
              <span class="material-symbols-outlined">flag</span>
              {{ submitting ? '提交中...' : '提交举报' }}
            </button>
          </div>
        </form>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { api } from '@/js/api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  targetType: { type: String, required: true },
  targetId: { type: [Number, String], required: true },
  targetName: { type: String, default: '' },
  targetLabel: { type: String, default: '对象' },
})
const emit = defineEmits(['update:modelValue', 'submitted'])

const reasonOptions = [
  { value: 'wrong_data', label: '数据错误' },
  { value: 'abuse', label: '攻击谩骂' },
  { value: 'spam', label: '垃圾信息' },
  { value: 'duplicate', label: '重复内容' },
  { value: 'illegal', label: '违规内容' },
  { value: 'other', label: '其他原因' },
]

const form = reactive({ reason: '', detail: '' })
const submitting = ref(false)
const error = ref('')

watch(() => props.modelValue, value => {
  if (!value) return
  form.reason = ''
  form.detail = ''
  error.value = ''
})

function close() {
  if (submitting.value) return
  emit('update:modelValue', false)
}

async function submit() {
  if (!form.reason || submitting.value) return
  submitting.value = true
  error.value = ''
  try {
    await api.reportTarget({
      targetType: props.targetType,
      targetId: props.targetId,
      reason: form.reason,
      detail: form.detail,
    })
    emit('submitted')
    emit('update:modelValue', false)
  } catch (err) {
    error.value = err.message || '提交举报失败'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.report-backdrop {
  position: fixed;
  inset: 0;
  z-index: 130;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(9, 10, 14, 0.78);
  backdrop-filter: blur(12px);
}

.report-panel {
  position: relative;
  width: min(500px, 100%);
  display: grid;
  gap: 16px;
  padding: 28px;
  color: #eef0f3;
  background: rgba(24, 26, 31, 0.98);
  border: 1px solid rgba(255, 180, 171, 0.32);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.48);
}

.report-close {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  color: rgba(255, 255, 255, 0.68);
}

.report-kicker {
  width: fit-content;
  color: var(--state-error);
  font-family: 'JetBrains Mono';
  font-size: 12px;
  letter-spacing: 0.08em;
}

.report-panel h2 {
  margin: 0;
  color: #fff;
  font-family: Anybody;
  font-size: 30px;
  font-weight: 900;
  font-style: italic;
  line-height: 1;
}

.report-target {
  margin: -6px 0 0;
  color: rgba(255, 255, 255, 0.64);
  overflow-wrap: anywhere;
}

.report-panel label {
  display: grid;
  gap: 8px;
  color: rgba(255, 255, 255, 0.74);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}

.report-panel select,
.report-panel textarea {
  width: 100%;
  border: 1px solid rgba(255, 180, 171, 0.26);
  color: #fff;
  background: #111318;
  outline: none;
}

.report-panel select {
  height: 46px;
  padding: 0 12px;
}

.report-panel textarea {
  min-height: 120px;
  padding: 12px;
  resize: vertical;
}

.report-panel select:focus,
.report-panel textarea:focus {
  border-color: var(--state-error);
  box-shadow: 0 0 0 3px rgba(255, 180, 171, 0.12);
}

.report-error {
  margin: 0;
  color: var(--state-error);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}

.report-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.report-actions button {
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 16px;
  font-family: Anybody;
  font-weight: 900;
  font-style: italic;
  transform: skewX(-8deg);
}

.report-actions button > span {
  transform: skewX(8deg);
}

.report-actions .ghost {
  color: rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.08);
}

.report-actions .danger {
  color: #fff;
  background: #d32f3f;
}

.report-actions button:disabled {
  opacity: .58;
  cursor: not-allowed;
}

.report-dialog-enter-active,
.report-dialog-leave-active {
  transition: opacity 0.16s ease;
}

.report-dialog-enter-from,
.report-dialog-leave-to {
  opacity: 0;
}

@media (max-width: 540px) {
  .report-backdrop {
    padding: 12px;
  }

  .report-panel {
    max-height: calc(100dvh - 24px);
    padding: 22px;
    overflow-y: auto;
  }

  .report-actions {
    flex-direction: column-reverse;
  }

  .report-actions button {
    width: 100%;
  }
}
</style>
