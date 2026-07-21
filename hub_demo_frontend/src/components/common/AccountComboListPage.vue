<template>
  <div class="account-combo-page">
    <header class="account-combo-head">
      <router-link to="/" class="back-link">
        <span class="material-symbols-outlined">arrow_back</span>
        返回角色
      </router-link>
      <span>{{ kicker }}</span>
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
    </header>

    <section v-if="checkingSession" class="auth-required-panel">
      <span class="material-symbols-outlined">sync</span>
      <h2>正在确认登录状态</h2>
      <p>马上就好。</p>
    </section>

    <section v-else-if="!auth.isLoggedIn" class="auth-required-panel">
      <span class="material-symbols-outlined">lock</span>
      <h2>登录后查看</h2>
      <p>这里会读取你的账号数据，请先登录。</p>
      <router-link to="/login">前往登录</router-link>
    </section>

    <section v-else class="account-combo-panel">
      <div class="panel-head">
        <div>
          <strong>{{ panelTitle }}</strong>
          <small>{{ totalText }}</small>
        </div>
        <button type="button" :disabled="loading" @click="loadCombos(page)">
          <span class="material-symbols-outlined">refresh</span>
          刷新
        </button>
      </div>

      <div v-if="loading" class="state-panel">
        <span class="material-symbols-outlined">progress_activity</span>
        <p>{{ loadingText }}</p>
      </div>

      <div v-else-if="error" class="state-panel error">
        <span class="material-symbols-outlined">error</span>
        <p>{{ error }}</p>
        <button type="button" @click="loadCombos(page)">重试</button>
      </div>

      <div v-else-if="combos.length" class="combo-grid">
        <router-link v-for="combo in combos" :key="combo.id" class="account-combo-card" :to="`/combos/${combo.id}`">
          <div class="combo-card-head">
            <div>
              <h2>{{ combo.route }}</h2>
              <ComboNotation :route="combo.route" size="sm" />
              <p v-if="comboMeta(combo)" class="combo-meta">
                <span class="material-symbols-outlined">person</span>
                {{ comboMeta(combo) }}
              </p>
            </div>
            <div class="type-pill-list">
              <span
                v-for="tag in comboTags(combo)"
                :key="tag"
                :class="['type-pill', comboTagColor(tag)]"
              >{{ comboTagLabel(tag) }}</span>
            </div>
          </div>

          <div class="combo-metrics">
            <span>{{ combo.damage || 0 }} DMG</span>
            <span>{{ combo.driveCost || 0 }} Drive</span>
            <span>{{ combo.difficulty || '未标难度' }}</span>
            <span v-if="combo.cornerOnly">版边</span>
          </div>

          <div class="combo-social-summary">
            <span class="combo-social-count like">
              <span class="material-symbols-outlined">thumb_up</span>
              <strong>{{ combo.likes || 0 }}</strong>
            </span>
            <span class="combo-social-count favorite">
              <span class="material-symbols-outlined">bookmark</span>
              <strong>{{ combo.favorites || 0 }}</strong>
            </span>
          </div>

          <div class="combo-actions" @click.stop @keydown.stop>
            <button
              v-if="kind === 'favorites'"
              type="button"
              class="favorite-action active"
              :disabled="favoriteBusy === combo.id"
              @click.prevent.stop="toggleFavorite(combo)"
            >
              <span class="material-symbols-outlined">bookmark</span>
              {{ favoriteBusy === combo.id ? '处理中...' : '取消收藏' }}
            </button>
            <template v-else>
              <span v-if="combo.status === 'rejected' && combo.rejectionReason" class="rejection-reason">
                <span class="material-symbols-outlined">error</span>
                {{ combo.rejectionReason }}
              </span>
              <div v-if="canManageRejectedCombo(combo)" class="rejected-action-row">
                <button type="button" class="combo-manage-action" @click.prevent.stop="openEditCombo(combo)">
                  <span class="material-symbols-outlined">edit</span>
                  修改重传
                </button>
                <button type="button" class="combo-manage-action danger" :disabled="deleteBusy === combo.id" @click.prevent.stop="deleteRejectedCombo(combo)">
                  <span class="material-symbols-outlined">delete</span>
                  {{ deleteBusy === combo.id ? '删除中...' : '删除' }}
                </button>
              </div>
              <span :class="['status-pill', statusClass(combo.status)]">{{ statusLabel(combo.status) }}</span>
            </template>
          </div>
        </router-link>
      </div>

      <div v-else class="state-panel empty">
        <span class="material-symbols-outlined">{{ emptyIcon }}</span>
        <h2>{{ emptyTitle }}</h2>
        <p>{{ emptyText }}</p>
        <router-link :to="emptyActionTo">{{ emptyActionText }}</router-link>
      </div>

      <div v-if="total > pageSize" class="pager-row">
        <button type="button" :disabled="page <= 1 || loading" @click="changePage(page - 1)">
          <span class="material-symbols-outlined">chevron_left</span>
          上一页
        </button>
        <span>{{ page }} / {{ pageCount }}</span>
        <button type="button" :disabled="page >= pageCount || loading" @click="changePage(page + 1)">
          下一页
          <span class="material-symbols-outlined">chevron_right</span>
        </button>
      </div>
    </section>

    <Teleport to="body">
      <transition name="combo-edit">
        <div v-if="editingCombo" class="combo-edit-backdrop" @click.self="closeEditCombo">
          <ComboUploadForm
            v-model="editModalOpen"
            :combo="editingCombo"
            @saved="handleComboResubmitted"
          />
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/js/stores/auth'
import { useComboStore } from '@/js/stores/combos'
import { useRealtimeStore } from '@/js/stores/realtime'
import { useUiStore } from '@/js/stores/ui'
import { api } from '@/js/api'
import ComboUploadForm from '@/components/common/ComboUploadForm.vue'
import ComboNotation from '@/components/common/ComboNotation.vue'
import { comboTagColor, comboTagLabel, normalizeComboTags } from '@/js/utils/helpers'

