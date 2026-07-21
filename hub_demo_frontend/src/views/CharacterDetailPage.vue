<template>
  <div v-if="character">
    <router-link to="/" class="detail-back-link">
      <span class="material-symbols-outlined">arrow_back</span>
      返回角色列表
    </router-link>

    <!-- Hero -->
    <section class="character-hero relative bg-surface-container-low border-2 border-tertiary-container clip-chamfer p-8 md:p-12 overflow-hidden mb-12">
      <div class="absolute top-0 right-0 w-1/2 h-full bg-gradient-to-l from-tertiary-container/10 to-transparent -skew-x-[15deg]"></div>
      <div class="character-hero-layout relative z-10">
        <div class="relative shrink-0">
          <div class="w-40 h-40 md:w-48 md:h-48 border-4 border-primary-container overflow-hidden clip-chamfer-reverse glow-y">
            <img :src="character.avatar" :alt="`${character.name} 角色头像`" class="w-full h-full object-cover" />
          </div>
        </div>
        <div class="character-hero-copy flex-grow flex flex-col items-center md:items-start text-center md:text-left gap-4">
          <div>
            <h1 class="font-headline-xl text-headline-xl text-primary italic uppercase tracking-tighter">{{ character.name }}</h1>
            <p class="font-label-mono text-label-mono text-outline-variant">{{ character.nameJa }}</p>
          </div>
          <p class="font-body-md text-body-md text-on-surface max-w-2xl mt-2">{{ character.description }}</p>
        </div>
        <aside v-if="profileData" class="character-data-panel" aria-labelledby="character-data-heading">
          <div class="character-data-head">
            <div>
              <span>FIGHTER METRICS</span>
              <h2 id="character-data-heading">角色数据</h2>
            </div>
          </div>
          <dl class="character-data-grid">
            <div v-for="stat in profileData.stats" :key="stat.key" class="character-data-stat">
              <dt :title="stat.sourceLabel">{{ stat.label }}</dt>
              <dd>{{ stat.value }}</dd>
            </div>
          </dl>
        </aside>
      </div>
    </section>

    <!-- Frame Data Table -->
    <section class="mb-12 frame-panel">
      <div class="frame-panel-head border-b-2 border-surface-variant pb-4 slanted" :class="{ collapsed: framesCollapsed }">
        <div class="frame-mode-switch un-slant" role="radiogroup" aria-label="Frame mode">
          <button
            v-for="option in frameControlOptions"
            :key="option.value"
            type="button"
            :class="['frame-mode-option', `is-${option.value}`, { active: frameMode === option.value }]"
            :aria-checked="frameMode === option.value"
            role="radio"
            @click="frameMode = option.value"
          >
            {{ option.label }}
          </button>
        </div>
        <h2 class="font-headline-lg text-headline-lg text-primary italic uppercase un-slant">帧数</h2>
        <button
          type="button"
          class="frame-collapse-toggle un-slant"
          :aria-expanded="!framesCollapsed"
          aria-controls="character-frame-table"
          :aria-label="framesCollapsed ? '展开帧数' : '折叠帧数'"
          @click="toggleFrames"
        >
          <span class="material-symbols-outlined">{{ framesCollapsed ? 'expand_more' : 'expand_less' }}</span>
        </button>
      </div>
      <div v-if="framesMounted" v-show="!framesCollapsed" id="character-frame-table" class="frame-table-wrap">
        <table class="frame-table w-full text-left font-label-mono text-label-mono">
          <thead>
            <tr class="border-b-2 border-surface-variant text-outline uppercase text-xs tracking-wider">
              <th class="py-3 px-4 frame-move-head">招式</th>
              <th class="py-3 px-4">启动</th>
              <th class="py-3 px-4">持续</th>
              <th class="py-3 px-4">恢复</th>
              <th class="py-3 px-4">防御</th>
              <th class="py-3 px-4">命中</th>
              <th class="py-3 px-4">伤害</th>
              <th class="py-3 px-4">Cancel</th>
              <th class="py-3 px-4">Property</th>
            </tr>
          </thead>
          <tbody v-if="visibleFrames.length">
            <tr v-for="f in visibleFrames" :key="f.id || `${f.move}-${f.displayOrder}`" class="border-b border-surface-variant/50 hover:bg-surface-container transition-colors">
              <td class="py-3 px-4 text-primary font-bold frame-move-cell">
                <ComboNotation :route="f.move" size="sm" />
              </td>
              <td class="py-3 px-4">{{ formatFrameValue(f.startup) }}</td>
              <td class="py-3 px-4">{{ formatFrameValue(f.active) }}</td>
              <td class="py-3 px-4">{{ formatFrameValue(f.recovery) }}</td>
              <td :class="frameAdvantageClass(f.onBlock)">{{ formatSignedValue(f.onBlock) }}</td>
              <td :class="frameAdvantageClass(f.onHit, 2)">{{ formatSignedValue(f.onHit) }}</td>
              <td class="py-3 px-4">{{ f.damage || '-' }}</td>
              <td class="py-3 px-4">{{ f.cancel || '-' }}</td>
              <td class="py-3 px-4">{{ f.properties || '-' }}</td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr class="border-b border-surface-variant/50">
              <td class="py-6 px-4 text-outline" colspan="9">暂无帧数数据</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="framesMounted" v-show="!framesCollapsed" class="frame-panel-footer">
        <button
          type="button"
          class="frame-collapse-bottom"
          aria-controls="character-frame-table"
          @click="framesCollapsed = true"
        >
          <span class="material-symbols-outlined">expand_less</span>
          折叠帧数
        </button>
      </div>
    </section>

    <section class="combo-panel">
      <div class="combo-panel-head">
        <div class="combo-panel-title">
          <span>COMBO FILTER</span>
          <h2>连招</h2>
        </div>
        <div class="combo-control-switch" role="radiogroup" aria-label="连招类型">
          <button
            v-for="option in characterComboControlOptions"
            :key="option.value"
            type="button"
            :class="['combo-control-option', `is-${option.value}`, { active: comboFilters.controlType === option.value }]"
            :aria-checked="comboFilters.controlType === option.value"
            role="radio"
            @click="comboFilters.controlType = option.value"
          >
            {{ option.label }}
          </button>
        </div>
        <div class="combo-panel-actions">
          <router-link :to="comboUploadPath" class="combo-upload-link">
            <span class="material-symbols-outlined">upload</span>
            上传连招
          </router-link>
          <strong>{{ comboTotal }} 条</strong>
        </div>
      </div>

      <div class="combo-filter-shell">
        <div class="combo-filter-toolbar">
          <label class="filter-sort-field">
            <span>排序方式</span>
            <select v-model="comboFilters.sort" class="filter-select">
              <option value="damage">伤害从高到低</option>
              <option value="likes">点赞量从高到低</option>
              <option value="createdAt">最新上传</option>
            </select>
          </label>
          <button
            type="button"
            class="filter-toggle"
            :class="{ open: filtersExpanded }"
            :aria-expanded="filtersExpanded"
            aria-controls="combo-advanced-filters"
            @click="filtersExpanded = !filtersExpanded"
          >
            <span class="material-symbols-outlined" aria-hidden="true">tune</span>
            <span>{{ filtersExpanded ? '收起筛选' : '更多筛选' }}</span>
            <strong v-if="activeFilterCount">{{ activeFilterCount }}</strong>
            <span class="material-symbols-outlined filter-toggle-chevron" aria-hidden="true">expand_more</span>
          </button>
        </div>

        <Transition name="filter-reveal">
          <div v-if="filtersExpanded" id="combo-advanced-filters" class="combo-filter-reveal">
            <div class="combo-filter-panel">
          <fieldset class="filter-group">
            <legend>SA 消耗数</legend>
            <div class="filter-choice-row">
              <label v-for="option in saCostOptions" :key="option.value" class="filter-choice">
                <input v-model="comboFilters.saCost" type="radio" :value="option.value" />
                <span>{{ option.label }}</span>
              </label>
            </div>
          </fieldset>

          <DriveCostFilter v-model="comboFilters.driveCost" />

          <div class="filter-group">
            <span class="filter-group-label">伤害范围</span>
            <div class="damage-range-fields">
              <label>
                <span class="sr-only">最低伤害</span>
                <input v-model.number="comboFilters.damageMin" type="number" min="0" max="100000" step="100" placeholder="最低伤害" />
              </label>
              <span aria-hidden="true">至</span>
              <label>
                <span class="sr-only">最高伤害</span>
                <input v-model.number="comboFilters.damageMax" type="number" min="0" max="100000" step="100" placeholder="最高伤害" />
              </label>
            </div>
          </div>

          <div class="filter-group filter-starter-field">
            <span class="filter-group-label">起手式</span>
            <StarterFilterSelect v-model="comboFilters.starter" :options="starterOptions" />
          </div>

          <fieldset class="filter-group filter-tags-group">
            <legend>标签</legend>
            <div class="filter-choice-row">
              <label v-for="option in availableTagOptions" :key="option.value" class="filter-choice filter-tag-choice">
                <input v-model="comboFilters.tags" type="checkbox" :value="option.value" />
                <span>{{ option.label }}</span>
              </label>
              <p v-if="!availableTagOptions.length" class="filter-empty">暂无可用标签</p>
            </div>
          </fieldset>

              <div class="filter-panel-footer">
                <span>{{ activeFilterCount ? `已启用 ${activeFilterCount} 项筛选` : '当前显示全部连招' }}</span>
                <button type="button" class="filter-reset" :disabled="!activeFilterCount" @click="clearAdvancedFilters">重置筛选</button>
              </div>
            </div>
          </div>
        </Transition>
      </div>

      <div v-if="pagedCombos.length" class="combo-list">
        <ComboItem
          v-for="c in pagedCombos"
          :key="c.id"
          :combo="c"
          social-interactive
          :social-busy="socialBusy"
          @like="handleComboLike"
          @favorite="handleComboFavorite"
        />
      </div>
      <div v-else class="combo-empty">
        <span class="material-symbols-outlined">sports_mma</span>
        <p>没有匹配的连招</p>
      </div>
      <div v-if="comboTotal > comboPageSize" class="pager-row">
        <button type="button" :disabled="comboPage <= 1" @click="changeComboPage(comboPage - 1)">上一页</button>
        <span>{{ comboPage }} / {{ comboPageCount }}</span>
        <button type="button" :disabled="comboPage >= comboPageCount" @click="changeComboPage(comboPage + 1)">下一页</button>
      </div>
    </section>
  </div>
  <div v-else-if="store.loading" class="text-center py-20">
    <span class="material-symbols-outlined text-6xl text-outline-variant mb-4">hourglass_top</span>
    <p class="font-headline-lg text-headline-lg text-outline">LOADING...</p>
  </div>
  <div v-else class="text-center py-20">
    <span class="material-symbols-outlined text-6xl text-outline-variant mb-4">person_off</span>
    <p class="font-headline-lg text-headline-lg text-outline">角色不存在</p>
    <router-link to="/characters" class="font-label-mono text-label-mono text-tertiary-container hover:underline mt-4 inline-block">返回角色列表</router-link>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useCharacterStore } from '@/js/stores/characters'
