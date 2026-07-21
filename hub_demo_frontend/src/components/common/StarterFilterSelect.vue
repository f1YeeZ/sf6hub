<template>
  <div ref="root" class="starter-filter-select">
    <button
      ref="trigger"
      type="button"
      class="starter-filter-trigger"
      :class="{ open }"
      role="combobox"
      aria-haspopup="listbox"
      :aria-expanded="open"
      :aria-controls="listboxId"
      @click="toggle"
      @keydown.down.prevent="openList"
      @keydown.up.prevent="openList(true)"
      @keydown.esc.prevent="closeList"
    >
      <span v-if="modelValue" class="starter-filter-value">
        <ComboNotation :route="modelValue" size="sm" />
        <code>{{ modelValue }}</code>
      </span>
      <span v-else class="starter-filter-placeholder">
        <span class="material-symbols-outlined" aria-hidden="true">all_inclusive</span>
        全部起手式
      </span>
      <span class="material-symbols-outlined starter-filter-chevron" aria-hidden="true">expand_more</span>
    </button>

    <Transition name="starter-drawer">
      <div v-if="open" :id="listboxId" class="starter-filter-drawer" role="listbox" aria-label="起手式">
        <TransitionGroup name="starter-option" tag="div" class="starter-filter-options" appear>
          <button
            v-for="(option, index) in allOptions"
            :key="option || 'all'"
            :ref="element => setOptionRef(element, index)"
            type="button"
            class="starter-filter-option"
            :class="{ selected: option === modelValue }"
            :style="{ '--starter-option-index': Math.min(index, 8) }"
            role="option"
            :aria-selected="option === modelValue"
            @click="selectOption(option)"
            @keydown.down.prevent="focusOption(index + 1)"
            @keydown.up.prevent="focusOption(index - 1)"
            @keydown.home.prevent="focusOption(0)"
            @keydown.end.prevent="focusOption(allOptions.length - 1)"
            @keydown.esc.prevent="closeList(true)"
          >
            <span v-if="option" class="starter-filter-value">
              <ComboNotation :route="option" size="sm" />
              <code>{{ option }}</code>
            </span>
            <span v-else class="starter-filter-placeholder">
              <span class="material-symbols-outlined" aria-hidden="true">all_inclusive</span>
              全部起手式
            </span>
            <span v-if="option === modelValue" class="material-symbols-outlined starter-filter-check" aria-hidden="true">check</span>
          </button>
        </TransitionGroup>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import ComboNotation from '@/components/common/ComboNotation.vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  options: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['update:modelValue'])
const root = ref(null)
const trigger = ref(null)
const open = ref(false)
const optionElements = ref([])
const listboxId = `starter-filter-${getCurrentInstance()?.uid ?? 'select'}`
const allOptions = computed(() => ['', ...props.options.filter(option => option)])
const selectedIndex = computed(() => Math.max(0, allOptions.value.indexOf(props.modelValue)))

function toggle() {
  if (open.value) closeList()
  else openList()
}

function openList(fromEnd = false) {
  open.value = true
  nextTick(() => focusOption(fromEnd ? allOptions.value.length - 1 : selectedIndex.value))
}

function closeList(returnFocus = false) {
  open.value = false
  if (returnFocus) nextTick(() => trigger.value?.focus())
}

function selectOption(option) {
  emit('update:modelValue', option)
  closeList(true)
}

function setOptionRef(element, index) {
  if (element) optionElements.value[index] = element
}

function focusOption(index) {
  if (!allOptions.value.length) return
  const normalizedIndex = (index + allOptions.value.length) % allOptions.value.length
  optionElements.value[normalizedIndex]?.focus()
}

function handleOutsidePointer(event) {
  if (open.value && !root.value?.contains(event.target)) closeList()
}

onMounted(() => document.addEventListener('pointerdown', handleOutsidePointer, true))
onBeforeUnmount(() => document.removeEventListener('pointerdown', handleOutsidePointer, true))
</script>

<style scoped>
.starter-filter-select {
  position: relative;
  z-index: 20;
  display: grid;
}

.starter-filter-trigger,
.starter-filter-option {
  width: 100%;
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 10px;
  color: var(--ink-text);
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  text-align: left;
  outline: none;
}

.starter-filter-trigger:hover,
.starter-filter-trigger:focus-visible,
.starter-filter-trigger.open {
  border-color: var(--accent-red);
  box-shadow: 0 0 0 3px rgba(255, 64, 84, 0.12);
}

.starter-filter-value {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.starter-filter-value :deep(.combo-notation) {
  flex: 0 1 auto;
  flex-wrap: nowrap;
}

.starter-filter-value :deep(.combo-move) {
  flex-wrap: nowrap;
  white-space: nowrap;
}

.starter-filter-value code {
  overflow: hidden;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.starter-filter-placeholder {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--ink-text-soft);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}

.starter-filter-placeholder .material-symbols-outlined {
  color: var(--ink-muted);
  font-size: 18px;
}

.starter-filter-chevron {
  flex: 0 0 auto;
  color: var(--ink-muted);
  font-size: 19px;
  transition: transform 0.18s ease;
}

.starter-filter-trigger.open .starter-filter-chevron {
  transform: rotate(180deg);
}

.starter-filter-drawer {
  position: absolute;
  z-index: 80;
  top: calc(100% + 6px);
  right: 0;
  left: 0;
  max-height: 264px;
  overflow: hidden;
  transform-origin: top center;
  filter: drop-shadow(0 16px 24px rgba(0, 0, 0, 0.48));
}

.starter-filter-options {
  max-height: 264px;
  display: grid;
  gap: 4px;
  overflow-y: auto;
  padding: 5px;
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line-strong);
  border-radius: 4px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03), 0 0 0 1px rgba(255, 64, 84, 0.08);
}

.starter-drawer-enter-active {
  transition: max-height 280ms cubic-bezier(0.22, 1, 0.36, 1), opacity 180ms ease-out, transform 280ms cubic-bezier(0.22, 1, 0.36, 1);
}

.starter-drawer-leave-active {
  transition: max-height 190ms cubic-bezier(0.4, 0, 1, 1), opacity 130ms ease-in, transform 190ms cubic-bezier(0.4, 0, 1, 1);
}

.starter-drawer-enter-from,
.starter-drawer-leave-to {
  max-height: 0;
  opacity: 0;
  transform: scaleY(0.84) translateY(-6px);
}

.starter-option-enter-active {
  transition: opacity 180ms ease-out, transform 240ms cubic-bezier(0.22, 1, 0.36, 1);
  transition-delay: calc(var(--starter-option-index) * 38ms + 35ms);
}

.starter-option-leave-active {
  transition: opacity 100ms ease-in, transform 140ms ease-in;
}

.starter-option-enter-from,
.starter-option-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.97);
}

.starter-filter-option {
  flex: 0 0 auto;
  background: var(--ink-surface);
  cursor: pointer;
}

.starter-filter-option:hover,
.starter-filter-option:focus-visible {
  background: var(--ink-surface-high);
  border-color: rgba(255, 64, 84, 0.58);
}

.starter-filter-option.selected {
  background: rgba(255, 64, 84, 0.1);
  border-color: rgba(255, 64, 84, 0.68);
}

.starter-filter-check {
  flex: 0 0 auto;
  color: var(--accent-red);
  font-size: 18px;
}

@media (max-width: 420px) {
  .starter-filter-value code {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .starter-drawer-enter-active,
  .starter-drawer-leave-active,
  .starter-option-enter-active,
  .starter-option-leave-active {
    transition: none;
  }
}
</style>
