<template>
  <div class="tier-list-page">
    <header class="tier-hero">
      <div class="tier-title-block">
        <span class="tier-kicker">RANKING LAB</span>
        <h1>排名</h1>
        <p>拖动角色完成你的私人榜单。等级名称、颜色和顺序都可以自由调整。</p>
      </div>
      <div class="tier-progress" aria-live="polite">
        <strong>{{ rankedCount }}</strong>
        <span>/ {{ store.characters.length }} 已排名</span>
      </div>
    </header>

    <section class="ranking-toolbar" aria-label="排名工具">
      <div class="toolbar-actions">
        <button type="button" class="tool-button primary" :disabled="tiers.length >= maxTierCount" @click="addTier">
          <span class="material-symbols-outlined">add</span>
          新增等级
        </button>
        <button type="button" class="tool-button" :disabled="rankedCount === 0" @click="clearRanking">
          <span class="material-symbols-outlined">ink_eraser</span>
          清空排名
        </button>
        <button type="button" class="tool-button" @click="resetBoard">
          <span class="material-symbols-outlined">restart_alt</span>
          恢复默认
        </button>
      </div>
    </section>

    <transition name="quick-move">
      <section v-if="selectedCharacter" class="quick-move-bar" aria-label="快速移动角色">
        <div class="quick-move-fighter">
          <span class="quick-avatar" :style="portraitStyle(selectedCharacter)"></span>
          <span>移动 <strong>{{ selectedCharacter.name }}</strong> 到</span>
        </div>
        <div class="quick-targets">
          <button
            v-for="tier in tiers"
            :key="tier.id"
            type="button"
            :style="{ '--tier-color': tier.color }"
            @click="moveCharacter(selectedCharacter.id, tier.id)"
          >
            {{ tier.label || '未命名等级' }}
          </button>
          <button type="button" class="unrank-target" @click="moveCharacter(selectedCharacter.id, unrankedZoneId)">
            移回角色池
          </button>
          <button type="button" class="cancel-target" aria-label="取消选择" @click="selectedId = null">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
      </section>
    </transition>

    <section v-if="draggedCharacter" class="drag-dock" aria-label="拖拽目标等级">
      <button
        v-for="tier in tiers"
        :key="tier.id"
        type="button"
        :style="{ '--tier-color': tier.color }"
        @dragenter.prevent="dragTargetId = tier.id"
        @dragover.prevent
        @drop.prevent="dropCharacter(tier.id)"
      >
        {{ tier.label }}
      </button>
      <button
        type="button"
        class="drag-dock-unranked"
        @dragenter.prevent="dragTargetId = unrankedZoneId"
        @dragover.prevent
        @drop.prevent="dropCharacter(unrankedZoneId)"
      >
        角色池
      </button>
    </section>

    <div v-if="store.loading && !store.characters.length" class="page-state">
      <span class="state-loader" aria-hidden="true"></span>
      正在读取角色名单
    </div>
    <div v-else-if="store.error && !store.characters.length" class="page-state error">
      <span class="material-symbols-outlined">error</span>
      <span>{{ store.error }}</span>
      <button type="button" @click="loadCharacters">重新加载</button>
    </div>

    <template v-else>
      <section class="tier-board" aria-label="角色排名表">
        <article
          v-for="(tier, tierIndex) in tiers"
          :key="tier.id"
          :class="['tier-row', { 'is-drop-target': dragTargetId === tier.id }]"
          :style="{ '--tier-color': tier.color }"
          @dragenter.prevent="dragTargetId = tier.id"
          @dragover.prevent
          @dragleave="leaveDropTarget($event, tier.id)"
          @drop.prevent="dropCharacter(tier.id)"
        >
          <div class="tier-label-panel">
            <label class="tier-color-control" title="修改等级颜色">
              <span class="sr-only">修改 {{ tier.label }} 的颜色</span>
              <input v-model="tier.color" type="color" />
            </label>
            <input
              v-model.trim="tier.label"
              class="tier-name-input"
              type="text"
              maxlength="12"
              :aria-label="`第 ${tierIndex + 1} 个等级名称`"
              @blur="ensureTierLabel(tier, tierIndex)"
            />
            <div class="tier-order-controls">
              <button type="button" :disabled="tierIndex === 0" :aria-label="`上移 ${tier.label}`" @click="moveTier(tierIndex, -1)">
                <span class="material-symbols-outlined">keyboard_arrow_up</span>
              </button>
              <button type="button" :disabled="tierIndex === tiers.length - 1" :aria-label="`下移 ${tier.label}`" @click="moveTier(tierIndex, 1)">
                <span class="material-symbols-outlined">keyboard_arrow_down</span>
              </button>
              <button type="button" :disabled="tiers.length === 1" :aria-label="`删除 ${tier.label}`" @click="removeTier(tier.id)">
                <span class="material-symbols-outlined">delete</span>
              </button>
            </div>
          </div>

          <div class="tier-drop-zone">
            <button
              v-for="character in charactersForTier(tier)"
              :key="character.id"
              type="button"
              draggable="true"
              :class="['rank-card', { selected: isSelected(character.id) }]"
              :aria-pressed="isSelected(character.id)"
              :aria-label="`${character.name}，当前位于 ${tier.label}，点按后可移动`"
              @click="toggleSelected(character.id)"
              @dragstart="startDragging($event, character.id)"
              @dragend="endDragging"
            >
              <span class="rank-card-image" :style="portraitStyle(character)">
                <span class="rank-card-fallback">{{ characterInitials(character) }}</span>
              </span>
              <strong>{{ character.name }}</strong>
              <span v-if="isSelected(character.id)" class="selected-mark material-symbols-outlined">check</span>
            </button>
          </div>
        </article>
      </section>

      <section
        :class="['fighter-pool', { 'is-drop-target': dragTargetId === unrankedZoneId }]"
        @dragenter.prevent="dragTargetId = unrankedZoneId"
        @dragover.prevent
        @dragleave="leaveDropTarget($event, unrankedZoneId)"
        @drop.prevent="dropCharacter(unrankedZoneId)"
      >
        <div v-if="unrankedCharacters.length" class="pool-grid">
          <button
            v-for="character in unrankedCharacters"
            :key="character.id"
            type="button"
            draggable="true"
            :class="['rank-card', { selected: isSelected(character.id) }]"
            :aria-pressed="isSelected(character.id)"
            :aria-label="`${character.name}，尚未排名，点按后可选择等级`"
            @click="toggleSelected(character.id)"
            @dragstart="startDragging($event, character.id)"
            @dragend="endDragging"
          >
            <span class="rank-card-image" :style="portraitStyle(character)">
              <span class="rank-card-fallback">{{ characterInitials(character) }}</span>
            </span>
            <strong>{{ character.name }}</strong>
            <span v-if="isSelected(character.id)" class="selected-mark material-symbols-outlined">check</span>
          </button>
        </div>
      </section>
    </template>

    <p v-if="feedback" class="save-feedback" role="status">{{ feedback }}</p>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useCharacterStore } from '@/js/stores/characters'
