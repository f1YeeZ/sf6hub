<template>
  <div class="side-nav-system">
    <button
      type="button"
      :class="['menu-trigger', { notified: hasUnreadComboReview }]"
      :aria-expanded="open"
      aria-controls="site-side-nav"
      aria-label="打开菜单"
      @click="open = true"
    >
      <span v-if="hasUnreadComboReview" class="notice-dot" aria-label="new combo review result"></span>
      <span class="material-symbols-outlined">menu</span>
    </button>

    <transition name="side-nav-backdrop">
      <div v-if="open" class="side-nav-backdrop" @click.self="close">
        <aside id="site-side-nav" class="side-nav-panel" aria-label="站点菜单">
          <div class="side-nav-head">
            <img
              class="side-nav-brand"
              src="/combos-hub-brush-wordmark.webp"
              width="512"
              height="254"
              loading="lazy"
              decoding="async"
              alt="Combos Hub"
            />
            <button type="button" aria-label="关闭菜单" @click="close">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <nav class="side-nav-links">
            <button type="button" :class="{ active: route.path === '/' }" @click="go('/')">
              <span class="material-symbols-outlined">groups</span>
              <strong>角色</strong>
            </button>
            <button type="button" :class="{ active: route.path === '/upload' }" @click="go('/upload')">
              <span class="material-symbols-outlined">upload_file</span>
              <strong>连招上传</strong>
            </button>
            <button type="button" :class="{ active: route.path === '/guide' }" @click="go('/guide')">
              <span class="material-symbols-outlined">menu_book</span>
              <strong>使用指南</strong>
            </button>
            <button type="button" :class="{ active: route.path === '/tier-list' }" @click="go('/tier-list')">
              <span class="material-symbols-outlined">leaderboard</span>
              <strong>小游戏</strong>
            </button>
            <button type="button" :class="{ active: route.path === '/feedback' }" @click="go('/feedback')">
              <span class="material-symbols-outlined">flag</span>
              <strong>提交反馈</strong>
            </button>
            <button v-if="!auth.isLoggedIn" type="button" :class="{ active: route.path === '/login' }" @click="go('/login')">
              <span class="material-symbols-outlined">login</span>
              <strong>登录</strong>
            </button>
          </nav>

          <div v-if="auth.isLoggedIn" class="side-nav-account">
            <transition name="account-menu">
              <section v-if="accountMenuOpen" class="account-popover" aria-label="账号菜单">
                <div class="account-popover-head">
                  <span class="user-avatar">{{ avatarText }}</span>
                  <div>
                    <strong>{{ displayName }}</strong>
                    <small>{{ auth.user?.email || '已登录' }}</small>
                  </div>
                </div>
                <button
                  type="button"
                  :class="['account-menu-action', { active: route.path === '/notifications', notified: hasUnreadComboReview }]"
                  @click="goAccount('/notifications')"
                >
                  <span v-if="hasUnreadComboReview" class="notice-dot" aria-label="有新的通知"></span>
                  <span class="material-symbols-outlined">notifications</span>
                  <strong>通知中心</strong>
                </button>
                <button
                  type="button"
                  :class="['account-menu-action', { active: route.path === '/profile' }]"
                  @click="goAccount('/profile')"
                >
                  <span class="material-symbols-outlined">manage_accounts</span>
                  <strong>个人资料</strong>
                </button>
                <button
                  type="button"
                  :class="['account-menu-action', { active: route.path === '/combo-favorites' }]"
                  @click="goAccount('/combo-favorites')"
                >
                  <span class="material-symbols-outlined">bookmark</span>
                  <strong>连招收藏</strong>
                </button>
                <button
                  type="button"
                  :class="['account-menu-action', { active: route.path === '/my-combos', notified: hasUnreadComboReview }]"
                  @click="goMyCombos"
                >
                  <span v-if="hasUnreadComboReview" class="notice-dot" aria-label="有新的审核结果"></span>
                  <span class="material-symbols-outlined">inventory_2</span>
                  <strong>已上传连招</strong>
                </button>
                <button type="button" class="logout-action" :disabled="loggingOut" @click="logout">
                  <span class="material-symbols-outlined">logout</span>
                  <strong>{{ loggingOut ? '退出中...' : '退出登录' }}</strong>
                </button>
              </section>
            </transition>

            <button
              type="button"
              :class="['account-trigger', { notified: hasUnreadComboReview }]"
              :aria-expanded="accountMenuOpen"
              aria-label="打开账号菜单"
              @click="accountMenuOpen = !accountMenuOpen"
            >
              <span v-if="hasUnreadComboReview" class="notice-dot" aria-label="有新的审核结果"></span>
              <span class="user-avatar">{{ avatarText }}</span>
              <span class="account-name">{{ displayName }}</span>
              <span class="material-symbols-outlined account-chevron">chevron_right</span>
            </button>
          </div>
        </aside>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/js/api'
