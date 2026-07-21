<template>
  <div class="guide-page">
    <header class="guide-head">
      <router-link to="/" class="back-link">
        <span class="material-symbols-outlined">arrow_back</span>
        返回角色
      </router-link>
      <span>GUIDE</span>
      <h1>使用指南</h1>
      <p>这里展示后台维护的指南条目。点击卡片查看完整内容。</p>
    </header>

    <section class="guide-list" aria-label="使用指南列表">
      <button
        v-for="item in guideItems"
        :key="item.id"
        type="button"
        :class="['guide-card', guideLevelClass(item.level)]"
        @click="openGuide(item)"
      >
        <div class="guide-card-meta">
          <mark>{{ guideLevelLabel(item.level) }}</mark>
          <time>{{ formatDateTime(item.createdAt) }}</time>
        </div>
        <h2>{{ item.title }}</h2>
        <p>{{ previewText(item.content) }}</p>
      </button>

      <p v-if="guideLoading" class="guide-empty">正在读取使用指南...</p>
      <p v-else-if="!guideItems.length" class="guide-empty">暂无使用指南</p>
    </section>

    <div v-if="activeGuide" class="guide-detail-backdrop" role="presentation" @click.self="closeGuide">
      <section class="guide-detail" role="dialog" aria-modal="true" :aria-labelledby="`guide-title-${activeGuide.id}`">
        <div class="guide-detail-head">
          <div>
            <mark>{{ guideLevelLabel(activeGuide.level) }}</mark>
            <time>{{ formatDateTime(activeGuide.createdAt) }}</time>
          </div>
          <button type="button" class="guide-close" aria-label="关闭指南" @click="closeGuide">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <h2 :id="`guide-title-${activeGuide.id}`">{{ activeGuide.title }}</h2>
        <p>{{ activeGuide.content }}</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { api } from '@/js/api'
import { useRealtimeStore } from '@/js/stores/realtime'

const guideItems = ref([])
const guideLoading = ref(false)
const activeGuide = ref(null)
const realtime = useRealtimeStore()

onMounted(loadGuides)

watch(() => realtime.lastEvent, event => {
  const areas = event?.areas || []
  if (areas.includes('announcements')) loadGuides()
})

async function loadGuides() {
  guideLoading.value = true
  try {
    guideItems.value = await api.getAnnouncements({ limit: 20 }) || []
  } catch (_) {
    guideItems.value = []
  } finally {
    guideLoading.value = false
  }
}

function openGuide(item) {
  activeGuide.value = item
}

function closeGuide() {
  activeGuide.value = null
}

function previewText(value) {
  const text = String(value || '').replace(/\s+/g, ' ').trim()
  if (text.length <= 92) return text || '暂无内容'
  return `${text.slice(0, 92)}...`
}

function formatDateTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

function guideLevelLabel(level) {
  return {
    info: '基础指南',
    warning: '重点指南',
    maintenance: '规则说明',
  }[level] || '基础指南'
}

function guideLevelClass(level) {
  return {
    info: 'info',
    warning: 'warning',
    maintenance: 'maintenance',
  }[level] || 'info'
}
</script>

<style scoped>
.guide-page {
  display: grid;
  gap: 24px;
}

.guide-head {
  display: grid;
  gap: 8px;
  padding-left: 58px;
}

.guide-head > span,
.back-link,
.guide-card-meta,
.guide-empty,
.guide-detail-head {
  font-family: 'JetBrains Mono';
}

.guide-head > span {
  color: var(--accent-red);
  font-size: 12px;
  letter-spacing: 0;
}

.guide-head h1 {
  margin: 0;
  color: var(--ink-text);
  font-family: inherit;
  font-size: 40px;
  font-weight: 900;
  line-height: 1.08;
}

.guide-head p {
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

.guide-list {
  display: grid;
  gap: 12px;
}

.guide-card {
  min-height: 124px;
  width: 100%;
  display: grid;
  gap: 8px;
  padding: 18px;
  text-align: left;
  border: 1px solid var(--ink-line);
  border-radius: 6px;
  background:
    linear-gradient(90deg, rgba(91, 214, 199, 0.07), transparent 42%),
    var(--ink-surface);
  cursor: pointer;
  transition: border-color .18s ease, background .18s ease, transform .18s ease;
}

.guide-card:hover,
.guide-card:focus-visible {
  border-color: rgba(91, 214, 199, 0.55);
  background:
    linear-gradient(90deg, rgba(91, 214, 199, 0.11), transparent 48%),
    #101722;
  transform: translateY(-1px);
}

.guide-card.warning {
  background:
    linear-gradient(90deg, rgba(255, 190, 106, 0.09), transparent 42%),
    var(--ink-surface);
}

.guide-card.maintenance {
  background:
    linear-gradient(90deg, rgba(255, 118, 135, 0.08), transparent 42%),
    var(--ink-surface);
}

.guide-card-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--ink-muted);
  font-size: 12px;
}

.guide-card mark,
.guide-detail mark {
  padding: 3px 8px;
  color: #190607;
  background: var(--accent-red);
}

.guide-card.warning mark,
.guide-detail.warning mark {
  background: var(--accent-amber);
}

.guide-card.maintenance mark,
.guide-detail.maintenance mark {
  color: #1d070b;
  background: var(--state-error);
}

.guide-card h2 {
  margin: 0;
  color: var(--ink-text);
  font-family: inherit;
  font-size: 22px;
  font-weight: 900;
}

.guide-card p {
  max-width: 86ch;
  margin: 0;
  color: var(--ink-text-soft);
  line-height: 1.62;
}

.guide-empty {
  margin: 0;
  padding: 18px;
  color: var(--ink-muted);
  border: 1px dashed var(--ink-line);
  border-radius: 6px;
  background: rgba(13, 17, 25, 0.62);
  font-size: 12px;
}

.guide-detail-backdrop {
  position: fixed;
  inset: 0;
  z-index: 120;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(2, 6, 14, 0.72);
}

.guide-detail {
  width: min(720px, 100%);
  max-height: calc(100dvh - 48px);
  display: grid;
  gap: 14px;
  overflow: auto;
  padding: 24px;
  border: 1px solid rgba(91, 214, 199, 0.32);
  border-radius: 6px;
  background: var(--ink-surface);
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.48);
}

.guide-detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.guide-detail-head > div {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--ink-muted);
  font-size: 12px;
}

.guide-close {
  width: 36px;
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-muted);
  border: 1px solid var(--ink-line);
  border-radius: 6px;
  background: transparent;
}

.guide-close:hover {
  color: #fff;
  border-color: var(--accent-red);
  background: var(--ink-surface-high);
}

.guide-detail h2 {
  margin: 0;
  color: #fff;
  font-family: inherit;
  font-size: 30px;
  font-weight: 900;
  line-height: 1.12;
  overflow-wrap: anywhere;
}

.guide-detail p {
  margin: 0;
  color: #d8e1f0;
  line-height: 1.78;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

@media (max-width: 560px) {
  .guide-head {
    padding-left: 46px;
  }

  .guide-head h1 {
    font-size: 32px;
  }

  .guide-card {
    padding: 15px;
  }

  .guide-detail-backdrop {
    padding: 12px;
  }

  .guide-detail {
    max-height: calc(100dvh - 24px);
    padding: 18px;
  }
}
</style>
