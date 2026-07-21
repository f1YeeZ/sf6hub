<template>
  <div v-if="isAdminRoute" class="admin-shell-root">
    <router-view v-slot="{ Component }">
      <RouteErrorBoundary>
        <div :key="route.path" class="route-page-shell">
          <component :is="Component" />
        </div>
      </RouteErrorBoundary>
    </router-view>
  </div>
  <div v-else class="app-shell">
    <SideNavDrawer />
    <main :class="['site-main flex-grow relative z-10 px-margin-mobile md:px-margin-desktop w-full max-w-[1440px] mx-auto pt-[82px] pb-12', { 'login-site-main': isLoginRoute }]">
      <router-view v-slot="{ Component }">
        <RouteErrorBoundary>
          <div :key="route.path" class="route-page-shell">
            <component :is="Component" />
          </div>
        </RouteErrorBoundary>
      </router-view>
    </main>
  </div>
  <SocialMediaLink v-if="!isAdminRoute" />
  <AppDialog />
</template>

<script setup>
import SideNavDrawer from '@/components/layout/SideNavDrawer.vue'
import AppDialog from '@/components/common/AppDialog.vue'
import RouteErrorBoundary from '@/components/common/RouteErrorBoundary.vue'
import SocialMediaLink from '@/components/common/SocialMediaLink.vue'
import { computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/js/stores/auth'
import { useRealtimeStore } from '@/js/stores/realtime'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const realtime = useRealtimeStore()
const isAdminRoute = computed(() => route.path.startsWith('/admin'))
const isLoginRoute = computed(() => route.path === '/login')

watch(() => auth.showLogin, value => {
  if (!value) return
  auth.closeLogin()
  if (route.path !== '/login') router.push('/login').catch(() => {})
})

onMounted(() => {
  realtime.connect()
})

onBeforeUnmount(() => {
  realtime.disconnect()
})
</script>

<style>
.admin-shell-root {
  min-height: 100vh;
  background: #101114;
  color: #fff8ef;
}

.app-shell {
  position: relative;
  isolation: isolate;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--ink-bg);
  color: var(--ink-text);
}

.app-shell::before,
.app-shell::after {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
}

.app-shell::before {
  z-index: -2;
  background:
    linear-gradient(180deg, rgba(7, 5, 6, 0.48), rgba(7, 5, 6, 0.86)),
    url('/cyberpunk-alley-background.webp') center / cover no-repeat;
}

.app-shell::after {
  z-index: -1;
  background:
    radial-gradient(circle at 52% 43%, rgba(49, 213, 230, 0.1), transparent 34%),
    radial-gradient(circle at 20% 24%, rgba(255, 64, 84, 0.18), transparent 30%),
    radial-gradient(circle at 74% 78%, rgba(255, 193, 93, 0.08), transparent 28%),
    rgba(7, 5, 6, 0.38);
}

.route-page-shell {
  min-width: 0;
  animation: route-page-enter 320ms ease-out both;
}

@keyframes route-page-enter {
  from {
    opacity: 0;
    transform: translate3d(0, -14px, 0);
  }
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .route-page-shell {
    animation: none;
  }
}

.login-site-main {
  display: grid;
  align-items: center;
  max-width: 100%;
  min-height: 100vh;
  padding-top: 72px;
  padding-bottom: 42px;
}

@media (max-width: 900px) {
  .site-main {
    padding-top: 74px;
  }
}

@media (max-width: 520px) {
  .site-main {
    padding-top: 68px;
    padding-bottom: 32px;
  }

  .login-site-main {
    padding-top: 72px;
  }
}
</style>