import { useAuthStore } from '@/js/stores/auth'
import { useRealtimeStore } from '@/js/stores/realtime'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const realtime = useRealtimeStore()
const open = ref(false)
const accountMenuOpen = ref(false)
const loggingOut = ref(false)
const reviewNotifications = ref([])
const unreadReviewTotal = ref(0)
let reviewPollTimer = null
let markingReviewNotificationsRead = false

const displayName = computed(() => auth.user?.username || '已登录用户')
const avatarText = computed(() => {
  const compactName = displayName.value.replace(/\s+/g, '')
  return Array.from(compactName).slice(0, 2).join('') || '用户'
})
const hasUnreadComboReview = computed(() => unreadReviewTotal.value > 0 || reviewNotifications.value.length > 0)

watch(open, value => {
  document.body.style.overflow = value ? 'hidden' : ''
  if (!value) accountMenuOpen.value = false
  if (value && auth.isLoggedIn) loadReviewNotifications()
})

watch(() => auth.isLoggedIn, loggedIn => {
  reviewNotifications.value = []
  unreadReviewTotal.value = 0
  clearInterval(reviewPollTimer)
  reviewPollTimer = null
  if (!loggedIn) return
  syncReviewIndicator()
  reviewPollTimer = setInterval(loadReviewNotifications, 60000)
})

watch(() => route.path, () => {
  if (!auth.isLoggedIn) return
  syncReviewIndicator()
})

watch(() => realtime.lastEvent, event => {
  if (!auth.isLoggedIn) return
  const areas = event?.areas || []
  if (areas.includes('notifications') || areas.includes('userCombos')) {
    if (markingReviewNotificationsRead) return
    syncReviewIndicator()
  }
  if (areas.includes('profile') || areas.includes('users')) {
    auth.hydrate()
  }
})

function close() {
  open.value = false
}

function go(path) {
  close()
  router.push(path).catch(() => {})
}

function goAccount(path) {
  accountMenuOpen.value = false
  go(path)
}

async function goMyCombos() {
  await markReviewNotificationsRead(true)
  goAccount('/my-combos')
}

async function loadReviewNotifications() {
  if (!auth.isLoggedIn) return
  try {
    const result = await api.getNotifications({
      page: 1,
      pageSize: 100,
      unreadOnly: true,
      type: 'combo_review',
    })
    reviewNotifications.value = result?.list || []
    unreadReviewTotal.value = Number(result?.total || reviewNotifications.value.length || 0)
    if (route.path === '/my-combos' && hasUnreadComboReview.value) {
      await markReviewNotificationsRead(true)
    }
  } catch (_) {
    reviewNotifications.value = []
    unreadReviewTotal.value = 0
  }
}

async function syncReviewIndicator() {
  if (route.path === '/my-combos') {
    await markReviewNotificationsRead(true)
    return
  }
  await loadReviewNotifications()
}

async function markReviewNotificationsRead(force = false) {
  if (!force && !hasUnreadComboReview.value) return
  if (markingReviewNotificationsRead) return
  const cachedIds = reviewNotifications.value.map(notification => notification.id).filter(Boolean)
  reviewNotifications.value = []
  unreadReviewTotal.value = 0
  markingReviewNotificationsRead = true
  try {
    await api.markNotificationsReadAll({ type: 'combo_review' })
  } catch (_) {
    await markCachedReviewNotificationsRead(cachedIds)
  } finally {
    markingReviewNotificationsRead = false
  }
}

async function markCachedReviewNotificationsRead(cachedIds = []) {
  let ids = cachedIds
  if (!ids.length) {
    try {
      const result = await api.getNotifications({
        page: 1,
        pageSize: 100,
        unreadOnly: true,
        type: 'combo_review',
      })
      ids = (result?.list || []).map(notification => notification.id).filter(Boolean)
    } catch (_) {
      ids = []
    }
  }
  if (!ids.length) return
  await Promise.allSettled(ids.map(id => api.markNotificationRead(id)))
}

async function logout() {
  if (loggingOut.value) return
  loggingOut.value = true
  try {
    await auth.logout()
    accountMenuOpen.value = false
    close()
    if (route.meta?.requiresAuth) router.push('/').catch(() => {})
  } finally {
    loggingOut.value = false
  }
}