import { useComboStore } from '@/js/stores/combos'
import { useAuthStore } from '@/js/stores/auth'
import { useRealtimeStore } from '@/js/stores/realtime'
import ComboItem from '@/components/common/ComboItem.vue'
import ComboNotation from '@/components/common/ComboNotation.vue'
import DriveCostFilter from '@/components/common/DriveCostFilter.vue'
import StarterFilterSelect from '@/components/common/StarterFilterSelect.vue'
import { comboControlOptions, comboMatchesControlType, comboTagLabel, comboTagOptions } from '@/js/utils/helpers'
import { getCharacterProfileData } from '@/js/data/characterProfileData'

const route = useRoute()
const store = useCharacterStore()
const comboStore = useComboStore()
const auth = useAuthStore()
const realtime = useRealtimeStore()
const detailCharacter = ref(null)
const socialBusy = ref('')
const pagedCombos = ref([])
const comboPage = ref(1)
const comboPageSize = 12
const comboTotal = ref(0)
const comboFilterOptions = ref({ starters: [], tags: [] })
const comboFilters = reactive({
  controlType: 'classic',
  starter: '',
  tags: [],
  saCost: '',
  driveCost: '',
  damageMin: '',
  damageMax: '',
  sort: 'createdAt',
})
const characterComboControlOptions = comboControlOptions.filter(option => option.value !== 'world-tour')
const character = computed(() => detailCharacter.value || store.findById(route.params.id))
const profileData = computed(() => getCharacterProfileData(character.value))
const comboPageCount = computed(() => Math.max(1, Math.ceil(comboTotal.value / comboPageSize)))
const comboUploadPath = computed(() => character.value?.id ? `/upload?characterId=${character.value.id}` : '/upload')
const starterOptions = computed(() => comboFilterOptions.value.starters)
const availableTagOptions = computed(() => {
  const presentTags = new Set(comboFilterOptions.value.tags)
  const knownOptions = comboTagOptions.filter(option => presentTags.has(option.value))
  const customOptions = [...presentTags]
    .filter(tag => !comboTagOptions.some(option => option.value === tag))
    .sort((left, right) => left.localeCompare(right))
    .map(tag => ({ value: tag, label: comboTagLabel(tag) }))
  return [...knownOptions, ...customOptions]
})
const filtersExpanded = ref(false)
const saCostOptions = [
  { value: '', label: '全部' },
  { value: 0, label: '0' },
  { value: 1, label: '1' },
  { value: 2, label: '2' },
  { value: 3, label: '3' },
]
const activeFilterCount = computed(() => [
  comboFilters.saCost !== '',
  comboFilters.driveCost !== '',
  comboFilters.damageMin !== '',
  comboFilters.damageMax !== '',
  Boolean(comboFilters.starter),
  ...comboFilters.tags.map(() => true),
].filter(Boolean).length)
const framesCollapsed = ref(true)
const framesMounted = ref(false)
const frameMode = ref('classic')
const frameControlOptions = [
  { value: 'classic', label: '经典' },
  { value: 'modern', label: '现代' },
]
const visibleFrames = computed(() => {
  const frames = character.value?.frames || []
  return frames.filter(frame => (frame.controlType || 'classic') === frameMode.value)
})

