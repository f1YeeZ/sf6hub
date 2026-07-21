<template>
  <div class="characters-page">
    <header class="characters-header">
      <div class="characters-header-copy">
        <span>{{ activeView === 'roster' ? 'CHARACTER SELECT' : 'WORLD TOUR' }}</span>
        <h1>{{ activeView === 'roster' ? '角色列表' : '环球游历' }}</h1>
        <p>{{ activeView === 'roster' ? '查看所有角色的帧数、判定和连招数据。' : '直接查看玩家上传并通过审核的环球游历连招。' }}</p>
      </div>
      <section class="weekly-upload-leaderboard" aria-labelledby="weekly-upload-title">
        <h2 id="weekly-upload-title">本周上传贡献榜</h2>
        <ol v-if="weeklyContributorsLoading" class="weekly-upload-list weekly-upload-skeleton" aria-hidden="true">
          <li v-for="rank in 3" :key="rank">
            <i class="material-symbols-outlined">emoji_events</i>
            <span></span>
            <em></em>
          </li>
        </ol>
        <ol v-else-if="weeklyContributors.length" class="weekly-upload-list">
          <li
            v-for="(entry, index) in weeklyContributors"
            :key="entry.userId"
            :class="`rank-${index + 1}`"
            :aria-label="`第 ${index + 1} 名，${entry.username}，本周上传 ${entry.comboCount} 条连招`"
          >
            <i class="material-symbols-outlined" aria-hidden="true">emoji_events</i>
            <strong :title="entry.username">{{ entry.username }}</strong>
            <em>{{ entry.comboCount }}</em>
          </li>
        </ol>
        <p v-else class="weekly-upload-empty">本周暂无上榜投稿</p>
        <span v-if="weeklyContributorsLoading" class="sr-only" aria-live="polite">正在加载本周上传榜</span>
      </section>
      <div class="characters-header-actions">
        <div class="character-view-switch" role="radiogroup" aria-label="角色页面内容">
          <button
            type="button"
            class="roster-option"
            :class="{ active: activeView === 'roster' }"
            :aria-checked="activeView === 'roster'"
            role="radio"
            @click="activeView = 'roster'"
          >
            角色列表
          </button>
          <button
            type="button"
            class="world-tour-option"
            :class="{ active: activeView === 'world-tour' }"
            :aria-checked="activeView === 'world-tour'"
            role="radio"
            @click="activeView = 'world-tour'"
          >
            环球游历
          </button>
        </div>
        <strong>{{ activeView === 'roster' ? `${store.characters.length} FIGHTERS` : `${worldTourTotal} COMBOS` }}</strong>
      </div>
    </header>

    <template v-if="activeView === 'roster'">
      <div v-if="store.loading" class="page-state">LOADING...</div>
      <div v-else-if="store.error" class="page-state error">{{ store.error }}</div>
      <div v-else class="character-grid">
        <CharacterCard v-for="(c, index) in store.characters" :key="c.id" :character="c" :tone="index % 6 < 3 ? 'red' : 'blue'" />
      </div>
    </template>

    <section v-else class="world-tour-panel" aria-labelledby="world-tour-combo-title">
      <div class="world-tour-panel-head">
        <div>
          <span>WORLD TOUR ROUTES</span>
          <h2 id="world-tour-combo-title">环球连招</h2>
        </div>
        <router-link to="/upload?controlType=world-tour" class="world-tour-upload-link">
          <span class="material-symbols-outlined">upload</span>
          上传环球连招
        </router-link>
      </div>

      <div class="combo-filter-shell">
        <div class="combo-filter-toolbar">
          <label class="filter-sort-field">
            <span>排序方式</span>
            <select v-model="worldTourFilters.sort" class="filter-select">
              <option value="damage">伤害从高到低</option>
              <option value="likes">点赞量从高到低</option>
              <option value="createdAt">最新上传</option>
            </select>
          </label>
          <button
            type="button"
            class="filter-toggle"
            :class="{ open: worldTourFiltersExpanded }"
            :aria-expanded="worldTourFiltersExpanded"
            aria-controls="world-tour-advanced-filters"
            @click="worldTourFiltersExpanded = !worldTourFiltersExpanded"
          >
            <span class="material-symbols-outlined" aria-hidden="true">tune</span>
            <span>{{ worldTourFiltersExpanded ? '收起筛选' : '更多筛选' }}</span>
            <strong v-if="worldTourActiveFilterCount">{{ worldTourActiveFilterCount }}</strong>
            <span class="material-symbols-outlined filter-toggle-chevron" aria-hidden="true">expand_more</span>
          </button>
        </div>

        <Transition name="filter-reveal">
          <div v-if="worldTourFiltersExpanded" id="world-tour-advanced-filters" class="combo-filter-reveal">
            <div class="combo-filter-panel">
              <fieldset class="filter-group">
                <legend>SA 消耗数</legend>
                <div class="filter-choice-row">
                  <label v-for="option in saCostOptions" :key="option.value" class="filter-choice">
                    <input v-model="worldTourFilters.saCost" type="radio" :value="option.value" />
                    <span>{{ option.label }}</span>
                  </label>
                </div>
              </fieldset>

              <DriveCostFilter v-model="worldTourFilters.driveCost" />

              <div class="filter-group">
                <span class="filter-group-label">伤害范围</span>
                <div class="damage-range-fields">
                  <label>
                    <span class="sr-only">最低伤害</span>
                    <input v-model.number="worldTourFilters.damageMin" type="number" min="0" max="100000" step="100" placeholder="最低伤害" />
                  </label>
                  <span aria-hidden="true">至</span>
                  <label>
                    <span class="sr-only">最高伤害</span>
                    <input v-model.number="worldTourFilters.damageMax" type="number" min="0" max="100000" step="100" placeholder="最高伤害" />
                  </label>
                </div>
              </div>

              <div class="filter-group filter-starter-field">
                <span class="filter-group-label">起手式</span>
                <StarterFilterSelect v-model="worldTourFilters.starter" :options="worldTourStarterOptions" />
              </div>

              <fieldset class="filter-group filter-tags-group">
                <legend>标签</legend>
                <div class="filter-choice-row">
                  <label v-for="option in worldTourTagOptions" :key="option.value" class="filter-choice filter-tag-choice">
                    <input v-model="worldTourFilters.tags" type="checkbox" :value="option.value" />
                    <span>{{ option.label }}</span>
                  </label>
                  <p v-if="!worldTourTagOptions.length" class="filter-empty">暂无可用标签</p>
                </div>
              </fieldset>

              <div class="filter-panel-footer">
                <span>{{ worldTourActiveFilterCount ? `已启用 ${worldTourActiveFilterCount} 项筛选` : '当前显示全部连招' }}</span>
                <button type="button" class="filter-reset" :disabled="!worldTourActiveFilterCount" @click="clearWorldTourFilters">重置筛选</button>
              </div>
            </div>
          </div>
        </Transition>
      </div>

      <div v-if="worldTourLoading" class="page-state">正在加载环球连招...</div>
      <div v-else-if="worldTourError" class="page-state error">
        <p>{{ worldTourError }}</p>
        <button type="button" @click="loadWorldTourCombos(worldTourPage)">重试</button>
      </div>
      <div v-else-if="worldTourCombos.length" class="world-tour-combo-list">
        <ComboItem
          v-for="combo in worldTourCombos"
          :key="combo.id"
          :combo="combo"
          social-interactive
          :social-busy="socialBusy"
          @like="handleComboLike"
          @favorite="handleComboFavorite"
        />
      </div>
      <div v-else class="world-tour-empty">
        <span class="material-symbols-outlined">public</span>
        <h2>{{ worldTourActiveFilterCount ? '没有符合筛选条件的连招' : '还没有环球连招' }}</h2>
        <p>{{ worldTourActiveFilterCount ? '试试放宽筛选条件，或重置后查看全部连招。' : '第一条通过审核的环球游历路线会显示在这里。' }}</p>
        <button v-if="worldTourActiveFilterCount" type="button" class="world-tour-empty-reset" @click="clearWorldTourFilters">重置筛选</button>
        <router-link v-else to="/upload?controlType=world-tour">上传环球连招</router-link>
      </div>

      <div v-if="worldTourTotal > worldTourPageSize" class="world-tour-pager">
        <button type="button" :disabled="worldTourLoading || worldTourPage <= 1" @click="loadWorldTourCombos(worldTourPage - 1)">上一页</button>
        <span>{{ worldTourPage }} / {{ worldTourPageCount }}</span>
        <button type="button" :disabled="worldTourLoading || worldTourPage >= worldTourPageCount" @click="loadWorldTourCombos(worldTourPage + 1)">下一页</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useCharacterStore } from '@/js/stores/characters'
