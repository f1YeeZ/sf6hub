<template>
  <div class="combo-detail-page">
  <div v-if="loading" class="text-center py-20 font-headline-lg text-outline italic">LOADING...</div>

  <div v-else-if="combo" class="combo-detail-shell mx-auto">
    <router-link :to="backPath" class="font-label-mono text-label-mono text-tertiary-container hover:underline inline-flex items-center gap-1 mb-6">
      <span class="material-symbols-outlined text-sm">arrow_back</span> {{ isWorldTour ? '返回环球游历' : '返回角色' }}
    </router-link>

    <section class="detail-workbench mb-8">
      <section class="detail-route-panel" aria-label="连招路线">
        <div class="detail-route-head">
          <div class="combo-tag-row">
            <span
              v-for="tag in comboTags"
              :key="tag"
              :class="['font-label-mono text-xs px-3 py-1 slanted', comboTagColor(tag)]"
            >{{ comboTagLabel(tag) }}</span>
            <span v-if="combo.cornerOnly" class="font-label-mono text-xs text-secondary border border-secondary px-3 py-1 slanted">角落限定</span>
          </div>
          <h1 class="detail-title">{{ isWorldTour ? '环球游历' : combo.characterName }} / {{ combo.starter }}</h1>
          <p class="detail-subtitle">作者 {{ combo.author || 'ComboTier' }}</p>
        </div>

        <div class="route-notation-block">
          <span class="panel-kicker">连招输入</span>
          <div class="route-scroll">
            <WorldTourComboRoute
              v-if="isWorldTour"
              :route="combo.route"
              :character-ids="combo.routeCharacterIds"
              :characters="combo.routeCharacters"
              size="lg"
            />
            <ComboNotation v-else :route="combo.route" size="lg" layout="vertical" />
          </div>
        </div>
      </section>

      <article :class="['detail-hero', { 'has-video': combo.videoUrl }]" aria-label="连招视频">
        <video
          v-if="combo.videoUrl"
          class="detail-avatar detail-video"
          :src="combo.videoUrl"
          controls
          preload="metadata"
        ></video>
        <template v-else>
          <div class="detail-avatar" :style="{ backgroundImage: `url(${combo.characterAvatar})` }"></div>
          <div class="detail-video-empty">
            <span class="material-symbols-outlined" aria-hidden="true">videocam_off</span>
            <strong>暂无视频</strong>
          </div>
        </template>
      </article>
    </section>

    <section class="detail-actions mb-8">
      <button :class="['detail-action', { active: combo.liked }]" :disabled="socialBusy === 'like'" @click="handleLike">
        <span class="material-symbols-outlined">thumb_up</span>
        <strong>{{ combo.liked ? '已点赞' : '点赞' }}</strong>
        <small>{{ combo.likes || 0 }}</small>
      </button>
      <button :class="['detail-action', { active: combo.favorited }]" :disabled="socialBusy === 'favorite'" @click="handleFavorite">
        <span class="material-symbols-outlined">{{ combo.favorited ? 'bookmark' : 'bookmark_add' }}</span>
        <strong>{{ combo.favorited ? '已收藏' : '收藏' }}</strong>
        <small>{{ combo.favorites || 0 }}</small>
      </button>
      <button class="detail-action danger" @click="openReport">
        <span class="material-symbols-outlined">flag</span>
        <strong>举报</strong>
        <small>管理员处理</small>
      </button>
    </section>

    <section class="grid grid-cols-2 lg:grid-cols-5 gap-gutter mb-8">
      <div class="stat-card"><span>伤害</span><strong>{{ combo.damage }}</strong></div>
      <div class="stat-card"><span>Drive 消耗</span><strong>{{ combo.driveCost }}</strong></div>
      <div class="stat-card"><span>SA 消耗</span><strong>{{ combo.saCost }}</strong></div>
      <div class="stat-card"><span>有利帧数</span><strong>{{ combo.advantageFrames || '-' }}</strong></div>
      <div class="stat-card"><span>难度</span><strong>{{ combo.difficulty }}</strong></div>
    </section>

    <section class="info-panel">
        <h2 class="panel-title">训练要点</h2>
        <p class="training-notes">{{ combo.trainingNotes || '暂无训练要点' }}</p>
    </section>

    <section v-if="showFollowupPanel" class="info-panel followup-panel">
      <div class="panel-title-row">
        <h2 class="panel-title">后续压制连招</h2>
        <router-link :to="followupUploadPath" class="followup-add-link">
          <span class="material-symbols-outlined">add</span>
          添加后续压制连招
        </router-link>
      </div>
      <div v-if="followupsLoading" class="followup-loading">同步后续压制连招中...</div>
      <div v-else-if="followupCombos.length" class="followup-grid">
        <ComboItem
          v-for="item in followupCombos"
          :key="item.id"
          :combo="item"
          social-interactive
          :social-busy="socialBusy"
          @like="handleComboLike"
          @favorite="handleComboFavorite"
        />
      </div>
      <div v-else class="followup-empty">
        <span class="material-symbols-outlined">add_link</span>
        <strong>暂无后续压制连招</strong>
        <p>可以从上传页切换为后续压制连招模式，为这个路线补一段可衔接的打法。</p>
      </div>
    </section>
  </div>

  <div v-else class="text-center py-20">
    <span class="material-symbols-outlined text-6xl text-outline-variant mb-4">sports_mma</span>
    <p class="font-headline-lg text-headline-lg text-outline">连招不存在</p>
    <router-link to="/" class="font-label-mono text-label-mono text-tertiary-container hover:underline mt-4 inline-block">返回角色</router-link>
  </div>

  <ReportDialog
    v-if="combo"
    v-model="showReport"
    target-type="combo"
    :target-id="combo.id"
    :target-name="`${combo.characterName || ''} / ${combo.starter || ''}`"
    target-label="连招"
    @submitted="reportSubmitted"
  />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { comboTagColor, comboTagLabel, normalizeComboControlType, normalizeComboTags } from '@/js/utils/helpers'