function toggleFrames() {
  if (framesCollapsed.value) framesMounted.value = true
  framesCollapsed.value = !framesCollapsed.value
}

function numericFrameValue(value) {
  const match = String(value ?? '').match(/-?\d+/)
  return match ? Number(match[0]) : null
}

function formatFrameValue(value) {
  if (value === undefined || value === null || value === '') return '-'
  const text = String(value)
  return /^-?\d+$/.test(text) ? `${text}f` : text
}

function formatSignedValue(value) {
  if (value === undefined || value === null || value === '') return '-'
  const text = String(value)
  const number = numericFrameValue(text)
  if (number === null || !/^-?\d+$/.test(text)) return text
  return number > 0 ? `+${number}` : String(number)
}

function frameAdvantageClass(value, goodThreshold = 0) {
  const number = numericFrameValue(value)
  return [
    'frame-value-cell font-bold',
    number === null
      ? 'text-on-surface'
      : number < -5
        ? 'text-error'
        : number >= goodThreshold
          ? 'text-tertiary-container'
          : 'text-on-surface',
  ]
}

async function loadDetail(options = {}) {
  const { resetFilters = true } = options
  if (resetFilters) {
    detailCharacter.value = null
    resetComboFilters()
  }
  detailCharacter.value = await store.loadCharacterDetail(route.params.id)
  await Promise.all([
    loadCombosPage(resetFilters ? 1 : comboPage.value),
    loadComboFilterOptions(),
  ])
}

