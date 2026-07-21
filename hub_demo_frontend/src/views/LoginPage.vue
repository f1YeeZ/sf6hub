<template>
  <div class="login-page">
    <router-link to="/" class="back-link">
      <span class="material-symbols-outlined">arrow_back</span>
      返回角色
    </router-link>

    <section v-if="auth.isLoggedIn" class="account-panel">
      <header class="card-head">
        <h1>账号中心</h1>
        <p>当前账号已登录，可以继续浏览角色数据或退出登录。</p>
      </header>
      <div class="account-card">
        <div class="account-avatar">
          <img v-if="auth.user?.avatar" :src="auth.user.avatar" :alt="auth.user?.username" />
          <span v-else class="material-symbols-outlined">person</span>
        </div>
        <div>
          <h2>{{ auth.user?.username || '已登录用户' }}</h2>
          <p>{{ auth.user?.email || '未记录邮箱' }}</p>
          <small v-if="auth.user?.role">ROLE: {{ auth.user.role }}</small>
        </div>
      </div>
      <div class="account-actions">
        <router-link to="/" class="ghost-action">返回角色列表</router-link>
        <button type="button" class="danger-action" :disabled="submitting" @click="logout">
          {{ submitting ? '退出中...' : '退出登录' }}
        </button>
      </div>
    </section>

    <section v-else class="login-panel">
      <form class="login-card" @submit.prevent="handleSubmit">
        <button v-if="mode !== 'login'" type="button" class="return-login-link" @click="switchMode('login')">
          返回登录
        </button>

        <header class="card-head">
          <h1>{{ modeTitle }}</h1>
          <p>{{ modeSubtitle }}</p>
        </header>

        <label v-if="mode === 'register'" class="form-field">
          用户名
          <input v-model.trim="form.username" type="text" autocomplete="username" placeholder="请输入用户名" />
        </label>

        <label v-if="mode !== 'reset'" class="form-field">
          邮箱
          <div :class="['email-row', { withCode: mode === 'forgot' }]">
            <input v-model.trim="form.email" type="email" autocomplete="email" placeholder="请输入邮箱" />
            <button
              v-if="mode === 'forgot'"
              type="button"
              class="code-btn"
              :disabled="codeSending || codeCooldown > 0 || !form.email.trim()"
              @click="sendCode"
            >
              {{ codeButtonText }}
            </button>
          </div>
        </label>

        <label v-if="mode === 'forgot'" class="form-field">
          验证码
          <input v-model.trim="form.code" type="text" inputmode="numeric" maxlength="6" placeholder="输入 6 位验证码" />
        </label>

        <div v-if="mode === 'reset'" class="reset-email-chip">
          <span class="material-symbols-outlined">mark_email_read</span>
          <span>{{ form.email }}</span>
        </div>

        <div v-if="mode !== 'forgot'" class="form-field">
          <span class="field-head">
            <label for="auth-password">密码</label>
            <button v-if="mode === 'login'" type="button" class="forgot-link" @click="switchMode('forgot')">
              忘记密码？
            </button>
          </span>
          <span class="password-control">
            <input
              id="auth-password"
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
              placeholder="至少 8 位"
            />
            <button type="button" class="password-toggle" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword">
              <span class="material-symbols-outlined">{{ showPassword ? 'visibility_off' : 'visibility' }}</span>
            </button>
          </span>
        </div>

        <label v-if="mode === 'login'" class="remember-row">
          <input v-model="rememberMe" type="checkbox" />
          <span>记住我</span>
        </label>

        <label v-if="mode === 'reset'" class="form-field">
          确认密码
          <span class="password-control">
            <input
              v-model="form.confirmPassword"
              :type="showConfirmPassword ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="再次输入新密码"
            />
            <button type="button" class="password-toggle" :aria-label="showConfirmPassword ? '隐藏确认密码' : '显示确认密码'" @click="showConfirmPassword = !showConfirmPassword">
              <span class="material-symbols-outlined">{{ showConfirmPassword ? 'visibility_off' : 'visibility' }}</span>
            </button>
          </span>
        </label>

        <p v-if="error" class="form-error">{{ error }}</p>
        <p v-if="notice" class="form-notice">{{ notice }}</p>

        <button class="submit-btn" type="submit" :disabled="submitting">
          {{ submitting ? '处理中...' : submitText }}
        </button>

        <p v-if="mode === 'login'" class="switch-copy">
          没有账号？
          <button type="button" class="inline-mode-link" @click="switchMode('register')">
            立即注册
          </button>
        </p>
        <p v-else-if="mode === 'register'" class="switch-copy">
          已有账号？
          <button type="button" class="inline-mode-link" @click="switchMode('login')">
            返回登录
          </button>
        </p>
        <p v-else-if="mode === 'forgot' || mode === 'reset'" class="switch-copy">
          想起密码？
          <button type="button" class="inline-mode-link" @click="switchMode('login')">
            返回登录
          </button>
        </p>
      </form>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/js/api'