onMounted(() => {
  if (!auth.isLoggedIn) return
  syncReviewIndicator()
  reviewPollTimer = setInterval(loadReviewNotifications, 60000)
})

onUnmounted(() => {
  clearInterval(reviewPollTimer)
})
</script>

<style scoped>
.menu-trigger {
  position: fixed;
  top: 18px;
  left: 18px;
  z-index: 70;
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  color: var(--ink-text);
  background: rgba(18, 16, 15, 0.94);
  border: 1px solid var(--ink-line);
  border-radius: 999px;
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.24);
  transition: transform 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.menu-trigger:hover,
.menu-trigger:focus-visible {
  border-color: var(--accent-red);
  background: #211514;
  transform: translateY(-1px);
}

.side-nav-backdrop {
  position: fixed;
  inset: 0;
  z-index: 115;
  display: flex;
  justify-content: flex-start;
  padding: 0;
  background: rgba(5, 2, 3, 0.42);
}

.side-nav-panel {
  width: min(270px, 82vw);
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  padding: 24px 18px;
  color: var(--ink-text-soft);
  background:
    linear-gradient(180deg, rgba(35, 16, 17, 0.7), rgba(10, 7, 7, 0.96)),
    var(--ink-surface);
  border-right: 1px solid var(--ink-line-strong);
}

.side-nav-head {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 76px;
  padding: 0 42px 28px;
}

.side-nav-brand {
  width: min(216px, 100%);
  height: 76px;
  flex: 0 0 auto;
  object-fit: contain;
  object-position: center;
  transform: translateY(1px) rotate(-2deg) scale(1.08);
  filter: drop-shadow(0 4px 0 rgba(0, 0, 0, 0.52)) drop-shadow(0 0 12px rgba(39, 245, 238, 0.16));
}

.side-nav-head button {
  position: absolute;
  top: 0;
  right: 0;
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  color: var(--ink-muted);
  border: 1px solid var(--ink-line);
  border-radius: 999px;
}

.side-nav-links {
  display: grid;
  gap: 10px;
  margin-top: 0;
}

.side-nav-links button {
  min-height: 58px;
  display: grid;
  grid-template-columns: 34px 1fr;
  align-items: center;
  gap: 12px;
  padding: 0 14px;
  color: var(--ink-muted);
  text-align: left;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 0 12px 12px 0;
  transition: color 0.18s ease, border-color 0.18s ease, background 0.18s ease, transform 0.18s ease;
}

.side-nav-links button:hover,
.side-nav-links button:focus-visible,
.side-nav-links button.active {
  color: var(--ink-text);
  border-color: rgba(255, 64, 84, 0.42);
  border-left-color: var(--accent-red);
  background:
    linear-gradient(90deg, rgba(255, 64, 84, 0.18), rgba(49, 213, 230, 0.06)),
    #1a1111;
  transform: translateX(4px);
}

.side-nav-links .material-symbols-outlined {
  justify-self: center;
  color: #927f78;
}

.side-nav-links button.active .material-symbols-outlined,
.side-nav-links button:hover .material-symbols-outlined,
.side-nav-links button:focus-visible .material-symbols-outlined {
  color: var(--accent-red-hot);
}

.side-nav-links strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: inherit;
  font-size: 14px;
  font-weight: 800;
}

.side-nav-account {
  position: relative;
  margin-top: auto;
  padding-top: 18px;
}

.account-trigger {
  position: relative;
  width: 100%;
  min-height: 64px;
  display: grid;
  grid-template-columns: 38px 1fr 22px;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  color: var(--ink-text);
  text-align: left;
  background: #0b0808;
  border: 1px solid var(--ink-line);
  border-radius: 12px;
  transition: background 0.18s ease, border-color 0.18s ease, transform 0.18s ease;
}

.account-trigger:hover,
.account-trigger:focus-visible,
.account-trigger[aria-expanded='true'] {
  background: #1a1211;
  border-color: var(--ink-line-strong);
}

.account-trigger:active {
  transform: translateY(1px);
}

.user-avatar {
  width: 34px;
  height: 34px;
  display: inline-grid;
  place-items: center;
  flex: 0 0 auto;
  color: #fff8ef;
  background: linear-gradient(135deg, #ff4054, #7e1a22);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  line-height: 1;
}

.account-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 800;
}

.account-chevron {
  color: var(--ink-muted);
  transition: transform 0.18s ease;
}