import { useRealtimeStore } from '@/js/stores/realtime'

const storageKey = 'combos-hub-tier-list-v2'
const unrankedZoneId = '__unranked__'
const maxTierCount = 10
const tierPalette = ['#ff6381', '#ff8a5b', '#f5c451', '#83d97c', '#45c7d8', '#7d8cff', '#bd7cff']
const defaultTierBlueprint = [
  { label: 'S', color: '#ff6381' },
  { label: 'A', color: '#ff8a5b' },
  { label: 'B', color: '#f5c451' },
  { label: 'C', color: '#83d97c' },
  { label: 'D', color: '#55b8dc' },
]

const store = useCharacterStore()
const realtime = useRealtimeStore()
const tiers = ref([])
const selectedId = ref(null)
const draggedId = ref(null)
const dragTargetId = ref(null)
const hydrated = ref(false)
const feedback = ref('')
let feedbackTimer = null

const allCharacterIds = computed(() => store.characters.map(character => String(character.id)))
const rankedIds = computed(() => new Set(tiers.value.flatMap(tier => tier.characterIds)))
const rankedCount = computed(() => rankedIds.value.size)
const unrankedCharacters = computed(() => store.characters.filter(character => !rankedIds.value.has(String(character.id))))
const selectedCharacter = computed(() => store.characters.find(character => String(character.id) === selectedId.value) || null)
const draggedCharacter = computed(() => store.characters.find(character => String(character.id) === draggedId.value) || null)