const props = defineProps({
  kind: { type: String, required: true },
  kicker: { type: String, required: true },
  title: { type: String, required: true },
  description: { type: String, required: true },
  panelTitle: { type: String, required: true },
  loadingText: { type: String, required: true },
  emptyIcon: { type: String, required: true },
  emptyTitle: { type: String, required: true },
  emptyText: { type: String, required: true },
  emptyActionText: { type: String, required: true },
  emptyActionTo: { type: String, required: true },
})

const router = useRouter()
const auth = useAuthStore()
const comboStore = useComboStore()
const realtime = useRealtimeStore()
const ui = useUiStore()
const combos = ref([])
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const error = ref('')
const checkingSession = ref(false)
const favoriteBusy = ref(null)
const deleteBusy = ref(null)
const editingCombo = ref(null)
const editModalOpen = ref(false)
const pageSize = 6

const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const totalText = computed(() => `共 ${total.value} 条`)

onMounted(async () => {
  if (!(await ensureLoggedIn())) return
  loadCombos(1)
})

watch(() => auth.isLoggedIn, loggedIn => {
  if (!loggedIn) {
    combos.value = []
    total.value = 0
    router.push('/login').catch(() => {})
    return
  }
  loadCombos(1)
})

watch(() => realtime.lastEvent, event => {
  if (!auth.isLoggedIn) return
  const areas = event?.areas || []
  const shouldReload = props.kind === 'favorites'
    ? areas.includes('favoriteCombos') || areas.includes('combos')
    : areas.includes('userCombos') || areas.includes('combos')
  if (shouldReload) loadCombos(page.value)
})

watch(editModalOpen, value => {
  if (!value) editingCombo.value = null
})

async function ensureLoggedIn() {
  if (auth.isLoggedIn) return true
  checkingSession.value = true
  try {
    await auth.hydrate()
  } finally {
    checkingSession.value = false
  }
  if (auth.isLoggedIn) return true
  router.push('/login').catch(() => {})
  return false
}

async function loadCombos(nextPage = page.value) {
  if (!auth.isLoggedIn || !auth.user?.id || loading.value) return
  loading.value = true
  error.value = ''
  try {
    const loader = props.kind === 'favorites'
      ? comboStore.getUserFavoriteCombos
      : comboStore.getUserCombos
    const result = await loader(auth.user.id, { page: nextPage, pageSize })
    combos.value = result.list
    page.value = result.page
    total.value = result.total
  } catch (err) {
    error.value = err.message || '连招加载失败'
  } finally {
    loading.value = false
  }
}