.account-trigger[aria-expanded='true'] .account-chevron {
  transform: rotate(-90deg);
}

.account-popover {
  position: absolute;
  left: 0;
  right: 0;
  bottom: calc(100% + 8px);
  z-index: 2;
  padding: 10px;
  color: var(--ink-text);
  background: #181211;
  border: 1px solid var(--ink-line-strong);
  border-radius: 12px;
  box-shadow: 0 16px 30px rgba(0, 0, 0, 0.32);
}

.account-popover-head {
  display: grid;
  grid-template-columns: 38px 1fr;
  align-items: center;
  gap: 10px;
  padding: 8px 8px 12px;
  border-bottom: 1px solid rgba(255, 248, 239, 0.12);
}

.account-popover-head strong,
.account-popover-head small {
  min-width: 0;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-popover-head strong {
  color: var(--ink-text);
  font-size: 14px;
  font-weight: 850;
}

.account-popover-head small {
  margin-top: 2px;
  color: var(--ink-muted);
  font-size: 12px;
}

.account-menu-action,
.logout-action {
  position: relative;
  width: 100%;
  min-height: 44px;
  display: grid;
  grid-template-columns: 30px 1fr;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 0 10px;
  color: var(--ink-text-soft);
  text-align: left;
  border-radius: 8px;
  transition: background 0.18s ease, color 0.18s ease;
}

.account-menu-action {
  margin-top: 8px;
  color: var(--ink-text-soft);
}

.account-menu-action.active {
  color: #ffe8e8;
  background: rgba(255, 64, 84, 0.13);
}

.account-menu-action.active .material-symbols-outlined {
  color: var(--accent-red-hot);
}

.logout-action:hover,
.logout-action:focus-visible,
.account-menu-action:hover,
.account-menu-action:focus-visible {
  background: rgba(255, 248, 239, 0.08);
}

.logout-action:disabled {
  cursor: progress;
  color: #8e7f79;
}

.account-menu-action .material-symbols-outlined,
.logout-action .material-symbols-outlined {
  justify-self: center;
  color: currentColor;
}

.account-menu-action strong,
.logout-action strong {
  font-size: 14px;
  font-weight: 750;
}

.notice-dot {
  position: absolute;
  top: 9px;
  left: 9px;
  z-index: 1;
  width: 9px;
  height: 9px;
  background: var(--accent-red);
  border: 2px solid #181211;
  border-radius: 999px;
  box-shadow: 0 0 0 3px rgba(255, 59, 79, 0.16), 0 0 12px rgba(255, 59, 79, 0.72);
  pointer-events: none;
}

.account-trigger .notice-dot {
  top: 10px;
  left: 10px;
  border-color: #0b0808;
}

.menu-trigger .notice-dot {
  top: 3px;
  right: 3px;
  left: auto;
  border-color: rgba(18, 16, 15, 0.94);
}

.menu-trigger.notified:hover .notice-dot,
.menu-trigger.notified:focus-visible .notice-dot {
  border-color: #211514;
}

.account-trigger.notified:hover .notice-dot,
.account-trigger.notified:focus-visible .notice-dot,
.account-trigger.notified[aria-expanded='true'] .notice-dot {
  border-color: #1a1211;
}

.account-menu-action.notified.active .notice-dot,
.account-menu-action.notified:hover .notice-dot,
.account-menu-action.notified:focus-visible .notice-dot {
  border-color: #241716;
}

.account-menu-enter-active,
.account-menu-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.account-menu-enter-from,
.account-menu-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

.side-nav-backdrop-enter-active,
.side-nav-backdrop-leave-active {
  transition: opacity 0.18s ease;
}

.side-nav-backdrop-enter-active .side-nav-panel,
.side-nav-backdrop-leave-active .side-nav-panel {
  transition: transform 0.22s cubic-bezier(0.16, 1, 0.3, 1);
}

.side-nav-backdrop-enter-from,
.side-nav-backdrop-leave-to {
  opacity: 0;
}

.side-nav-backdrop-enter-from .side-nav-panel,
.side-nav-backdrop-leave-to .side-nav-panel {
  transform: translateX(-24px);
}

@media (max-width: 560px) {
  .menu-trigger {
    top: 12px;
    left: 12px;
    width: 44px;
    height: 44px;
  }

  .side-nav-backdrop {
    padding: 0;
  }

  .side-nav-panel {
    width: min(270px, 86vw);
    min-height: 100dvh;
    padding: 20px 16px;
  }
}
</style>
