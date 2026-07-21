<template>
  <div class="profile-page">
    <header class="profile-head">
      <router-link to="/" class="back-link">
        <span class="material-symbols-outlined">arrow_back</span>
        返回角色
      </router-link>
      <span>ACCOUNT</span>
      <h1>个人资料</h1>
      <p>管理你的账号基础信息。邮箱和密码修改需要通过邮箱验证码确认。</p>
    </header>

    <section v-if="checkingSession" class="auth-required-panel">
      <span class="material-symbols-outlined">sync</span>
      <h2>正在确认登录状态</h2>
      <p>马上就好。</p>
    </section>

    <section v-else-if="!auth.isLoggedIn" class="auth-required-panel">
      <span class="material-symbols-outlined">lock</span>
      <h2>登录后修改</h2>
      <p>个人资料需要读取你的账号信息，请先登录。</p>
      <router-link to="/login">前往登录</router-link>
    </section>

    <section v-else class="profile-panel">
      <div class="profile-summary">
        <span class="profile-avatar">{{ avatarText }}</span>
        <div>
          <strong>{{ auth.user?.username || '已登录用户' }}</strong>
          <small>{{ auth.user?.email || '未记录邮箱' }}</small>
        </div>
      </div>

      <div class="profile-list">
        <article class="profile-row">
          <div>
            <span>用户名</span>
            <strong>{{ auth.user?.username || '未设置' }}</strong>
          </div>
          <button type="button" @click="openDialog('username')">修改</button>
        </article>

        <article class="profile-row">
          <div>
            <span>邮箱</span>
            <strong>{{ auth.user?.email || '未设置' }}</strong>
          </div>
          <button type="button" @click="openDialog('email')">修改</button>
        </article>

        <article class="profile-row">
          <div>
            <span>密码</span>
            <strong>********</strong>
          </div>
          <button type="button" @click="openDialog('password')">修改</button>
        </article>
      </div>
    </section>

    <Teleport to="body">
      <transition name="profile-dialog">
        <div v-if="dialogOpen" class="profile-dialog-backdrop" @click.self="closeDialog">
          <form class="profile-dialog" role="dialog" aria-modal="true" :aria-labelledby="dialogTitleId" @submit.prevent="submitDialog">
            <header>
              <span class="material-symbols-outlined">{{ dialogIcon }}</span>
              <div>
                <h2 :id="dialogTitleId">{{ dialogTitle }}</h2>
                <p>{{ dialogDescription }}</p>
              </div>
              <button type="button" class="icon-close" aria-label="关闭弹窗" @click="closeDialog">
                <span class="material-symbols-outlined">close</span>
              </button>
            </header>

            <div v-if="dialogMode === 'username'" class="dialog-fields">
              <label class="form-field">
                新用户名
                <input v-model.trim="usernameForm.username" type="text" autocomplete="username" placeholder="3-20 个字符" />
              </label>
            </div>

            <div v-else-if="dialogMode === 'email'" class="dialog-fields">
              <label class="form-field">
                当前邮箱
                <div class="code-row">
                  <input :value="auth.user?.email || ''" type="email" disabled />
                  <button type="button" :disabled="emailCurrentSending || emailCurrentCooldown > 0" @click="sendCurrentEmailCode">
                    {{ currentEmailCodeText }}
                  </button>
                </div>
              </label>

              <label class="form-field">
                当前邮箱验证码
                <input v-model.trim="emailForm.currentEmailCode" type="text" inputmode="numeric" maxlength="6" placeholder="输入 6 位验证码" />
              </label>

              <label class="form-field">
                新邮箱
                <div class="code-row">
                  <input v-model.trim="emailForm.newEmail" type="email" autocomplete="email" placeholder="请输入新邮箱" />
                  <button type="button" :disabled="emailNewSending || emailNewCooldown > 0 || !emailForm.newEmail.trim()" @click="sendNewEmailCode">
                    {{ newEmailCodeText }}
                  </button>
                </div>
              </label>

              <label class="form-field">
                新邮箱验证码
                <input v-model.trim="emailForm.newEmailCode" type="text" inputmode="numeric" maxlength="6" placeholder="输入 6 位验证码" />
              </label>
            </div>

            <div v-else class="dialog-fields">
              <label class="form-field">
                邮箱验证
                <div class="code-row">
                  <input :value="auth.user?.email || ''" type="email" disabled />
                  <button type="button" :disabled="passwordCodeSending || passwordCodeCooldown > 0" @click="sendPasswordCode">
                    {{ passwordCodeText }}
                  </button>
                </div>
              </label>

              <label class="form-field">
                邮箱验证码
                <input v-model.trim="passwordForm.emailCode" type="text" inputmode="numeric" maxlength="6" placeholder="输入 6 位验证码" />
              </label>

              <label class="form-field">
                新密码
                <span class="password-control">
                  <input
                    v-model="passwordForm.password"
                    :type="showPassword ? 'text' : 'password'"
                    autocomplete="new-password"
                    placeholder="至少 8 位"
                  />
                  <button type="button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword">
                    <span class="material-symbols-outlined">{{ showPassword ? 'visibility_off' : 'visibility' }}</span>
                  </button>
                </span>
              </label>

              <label class="form-field">
                确认新密码
                <span class="password-control">
                  <input
                    v-model="passwordForm.confirmPassword"
                    :type="showConfirmPassword ? 'text' : 'password'"
                    autocomplete="new-password"
                    placeholder="再次输入新密码"
                  />
                  <button type="button" :aria-label="showConfirmPassword ? '隐藏确认密码' : '显示确认密码'" @click="showConfirmPassword = !showConfirmPassword">
                    <span class="material-symbols-outlined">{{ showConfirmPassword ? 'visibility_off' : 'visibility' }}</span>
                  </button>
                </span>
              </label>
            </div>

            <p v-if="error" class="form-error">{{ error }}</p>
            <p v-if="notice" class="form-notice">{{ notice }}</p>

            <footer>
              <button type="button" class="ghost-btn" @click="closeDialog">取消</button>
              <button type="submit" class="submit-btn" :disabled="submitting">
                {{ submitting ? '修改中...' : '确认修改' }}
              </button>
            </footer>
          </form>
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/js/api'
import { useAuthStore } from '@/js/stores/auth'
import { useRealtimeStore } from '@/js/stores/realtime'

