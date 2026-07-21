<template>
  <section class="notifications-page">
    <header class="notifications-head">
      <div>
        <span>NOTIFICATIONS</span>
        <h1>通知中心</h1>
        <p>查看审核结果、互动和系统消息。</p>
      </div>
      <button type="button" :disabled="loading || !unreadTotal" @click="markAllRead">
        全部标为已读
      </button>
    </header>

    <div class="notification-filters">
      <button type="button" :class="{ active: unreadOnly }" @click="setUnreadOnly(true)">未读 {{ unreadTotal }}</button>
      <button type="button" :class="{ active: !unreadOnly }" @click="setUnreadOnly(false)">全部</button>
      <select v-model="typeFilter" @change="reloadFromFirstPage">
        <option value="">全部类型</option>
        <option v-for="option in notificationTypeOptions" :key="option.value" :value="option.value">
          {{ option.label }}
        </option>
      </select>
    </div>

    <div v-if="loading" class="notification-state">加载中...</div>
    <div v-else-if="!notifications.length" class="notification-state">暂无通知</div>
    <div v-else class="notification-list">
      <article
        v-for="item in notifications"
        :key="item.id"
        :class="['notification-item', { unread: !item.read }]"
      >
        <div>
          <span>{{ notificationTypeLabel(item.type) }}</span>
          <strong>{{ item.title || '通知' }}</strong>
          <p>{{ item.content || '无内容' }}</p>
          <small>{{ formatDateTime(item.createdAt) }}</small>
        </div>
        <div class="notification-actions">
          <button v-if="!item.read" type="button" @click="markRead(item)">标为已读</button>
          <button v-if="item.targetUrl" type="button" class="primary" @click="openTarget(item)">查看</button>
        </div>
      </article>
    </div>

    <div v-if="total > pageSize" class="notification-pager">
      <button type="button" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
      <span>{{ page }} / {{ pageCount }}</span>
      <button type="button" :disabled="page >= pageCount" @click="changePage(page + 1)">下一页</button>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/js/api'
import { useRealtimeStore } from '@/js/stores/realtime'
import { notificationTypeLabel, notificationTypeOptions } from '@/js/utils/notificationTypes'

const router = useRouter()
const realtime = useRealtimeStore()
const notifications = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 12
const total = ref(0)
const unreadTotal = ref(0)
const unreadOnly = ref(true)
const typeFilter = ref('')
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function loadNotifications() {
  loading.value = true
  try {
    const result = await api.getNotifications({
      page: page.value,
      pageSize,
      unreadOnly: unreadOnly.value,
      type: typeFilter.value,
    })
    notifications.value = result?.list || []
    page.value = Number(result?.page || page.value)
    total.value = Number(result?.total || 0)
    await loadUnreadTotal()
  } finally {
    loading.value = false
  }
}

async function loadUnreadTotal() {
  const result = await api.getNotifications({ page: 1, pageSize: 1, unreadOnly: true, type: typeFilter.value })
  unreadTotal.value = Number(result?.total || 0)
}

function setUnreadOnly(value) {
  unreadOnly.value = value
  reloadFromFirstPage()
}

function reloadFromFirstPage() {
  page.value = 1
  loadNotifications()
}

async function changePage(nextPage) {
  const safePage = Math.min(Math.max(1, nextPage), pageCount.value)
  if (safePage === page.value) return
  page.value = safePage
  await loadNotifications()
}

async function markRead(item) {
  await api.markNotificationRead(item.id)
  item.read = true
  await loadUnreadTotal()
  if (unreadOnly.value) {
    notifications.value = notifications.value.filter(notification => notification.id !== item.id)
    total.value = Math.max(0, total.value - 1)
  }
}

async function markAllRead() {
  await api.markNotificationsReadAll({ type: typeFilter.value })
  await reloadFromFirstPage()
}

async function openTarget(item) {
  if (!item.read) await markRead(item)
  if (item.targetUrl) router.push(item.targetUrl).catch(() => {})
}

function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString()
}

onMounted(loadNotifications)

watch(() => realtime.lastEvent, event => {
  const areas = event?.areas || []
  if (areas.includes('notifications')) loadNotifications()
})
</script>

<style scoped>
.notifications-page {
  display: grid;
  gap: 18px;
}

.notifications-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding: 0 0 18px 58px;
  border-bottom: 1px solid var(--ink-line);
}

.notifications-head span,
.notifications-head button,
.notification-filters,
.notification-item span,
.notification-item small,
.notification-actions,
.notification-state,
.notification-pager {
  font-family: 'JetBrains Mono';
}

.notifications-head span {
  color: var(--accent-red-hot);
  font-size: 12px;
}

.notifications-head h1 {
  margin: 8px 0 0;
  color: var(--ink-text);
  font-size: 36px;
  font-weight: 900;
}

.notifications-head p {
  margin: 8px 0 0;
  color: var(--ink-text-soft);
}

.notifications-head button,
.notification-filters button,
.notification-actions button,
.notification-pager button {
  min-height: 38px;
  padding: 0 12px;
  color: var(--ink-text);
  background: var(--ink-surface-high);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
}

.notifications-head button:disabled,
.notification-pager button:disabled {
  opacity: .45;
  cursor: not-allowed;
}

.notification-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 14px;
  background: var(--ink-surface);
  border: 1px solid var(--ink-line);
  border-radius: 6px;
}

.notification-filters button.active {
  color: #190607;
  border-color: var(--accent-red);
  background: var(--accent-red);
}

.notification-filters select {
  min-height: 38px;
  margin-left: auto;
  padding: 0 10px;
  color: var(--ink-text);
  background: var(--ink-surface-high);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
}

.notification-list {
  display: grid;
  gap: 10px;
}

.notification-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  padding: 14px;
  background: var(--ink-surface);
  border: 1px solid var(--ink-line);
  border-radius: 6px;
}

.notification-item.unread {
  border-color: rgba(49, 213, 230, 0.42);
  background: rgba(49, 213, 230, 0.06);
}

.notification-item span {
  color: var(--accent-cyan-soft);
  font-size: 11px;
}

.notification-item strong {
  display: block;
  margin-top: 6px;
  color: var(--ink-text);
  font-size: 18px;
}

.notification-item p {
  margin: 6px 0 0;
  color: var(--ink-text-soft);
  line-height: 1.6;
}

.notification-item small {
  display: block;
  margin-top: 8px;
  color: var(--ink-muted);
  font-size: 11px;
}

.notification-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notification-actions .primary {
  color: #190607;
  background: var(--accent-red);
  border-color: var(--accent-red);
}

.notification-state {
  min-height: 180px;
  display: grid;
  place-items: center;
  color: var(--ink-muted);
  background: var(--ink-surface);
  border: 1px dashed var(--ink-line-strong);
  border-radius: 6px;
}

.notification-pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
}

.notification-pager span {
  color: var(--ink-muted);
  font-size: 12px;
}

@media (max-width: 640px) {
  .notifications-head,
  .notification-item {
    grid-template-columns: 1fr;
  }

  .notifications-head {
    align-items: flex-start;
    flex-direction: column;
    padding-left: 46px;
  }

  .notification-filters select,
  .notification-actions {
    width: 100%;
  }

  .notification-actions button {
    flex: 1;
  }
}
</style>