import { useComboStore } from '@/js/stores/combos'
import { useAuthStore } from '@/js/stores/auth'
import { useRealtimeStore } from '@/js/stores/realtime'
import { api } from '@/js/api'
import { comboTagLabel, comboTagOptions } from '@/js/utils/helpers'
import CharacterCard from '@/components/common/CharacterCard.vue'
import ComboItem from '@/components/common/ComboItem.vue'
import DriveCostFilter from '@/components/common/DriveCostFilter.vue'
import StarterFilterSelect from '@/components/common/StarterFilterSelect.vue'

const store = useCharacterStore()
const route = useRoute()
const comboStore = useComboStore()
const auth = useAuthStore()
const realtime = useRealtimeStore()
const activeView = ref(route.query.view === 'world-tour' ? 'world-tour' : 'roster')
const worldTourCombos = ref([])
const worldTourPage = ref(1)
const worldTourTotal = ref(0)
const worldTourLoading = ref(false)
const worldTourError = ref('')
const worldTourFilterOptions = ref({ starters: [], tags: [] })
const worldTourFilters = reactive({
  starter: '',
  tags: [],
  saCost: '',
  driveCost: '',
  damageMin: '',
  damageMax: '',
  sort: 'createdAt',
})
const worldTourFiltersExpanded = ref(false)
const socialBusy = ref('')
const weeklyContributors = ref([])
const weeklyContributorsLoading = ref(true)
const worldTourPageSize = 12
const worldTourPageCount = computed(() => Math.max(1, Math.ceil(worldTourTotal.value / worldTourPageSize)))
const worldTourStarterOptions = computed(() => worldTourFilterOptions.value.starters)
const worldTourTagOptions = computed(() => {
  const presentTags = new Set(worldTourFilterOptions.value.tags)
  const knownOptions = comboTagOptions.filter(option => presentTags.has(option.value))
  const customOptions = [...presentTags]
    .filter(tag => !comboTagOptions.some(option => option.value === tag))
    .sort((left, right) => left.localeCompare(right))
    .map(tag => ({ value: tag, label: comboTagLabel(tag) }))
  return [...knownOptions, ...customOptions]
})
const worldTourActiveFilterCount = computed(() => [
  worldTourFilters.saCost !== '',
  worldTourFilters.driveCost !== '',
  worldTourFilters.damageMin !== '',
  worldTourFilters.damageMax !== '',
  Boolean(worldTourFilters.starter),
  ...worldTourFilters.tags.map(() => true),
].filter(Boolean).length)
const saCostOptions = [
  { value: '', label: '全部' },
  { value: 0, label: '0' },
  { value: 1, label: '1' },
  { value: 2, label: '2' },
  { value: 3, label: '3' },
]