import { useAuthStore } from '@/js/stores/auth'
import { useUiStore } from '@/js/stores/ui'

const router = useRouter()
const auth = useAuthStore()
const ui = useUiStore()
const mode = ref('login')
const error = ref('')
const notice = ref('')
const submitting = ref(false)
const codeSending = ref(false)
const codeCooldown = ref(0)
const rememberMe = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
let cooldownTimer = null
const REMEMBER_EMAIL_KEY = 'sf6-login-email'
const form = reactive({
  username: '',
  email: '',
  code: '',
  password: '',
  confirmPassword: '',
  resetToken: '',
})

const modeTitle = computed(() => ({
  login: '登录',
  register: '注册',
  forgot: '找回密码',
  reset: '重置密码',
})[mode.value])

const modeSubtitle = computed(() => mode.value === 'forgot' || mode.value === 'reset'
  ? '验证注册邮箱后设置新密码。'
  : '登录后可以上传连招、提交反馈，并保留你的训练记录。')

const submitText = computed(() => ({
  login: '登录',
  register: '注册',
  forgot: '验证邮箱',
  reset: '重置密码',
})[mode.value])

const codeButtonText = computed(() => codeCooldown.value > 0
  ? `重新发送 (${codeCooldown.value}s)`
  : '发送验证码')

function switchMode(nextMode, options = {}) {
  mode.value = nextMode
  error.value = ''
  notice.value = ''
  if (!options.keepForm) resetForm()
  showPassword.value = false
  showConfirmPassword.value = false
}

function resetForm() {
  form.username = ''
  form.email = ''
  form.code = ''
  form.password = ''
  form.confirmPassword = ''
  form.resetToken = ''
}

function validateUsername(username) {
  const value = username.trim()
  if (value.length < 3 || value.length > 20) return '用户名长度需为 3-20 个字符'
  if (!/^[A-Za-z0-9_-]+$/.test(value)) return '用户名只能包含字母、数字、下划线或短横线'
  return ''
}

async function handleSubmit() {
  if (submitting.value) return
  submitting.value = true
  error.value = ''
  notice.value = ''
  try {
    if (mode.value === 'login') {
      if (!form.email.trim()) throw new Error('请输入注册邮箱')
      const user = await api.login(form.email, form.password)
      syncRememberedEmail()
      auth.login(user)
      resetForm()
      await router.push('/')
      return
    }

    if (mode.value === 'register') {
      const usernameError = validateUsername(form.username)
      if (usernameError) throw new Error(usernameError)
      if (form.password.length < 8 || form.password.length > 128) throw new Error('密码长度需为 8-128 个字符')
      const user = await api.register({
        username: form.username,
        email: form.email,
        password: form.password,
      })
      auth.login(user)
      resetForm()
      await router.push('/')
      return
    }

    if (mode.value === 'forgot') {
      const result = await api.verifyPasswordResetCode({
        email: form.email,
        code: form.code,
      })
      form.resetToken = result.resetToken
      form.password = ''
      form.confirmPassword = ''
      switchMode('reset', { keepForm: true })
      notice.value = '验证通过，请设置新密码'
      return
    }

    if (!form.password || form.password !== form.confirmPassword) throw new Error('两次输入的密码不一致')
    if (form.password.length < 8 || form.password.length > 128) throw new Error('密码长度需为 8-128 个字符')
    await api.resetPassword({
      email: form.email,
      resetToken: form.resetToken,
      password: form.password,
    })
    resetForm()
    mode.value = 'login'
    notice.value = '密码已重置，请重新登录'
  } catch (err) {
    error.value = err.message || '操作失败，请重试'
  } finally {
    submitting.value = false
  }
}

