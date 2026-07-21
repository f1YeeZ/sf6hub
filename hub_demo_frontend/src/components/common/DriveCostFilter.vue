<template>
  <fieldset class="drive-cost-filter">
    <legend>斗气消耗数</legend>
    <div class="drive-cost-heading">
      <span>{{ active ? `${formattedValue} 格` : '未限制' }}</span>
      <button
        type="button"
        :class="['unlimited-switch', { selected: !active }]"
        role="switch"
        :aria-checked="!active"
        :aria-label="active ? '开启不限斗气消耗' : '关闭不限并恢复斗气消耗值'"
        @click="toggleUnlimited"
      >
        <span>不限</span>
        <span class="unlimited-switch-track" aria-hidden="true">
          <span class="unlimited-switch-knob" />
        </span>
      </button>
    </div>
    <div :class="['drive-cost-control', { active }]">
      <input
        class="drive-cost-range"
        type="range"
        min="0"
        :max="max"
        step="0.1"
        :value="sliderValue"
        :style="{ '--drive-progress': `${progress}%` }"
        aria-label="斗气消耗数滑动条"
        @input="updateFromRange"
      />
      <label class="drive-cost-number">
        <span class="sr-only">输入斗气消耗数</span>
        <input
          type="number"
          min="0"
          :max="max"
          step="0.1"
          inputmode="decimal"
          placeholder="不限"
          :value="numberInput"
          @focus="editingNumber = true"
          @input="updateFromNumber"
          @blur="finishNumberInput"
        />
        <span>格</span>
      </label>
    </div>
    <div class="drive-cost-scale" aria-hidden="true">
      <span>0.0</span>
      <span>{{ Number(max).toFixed(1) }}</span>
    </div>
  </fieldset>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: [Number, String],
    default: '',
  },
  max: {
    type: Number,
    default: 6,
  },
})

const emit = defineEmits(['update:modelValue'])
const active = computed(() => props.modelValue !== '' && props.modelValue !== null && props.modelValue !== undefined)
const numericValue = computed(() => active.value && Number.isFinite(Number(props.modelValue)) ? Number(props.modelValue) : 0)
const sliderValue = computed(() => normalize(numericValue.value))
const formattedValue = computed(() => sliderValue.value.toFixed(1))
const progress = computed(() => props.max > 0 ? (sliderValue.value / props.max) * 100 : 0)
const numberInput = ref(active.value ? formattedValue.value : '')
const editingNumber = ref(false)
const lastValue = ref(active.value ? sliderValue.value : 0)

watch(() => props.modelValue, () => {
  if (active.value) lastValue.value = sliderValue.value
  if (!editingNumber.value) numberInput.value = active.value ? formattedValue.value : ''
})

function normalize(value) {
  const number = Number(value)
  if (!Number.isFinite(number)) return 0
  return Math.min(props.max, Math.max(0, Math.round(number * 10) / 10))
}

function updateFromRange(event) {
  const value = normalize(event.target.value)
  lastValue.value = value
  numberInput.value = value.toFixed(1)
  emit('update:modelValue', value)
}

function updateFromNumber(event) {
  const rawValue = event.target.value
  if (rawValue === '') {
    if (active.value) lastValue.value = sliderValue.value
    numberInput.value = ''
    emit('update:modelValue', '')
    return
  }
  const decimalPlaces = rawValue.includes('.') ? rawValue.split('.')[1].length : 0
  const value = normalize(rawValue)
  lastValue.value = value
  numberInput.value = decimalPlaces > 1 || value !== Number(rawValue) ? value.toFixed(1) : rawValue
  if (numberInput.value !== rawValue) event.target.value = numberInput.value
  emit('update:modelValue', value)
}

function finishNumberInput() {
  editingNumber.value = false
  numberInput.value = active.value ? formattedValue.value : ''
}

function toggleUnlimited() {
  if (active.value) {
    lastValue.value = sliderValue.value
    editingNumber.value = false
    numberInput.value = ''
    emit('update:modelValue', '')
    return
  }
  const value = normalize(lastValue.value)
  numberInput.value = value.toFixed(1)
  emit('update:modelValue', value)
}
</script>

<style scoped>
.drive-cost-filter {
  min-width: 0;
  margin: 0;
  padding: 0;
  border: 0;
}