onMounted(() => {
  store.loadCharacters()
  loadWeeklyContributors()
  if (activeView.value === 'world-tour') {
    loadWorldTourCombos(1)
    loadWorldTourFilterOptions()
  }
})

watch(activeView, value => {
  if (value === 'world-tour') {
    loadWorldTourCombos(1)
    if (!worldTourFilterOptions.value.starters.length && !worldTourFilterOptions.value.tags.length) {
      loadWorldTourFilterOptions()
    }
  }
})

watch(() => realtime.lastEvent, event => {
  const areas = event?.areas || []
  if (areas.includes('characters')) store.loadCharacters({ force: true })
  if (areas.includes('combos')) loadWeeklyContributors()
  if (areas.includes('combos') && activeView.value === 'world-tour') {
    loadWorldTourCombos(worldTourPage.value)
    loadWorldTourFilterOptions()
  }
})

watch(worldTourFilters, scheduleWorldTourReload, { deep: true })

async function loadWeeklyContributors() {
  try {
    const result = await api.getWeeklyComboContributors()
    weeklyContributors.value = Array.isArray(result) ? result.slice(0, 3) : []
  } catch (_) {
    weeklyContributors.value = []
  } finally {
    weeklyContributorsLoading.value = false
  }
}

async function loadWorldTourCombos(nextPage = worldTourPage.value) {
  const requestId = ++worldTourRequestId
  worldTourLoading.value = true
  worldTourError.value = ''
  try {
    const result = await comboStore.getWorldTourCombos({
      page: nextPage,
      pageSize: worldTourPageSize,
      starter: worldTourFilters.starter,
      tags: worldTourFilters.tags.join(','),
      saCost: worldTourFilters.saCost,
      driveCost: worldTourFilters.driveCost,
      damageMin: worldTourFilters.damageMin,
      damageMax: worldTourFilters.damageMax,
      sort: worldTourFilters.sort,
    })
    if (requestId !== worldTourRequestId) return
    worldTourCombos.value = result.list
    worldTourPage.value = result.page
    worldTourTotal.value = result.total
  } catch (error) {
    if (requestId !== worldTourRequestId) return
    worldTourError.value = error.message || '环球连招加载失败'
  } finally {
    if (requestId === worldTourRequestId) worldTourLoading.value = false
  }
}