function changePage(nextPage) {
  if (nextPage < 1 || nextPage > pageCount.value) return
  loadCombos(nextPage)
}

async function toggleFavorite(combo) {
  if (props.kind !== 'favorites' || favoriteBusy.value) return
  favoriteBusy.value = combo.id
  try {
    const patch = await comboStore.toggleFavorite(combo)
    if (patch.favorited) return
    removeFavorite(combo)
  } finally {
    favoriteBusy.value = null
  }
}

function removeFavorite(combo) {
  combos.value = combos.value.filter(item => Number(item.id) !== Number(combo.id))
  total.value = Math.max(0, total.value - 1)
  if (!combos.value.length && page.value > 1) {
    loadCombos(page.value - 1)
  }
}

function canManageRejectedCombo(combo) {
  return props.kind === 'uploads' && combo?.status === 'rejected'
}

function openEditCombo(combo) {
  if (!canManageRejectedCombo(combo)) return
  editingCombo.value = { ...combo }
  editModalOpen.value = true
}

function closeEditCombo() {
  editModalOpen.value = false
  editingCombo.value = null
}

async function handleComboResubmitted() {
  closeEditCombo()
  await loadCombos(page.value)
}

async function deleteRejectedCombo(combo) {
  if (!canManageRejectedCombo(combo) || deleteBusy.value) return
  const confirmed = await ui.confirmDialog({
    title: '删除被驳回连招',
    message: '删除后这条连招会从数据库彻底移除，无法恢复。确定删除吗？',
    tone: 'danger',
    confirmText: '确认删除',
  })
  if (!confirmed) return
  deleteBusy.value = combo.id
  try {
    await api.deleteCombo(combo.id)
    combos.value = combos.value.filter(item => Number(item.id) !== Number(combo.id))
    total.value = Math.max(0, total.value - 1)
    if (!combos.value.length && page.value > 1) {
      await loadCombos(page.value - 1)
    }
    await ui.alertDialog({
      title: '连招已删除',
      message: '被驳回的连招已从数据库删除。',
      tone: 'success',
    })
  } catch (err) {
    await ui.alertDialog({
      title: '删除失败',
      message: err.message || '删除连招失败，请稍后重试。',
      tone: 'danger',
    })
  } finally {
    deleteBusy.value = null
  }
}

function comboMeta(combo) {
  const parts = []
  const uploader = combo.authorName || combo.author || ''
  const uploadedAt = formatUploadTime(combo.createdAt)
  if (uploader) parts.push(`由 ${uploader} 上传`)
  if (uploadedAt) parts.push(uploadedAt)
  return parts.join(' · ')
}

function comboTags(combo) {
  return normalizeComboTags(combo, combo.type)
}

function formatUploadTime(value) {
  if (!value) return ''
  const rawValue = String(value)
  if (/^\d{4}-\d{2}-\d{2}$/.test(rawValue)) return rawValue
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return rawValue
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
}

function statusLabel(status) {
  return {
    approved: '已通过',
    pending: '审核中',
    rejected: '已驳回',
  }[status] || '待审核'
}

function statusClass(status) {
  return {
    approved: 'approved',
    pending: 'pending',
    rejected: 'rejected',
  }[status] || 'pending'
}
</script>

<style scoped>
.account-combo-page {
  display: grid;
  gap: 24px;
}

.account-combo-head {
  display: grid;
  gap: 8px;
  padding-left: 58px;
}

.account-combo-head > span,
.back-link,
.panel-head small,
.panel-head button,
.pager-row,
.state-panel,
.status-pill {
  font-family: 'JetBrains Mono';
}

.account-combo-head > span {
  color: var(--accent-red);
  font-size: 12px;
  letter-spacing: 0;
}

.account-combo-head h1 {
  margin: 0;
  color: var(--ink-text);
  font-family: inherit;
  font-size: 40px;
  font-weight: 900;
  line-height: 1.08;
}

.account-combo-head p {
  max-width: 66ch;
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

.account-combo-panel,
.auth-required-panel {
  background: var(--ink-surface);
  border: 1px solid var(--ink-line);
  border-radius: 6px;
}

.account-combo-panel {
  overflow: hidden;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--ink-line);
  background: var(--ink-surface-low);
}

.panel-head strong {
  display: block;
  color: var(--ink-text);
  font-size: 18px;
  font-weight: 900;
}