import { useComboStore } from '@/js/stores/combos'
import { useAuthStore } from '@/js/stores/auth'
import { useUiStore } from '@/js/stores/ui'
import { useRealtimeStore } from '@/js/stores/realtime'
import ComboNotation from '@/components/common/ComboNotation.vue'
import WorldTourComboRoute from '@/components/common/WorldTourComboRoute.vue'
import ComboItem from '@/components/common/ComboItem.vue'
import ReportDialog from '@/components/common/ReportDialog.vue'

const route = useRoute()
const store = useComboStore()
const auth = useAuthStore()
const ui = useUiStore()
const realtime = useRealtimeStore()
const combo = ref(null)
const followupCombos = ref([])
const loading = ref(false)
const followupsLoading = ref(false)
const socialBusy = ref('')
const showReport = ref(false)
const isWorldTour = computed(() => normalizeComboControlType(combo.value?.controlType) === 'world-tour')
const backPath = computed(() => isWorldTour.value ? '/?view=world-tour' : (combo.value?.characterId ? `/characters/${combo.value.characterId}` : '/'))
const followupUploadPath = computed(() => combo.value?.id ? `/upload?mode=followup&comboId=${combo.value.id}` : '/upload?mode=followup')
const comboTags = computed(() => normalizeComboTags(combo.value, combo.value?.type))
const showFollowupPanel = computed(() => combo.value?.id && !combo.value?.followupParentId)
let loadSequence = 0
let followupsLoadSequence = 0

function requireLogin() {
  if (auth.isLoggedIn) return true
  auth.openLogin()
  return false
}

async function handleLike() {
  if (!combo.value || !requireLogin() || socialBusy.value) return
  socialBusy.value = 'like'
  try {
    await store.toggleLike(combo.value)
  } finally {
    socialBusy.value = ''
  }
}

async function handleFavorite() {
  if (!combo.value || !requireLogin() || socialBusy.value) return
  socialBusy.value = 'favorite'
  try {
    await store.toggleFavorite(combo.value)
  } finally {
    socialBusy.value = ''
  }
}

async function handleComboLike(targetCombo) {
  if (!targetCombo || !requireLogin() || socialBusy.value) return
  socialBusy.value = `like:${targetCombo.id}`
  try {
    await store.toggleLike(targetCombo)
  } finally {
    socialBusy.value = ''
  }
}

async function handleComboFavorite(targetCombo) {
  if (!targetCombo || !requireLogin() || socialBusy.value) return
  socialBusy.value = `favorite:${targetCombo.id}`
  try {
    await store.toggleFavorite(targetCombo)
  } finally {
    socialBusy.value = ''
  }
}

