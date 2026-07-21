<template>
  <div v-if="error" :class="['route-error', { admin: isAdminRoute }]">
    <section class="route-error-panel">
      <span class="material-symbols-outlined">error</span>
      <h1>页面加载失败</h1>
      <p>请刷新或返回后重试。</p>
      <router-link :to="isAdminRoute ? '/admin' : '/'">{{ isAdminRoute ? '返回后台' : '返回首页' }}</router-link>
    </section>
  </div>
  <slot v-else></slot>
</template>

<script setup>
import { computed, onErrorCaptured, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const error = ref(null)
const isAdminRoute = computed(() => route.path.startsWith('/admin'))

onErrorCaptured(err => {
  error.value = err
  console.error(err)
  return false
})

watch(() => route.fullPath, () => {
  error.value = null
})
</script>

<style scoped>
.route-error {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  color: var(--ink-text-soft);
  background: var(--ink-bg);
  text-align: center;
}

.route-error.admin {
  background: var(--ink-bg);
}

.route-error-panel {
  width: min(420px, 100%);
  display: grid;
  justify-items: center;
  gap: 12px;
  padding: 26px;
  border: 1px solid #243044;
  border-radius: 8px;
  background: #111827;
}

.route-error .material-symbols-outlined {
  color: var(--state-error);
  font-size: 48px;
}

.route-error h1 {
  margin: 0;
  color: var(--ink-text);
  font-size: 22px;
  font-weight: 900;
}

.route-error p {
  margin: 0;
  color: var(--ink-text-soft);
  font-family: 'JetBrains Mono';
  font-size: 14px;
}

.route-error a {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 4px;
  padding: 0 14px;
  color: #190607;
  background: var(--accent-red);
  border: 1px solid rgba(91, 214, 199, 0.54);
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  font-size: 13px;
  font-weight: 900;
  text-decoration: none;
}
</style>