watch(tiers, persistBoard, { deep: true })

watch(() => realtime.lastEvent, async event => {
  if (!(event?.areas || []).includes('characters')) return
  await store.loadCharacters()
  sanitizeAssignments()
})

onMounted(loadCharacters)

onBeforeUnmount(() => {
  clearTimeout(feedbackTimer)
})

async function loadCharacters() {
  if (!store.hasLoadedList) await store.loadCharacters()
  hydrateBoard()
}

function createTier(label, color, characterIds = []) {
  return {
    id: globalThis.crypto?.randomUUID?.() || `tier-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    label,
    color,
    characterIds: characterIds.map(String),
  }
}

function createDefaultTiers() {
  return defaultTierBlueprint.map(tier => createTier(tier.label, tier.color))
}

function hydrateBoard() {
  if (hydrated.value) {
    sanitizeAssignments()
    return
  }

  let savedTiers = null
  try {
    const saved = JSON.parse(localStorage.getItem(storageKey) || 'null')
    if (Array.isArray(saved?.tiers) && saved.tiers.length) savedTiers = saved.tiers
  } catch (_) {
    savedTiers = null
  }

  tiers.value = savedTiers
    ? savedTiers.slice(0, maxTierCount).map((tier, index) => ({
        id: String(tier.id || `saved-tier-${index}`),
        label: String(tier.label || `等级 ${index + 1}`).slice(0, 12),
        color: isHexColor(tier.color) ? tier.color : tierPalette[index % tierPalette.length],
        characterIds: Array.isArray(tier.characterIds) ? tier.characterIds.map(String) : [],
      }))
    : createDefaultTiers()

  hydrated.value = true
  sanitizeAssignments()
}

function sanitizeAssignments() {
  if (!hydrated.value || !store.characters.length) return
  const available = new Set(allCharacterIds.value)
  const seen = new Set()
  tiers.value.forEach(tier => {
    tier.characterIds = tier.characterIds.filter(id => {
      const normalizedId = String(id)
      if (!available.has(normalizedId) || seen.has(normalizedId)) return false
      seen.add(normalizedId)
      return true
    })
  })
  if (selectedId.value && !available.has(selectedId.value)) selectedId.value = null
}

function persistBoard() {
  if (!hydrated.value) return
  localStorage.setItem(storageKey, JSON.stringify({ tiers: tiers.value }))
}

function isHexColor(value) {
  return /^#[0-9a-f]{6}$/i.test(String(value || ''))
}

function charactersForTier(tier) {
  const characterById = new Map(store.characters.map(character => [String(character.id), character]))
  return tier.characterIds.map(id => characterById.get(String(id))).filter(Boolean)
}

function addTier() {
  if (tiers.value.length >= maxTierCount) return
  const nextIndex = tiers.value.length
  tiers.value.push(createTier(`新等级 ${nextIndex + 1}`, tierPalette[nextIndex % tierPalette.length]))
  announce('已新增等级，可直接修改名称和颜色')
}

function removeTier(tierId) {
  if (tiers.value.length === 1) return
  const index = tiers.value.findIndex(tier => tier.id === tierId)
  if (index < 0) return
  const [removed] = tiers.value.splice(index, 1)
  announce(`${removed.label} 已删除，其中的角色已移回角色池`)
}

function moveTier(index, direction) {
  const nextIndex = index + direction
  if (nextIndex < 0 || nextIndex >= tiers.value.length) return
  const [tier] = tiers.value.splice(index, 1)
  tiers.value.splice(nextIndex, 0, tier)
}

function ensureTierLabel(tier, index) {
  if (!tier.label) tier.label = `等级 ${index + 1}`
}

function clearRanking() {
  tiers.value.forEach(tier => { tier.characterIds = [] })
  selectedId.value = null
  announce('排名已清空，角色全部回到角色池')
}

function resetBoard() {
  tiers.value = createDefaultTiers()
  selectedId.value = null
  announce('已恢复默认等级，排名同时清空')
}

function moveCharacter(characterId, targetTierId) {
  const normalizedId = String(characterId)
  tiers.value.forEach(tier => {
    tier.characterIds = tier.characterIds.filter(id => id !== normalizedId)
  })
  if (targetTierId !== unrankedZoneId) {
    const target = tiers.value.find(tier => tier.id === targetTierId)
    if (target) target.characterIds.push(normalizedId)
  }
  selectedId.value = null
  draggedId.value = null
  dragTargetId.value = null
}

function toggleSelected(characterId) {
  const normalizedId = String(characterId)
  selectedId.value = selectedId.value === normalizedId ? null : normalizedId
}

function isSelected(characterId) {
  return selectedId.value === String(characterId)
}

function startDragging(event, characterId) {
  draggedId.value = String(characterId)
  selectedId.value = null
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', draggedId.value)
}

function endDragging() {
  draggedId.value = null
  dragTargetId.value = null
}

function dropCharacter(targetTierId) {
  if (!draggedId.value) return
  moveCharacter(draggedId.value, targetTierId)
}

function leaveDropTarget(event, targetId) {
  if (event.currentTarget.contains(event.relatedTarget)) return
  if (dragTargetId.value === targetId) dragTargetId.value = null
}

function portraitStyle(character) {
  if (!character?.image) return {}
  return { backgroundImage: `url(${JSON.stringify(character.image)})` }
}

function characterInitials(character) {
  return Array.from(character?.name || '?').slice(0, 2).join('')
}

function announce(message) {
  feedback.value = message
  clearTimeout(feedbackTimer)
  feedbackTimer = setTimeout(() => { feedback.value = '' }, 2600)
}
</script>

<style scoped>
.tier-list-page {
  --board-edge: rgba(101, 67, 63, 0.86);
  display: grid;
  gap: 18px;
  padding-bottom: 28px;
}

.tier-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 0 0 18px 58px;
  border-bottom: 1px solid var(--board-edge);
}

.tier-title-block {
  min-width: 0;
}

.tier-kicker,
.tier-progress {
  font-family: 'JetBrains Mono', monospace;
}

.tier-kicker {
  color: var(--accent-cyan-soft);
  font-size: 12px;
  font-weight: 800;
}

.tier-hero h1 {
  margin: 8px 0 0;
  color: var(--ink-text);
  font-size: 40px;
  font-weight: 900;
  line-height: 1.08;
  text-wrap: balance;
}

.tier-hero p {
  max-width: 62ch;
  margin: 10px 0 0;
  color: var(--ink-text-soft);
  font-size: 15px;
  line-height: 1.55;
  text-wrap: pretty;
}

.tier-progress {
  flex: 0 0 auto;
  display: flex;
  align-items: baseline;
  gap: 6px;
  padding: 10px 12px;
  color: var(--ink-muted);
  background: rgba(12, 9, 10, 0.94);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-size: 12px;
}

.tier-progress strong {
  color: var(--accent-cyan-soft);
  font-size: 22px;
  line-height: 1;
}

.ranking-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px;
  background: rgba(12, 9, 10, 0.92);
  border: 1px solid var(--ink-line);
  border-radius: 6px;
}

.toolbar-actions,
.quick-move-fighter,
.quick-targets {
  display: flex;
  align-items: center;
}

.toolbar-actions {
  flex-wrap: wrap;
  gap: 8px;
}

.tool-button {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 12px;
  color: var(--ink-text-soft);
  background: #171211;
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-size: 13px;
  font-weight: 800;
  transition: color 180ms ease, background-color 180ms ease, border-color 180ms ease, transform 180ms ease;
}

.tool-button.primary {
  color: #190607;
  background: var(--accent-red);
  border-color: var(--accent-red);
}

.tool-button:hover:not(:disabled),
.tool-button:focus-visible:not(:disabled) {
  color: var(--ink-text);
  background: #241817;
  border-color: var(--accent-red-hot);
  transform: translateY(-1px);
}

.tool-button.primary:hover:not(:disabled),
.tool-button.primary:focus-visible:not(:disabled) {
  color: #190607;
  background: var(--accent-red-hot);
}

.tool-button:focus-visible,
.tier-order-controls button:focus-visible,
.quick-targets button:focus-visible,
.rank-card:focus-visible,
.page-state button:focus-visible {
  outline: 2px solid var(--accent-cyan);
  outline-offset: 2px;
}

.tool-button:disabled,
.tier-order-controls button:disabled {
  cursor: not-allowed;
  opacity: 0.38;
}

.tool-button .material-symbols-outlined {
  font-size: 18px;
}

.quick-move-bar {
  position: sticky;
  top: 14px;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 10px 12px;
  color: var(--ink-text-soft);
  background: #17110f;
  border: 1px solid var(--accent-cyan);
  border-radius: 6px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.34);
}

.quick-move-fighter {
  flex: 0 0 auto;
  gap: 9px;
  font-size: 13px;
}

.quick-avatar {
  width: 34px;
  height: 34px;
  background-color: var(--ink-surface-high);
  background-position: center top;
  background-size: cover;
  border: 1px solid var(--accent-cyan);
  border-radius: 50%;
}

.quick-targets {
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 6px;
}

.quick-targets button {
  min-height: 32px;
  padding: 0 10px;
  color: var(--ink-text);
  background: color-mix(in srgb, var(--tier-color) 20%, #15100f);
  border: 1px solid color-mix(in srgb, var(--tier-color) 72%, #15100f);
  border-radius: 4px;
  font-size: 12px;
  font-weight: 900;
  transition: background-color 180ms ease, transform 180ms ease;
}

.quick-targets button:hover {
  background: color-mix(in srgb, var(--tier-color) 36%, #15100f);
  transform: translateY(-1px);
}

.quick-targets .unrank-target {
  color: var(--ink-text-soft);
  background: #0c090a;
  border-color: var(--ink-line-strong);
}

.quick-targets .cancel-target {
  width: 32px;
  padding: 0;
  color: var(--ink-muted);
  background: transparent;
  border-color: transparent;
}

.quick-move-enter-active,
.quick-move-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}

.quick-move-enter-from,
.quick-move-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.drag-dock {
  position: fixed;
  top: 12px;
  left: 50%;
  z-index: 120;
  width: min(760px, calc(100vw - 100px));
  min-height: 54px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 7px;
  padding: 8px 10px;
  color: var(--ink-text-soft);
  background: #100c0c;
  border: 1px solid var(--accent-cyan);
  border-radius: 6px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.48);
  transform: translateX(-50%);
}

.drag-dock button {
  min-width: 48px;
  min-height: 34px;
  padding: 0 11px;
  color: #140b0c;
  background: var(--tier-color);
  border: 1px solid color-mix(in srgb, var(--tier-color) 76%, #ffffff);
  border-radius: 3px;
  font-weight: 950;
}

.drag-dock .drag-dock-unranked {
  color: var(--ink-text-soft);
  background: #211918;
  border-color: var(--ink-line-strong);
}

.tier-board {
  background: #090708;
  border: 1px solid var(--board-edge);
  border-radius: 6px;
}

.tier-row {
  --tier-color: #ff6381;
  min-height: 104px;
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  background: rgba(11, 9, 9, 0.96);
  transition: background-color 180ms ease, box-shadow 180ms ease;
}

.tier-row + .tier-row {
  border-top: 1px solid var(--board-edge);
}

.tier-row.is-drop-target {
  background: color-mix(in srgb, var(--tier-color) 12%, #0b0909);
  box-shadow: inset 0 0 0 2px var(--tier-color);
}

.tier-label-panel {
  position: relative;
  display: grid;
  align-content: center;
  gap: 9px;
  padding: 16px 12px;
  overflow: hidden;
  background: var(--tier-color);
  color: #140b0c;
}

.tier-label-panel::after {
  content: '';
  position: absolute;
  right: -24px;
  bottom: -44px;
  width: 90px;
  height: 90px;
  background: rgba(255, 255, 255, 0.16);
  transform: rotate(24deg);
  pointer-events: none;
}

.tier-color-control {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 2;
  width: 22px;
  height: 22px;
  overflow: hidden;
  background: rgba(20, 11, 12, 0.16);
  border: 1px solid rgba(20, 11, 12, 0.46);
  border-radius: 50%;
  cursor: pointer;
  opacity: 0;
  pointer-events: none;
  transform: translateY(-3px);
  transition: opacity 160ms ease, transform 160ms ease;
}

.tier-color-control input {
  width: 34px;
  height: 34px;
  margin: -6px;
  cursor: pointer;
  opacity: 0;
}

.tier-name-input {
  position: relative;
  z-index: 1;
  width: calc(100% - 18px);
  min-width: 0;
  padding: 3px 2px;
  color: #140b0c;
  background: transparent;
  border: 0;
  border-bottom: 1px solid transparent;
  border-radius: 0;
  font-size: 18px;
  font-weight: 950;
  line-height: 1.15;
  text-align: center;
  outline: none;
}

.tier-name-input:hover,
.tier-name-input:focus {
  border-bottom-color: rgba(20, 11, 12, 0.58);
}

.tier-order-controls {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
  gap: 4px;
  opacity: 0;
  pointer-events: none;
  transform: translateY(3px);
  transition: opacity 160ms ease, transform 160ms ease;
}

.tier-label-panel:hover .tier-color-control,
.tier-label-panel:focus-within .tier-color-control,
.tier-label-panel:hover .tier-order-controls,
.tier-label-panel:focus-within .tier-order-controls {
  opacity: 1;
  pointer-events: auto;
  transform: translateY(0);
}

.tier-order-controls button {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  color: #140b0c;
  background: rgba(255, 255, 255, 0.22);
  border: 1px solid rgba(20, 11, 12, 0.26);
  border-radius: 3px;
  transition: background-color 160ms ease, transform 160ms ease;
}

.tier-order-controls button:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.48);
  transform: translateY(-1px);
}

.tier-order-controls .material-symbols-outlined {
  font-size: 17px;
}

.tier-drop-zone {
  position: relative;
  min-width: 0;
  min-height: 104px;
  display: flex;
  align-items: center;
  align-content: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px;
}

.rank-card {
  position: relative;
  width: 72px;
  flex: 0 0 72px;
  display: grid;
  grid-template-rows: 70px auto;
  overflow: hidden;
  padding: 0;
  color: var(--ink-text);
  text-align: left;
  background: #15100f;
  border: 1px solid #4c3936;
  border-radius: 4px;
  cursor: grab;
  user-select: none;
  transition: border-color 160ms ease, transform 160ms ease, box-shadow 160ms ease;
}

.rank-card:hover,
.rank-card:focus-visible,
.rank-card.selected {
  z-index: 2;
  border-color: var(--accent-cyan);
  transform: translateY(-3px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.4);
}

.rank-card.selected {
  box-shadow: 0 0 0 2px rgba(49, 213, 230, 0.28);
}

.rank-card:active {
  cursor: grabbing;
}

.rank-card-image {
  position: relative;
  display: grid;
  place-items: center;
  overflow: hidden;
  background-color: #1b1414;
  background-position: center top;
  background-size: cover;
  filter: saturate(0.78) contrast(1.06);
}

.rank-card-image::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 58%, rgba(7, 5, 6, 0.74));
  pointer-events: none;
}

.rank-card-fallback {
  color: var(--ink-muted);
  font-size: 18px;
  font-weight: 900;
}

.rank-card strong {
  min-width: 0;
  overflow: hidden;
  padding: 7px 6px 8px;
  font-size: 11px;
  font-weight: 900;
  line-height: 1.15;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-mark {
  position: absolute;
  top: 5px;
  right: 5px;
  z-index: 2;
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  color: #051012;
  background: var(--accent-cyan);
  border-radius: 50%;
  font-size: 16px;
}

.fighter-pool {
  padding: 12px;
  background: rgba(12, 9, 10, 0.94);
  border: 1px solid var(--ink-line);
  border-radius: 6px;
  transition: background-color 180ms ease, border-color 180ms ease;
}

.fighter-pool.is-drop-target {
  background: rgba(49, 213, 230, 0.09);
  border-color: var(--accent-cyan);
}

.pool-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  padding: 2px;
}

.page-state {
  min-height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--ink-muted);
  background: rgba(12, 9, 10, 0.88);
  border: 1px solid var(--ink-line);
  border-radius: 6px;
  font-size: 14px;
}

.page-state.error {
  flex-wrap: wrap;
  color: var(--state-error);
}

.page-state button {
  padding: 7px 10px;
  color: var(--ink-text);
  background: var(--ink-surface-high);
  border: 1px solid var(--ink-line-strong);
  border-radius: 4px;
}

.state-loader {
  width: 18px;
  height: 18px;
  border: 2px solid var(--ink-line-strong);
  border-top-color: var(--accent-cyan);
  border-radius: 50%;
  animation: tier-loader 700ms linear infinite;
}

@keyframes tier-loader {
  to { transform: rotate(360deg); }
}

.save-feedback {
  position: fixed;
  right: 18px;
  bottom: 18px;
  z-index: 80;
  max-width: min(360px, calc(100vw - 36px));
  margin: 0;
  padding: 11px 14px;
  color: #07110c;
  background: var(--state-success);
  border-radius: 4px;
  font-size: 13px;
  font-weight: 850;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.4);
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

@media (max-width: 760px) {
  .tier-list-page {
    gap: 14px;
  }

  .tier-hero {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
    padding-left: 46px;
    padding-bottom: 14px;
  }

  .tier-hero h1 {
    font-size: 30px;
  }

  .tier-hero p {
    font-size: 14px;
  }

  .ranking-toolbar,
  .quick-move-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .quick-move-bar {
    top: 8px;
  }

  .drag-dock {
    top: 8px;
    width: calc(100vw - 24px);
  }

  .quick-targets {
    justify-content: flex-start;
  }

  .tier-row {
    min-height: 96px;
    grid-template-columns: 108px minmax(0, 1fr);
  }

  .tier-label-panel {
    padding: 13px 7px;
  }

  .tier-name-input {
    width: calc(100% - 10px);
    font-size: 15px;
  }

  .tier-order-controls {
    gap: 3px;
  }

  .tier-order-controls button {
    width: 25px;
    height: 25px;
  }

  .tier-drop-zone {
    min-height: 96px;
    gap: 6px;
    padding: 7px;
  }

  .rank-card {
    width: 66px;
    flex-basis: 66px;
    grid-template-rows: 64px auto;
  }

  .fighter-pool {
    padding: 12px;
  }

  .pool-grid {
    gap: 7px;
  }
}

@media (max-width: 430px) {
  .toolbar-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .tool-button {
    width: 100%;
    padding-inline: 8px;
  }

  .tool-button.primary {
    grid-column: 1 / -1;
  }

  .tier-row {
    grid-template-columns: 88px minmax(0, 1fr);
  }

  .tier-name-input {
    width: 100%;
    padding-right: 10px;
    font-size: 13px;
  }

  .tier-color-control {
    width: 18px;
    height: 18px;
  }

  .tier-order-controls {
    flex-wrap: wrap;
  }

  .tier-order-controls button {
    width: 24px;
    height: 24px;
  }

  .rank-card {
    width: 62px;
    flex-basis: 62px;
    grid-template-rows: 60px auto;
  }

  .rank-card strong {
    padding-inline: 4px;
    font-size: 10px;
  }

}

@media (hover: none) {
  .tier-color-control,
  .tier-order-controls {
    opacity: 1;
    pointer-events: auto;
    transform: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .state-loader {
    animation: none;
  }

  .quick-move-enter-active,
  .quick-move-leave-active {
    transition: none;
  }
}
</style>