.panel-head small {
  display: block;
  margin-top: 4px;
  color: #8f9db1;
  font-size: 12px;
}

.panel-head button {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 12px;
  color: var(--ink-text-soft);
  background: var(--ink-surface-high);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-size: 12px;
}

.panel-head button:hover:not(:disabled) {
  border-color: var(--accent-red);
  color: var(--ink-text);
}

.panel-head button:disabled {
  cursor: wait;
  opacity: 0.58;
}

.panel-head button .material-symbols-outlined {
  font-size: 18px;
}

.combo-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  padding: 20px;
}

.account-combo-card {
  min-height: 220px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px;
  color: var(--ink-text-soft);
  background: #0b1018;
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  transition: border-color 0.18s ease, background 0.18s ease, transform 0.18s ease;
}

.account-combo-card:hover,
.account-combo-card:focus-visible {
  color: var(--ink-text);
  border-color: var(--accent-red);
  background: #101722;
  transform: translateY(-1px);
}

.combo-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.combo-card-head > div {
  min-width: 0;
}

.combo-card-head h2 {
  margin: 0 0 10px;
  color: var(--accent-red);
  font-family: 'JetBrains Mono';
  font-size: 18px;
  font-weight: 900;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.type-pill {
  flex: 0 0 auto;
}

.type-pill-list {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
  max-width: 180px;
}

.combo-meta {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 10px 0 0;
  color: #8f9db1;
  font-family: 'JetBrains Mono';
  font-size: 11px;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.combo-meta .material-symbols-outlined {
  flex: 0 0 auto;
  font-size: 13px;
}

.combo-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  margin-top: auto;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}

.combo-metrics span:first-child,
.combo-metrics span:nth-child(2) {
  color: var(--accent-red);
}

.combo-social-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-family: 'JetBrains Mono';
}

.combo-social-count {
  min-width: 74px;
  min-height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 10px;
  color: var(--ink-text-soft);
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-size: 12px;
  font-weight: 900;
}

.combo-social-count.like {
  color: var(--accent-cyan-soft);
  border-color: rgba(91, 214, 199, 0.42);
  background: rgba(91, 214, 199, 0.08);
}

.combo-social-count.favorite {
  color: var(--accent-amber);
  border-color: rgba(255, 209, 102, 0.42);
  background: rgba(255, 209, 102, 0.08);
}

.combo-social-count .material-symbols-outlined {
  font-size: 17px;
}

.combo-social-count strong {
  line-height: 1;
}

.combo-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.favorite-action {
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 12px;
  color: var(--accent-amber);
  background: rgba(255, 209, 102, 0.1);
  border: 1px solid rgba(255, 209, 102, 0.36);
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  font-weight: 800;
}

.favorite-action:hover:not(:disabled) {
  color: #fff0bf;
  border-color: rgba(255, 209, 102, 0.72);
}

.favorite-action:disabled {
  cursor: wait;
  opacity: 0.64;
}

.favorite-action .material-symbols-outlined {
  font-size: 17px;
}

.rejection-reason {
  min-height: 34px;
  min-width: 0;
  flex: 1 1 auto;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  color: #fff4f5;
  background: rgba(255, 59, 79, 0.16);
  border: 1px solid rgba(255, 118, 135, 0.52);
  border-radius: 4px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.04), 0 0 16px rgba(255, 59, 79, 0.08);
  font-family: 'JetBrains Mono';
  font-size: 12px;
  font-weight: 800;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.rejection-reason .material-symbols-outlined {
  flex: 0 0 auto;
  font-size: 17px;
}

.rejected-action-row {
  display: inline-flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: 8px;
}

.combo-manage-action {
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 11px;
  color: var(--accent-cyan-soft);
  background: rgba(91, 214, 199, 0.1);
  border: 1px solid rgba(91, 214, 199, 0.42);
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  font-weight: 800;
}

.combo-manage-action:hover:not(:disabled) {
  color: var(--ink-text);
  border-color: rgba(91, 214, 199, 0.78);
}

.combo-manage-action.danger {
  color: var(--state-error);
  background: rgba(255, 86, 105, 0.1);
  border-color: rgba(255, 118, 135, 0.42);
}

.combo-manage-action.danger:hover:not(:disabled) {
  color: #fff4f5;
  border-color: rgba(255, 118, 135, 0.78);
}