async function loadComboFilterOptions() {
  if (!character.value?.id) return
  comboFilterOptions.value = await store.loadCharacterComboFilterOptions(character.value.id, {
    controlType: comboFilters.controlType,
  })
}

async function loadCombosPage(nextPage = comboPage.value) {
  if (!character.value?.id) return
  const requestId = ++comboRequestId
  const result = await store.loadCharacterCombos(character.value.id, {
    page: nextPage,
    pageSize: comboPageSize,
    controlType: comboFilters.controlType,
    starter: comboFilters.starter,
    tags: comboFilters.tags.join(','),
    saCost: comboFilters.saCost,
    driveCost: comboFilters.driveCost,
    damageMin: comboFilters.damageMin,
    damageMax: comboFilters.damageMax,
    sort: comboFilters.sort,
  })
  if (requestId !== comboRequestId) return
  pagedCombos.value = result.list
  comboPage.value = Number(result.page || nextPage)
  comboTotal.value = Number(result.total || 0)
}

async function changeComboPage(nextPage) {
  const page = Math.min(Math.max(1, nextPage), comboPageCount.value)
  if (page === comboPage.value) return
  await loadCombosPage(page)
}

function requireLogin() {
  if (auth.isLoggedIn) return true
  auth.openLogin()
  return false
}