const router = useRouter()
const auth = useAuthStore()
const realtime = useRealtimeStore()
const checkingSession = ref(false)
const dialogOpen = ref(false)
const dialogMode = ref('username')
const submitting = ref(false)
const error = ref('')
const notice = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const emailCurrentSending = ref(false)
const emailNewSending = ref(false)
const passwordCodeSending = ref(false)
const emailCurrentCooldown = ref(0)
const emailNewCooldown = ref(0)
const passwordCodeCooldown = ref(0)
const dialogTitleId = `profile-dialog-title-${Math.random().toString(36).slice(2)}`
const timers = new Set()

const usernameForm = reactive({ username: '' })
const emailForm = reactive({
  currentEmailCode: '',
  newEmail: '',
  newEmailCode: '',
})
const passwordForm = reactive({
  emailCode: '',
  password: '',
  confirmPassword: '',
})

const avatarText = computed(() => {
  const name = (auth.user?.username || '用户').replace(/\s+/g, '')
  return Array.from(name).slice(0, 2).join('') || '用户'
})
const dialogTitle = computed(() => ({
  username: '修改用户名',
  email: '修改邮箱',
  password: '修改密码',
})[dialogMode.value])
const dialogDescription = computed(() => ({
  username: '用户名会展示在你的上传内容和账号菜单里。',
  email: '先验证当前邮箱，再验证新邮箱，全部通过后才能保存。',
  password: '先验证当前邮箱，再设置新密码。',
})[dialogMode.value])
const dialogIcon = computed(() => ({
  username: 'badge',
  email: 'alternate_email',
  password: 'lock_reset',
})[dialogMode.value])
const currentEmailCodeText = computed(() => codeText(emailCurrentCooldown.value, '发送验证码'))
const newEmailCodeText = computed(() => codeText(emailNewCooldown.value, '验证新邮箱'))
const passwordCodeText = computed(() => codeText(passwordCodeCooldown.value, '发送验证码'))
onMounted(async () => {
  if (auth.isLoggedIn) return
  checkingSession.value = true
  try {
    await auth.hydrate()
  } finally {
    checkingSession.value = false
  }
  if (!auth.isLoggedIn) router.push('/login').catch(() => {})
})

onUnmounted(() => {
  timers.forEach(timer => clearInterval(timer))
  timers.clear()
})

watch(() => realtime.lastEvent, event => {
  if (!auth.isLoggedIn) return
  const areas = event?.areas || []
  if (areas.includes('profile') || areas.includes('users')) {
    auth.hydrate()
  }
})

function openDialog(mode) {
  dialogMode.value = mode
  error.value = ''
  notice.value = ''
  submitting.value = false
  showPassword.value = false
  showConfirmPassword.value = false
  usernameForm.username = auth.user?.username || ''
  emailForm.currentEmailCode = ''
  emailForm.newEmail = ''
  emailForm.newEmailCode = ''
  passwordForm.emailCode = ''
  passwordForm.password = ''
  passwordForm.confirmPassword = ''
  dialogOpen.value = true
}