function syncRememberedEmail() {
  if (rememberMe.value) {
    localStorage.setItem(REMEMBER_EMAIL_KEY, form.email.trim())
  } else {
    localStorage.removeItem(REMEMBER_EMAIL_KEY)
  }
}

async function sendCode() {
  if (codeCooldown.value > 0 || codeSending.value) return
  codeSending.value = true
  error.value = ''
  notice.value = ''
  try {
    await api.sendPasswordResetCode(form.email)
    notice.value = '如果该邮箱已注册，重置验证码将发送到你的邮箱'
    startCooldown()
  } catch (err) {
    error.value = err.message || '验证码发送失败'
  } finally {
    codeSending.value = false
  }
}

function startCooldown() {
  codeCooldown.value = 60
  clearInterval(cooldownTimer)
  cooldownTimer = setInterval(() => {
    codeCooldown.value -= 1
    if (codeCooldown.value <= 0) clearInterval(cooldownTimer)
  }, 1000)
}

async function logout() {
  submitting.value = true
  try {
    await auth.logout()
    await ui.alertDialog({
      title: '已退出登录',
      message: '你可以继续浏览角色数据。',
      tone: 'success',
    })
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  const rememberedEmail = localStorage.getItem(REMEMBER_EMAIL_KEY) || ''
  if (rememberedEmail) {
    form.email = rememberedEmail
    rememberMe.value = true
  }
})

onUnmounted(() => clearInterval(cooldownTimer))
</script>

<style scoped>
.login-page {
  display: grid;
  gap: 18px;
  width: min(100%, 520px);
  margin-inline: auto;
  padding-top: 8px;
}

.back-link,
.login-card,
.form-field,
.remember-row,
.inline-mode-link,
.return-login-link,
.forgot-link,
.form-error,
.form-notice,
.account-card small {
  font-family: 'JetBrains Mono';
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
  color: #e8f7ff;
}

.back-link .material-symbols-outlined {
  font-size: 17px;
}

.login-panel {
  display: grid;
}

.login-panel,
.account-panel {
  width: 100%;
  padding: 36px;
  color: var(--ink-text);
  background: var(--ink-surface);
  border: 1px solid #242b38;
  border-radius: 6px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.28);
}

.login-card,
.account-panel {
  display: grid;
  gap: 18px;
}

.card-head {
  display: grid;
  gap: 8px;
  text-align: center;
}

.card-head h1 {
  margin: 0;
  color: var(--ink-text);
  font-family: inherit;
  font-size: 34px;
  font-weight: 900;
  line-height: 1.1;
}

.card-head p {
  margin: 0;
  color: var(--ink-text-soft);
  font-size: 16px;
  line-height: 1.6;
}

.form-field {
  display: grid;
  gap: 8px;
  color: var(--ink-text-soft);
  font-size: 14px;
  font-weight: 700;
}

.form-field input,
.password-control input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  color: var(--ink-text);
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  outline: none;
  font-size: 15px;
}

.form-field input::placeholder,
.password-control input::placeholder {
  color: #748196;
}

.form-field input:focus,
.password-control input:focus {
  border-color: var(--accent-red);
  box-shadow: 0 0 0 3px rgba(91, 214, 199, 0.14);
}

.field-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.password-control {
  position: relative;
  display: block;
}

.password-control input {
  padding-right: 46px;
}

.password-toggle {
  position: absolute;
  top: 50%;
  right: 10px;
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  color: var(--ink-muted);
  background: transparent;
  border: 0;
  border-radius: 4px;
  transform: translateY(-50%);
}

.password-toggle:hover {
  color: var(--ink-text);
  background: var(--ink-surface-high);
}

.password-toggle .material-symbols-outlined {
  font-size: 22px;
}