async function loadWorldTourFilterOptions() {
  try {
    worldTourFilterOptions.value = await comboStore.getWorldTourComboFilterOptions()
  } catch (_) {
    worldTourFilterOptions.value = { starters: [], tags: [] }
  }
}

function clearWorldTourFilters() {
  worldTourFilters.starter = ''
  worldTourFilters.tags = []
  worldTourFilters.saCost = ''
  worldTourFilters.driveCost = ''
  worldTourFilters.damageMin = ''
  worldTourFilters.damageMax = ''
}

let worldTourReloadTimer = null
let worldTourRequestId = 0

function scheduleWorldTourReload() {
  if (activeView.value !== 'world-tour') return
  window.clearTimeout(worldTourReloadTimer)
  worldTourReloadTimer = window.setTimeout(() => loadWorldTourCombos(1), 180)
}

onBeforeUnmount(() => {
  window.clearTimeout(worldTourReloadTimer)
  worldTourRequestId += 1
})

function requireLogin() {
  if (auth.isLoggedIn) return true
  auth.openLogin()
  return false
}

async function handleComboLike(combo) {
  if (!combo || !requireLogin() || socialBusy.value) return
  socialBusy.value = `like:${combo.id}`
  try {
    await comboStore.toggleLike(combo)
  } finally {
    socialBusy.value = ''
  }
}

async function handleComboFavorite(combo) {
  if (!combo || !requireLogin() || socialBusy.value) return
  socialBusy.value = `favorite:${combo.id}`
  try {
    await comboStore.toggleFavorite(combo)
  } finally {
    socialBusy.value = ''
  }
}
</script>

<style scoped>
.characters-page {
  display: grid;
  gap: 24px;
}