function closeDialog() {
  if (submitting.value) return
  dialogOpen.value = false
}

async function submitDialog() {
  if (submitting.value) return
  submitting.value = true
  error.value = ''
  notice.value = ''
  try {
    if (dialogMode.value === 'username') {
      const user = await api.updateProfileUsername(usernameForm.username)
      auth.updateUser(user)
      finishSuccess('用户名已修改')
      return
    }

    if (dialogMode.value === 'email') {
      validateCode(emailForm.currentEmailCode, '请输入当前邮箱验证码')
      validateCode(emailForm.newEmailCode, '请输入新邮箱验证码')
      const user = await api.updateProfileEmail(emailForm)
      auth.updateUser(user)
      finishSuccess('邮箱已修改')
      return
    }

    validateCode(passwordForm.emailCode, '请输入邮箱验证码')
    if (!passwordForm.password || passwordForm.password !== passwordForm.confirmPassword) throw new Error('两次输入的密码不一致')
    if (passwordForm.password.length < 8 || passwordForm.password.length > 128) throw new Error('密码长度需为 8-128 个字符')
    const user = await api.updateProfilePassword(passwordForm)
    auth.updateUser(user)
    finishSuccess('密码已修改')
  } catch (err) {
    error.value = err.message || '修改失败，请重试'
  } finally {
    submitting.value = false
  }
}

async function sendCurrentEmailCode() {
  await sendCode({
    sending: emailCurrentSending,
    cooldown: emailCurrentCooldown,
    action: () => api.sendProfileCurrentEmailCode(),
    message: '当前邮箱验证码已发送，请检查邮箱',
  })
}

async function sendNewEmailCode() {
  await sendCode({
    sending: emailNewSending,
    cooldown: emailNewCooldown,
    action: () => api.sendProfileNewEmailCode(emailForm.newEmail),
    message: '新邮箱验证码已发送，请检查邮箱',
  })
}

async function sendPasswordCode() {
  await sendCode({
    sending: passwordCodeSending,
    cooldown: passwordCodeCooldown,
    action: () => api.sendProfilePasswordCode(),
    message: '邮箱验证码已发送，请检查邮箱',
  })
}

async function sendCode({ sending, cooldown, action, message }) {
  if (sending.value || cooldown.value > 0) return
  sending.value = true
  error.value = ''
  notice.value = ''
  try {
    await action()
    notice.value = message
    startCooldown(cooldown)
  } catch (err) {
    error.value = err.message || '验证码发送失败'
  } finally {
    sending.value = false
  }
}

function startCooldown(target) {
  target.value = 60
  const timer = setInterval(() => {
    target.value -= 1
    if (target.value > 0) return
    clearInterval(timer)
    timers.delete(timer)
  }, 1000)
  timers.add(timer)
}

function codeText(cooldown, text) {
  return cooldown > 0 ? `重新发送 (${cooldown}s)` : text
}

function validateCode(value, message) {
  if (!/^\d{6}$/.test(String(value || '').trim())) throw new Error(message)
}

function finishSuccess(message) {
  notice.value = message
  setTimeout(() => {
    if (notice.value === message) dialogOpen.value = false
  }, 450)
}
</script>

<style scoped>
.profile-page {
  display: grid;
  gap: 24px;
  width: min(100%, 860px);
  margin-inline: auto;
}

.profile-head {
  display: grid;
  gap: 8px;
  padding-left: 58px;
}

.profile-head > span,
.back-link,
.profile-summary small,
.profile-row span,
.profile-row button,
.form-field,
.form-error,
.form-notice,
.profile-dialog footer button {
  font-family: 'JetBrains Mono';
}

.profile-head > span {
  color: var(--accent-red);
  font-size: 12px;
}

.profile-head h1 {
  margin: 0;
  color: var(--ink-text);
  font-size: 40px;
  font-weight: 900;
  line-height: 1.08;
}