function patchCharacterCombo(comboId, patch) {
  const combos = pagedCombos.value || []
  const target = combos.find(combo => Number(combo.id) === Number(comboId))
  if (target) Object.assign(target, patch)
}

async function handleComboLike(combo) {
  if (!combo || !requireLogin() || socialBusy.value) return
  socialBusy.value = `like:${combo.id}`
  try {
    const patch = await comboStore.toggleLike(combo)
    patchCharacterCombo(combo.id, patch)
  } finally {
    socialBusy.value = ''
  }
}

async function handleComboFavorite(combo) {
  if (!combo || !requireLogin() || socialBusy.value) return
  socialBusy.value = `favorite:${combo.id}`
  try {
    const patch = await comboStore.toggleFavorite(combo)
    patchCharacterCombo(combo.id, patch)
  } finally {
    socialBusy.value = ''
  }
}

function clearAdvancedFilters() {
  comboFilters.starter = ''
  comboFilters.tags = []
  comboFilters.saCost = ''
  comboFilters.driveCost = ''
  comboFilters.damageMin = ''
  comboFilters.damageMax = ''
}

function resetComboFilters() {
  comboFilters.controlType = 'classic'
  comboFilters.sort = 'createdAt'
  clearAdvancedFilters()
  filtersExpanded.value = false
}

onMounted(loadDetail)
watch(() => route.params.id, () => {
  framesCollapsed.value = true
  framesMounted.value = false
  loadDetail()
})
watch(() => realtime.lastEvent, event => {
  const areas = event?.areas || []
  if (areas.includes('characters') || areas.includes('frames') || areas.includes('combos')) {
    loadDetail({ resetFilters: false })
  }
})
watch(comboFilters, scheduleComboReload, { deep: true })
watch(() => comboFilters.controlType, () => {
  clearAdvancedFilters()
  loadComboFilterOptions()
})

let comboReloadTimer = null
let comboRequestId = 0

function scheduleComboReload() {
  window.clearTimeout(comboReloadTimer)
  comboReloadTimer = window.setTimeout(() => loadCombosPage(1), 180)
}

onBeforeUnmount(() => {
  window.clearTimeout(comboReloadTimer)
  comboRequestId += 1
})
</script>

<style scoped>
.detail-back-link {
  width: fit-content;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin: 0 0 18px;
  padding: 9px 12px;
  color: var(--ink-muted);
  background: var(--ink-surface);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  font-size: 12px;
}

.detail-back-link:hover,
.detail-back-link:focus-visible {
  color: #fff;
  border-color: var(--accent-red);
  background: var(--ink-surface-high);
}

.detail-back-link .material-symbols-outlined {
  font-size: 17px;
}

.combo-panel {
  display: grid;
  gap: 18px;
  padding: 22px;
  background: var(--ink-surface);
  border: 1px solid var(--ink-line);
  border-radius: 6px;
}

.combo-panel-head {
  display: grid;
  grid-template-columns: minmax(160px, 1fr) auto minmax(160px, 1fr);
  align-items: end;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--ink-line);
}

.combo-panel-head span,
.combo-panel-head strong,
.combo-control-option,
.filter-select,
.filter-reset,
.combo-empty,
.tag-filter-trigger,
.tag-filter-option,
.tag-filter-empty {
  font-family: 'JetBrains Mono';
}

.combo-panel-head span {
  color: var(--accent-red);
  font-size: 12px;
  letter-spacing: 0;
}

.combo-panel-head h2 {
  margin: 6px 0 0;
  color: var(--ink-text);
  font-family: inherit;
  font-size: 32px;
  font-weight: 900;
  line-height: 1;
}

.combo-control-switch {
  min-width: 216px;
  display: inline-grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-self: center;
  justify-self: center;
  padding: 4px;
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
}