.drive-cost-filter legend {
  display: block;
  margin-bottom: 10px;
  color: var(--ink-text-soft);
  font-family: 'JetBrains Mono';
  font-size: 12px;
  font-weight: 800;
}

.drive-cost-heading {
  min-height: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 11px;
}

.drive-cost-heading > span {
  color: var(--ink-text);
  font-weight: 900;
}

.unlimited-switch {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0;
  color: var(--ink-muted);
  background: transparent;
  border: 0;
  border-radius: 999px;
  font-family: 'JetBrains Mono';
  font-size: 10px;
  font-weight: 800;
  outline: none;
  transition: color 0.16s ease;
}

.unlimited-switch:hover,
.unlimited-switch:focus-visible,
.unlimited-switch.selected {
  color: var(--ink-text);
}

.unlimited-switch-track {
  position: relative;
  width: 38px;
  height: 22px;
  display: block;
  flex: 0 0 auto;
  background: var(--ink-surface-high);
  border: 1px solid var(--ink-line-strong);
  border-radius: 999px;
  transition: background 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.unlimited-switch-knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 14px;
  height: 14px;
  background: var(--ink-muted);
  border-radius: 50%;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.4);
  transition: transform 0.18s cubic-bezier(0.22, 1, 0.36, 1), background 0.18s ease;
}

.unlimited-switch.selected .unlimited-switch-track {
  background: rgba(255, 64, 84, 0.24);
  border-color: var(--accent-red);
}

.unlimited-switch.selected .unlimited-switch-knob {
  background: var(--accent-red);
  transform: translateX(16px);
}

.unlimited-switch:focus-visible .unlimited-switch-track {
  box-shadow: 0 0 0 3px rgba(255, 64, 84, 0.16);
}

.drive-cost-control {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) 104px;
  align-items: center;
  gap: 14px;
}

.drive-cost-range {
  width: 100%;
  height: 20px;
  margin: 0;
  appearance: none;
  -webkit-appearance: none;
  background: transparent;
  cursor: pointer;
  outline: none;
}

.drive-cost-range::-webkit-slider-runnable-track {
  height: 6px;
  background: linear-gradient(to right, var(--accent-red) 0 var(--drive-progress), var(--ink-line-strong) var(--drive-progress) 100%);
  border-radius: 999px;
}

.drive-cost-range::-moz-range-track {
  height: 6px;
  background: linear-gradient(to right, var(--accent-red) 0 var(--drive-progress), var(--ink-line-strong) var(--drive-progress) 100%);
  border-radius: 999px;
}

.drive-cost-range::-webkit-slider-thumb {
  width: 18px;
  height: 18px;
  margin-top: -6px;
  appearance: none;
  -webkit-appearance: none;
  background: var(--ink-text);
  border: 3px solid var(--accent-red);
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgba(255, 64, 84, 0.12);
}

.drive-cost-range::-moz-range-thumb {
  width: 14px;
  height: 14px;
  background: var(--ink-text);
  border: 3px solid var(--accent-red);
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgba(255, 64, 84, 0.12);
}

.drive-cost-range:focus-visible::-webkit-slider-thumb {
  box-shadow: 0 0 0 4px rgba(255, 64, 84, 0.24);
}

.drive-cost-range:focus-visible::-moz-range-thumb {
  box-shadow: 0 0 0 4px rgba(255, 64, 84, 0.24);
}

.drive-cost-control:not(.active) .drive-cost-range {
  opacity: 0.55;
}

.drive-cost-number {
  min-height: 44px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  overflow: hidden;
  color: var(--ink-muted);
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  font-size: 11px;
}

.drive-cost-number:focus-within {
  border-color: var(--accent-red);
  box-shadow: 0 0 0 3px rgba(255, 64, 84, 0.14);
}

.drive-cost-number input {
  width: 100%;
  min-width: 0;
  height: 42px;
  padding: 0 6px 0 11px;
  color: var(--ink-text);
  background: transparent;
  border: 0;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  outline: none;
}

.drive-cost-number > span:last-child {
  padding-right: 10px;
}

.drive-cost-scale {
  display: flex;
  justify-content: space-between;
  margin: 3px 118px 0 0;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 9px;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 420px) {
  .drive-cost-control {
    grid-template-columns: 1fr;
  }

  .drive-cost-scale {
    display: none;
  }
}
</style>