function openReport() {
  if (!combo.value || !requireLogin()) return
  showReport.value = true
}

async function reportSubmitted() {
  await ui.alertDialog({
    title: '举报已提交',
    message: '管理员会在后台统一处理该连招举报。',
    tone: 'success',
  })
}

async function loadCombo(options = {}) {
  const { silent = false, force = false } = options
  const shouldKeepCurrentContent = silent && combo.value
  const sequence = ++loadSequence
  if (!route.params.id) {
    combo.value = null
    loading.value = false
    return
  }
  if (!shouldKeepCurrentContent) {
    loading.value = true
    combo.value = null
  }
  try {
    const result = await store.findById(route.params.id, { force })
    if (sequence === loadSequence) {
      combo.value = result || null
      if (showFollowupPanel.value) loadFollowups({ silent })
      else followupCombos.value = []
    }
  } catch (_) {
    if (sequence === loadSequence && !shouldKeepCurrentContent) combo.value = null
  } finally {
    if (sequence === loadSequence && !shouldKeepCurrentContent) loading.value = false
  }
}

async function loadFollowups(options = {}) {
  const { silent = false } = options
  const sequence = ++followupsLoadSequence
  if (!showFollowupPanel.value) {
    followupCombos.value = []
    followupsLoading.value = false
    return
  }
  if (!silent) followupsLoading.value = true
  try {
    const result = await store.getFollowupCombos(combo.value.id)
    if (sequence === followupsLoadSequence) followupCombos.value = result
  } catch (_) {
    if (sequence === followupsLoadSequence && !silent) followupCombos.value = []
  } finally {
    if (sequence === followupsLoadSequence && !silent) followupsLoading.value = false
  }
}

watch(() => route.params.id, () => {
  loadCombo({ force: true })
}, {
  immediate: true,
})

watch(() => realtime.lastEvent, event => {
  const areas = event?.areas || []
  if (areas.includes('comboDetail') || areas.includes('combos')) {
    loadCombo({ force: true, silent: true })
    if (showFollowupPanel.value) loadFollowups({ silent: true })
  }
})
</script>

<style scoped>
.combo-detail-shell {
  width: min(100%, 1280px);
}

.detail-workbench {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(520px, 1.55fr);
  align-items: start;
  gap: 24px;
  padding: 24px;
  background: var(--ink-surface);
  border: 1px solid var(--ink-line);
  border-radius: 6px;
}