.combo-control-option {
  min-width: 96px;
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-text-soft);
  background: transparent;
  border: 1px solid transparent;
  border-radius: 3px;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
  transition: color 0.16s ease, background 0.16s ease, border-color 0.16s ease;
}

.combo-control-option:hover,
.combo-control-option:focus-visible {
  color: var(--ink-text);
  outline: none;
}

.combo-control-option.is-classic:hover,
.combo-control-option.is-classic:focus-visible {
  background: rgba(139, 92, 246, 0.16);
  border-color: rgba(167, 139, 250, 0.48);
}

.combo-control-option.is-modern:hover,
.combo-control-option.is-modern:focus-visible {
  background: rgba(249, 115, 22, 0.16);
  border-color: rgba(251, 146, 60, 0.48);
}

.combo-control-option.is-world-tour:hover,
.combo-control-option.is-world-tour:focus-visible {
  color: var(--accent-cyan-soft);
  background: rgba(49, 213, 230, 0.1);
  border-color: rgba(49, 213, 230, 0.44);
}

.combo-control-option.active {
  color: var(--ink-text);
}

.combo-control-option.is-classic.active {
  background: rgba(139, 92, 246, 0.34);
  border-color: rgba(167, 139, 250, 0.82);
  box-shadow: inset 0 0 0 1px rgba(221, 214, 254, 0.14);
}

.combo-control-option.is-modern.active {
  background: rgba(249, 115, 22, 0.34);
  border-color: rgba(251, 146, 60, 0.82);
  box-shadow: inset 0 0 0 1px rgba(255, 237, 213, 0.14);
}

.combo-control-option.is-world-tour.active {
  background: rgba(49, 213, 230, 0.2);
  border-color: rgba(49, 213, 230, 0.72);
  box-shadow: inset 0 0 0 1px rgba(207, 250, 254, 0.1);
}

.combo-panel-head strong {
  color: var(--ink-text);
  font-size: 12px;
  padding: 8px 10px;
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  background: var(--ink-surface-high);
}

.combo-panel-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.combo-upload-link {
  min-height: 36px;
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

.combo-upload-link:hover,
.combo-upload-link:focus-visible {
  color: #190607;
  background: var(--accent-red);
  border-color: var(--accent-red);
}

.combo-upload-link .material-symbols-outlined {
  color: currentColor;
  font-size: 18px;
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
  color: var(--ink-text);
  background: var(--ink-surface);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  padding: 0 12px;
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

.combo-list {
  display: grid;
  gap: 12px;
}

.combo-empty {
  min-height: 180px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  color: var(--ink-muted);
  border: 1px dashed var(--ink-line-strong);
  border-radius: 4px;
}

.pager-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding-top: 4px;
  font-family: 'JetBrains Mono';
}

.pager-row button {
  min-height: 38px;
  padding: 0 14px;
  color: var(--ink-text);
  background: var(--ink-surface-high);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
}

.pager-row button:disabled {
  opacity: .45;
  cursor: not-allowed;
}

.pager-row span {
  color: var(--ink-muted);
  font-size: 12px;
}

section.relative.bg-surface-container-low {
  background: var(--ink-surface) !important;
  border: 1px solid var(--ink-line) !important;
  border-radius: 6px;
  clip-path: none;
}

section.relative.bg-surface-container-low h1 {
  color: var(--ink-text) !important;
  font-family: inherit;
  font-style: normal;
  letter-spacing: 0;
}

section.relative.bg-surface-container-low p {
  color: var(--ink-text-soft) !important;
}

section.relative.bg-surface-container-low img {
  border-radius: 4px;
}

.character-hero-layout {
  display: grid;
  grid-template-columns: auto minmax(220px, 0.7fr) minmax(520px, 1.3fr);
  align-items: start;
  gap: 32px;
}

.character-hero-copy {
  min-width: 0;
}

.character-data-panel {
  min-width: 0;
  padding: 18px;
  background:
    linear-gradient(135deg, rgba(255, 64, 84, 0.09), transparent 36%),
    rgba(8, 12, 18, 0.82);
  border: 1px solid var(--ink-line-strong);
  border-left: 3px solid var(--accent-red);
  border-radius: 4px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.character-data-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--ink-line);
}