.characters-header {
  display: grid;
  grid-template-columns: minmax(260px, 0.8fr) minmax(540px, 1.5fr) minmax(280px, 0.9fr);
  align-items: flex-end;
  gap: 20px;
  padding: 0 0 18px 58px;
  border-bottom: 1px solid rgba(101, 67, 63, 0.82);
}

.characters-header-copy > span,
.characters-header-actions > strong,
.page-state {
  font-family: 'JetBrains Mono';
}

.characters-header-copy > span {
  display: inline-block;
  color: var(--accent-red-hot);
  font-size: 12px;
  letter-spacing: 0;
}

.characters-header h1 {
  margin: 8px 0 0;
  color: var(--ink-text);
  font-family: inherit;
  font-size: 40px;
  font-weight: 900;
  line-height: 1.08;
}

.characters-header p {
  max-width: 58ch;
  margin: 10px 0 0;
  color: var(--ink-text-soft);
  font-size: 16px;
  line-height: 1.6;
}

.characters-header-actions > strong {
  flex: 0 0 auto;
  color: var(--ink-text);
  font-size: 12px;
  padding: 8px 10px;
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  background: rgba(18, 16, 15, 0.9);
}

.characters-header-actions {
  grid-column: 3;
  justify-self: end;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.weekly-upload-leaderboard {
  grid-column: 2;
  width: 100%;
  min-height: 58px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 7px 9px 7px 14px;
  color: var(--ink-text);
  background: rgba(7, 5, 6, 0.5);
  border: 1px solid rgba(101, 67, 63, 0.46);
  border-radius: 4px;
  font-family: 'JetBrains Mono';
}

.weekly-upload-leaderboard > h2 {
  flex: 0 0 auto;
  margin: 0;
  color: var(--ink-muted);
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}

.weekly-upload-list {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.weekly-upload-list li {
  flex: 0 1 auto;
  width: max-content;
  min-width: 112px;
  max-width: 180px;
  height: 42px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 7px 5px 10px;
  background: rgba(18, 16, 15, 0.74);
  border: 1px solid rgba(101, 67, 63, 0.52);
  border-radius: 999px;
}

.weekly-upload-list i {
  flex: 0 0 auto;
  color: var(--ink-muted);
  font-size: 18px;
  font-style: normal;
}

.weekly-upload-list .rank-1 i { color: #ffd34e; }
.weekly-upload-list .rank-2 i { color: #c7d0db; }
.weekly-upload-list .rank-3 i { color: #c98652; }

.weekly-upload-list .rank-1 {
  border-color: rgba(255, 211, 78, 0.36);
}

.weekly-upload-list .rank-2 {
  border-color: rgba(199, 208, 219, 0.3);
}

.weekly-upload-list .rank-3 {
  border-color: rgba(201, 134, 82, 0.34);
}

.weekly-upload-list strong {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  color: var(--ink-text);
  font-size: 12px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.weekly-upload-list em {
  min-width: 32px;
  height: 28px;
  display: grid;
  place-items: center;
  padding: 0 7px;
  color: #dcecff;
  background: rgba(50, 105, 190, 0.34);
  border: 1px solid rgba(89, 143, 225, 0.42);
  border-radius: 999px;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
  white-space: nowrap;
}

.weekly-upload-empty {
  flex: 1 1 auto;
  margin: 0;
  color: var(--ink-muted);
  font-size: 11px;
  text-align: center;
}

.weekly-upload-skeleton li > * {
  background: rgba(255, 248, 239, 0.09);
  animation: weekly-upload-pulse 1.2s ease-in-out infinite alternate;
}

.weekly-upload-skeleton i {
  width: 18px;
  height: 18px;
  color: transparent;
  border-radius: 50%;
}

.weekly-upload-skeleton span {
  flex: 1 1 auto;
  height: 10px;
  border-radius: 3px;
}

.weekly-upload-skeleton em {
  width: 32px;
  min-width: 32px;
  height: 28px;
}

.weekly-upload-skeleton li {
  width: 150px;
}

@keyframes weekly-upload-pulse {
  to { background: rgba(255, 248, 239, 0.16); }
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

.character-view-switch {
  display: inline-flex;
  align-items: center;
  padding: 3px;
  background: rgba(18, 16, 15, 0.9);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
}

.character-view-switch button {
  min-height: 34px;
  padding: 0 13px;
  color: var(--ink-text-soft);
  background: transparent;
  border: 1px solid transparent;
  border-radius: 3px;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  font-weight: 900;
  transition: color 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.character-view-switch button:hover,
.character-view-switch button:focus-visible {
  color: var(--ink-text);
  outline: none;
}

.character-view-switch .roster-option:hover,
.character-view-switch .roster-option:focus-visible {
  border-color: rgba(255, 64, 84, 0.42);
  background: rgba(255, 64, 84, 0.1);
}

.character-view-switch .world-tour-option:hover,
.character-view-switch .world-tour-option:focus-visible {
  color: var(--accent-cyan-soft);
  border-color: rgba(49, 213, 230, 0.42);
  background: rgba(49, 213, 230, 0.09);
}

.character-view-switch button.active {
  color: #fff;
  border-color: rgba(255, 64, 84, 0.68);
  background: rgba(255, 64, 84, 0.18);
}

.character-view-switch .world-tour-option.active {
  color: var(--accent-cyan-soft);
  border-color: rgba(49, 213, 230, 0.62);
  background: rgba(49, 213, 230, 0.12);
}

.character-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px 14px;
  width: min(100%, 1048px);
  margin-inline: auto;
}

.page-state {
  padding: 72px 0;
  text-align: center;
  color: var(--ink-muted);
  font-size: 14px;
}

.page-state.error {
  color: var(--state-error);
}

.page-state p {
  margin: 0;
}

.page-state button {
  min-height: 36px;
  margin-top: 12px;
  padding: 0 14px;
  color: var(--ink-text);
  background: var(--ink-surface-high);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
}

.world-tour-panel {
  display: grid;
  gap: 18px;
  padding: 22px;
  background: var(--ink-surface);
  border: 1px solid var(--ink-line);
  border-radius: 6px;
}

.world-tour-panel-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--ink-line);
}

.world-tour-panel-head span,
.world-tour-pager {
  font-family: 'JetBrains Mono';
}

.world-tour-panel-head > div > span {
  color: var(--accent-cyan-soft);
  font-size: 11px;
}

.world-tour-panel-head h2 {
  margin: 5px 0 0;
  color: var(--ink-text);
  font-size: 26px;
  font-weight: 900;
}

.combo-filter-shell {
  position: relative;
  z-index: 5;
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  overflow: visible;
}

.combo-filter-toolbar {
  min-height: 70px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
}

.filter-sort-field {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--ink-text-soft);
  font-family: 'JetBrains Mono';
  font-size: 12px;
  font-weight: 700;
}

.filter-sort-field .filter-select {
  width: 190px;
}

.filter-select {
  width: 100%;
  min-height: 44px;
  padding: 0 12px;
  color: var(--ink-text);
  background: var(--ink-surface);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-size: 12px;
  outline: none;
}

.filter-select:focus {
  border-color: var(--accent-red);
  box-shadow: 0 0 0 3px rgba(255, 64, 84, 0.14);
}

.filter-toggle {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 14px;
  color: var(--ink-text);
  background: var(--ink-surface-high);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  font-weight: 800;
  outline: none;
  transition: color 0.18s ease, background 0.18s ease, border-color 0.18s ease;
}

.filter-toggle:hover,
.filter-toggle:focus-visible,
.filter-toggle.open {
  border-color: var(--accent-red);
  background: rgba(255, 64, 84, 0.1);
}

.filter-toggle .material-symbols-outlined {
  font-size: 18px;
}

.filter-toggle strong {
  min-width: 20px;
  height: 20px;
  display: inline-grid;
  place-items: center;
  padding: 0 5px;
  color: #190607;
  background: var(--accent-red);
  border-radius: 999px;
  font-size: 10px;
}

.filter-toggle-chevron {
  transition: transform 0.18s ease;
}

.filter-toggle.open .filter-toggle-chevron {
  transform: rotate(180deg);
}

.combo-filter-reveal {
  position: absolute;
  z-index: 40;
  top: calc(100% + 6px);
  right: -1px;
  left: -1px;
  display: grid;
  grid-template-rows: 1fr;
  filter: drop-shadow(0 18px 28px rgba(0, 0, 0, 0.5));
}

.filter-reveal-enter-active {
  overflow: hidden;
  transition: grid-template-rows 260ms cubic-bezier(0.22, 1, 0.36, 1), opacity 200ms ease-out;
}

.filter-reveal-leave-active {
  overflow: hidden;
  transition: grid-template-rows 190ms cubic-bezier(0.4, 0, 1, 1), opacity 140ms ease-in;
}

.filter-reveal-enter-from,
.filter-reveal-leave-to {
  grid-template-rows: 0fr;
  opacity: 0;
}

.filter-reveal-enter-active .combo-filter-panel {
  transition: transform 230ms cubic-bezier(0.22, 1, 0.36, 1);
}

.filter-reveal-enter-from .combo-filter-panel {
  transform: translateY(-8px);
}

.combo-filter-panel {
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px 28px;
  padding: 22px 24px 18px;
  border: 1px solid var(--ink-line-strong);
  border-radius: 4px;
  background: var(--ink-surface);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03), 0 0 0 1px rgba(255, 64, 84, 0.06);
}

.filter-group {
  min-width: 0;
  margin: 0;
  padding: 0;
  border: 0;
}

.filter-group legend,
.filter-group-label {
  display: block;
  margin-bottom: 10px;
  color: var(--ink-text-soft);
  font-family: 'JetBrains Mono';
  font-size: 12px;
  font-weight: 800;
}

.filter-choice-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-choice {
  position: relative;
  min-width: 52px;
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  color: var(--ink-text-soft);
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  cursor: pointer;
  transition: color 0.16s ease, background 0.16s ease, border-color 0.16s ease;
}

.filter-choice:hover,
.filter-choice:focus-within {
  color: var(--ink-text);
  border-color: rgba(255, 64, 84, 0.62);
}

.filter-choice:has(input:checked) {
  color: #190607;
  background: var(--accent-red);
  border-color: var(--accent-red);
}

.filter-choice input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.damage-range-fields {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}

.damage-range-fields input {
  width: 100%;
  min-height: 44px;
  padding: 0 12px;
  color: var(--ink-text);
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  outline: none;
}

.damage-range-fields input:focus {
  border-color: var(--accent-red);
  box-shadow: 0 0 0 3px rgba(255, 64, 84, 0.14);
}

.damage-range-fields input::placeholder {
  color: var(--ink-text-soft);
  opacity: 1;
}

.filter-starter-field .filter-select {
  background: var(--ink-surface-low);
}

.filter-tags-group,
.filter-panel-footer {
  grid-column: 1 / -1;
}

.filter-tag-choice {
  min-width: 0;
}

.filter-empty {
  margin: 8px 0;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}

.filter-panel-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--ink-line);
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 11px;
}

.filter-reset {
  min-height: 44px;
  padding: 0 14px;
  color: #190607;
  border: 0;
  border-radius: 4px;
  background: var(--accent-red);
  font-size: 12px;
}

.filter-reset:hover {
  color: #fff;
  background: var(--accent-red-hot);
}

.filter-reset:disabled {
  color: var(--ink-muted);
  background: var(--ink-surface-high);
  cursor: not-allowed;
  opacity: 0.7;
}

.world-tour-upload-link,
.world-tour-empty a,
.world-tour-empty-reset {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 13px;
  color: #061315;
  background: var(--accent-cyan-soft);
  border-radius: 4px;
  font-size: 13px;
  font-weight: 900;
}

.world-tour-empty-reset {
  margin-top: 8px;
  border: 0;
}

.world-tour-upload-link .material-symbols-outlined {
  font-size: 18px;
}

.world-tour-combo-list {
  display: grid;
  gap: 12px;
}

.world-tour-empty {
  min-height: 280px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 9px;
  padding: 28px;
  text-align: center;
  color: var(--ink-text-soft);
}

.world-tour-empty > .material-symbols-outlined {
  color: var(--accent-cyan-soft);
  font-size: 44px;
}

.world-tour-empty h2,
.world-tour-empty p {
  margin: 0;
}

.world-tour-empty h2 {
  color: var(--ink-text);
  font-size: 24px;
}

.world-tour-empty a {
  margin-top: 8px;
}

.world-tour-pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding-top: 4px;
  color: var(--ink-text-soft);
  font-size: 12px;
}