.profile-head p {
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

.profile-panel,
.auth-required-panel {
  background: var(--ink-surface);
  border: 1px solid var(--ink-line);
  border-radius: 6px;
}

.profile-panel {
  overflow: hidden;
}

.profile-summary {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 14px;
  padding: 20px;
  background: var(--ink-surface-low);
  border-bottom: 1px solid var(--ink-line);
}

.profile-avatar {
  width: 50px;
  height: 50px;
  display: inline-grid;
  place-items: center;
  color: var(--ink-text);
  background: linear-gradient(135deg, #c7564b, #8f3f55);
  border-radius: 999px;
  font-weight: 900;
}

.profile-summary strong,
.profile-summary small {
  min-width: 0;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-summary strong {
  color: var(--ink-text);
  font-size: 20px;
  font-weight: 900;
}

.profile-summary small {
  margin-top: 4px;
  color: #8f9db1;
  font-size: 12px;
}

.profile-list {
  display: grid;
}

.profile-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  min-height: 86px;
  padding: 18px 20px;
  border-top: 1px solid var(--ink-surface-high);
}

.profile-row:first-child {
  border-top: 0;
}

.profile-row span,
.profile-row strong {
  display: block;
}

.profile-row span {
  color: #8f9db1;
  font-size: 12px;
}

.profile-row strong {
  margin-top: 6px;
  color: var(--ink-text);
  font-size: 18px;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.profile-row button,
.auth-required-panel a {
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  color: #190607;
  background: var(--accent-red);
  border: 0;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 900;
}

.profile-row button:hover,
.auth-required-panel a:hover {
  background: var(--accent-red-hot);
}

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

.auth-required-panel .material-symbols-outlined {
  color: var(--accent-red);
  font-size: 42px;
}

.auth-required-panel h2 {
  margin: 0;
  color: var(--ink-text);
  font-size: 26px;
  font-weight: 900;
}

.auth-required-panel p {
  margin: 0;
  max-width: 46ch;
  line-height: 1.65;
}

.profile-dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 145;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(3, 7, 13, 0.78);
  backdrop-filter: blur(12px);
}

.profile-dialog {
  width: min(520px, 100%);
  display: grid;
  gap: 18px;
  padding: 22px;
  color: var(--ink-text);
  background: var(--ink-surface);
  border: 1px solid var(--ink-line-strong);
  border-radius: 8px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.48);
}

.profile-dialog header {
  display: grid;
  grid-template-columns: 42px 1fr 34px;
  gap: 12px;
  align-items: start;
}

.profile-dialog header > .material-symbols-outlined {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  color: var(--accent-red);
  background: rgba(91, 214, 199, 0.12);
  border: 1px solid rgba(91, 214, 199, 0.32);
  border-radius: 6px;
}

.profile-dialog h2 {
  margin: 0;
  color: var(--ink-text);
  font-size: 24px;
  font-weight: 900;
}

.profile-dialog p {
  margin: 6px 0 0;
  color: var(--ink-text-soft);
  line-height: 1.55;
}

.icon-close {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  color: var(--ink-text-soft);
  border: 1px solid var(--ink-line);
  border-radius: 999px;
}

.icon-close:hover {
  color: var(--ink-text);
  border-color: var(--accent-red);
}

.dialog-fields {
  display: grid;
  gap: 14px;
}

.form-field {
  display: grid;
  gap: 8px;
  color: var(--ink-text-soft);
  font-size: 13px;
  font-weight: 800;
}

.form-field input {
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

.form-field input:disabled {
  color: var(--ink-muted);
  background: #101621;
}

.form-field input::placeholder {
  color: #748196;
}

.form-field input:focus {
  border-color: var(--accent-red);
  box-shadow: 0 0 0 3px rgba(91, 214, 199, 0.14);
}

.code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.code-row button {
  min-width: 122px;
  color: var(--ink-text-soft);
  background: var(--ink-surface-high);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  font-weight: 800;
}

.code-row button:hover:not(:disabled) {
  color: var(--ink-text);
  border-color: var(--accent-red);
}

.code-row button:disabled,
.submit-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.password-control {
  position: relative;
  display: block;
}

.password-control input {
  padding-right: 46px;
}

.password-control button {
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

.password-control button:hover {
  color: var(--ink-text);
  background: var(--ink-surface-high);
}

.password-control .material-symbols-outlined {
  font-size: 21px;
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

.profile-dialog footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.profile-dialog footer button {
  min-height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 18px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 900;
}

.ghost-btn {
  color: var(--ink-text-soft);
  background: var(--ink-surface-high);
}

.submit-btn {
  color: #190607;
  background: var(--accent-red);
}

.profile-dialog-enter-active,
.profile-dialog-leave-active {
  transition: opacity 0.16s ease;
}

.profile-dialog-enter-from,
.profile-dialog-leave-to {
  opacity: 0;
}

@media (max-width: 560px) {
  .profile-head {
    padding-left: 46px;
  }

  .profile-head h1 {
    font-size: 32px;
  }

  .profile-row {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 16px;
  }

  .profile-row button,
  .profile-dialog footer button {
    width: 100%;
  }

  .profile-dialog {
    padding: 18px;
  }

  .profile-dialog header {
    grid-template-columns: 1fr 34px;
  }

  .profile-dialog header > .material-symbols-outlined {
    display: none;
  }

  .code-row {
    grid-template-columns: 1fr;
  }

  .profile-dialog footer {
    flex-direction: column-reverse;
  }
}
</style>
