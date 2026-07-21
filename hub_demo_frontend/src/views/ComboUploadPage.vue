<template>
  <div class="tool-page">
    <header class="tool-page-head">
      <router-link to="/" class="back-link">
        <span class="material-symbols-outlined">arrow_back</span>
        返回角色
      </router-link>
      <span class="tool-page-kicker">COMBO UPLOAD</span>
      <h1>连招上传</h1>
      <p>把训练模式里的路线整理成可检索数据。上传后会进入审核队列，通过后展示在对应角色详情页。</p>
    </header>

    <ComboUploadForm v-if="auth.isLoggedIn" embedded />
    <section v-else class="login-required">
      <span class="material-symbols-outlined">lock</span>
      <h2>登录后上传连招</h2>
      <p>上传路线需要绑定作者信息，请先登录或注册账号。</p>
      <router-link to="/login">前往登录</router-link>
    </section>
  </div>
</template>

<script setup>
import ComboUploadForm from '@/components/common/ComboUploadForm.vue'
import { useAuthStore } from '@/js/stores/auth'

const auth = useAuthStore()
</script>

<style scoped>
.tool-page {
  display: grid;
  gap: 22px;
  max-width: 760px;
  margin-inline: auto;
}

.tool-page-head {
  display: grid;
  gap: 8px;
  padding-left: 58px;
}

.back-link {
  font-family: 'JetBrains Mono';
}

.tool-page-kicker {
  color: var(--accent-red);
  font-family: 'JetBrains Mono';
  font-size: 12px;
  letter-spacing: 0;
}

.tool-page-head h1 {
  margin: 0;
  color: var(--ink-text);
  font-family: inherit;
  font-size: 40px;
  font-weight: 900;
  line-height: 1.08;
}

.tool-page-head p {
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

.login-required {
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 260px;
  padding: 28px;
  text-align: center;
  background: var(--ink-surface);
  border: 1px solid var(--ink-line);
  border-radius: 6px;
}

.login-required .material-symbols-outlined {
  color: var(--accent-red);
  font-size: 42px;
}

.login-required h2 {
  margin: 0;
  color: var(--ink-text);
  font-family: inherit;
  font-size: 28px;
  font-weight: 900;
}

.login-required p {
  margin: 0;
  color: var(--ink-text-soft);
}

.login-required a {
  min-height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 8px;
  padding: 0 18px;
  color: #190607;
  background: var(--accent-red);
  border-radius: 4px;
  font-family: inherit;
  font-weight: 900;
}

@media (max-width: 560px) {
  .tool-page-head {
    padding-left: 46px;
  }

  .tool-page-head h1 {
    font-size: 32px;
  }
}
</style>