.world-tour-pager button {
  min-height: 36px;
  padding: 0 13px;
  color: var(--ink-text);
  background: var(--ink-surface-high);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
}

.world-tour-pager button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

@media (max-width: 1280px) {
  .characters-header {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .characters-header-copy {
    grid-column: 1;
    grid-row: 1;
  }

  .characters-header-actions {
    grid-column: 2;
    grid-row: 1;
  }

  .weekly-upload-leaderboard {
    grid-column: 1 / -1;
    grid-row: 2;
    width: min(100%, 900px);
    justify-self: center;
  }
}

@media (max-width: 640px) {
  .characters-page {
    gap: 18px;
  }

  .characters-header {
    grid-template-columns: minmax(0, 1fr);
    align-items: flex-start;
    gap: 12px;
    padding-left: 46px;
    padding-bottom: 14px;
  }

  .characters-header-copy,
  .weekly-upload-leaderboard,
  .characters-header-actions {
    grid-column: 1;
    grid-row: auto;
    justify-self: stretch;
  }

  .weekly-upload-leaderboard {
    width: 100%;
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    gap: 8px;
    padding: 10px;
  }

  .weekly-upload-list {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
  }

  .weekly-upload-list li {
    width: 100%;
    max-width: none;
  }

  .characters-header-actions {
    width: 100%;
    align-items: flex-start;
    justify-content: flex-start;
  }

  .character-view-switch {
    width: 100%;
  }

  .character-view-switch button {
    flex: 1 1 0;
  }

  .characters-header h1 {
    font-size: 30px;
    line-height: 1.05;
  }

  .characters-header p {
    font-size: 14px;
    line-height: 1.45;
  }

  .character-grid {
    gap: 9px 10px;
    width: min(100%, 782px);
  }

  .world-tour-panel {
    padding: 14px;
  }

  .world-tour-panel-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .world-tour-upload-link {
    width: 100%;
  }

  .combo-filter-toolbar {
    align-items: stretch;
    flex-direction: column;
    padding: 12px;
  }

  .combo-filter-reveal {
    max-height: 70vh;
    overflow-y: auto;
  }

  .filter-sort-field {
    align-items: stretch;
    flex-direction: column;
    gap: 7px;
  }

  .filter-sort-field .filter-select,
  .filter-toggle {
    width: 100%;
  }

  .combo-filter-panel {
    grid-template-columns: 1fr;
    gap: 20px;
    padding: 18px 14px 14px;
  }

  .filter-tags-group,
  .filter-panel-footer {
    grid-column: auto;
  }

  .damage-range-fields {
    grid-template-columns: 1fr;
  }

  .damage-range-fields > span {
    display: none;
  }

  .filter-panel-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-reset {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .weekly-upload-skeleton li > * {
    animation: none;
  }
}
</style>