.remember-row {
  width: fit-content;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #cbd6e6;
  font-size: 14px;
}

.remember-row input {
  width: 18px;
  height: 18px;
  accent-color: var(--accent-red);
}

.inline-mode-link,
.return-login-link,
.forgot-link {
  width: fit-content;
  display: inline-flex;
  align-items: center;
  padding: 0;
  color: var(--accent-red-hot);
  background: transparent;
  border: 0;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
}

.forgot-link {
  align-self: center;
  justify-self: end;
  flex: 0 0 auto;
  line-height: 1;
}

.return-login-link {
  justify-self: start;
  margin-bottom: -2px;
}

.inline-mode-link:hover,
.return-login-link:hover,
.forgot-link:hover {
  color: #e8fffb;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.inline-mode-link:focus-visible,
.return-login-link:focus-visible,
.forgot-link:focus-visible,
.password-toggle:focus-visible,
.code-btn:focus-visible,
.submit-btn:focus-visible {
  outline: 2px solid rgba(91, 214, 199, 0.72);
  outline-offset: 4px;
}

.email-row {
  display: grid;
  gap: 10px;
}

.email-row.withCode {
  grid-template-columns: minmax(0, 1fr) auto;
}

.code-btn {
  min-width: 120px;
  min-height: 42px;
  color: var(--ink-text-soft);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  background: var(--ink-surface-high);
  font-family: 'JetBrains Mono';
  font-size: 12px;
  font-weight: 700;
}

.code-btn:hover:not(:disabled) {
  color: var(--ink-text);
  background: #172131;
  border-color: #526174;
}

.code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.reset-email-chip {
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  color: var(--ink-text-soft);
  background: var(--ink-surface-high);
  font-family: 'JetBrains Mono';
  font-size: 12px;
  padding: 10px 12px;
  overflow-wrap: anywhere;
}

.form-error,
.form-notice {
  margin: 0;
  padding: 10px 12px;
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.5;
}

.form-error {
  color: var(--state-error);
  background: rgba(255, 86, 105, 0.11);
  border: 1px solid rgba(255, 118, 135, 0.28);
}

.form-notice {
  color: var(--accent-cyan-soft);
  background: rgba(91, 214, 199, 0.1);
  border: 1px solid rgba(91, 214, 199, 0.28);
}

.submit-btn,
.danger-action,
.ghost-action {
  min-height: 58px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 18px;
  border: 0;
  border-radius: 4px;
  font-family: inherit;
  font-size: 15px;
  font-weight: 900;
  transition: background-color 160ms ease, color 160ms ease, opacity 160ms ease;
}

.submit-btn {
  margin-top: 2px;
  color: #190607;
  background: var(--accent-red);
}

.submit-btn:hover:not(:disabled) {
  background: var(--accent-red-hot);
}

.submit-btn:disabled,
.danger-action:disabled {
  opacity: 0.58;
  cursor: wait;
}

.account-card {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 16px;
  padding-top: 6px;
}

.account-avatar {
  width: 72px;
  height: 72px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  background: var(--ink-surface-high);
}

.account-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.account-card h2 {
  margin: 0;
  color: var(--ink-text);
  font-family: inherit;
  font-size: 24px;
  font-weight: 900;
  line-height: 1;
}

.account-card p {
  margin: 8px 0 0;
  color: var(--ink-text-soft);
}

.account-card small {
  display: inline-block;
  margin-top: 8px;
  color: var(--accent-red-hot);
  font-size: 11px;
}

.account-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.ghost-action {
  color: var(--ink-text-soft);
  background: var(--ink-surface-high);
}

.danger-action {
  color: #fff;
  background: #d32f3f;
}

.switch-copy {
  margin: 14px 0 0;
  color: var(--ink-text-soft);
  text-align: center;
  font-size: 15px;
}

@media (max-width: 560px) {
  .login-page {
    padding-inline: 0;
  }

  .login-panel,
  .account-panel {
    padding: 28px 22px;
  }

  .email-row.withCode {
    grid-template-columns: 1fr;
  }

  .account-card {
    grid-template-columns: 1fr;
  }

  .account-actions > * {
    width: 100%;
  }
}
</style>