.character-data-head > div > span {
  display: block;
  margin-bottom: 5px;
  color: var(--accent-red);
  font-family: 'JetBrains Mono';
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.character-data-head h2 {
  margin: 0;
  color: var(--ink-text);
  font-size: 22px;
  font-weight: 900;
  line-height: 1;
}

.character-data-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 0;
  border-top: 1px solid var(--ink-line);
  border-left: 1px solid var(--ink-line);
}

.character-data-stat {
  min-width: 0;
  min-height: 62px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 7px;
  padding: 9px 10px;
  border-right: 1px solid var(--ink-line);
  border-bottom: 1px solid var(--ink-line);
}

.character-data-stat dt {
  overflow: hidden;
  color: var(--ink-muted);
  font-size: 10px;
  font-weight: 700;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.character-data-stat dd {
  margin: 0;
  color: var(--ink-text);
  font-family: 'JetBrains Mono';
  font-size: 16px;
  font-weight: 900;
  line-height: 1;
}

@media (max-width: 1280px) {
  .character-hero-layout {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .character-data-panel {
    grid-column: 1 / -1;
  }
}

section.mb-12 {
  padding: 22px;
  background: var(--ink-surface);
  border: 1px solid var(--ink-line);
  border-radius: 6px;
}

section.mb-12 h2 {
  color: var(--ink-text) !important;
  font-family: inherit;
  font-style: normal;
}

.frame-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.frame-panel-head h2 {
  order: 1;
}

.frame-mode-switch {
  order: 2;
  display: inline-grid;
  grid-template-columns: repeat(2, minmax(54px, 1fr));
  gap: 2px;
  margin-left: auto;
  padding: 3px;
  background: var(--ink-surface-high);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
}

.frame-mode-option {
  min-height: 32px;
  padding: 0 10px;
  color: var(--ink-text-soft);
  background: transparent;
  border: 1px solid transparent;
  border-radius: 3px;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  font-weight: 700;
  outline: none;
  transition: color 0.16s ease, background 0.16s ease, border-color 0.16s ease;
}

.frame-mode-option.is-classic:hover,
.frame-mode-option.is-classic:focus-visible {
  color: var(--ink-text);
  background: rgba(139, 92, 246, 0.16);
  border-color: rgba(167, 139, 250, 0.48);
}

.frame-mode-option.is-modern:hover,
.frame-mode-option.is-modern:focus-visible {
  color: var(--ink-text);
  background: rgba(249, 115, 22, 0.16);
  border-color: rgba(251, 146, 60, 0.48);
}

.frame-mode-option.active {
  color: var(--ink-text);
}

.frame-mode-option.is-classic.active {
  background: rgba(139, 92, 246, 0.34);
  border-color: rgba(167, 139, 250, 0.82);
  box-shadow: inset 0 0 0 1px rgba(221, 214, 254, 0.14);
}

.frame-mode-option.is-modern.active {
  background: rgba(249, 115, 22, 0.34);
  border-color: rgba(251, 146, 60, 0.82);
  box-shadow: inset 0 0 0 1px rgba(255, 237, 213, 0.14);
}

.frame-panel-head.collapsed {
  margin-bottom: 0;
  border-bottom-color: transparent !important;
  padding-bottom: 0;
}

.frame-collapse-toggle {
  order: 3;
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  color: var(--ink-text-soft);
  background: var(--ink-surface-high);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  outline: none;
}

.frame-collapse-toggle:hover,
.frame-collapse-toggle:focus-visible {
  color: #190607;
  background: var(--accent-red);
  border-color: var(--accent-red);
}

.frame-collapse-toggle .material-symbols-outlined {
  font-size: 22px;
}

.frame-panel-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.frame-collapse-bottom {
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 14px;
  color: var(--ink-text-soft);
  background: var(--ink-surface-high);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  outline: none;
}

.frame-collapse-bottom:hover,
.frame-collapse-bottom:focus-visible {
  color: #190607;
  background: var(--accent-red);
  border-color: var(--accent-red);
}

.frame-collapse-bottom .material-symbols-outlined {
  font-size: 18px;
}

section.mb-12 table {
  color: var(--ink-text-soft);
}

.frame-table-wrap {
  width: 100%;
  overflow-x: visible;
}

.frame-table {
  table-layout: fixed;
  width: 100%;
  font-size: clamp(10px, 0.72vw, 12px);
}

.frame-table th,
.frame-table td,
.frame-value-cell {
  padding: 12px 8px;
  vertical-align: middle;
}

.frame-table th {
  white-space: nowrap;
}

.frame-table td {
  overflow-wrap: anywhere;
  word-break: normal;
}

section.mb-12 thead tr {
  color: var(--ink-muted);
  border-color: var(--ink-line);
}

section.mb-12 tbody tr {
  border-color: rgba(38, 50, 68, 0.72);
}

.frame-move-head,
.frame-move-cell {
  width: 152px;
}

.frame-move-cell :deep(.combo-notation),
.frame-move-cell :deep(.combo-move) {
  flex-wrap: nowrap;
}

.frame-move-cell :deep(.combo-move) {
  width: max-content;
  max-width: 100%;
  gap: 3px;
  padding: 3px 4px;
  overflow: hidden;
}

.frame-move-cell :deep(.combo-icon) {
  width: 18px;
  height: 18px;
}

.frame-move-cell :deep(.combo-text-token) {
  min-height: 18px;
  padding-inline: 3px;
  font-size: 10px;
}

.frame-table th:nth-child(2),
.frame-table th:nth-child(3),
.frame-table th:nth-child(4),
.frame-table th:nth-child(5),
.frame-table th:nth-child(6),
.frame-table td:nth-child(2),
.frame-table td:nth-child(3),
.frame-table td:nth-child(4),
.frame-table td:nth-child(5),
.frame-table td:nth-child(6) {
  width: 8%;
}

.frame-table th:nth-child(7),
.frame-table td:nth-child(7) {
  width: 9%;
}

.frame-table th:nth-child(8),
.frame-table td:nth-child(8) {
  width: 8%;
}

.frame-table th:nth-child(9),
.frame-table td:nth-child(9) {
  width: 7%;
}

.combo-empty span {
  font-size: 34px;
}

@media (max-width: 640px) {
  section.relative.bg-surface-container-low {
    padding: 18px;
    margin-bottom: 28px;
    border-width: 1px;
  }

  section.relative.bg-surface-container-low h1 {
    font-size: 34px;
    line-height: 1.05;
  }

  section.relative.bg-surface-container-low p {
    font-size: 14px;
    line-height: 1.55;
  }

  section.relative.bg-surface-container-low .w-40 {
    width: 132px;
    height: 132px;
  }

  section.relative.bg-surface-container-low .flex.flex-wrap {
    justify-content: center;
    gap: 8px;
  }

  .character-hero-layout {
    grid-template-columns: 1fr;
    justify-items: center;
    gap: 22px;
  }

  .character-data-panel {
    width: 100%;
    grid-column: auto;
    padding: 14px;
  }

  .character-data-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 9px;
  }

  .character-data-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  section.grid h2,
  section.mb-12 h2,
  section:not(.grid) h2 {
    font-size: 22px;
  }

  .frame-table-wrap {
    overflow-x: auto;
    margin-inline: -20px;
    padding-inline: 20px;
  }

  .frame-table {
    min-width: 900px;
    font-size: 12px;
  }

  .frame-table th,
  .frame-table td,
  .frame-value-cell {
    padding: 12px 10px;
  }

  .frame-move-head,
  .frame-move-cell {
    width: 136px;
  }

  .frame-move-cell :deep(.combo-icon) {
    width: var(--notation-icon-size);
    height: var(--notation-icon-size);
  }

  .combo-panel-head {
    align-items: flex-start;
    flex-direction: column;
    grid-template-columns: 1fr;
  }

  .combo-panel-head h2 {
    font-size: 24px;
  }

  .combo-control-switch {
    width: 100%;
    min-width: 0;
    justify-self: stretch;
  }

  .combo-control-option {
    min-width: 0;
  }

  .combo-panel-actions {
    width: 100%;
    justify-content: space-between;
  }

  .combo-upload-link {
    flex: 1 1 auto;
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

  .filter-sort-field .filter-select {
    width: 100%;
  }

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
  .filter-toggle,
  .filter-toggle-chevron,
  .filter-choice,
  .filter-reveal-enter-active,
  .filter-reveal-leave-active,
  .filter-reveal-enter-active .combo-filter-panel {
    transition: none;
  }
}
</style>