.detail-hero {
  position: relative;
  width: 100%;
  min-height: 0;
  aspect-ratio: 16 / 9;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  background: var(--ink-surface-low);
  border: 1px solid rgba(150, 164, 184, 0.22);
  border-radius: 6px;
  overflow: hidden;
}
.detail-hero.has-video {
  background: var(--ink-surface-low);
}
.detail-avatar {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  opacity: 0.2;
  filter: grayscale(0.2);
}
.detail-video {
  position: static;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  opacity: 1;
  filter: none;
}
.detail-video-empty {
  position: relative;
  z-index: 1;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: #cbd6e6;
  font-family: 'JetBrains Mono';
}
.detail-video-empty .material-symbols-outlined {
  font-size: 42px;
  color: var(--accent-red);
}
.detail-route-panel {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  justify-content: flex-start;
  gap: 24px;
  height: clamp(360px, 33vw, 470px);
  min-width: 0;
  overflow: hidden;
}
.detail-route-head {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.combo-tag-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.detail-title {
  margin: 0;
  color: var(--ink-text);
  font-family: inherit;
  font-size: 34px;
  font-weight: 800;
  font-style: italic;
  line-height: 1.08;
  text-transform: uppercase;
  text-wrap: balance;
}
.detail-subtitle {
  margin: 0;
  color: var(--ink-muted);
  font-size: 15px;
  line-height: 1.5;
}
.route-notation-block {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 16px;
  flex: 1;
  min-height: 0;
  min-width: 0;
}
.route-scroll {
  width: 100%;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 8px;
}
.route-scroll::-webkit-scrollbar {
  width: 6px;
}
.route-scroll::-webkit-scrollbar-track {
  background: var(--ink-surface-low);
}
.route-scroll::-webkit-scrollbar-thumb {
  background: var(--ink-line);
  border-radius: 999px;
}
.route-scroll:hover::-webkit-scrollbar-thumb {
  background: var(--accent-red);
}
.panel-kicker {
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 12px;
  letter-spacing: 0;
}
.detail-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.detail-action {
  min-width: 150px;
  min-height: 48px;
  display: inline-grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--ink-surface);
  border: 1px solid var(--ink-line);
  border-radius: 6px;
  color: var(--ink-text-soft);
  font-family: 'JetBrains Mono';
  transition: border-color 0.18s ease, color 0.18s ease, background 0.18s ease;
}
.detail-action:hover {
  border-color: var(--accent-red);
  color: var(--ink-text);
  background: var(--ink-surface-high);
}
.detail-action.active {
  border-color: rgba(91, 214, 199, 0.72);
  color: var(--accent-red);
  background: rgba(91, 214, 199, 0.1);
}
.detail-action.danger {
  border-color: rgba(255, 126, 101, 0.34);
  color: var(--state-error);
}
.detail-action.danger:hover {
  border-color: rgba(255, 180, 171, 0.68);
  color: #ffdad6;
  background: rgba(255, 70, 84, 0.12);
}
.detail-action:disabled {
  opacity: 0.7;
  cursor: wait;
}
.detail-action .material-symbols-outlined {
  font-size: 20px;
}
.detail-action strong,
.detail-action small {
  line-height: 1;
}
.stat-card,
.info-panel {
  background: var(--ink-surface);
  border: 1px solid var(--ink-line);
  border-radius: 6px;
  padding: 22px;
}
.stat-card span {
  display: block;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  color: var(--ink-muted);
  margin-bottom: 8px;
}
.stat-card strong {
  font-family: inherit;
  font-size: 36px;
  color: var(--ink-text);
}
.panel-title {
  font-family: inherit;
  font-size: 24px;
  color: var(--ink-text);
  margin-bottom: 16px;
}
.followup-panel {
  margin-top: 18px;
}
.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.panel-title-row .panel-title {
  margin-bottom: 0;
}
.followup-add-link {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 12px;
  color: var(--accent-red);
  background: rgba(255, 64, 84, 0.12);
  border: 1px solid rgba(255, 64, 84, 0.54);
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  font-weight: 900;
}
.followup-add-link:hover,
.followup-add-link:focus-visible {
  color: #190607;
  background: var(--accent-red);
  border-color: var(--accent-red);
}
.followup-add-link .material-symbols-outlined {
  color: currentColor;
  font-size: 18px;
}
.followup-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.followup-empty,
.followup-loading {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 170px;
  padding: 18px;
  color: var(--ink-text-soft);
  text-align: center;
  background: var(--ink-surface-low);
  border: 1px dashed var(--ink-line);
  border-radius: 4px;
}
.followup-empty .material-symbols-outlined {
  color: var(--accent-red);
  font-size: 34px;
}
.followup-empty strong {
  color: var(--ink-text);
  font-size: 18px;
}
.followup-empty p {
  max-width: 48ch;
  margin: 0;
  color: var(--ink-muted);
  line-height: 1.6;
}
.training-notes {
  margin: 0;
  color: #cbd6e6;
  white-space: pre-wrap;
  line-height: 1.8;
}

@media (max-width: 1024px) {
  .combo-detail-shell {
    max-width: 860px;
  }

  .detail-workbench {
    grid-template-columns: 1fr;
  }

  .detail-hero.has-video {
    min-height: auto;
  }
}

@media (max-width: 640px) {
  .detail-workbench {
    gap: 14px;
    padding: 14px;
  }

  .detail-title {
    font-size: 26px;
    line-height: 1.05;
  }

  .detail-subtitle {
    font-size: 14px;
    line-height: 1.5;
  }

  .detail-route-panel {
    gap: 16px;
    height: 360px;
  }

  .route-notation-block {
    gap: 10px;
  }

  .detail-action {
    width: 100%;
    min-width: 0;
  }

  .stat-card,
  .info-panel {
    padding: 14px;
  }

  .stat-card span {
    font-size: 10px;
    margin-bottom: 4px;
  }

  .stat-card strong {
    font-size: 24px;
    line-height: 1;
  }

  .panel-title {
    font-size: 20px;
  }

  .panel-title-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .followup-add-link {
    width: 100%;
  }

  .followup-grid {
    grid-template-columns: 1fr;
  }

  .training-notes {
    font-size: 14px;
    line-height: 1.65;
  }
}
</style>