.combo-manage-action:disabled {
  cursor: wait;
  opacity: 0.62;
}

.combo-manage-action .material-symbols-outlined {
  font-size: 17px;
}

.status-pill {
  min-width: 68px;
  height: 34px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-size: 12px;
  font-weight: 800;
}

.status-pill.approved {
  color: var(--accent-cyan-soft);
  border-color: rgba(91, 214, 199, 0.42);
  background: rgba(91, 214, 199, 0.1);
}

.status-pill.pending {
  color: var(--accent-amber);
  border-color: rgba(255, 209, 102, 0.38);
  background: rgba(255, 209, 102, 0.1);
}

.status-pill.rejected {
  color: var(--state-error);
  border-color: rgba(255, 118, 135, 0.38);
  background: rgba(255, 86, 105, 0.1);
}

.state-panel,
.auth-required-panel {
  min-height: 280px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  padding: 28px;
  color: var(--ink-muted);
  text-align: center;
}

.state-panel .material-symbols-outlined,
.auth-required-panel .material-symbols-outlined {
  color: var(--accent-red);
  font-size: 42px;
}

.state-panel h2,
.auth-required-panel h2 {
  margin: 0;
  color: var(--ink-text);
  font-family: inherit;
  font-size: 26px;
  font-weight: 900;
}

.state-panel p,
.auth-required-panel p {
  margin: 0;
  max-width: 46ch;
  line-height: 1.65;
}

.state-panel a,
.auth-required-panel a,
.state-panel button {
  min-height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 8px;
  padding: 0 18px;
  color: #190607;
  background: var(--accent-red);
  border: 0;
  border-radius: 4px;
  font-family: inherit;
  font-weight: 900;
}

.state-panel.error .material-symbols-outlined {
  color: #ff8998;
}

.pager-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 0 20px 20px;
}

.pager-row button {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 12px;
  color: var(--ink-text-soft);
  background: var(--ink-surface-high);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-size: 12px;
}

.pager-row button:hover:not(:disabled) {
  border-color: var(--accent-red);
  color: var(--ink-text);
}

.pager-row button:disabled {
  opacity: 0.48;
  cursor: not-allowed;
}

.pager-row button .material-symbols-outlined {
  font-size: 18px;
}

.pager-row > span {
  min-width: 58px;
  color: #8f9db1;
  text-align: center;
  font-size: 12px;
}

.combo-edit-backdrop {
  position: fixed;
  inset: 0;
  z-index: 180;
  display: grid;
  place-items: center;
  padding: 20px;
  overflow-y: auto;
  background: rgba(4, 7, 12, 0.76);
  backdrop-filter: blur(14px);
}

.combo-edit-enter-active,
.combo-edit-leave-active {
  transition: opacity 0.18s ease;
}

.combo-edit-enter-active :deep(.combo-upload-panel),
.combo-edit-leave-active :deep(.combo-upload-panel) {
  transition: transform 0.18s ease;
}

.combo-edit-enter-from,
.combo-edit-leave-to {
  opacity: 0;
}

.combo-edit-enter-from :deep(.combo-upload-panel),
.combo-edit-leave-to :deep(.combo-upload-panel) {
  transform: translateY(10px);
}

@media (max-width: 860px) {
  .combo-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .account-combo-head {
    padding-left: 46px;
  }

  .account-combo-head h1 {
    font-size: 32px;
  }

  .panel-head {
    align-items: flex-start;
    flex-direction: column;
    padding: 16px;
  }

  .panel-head button {
    width: 100%;
  }

  .combo-grid {
    gap: 12px;
    padding: 12px;
  }

  .account-combo-card {
    min-height: 0;
    padding: 14px;
  }

  .combo-card-head {
    flex-direction: column;
  }

  .type-pill-list {
    justify-content: flex-start;
    max-width: none;
  }

  .combo-card-head h2 {
    font-size: 15px;
  }

  .combo-actions {
    justify-content: stretch;
    flex-direction: column;
  }

  .favorite-action,
  .combo-manage-action,
  .status-pill,
  .rejection-reason,
  .combo-social-count {
    width: 100%;
  }

  .rejected-action-row {
    width: 100%;
  }

  .pager-row {
    padding: 0 12px 12px;
  }

  .pager-row button {
    flex: 1 1 0;
  }
}
</style>
