<template>
  <section class="auth-card">
    <header class="auth-head">
      <h1 class="brand">知己</h1>
      <p class="subtitle">{{ subtitle }}</p>
    </header>

    <form class="auth-form" novalidate @submit.prevent="submit">
      <!-- 用户名 -->
      <div class="field">
        <div class="field-head">
          <label class="field-label" for="username">用户名</label>
          <span class="field-hint">{{ usernameLength }}/{{ USERNAME_MAX }}</span>
        </div>
        <input
          id="username"
          v-model="form.username"
          class="input"
          type="text"
          :maxlength="USERNAME_MAX"
          placeholder="请输入用户名，最多 10 个字符"
          autocomplete="username"
          @input="clearMessage"
        />
      </div>

      <!-- 密码：登录 / 注册共用 -->
      <div v-if="!isPassword" class="field">
        <label class="field-label" for="password">密码</label>
        <input
          id="password"
          v-model="form.password"
          class="input"
          type="password"
          :placeholder="isLogin ? '请输入密码' : '设置一个密码'"
          :autocomplete="isLogin ? 'current-password' : 'new-password'"
          @input="clearMessage"
        />
      </div>

      <!-- 密码：修改密码模式 -->
      <template v-else>
        <div class="field">
          <label class="field-label" for="oldPassword">原密码</label>
          <input
            id="oldPassword"
            v-model="form.oldPassword"
            class="input"
            type="password"
            placeholder="请输入原密码"
            autocomplete="current-password"
            @input="clearMessage"
          />
        </div>
        <div class="field">
          <label class="field-label" for="newPassword">新密码</label>
          <input
            id="newPassword"
            v-model="form.newPassword"
            class="input"
            type="password"
            placeholder="请输入新密码"
            autocomplete="new-password"
            @input="clearMessage"
          />
        </div>
        <div class="field">
          <label class="field-label" for="confirmPassword">确认新密码</label>
          <input
            id="confirmPassword"
            v-model="form.confirmPassword"
            class="input"
            type="password"
            placeholder="请再次输入新密码"
            autocomplete="new-password"
            @input="clearMessage"
          />
        </div>
      </template>

      <p v-if="message.text" class="message" :class="message.type" role="status">{{ message.text }}</p>

      <button class="btn-primary" type="submit" :disabled="loading">
        {{ loading ? '处理中…' : submitText }}
      </button>

      <div class="switch">
        <template v-if="currentUser && isLogin">
          <span class="switch-text">已登录：{{ currentUser }}</span>
          <button class="btn-link" type="button" @click="logout">退出登录</button>
        </template>
        <template v-else-if="isLogin">
          <button class="btn-link" type="button" @click="switchTo('register')">注册</button>
          <span class="divider">·</span>
          <button class="btn-link" type="button" @click="switchTo('password')">修改密码</button>
        </template>
        <template v-else>
          <button class="btn-link" type="button" @click="switchTo('login')">返回登录</button>
          <span class="divider">·</span>
          <button class="btn-link" type="button" @click="switchTo(isRegister ? 'password' : 'register')">
            {{ isRegister ? '修改密码' : '注册' }}
          </button>
        </template>
      </div>
    </form>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { changePassword, loginUser, registerUser } from '../api.js'
import { push } from '../router.js'
import { currentUser, signIn, signOut } from '../session.js'

const USERNAME_MAX = 10

/** login：登录（默认）；register：注册；password：修改密码 */
const mode = ref('login')
const loading = ref(false)
const message = reactive({ type: '', text: '' })

const form = reactive({
  username: currentUser.value,
  password: '',
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const isLogin = computed(() => mode.value === 'login')
const isRegister = computed(() => mode.value === 'register')
const isPassword = computed(() => mode.value === 'password')
const usernameLength = computed(() => charLength(form.username))
const subtitle = computed(() => {
  if (isLogin.value) return '欢迎回来'
  return isRegister.value ? '创建一个账号' : '修改登录密码'
})
const submitText = computed(() => {
  if (isLogin.value) return '登 录'
  return isRegister.value ? '注 册' : '确认修改'
})

/** 按字符数计算（避免 emoji 被当成 2 个字符） */
function charLength(value) {
  return Array.from(value || '').length
}

function setMessage(type, text) {
  message.type = type
  message.text = text
}

function clearMessage() {
  message.type = ''
  message.text = ''
}

/** 切换卡片模式：登录 / 注册 / 修改密码 */
function switchTo(next) {
  mode.value = next
  form.password = ''
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  clearMessage()
}

/** 退出登录，回到未登录状态 */
function logout() {
  signOut()
  form.password = ''
  setMessage('success', '已退出登录')
}

/** 与后端一致的用户名校验 */
function validateUsername() {
  const username = form.username.trim()
  if (!username) return '用户名不能为空'
  if (charLength(username) > USERNAME_MAX) return `用户名不能超过 ${USERNAME_MAX} 个字符`
  return ''
}

async function submit() {
  if (loading.value) return
  clearMessage()

  const usernameError = validateUsername()
  if (usernameError) return setMessage('error', usernameError)

  const username = form.username.trim()

  if (isLogin.value) {
    if (!form.password) return setMessage('error', '密码不能为空')
    await send(() => loginUser(username, form.password), '登录成功')
    return
  }

  if (isRegister.value) {
    if (!form.password) return setMessage('error', '密码不能为空')
    await send(() => registerUser(username, form.password), '注册成功')
    return
  }

  if (!form.oldPassword) return setMessage('error', '请输入原密码')
  if (!form.newPassword) return setMessage('error', '请输入新密码')
  if (form.newPassword !== form.confirmPassword) return setMessage('error', '两次输入的新密码不一致')
  await send(() => changePassword(username, form.oldPassword, form.newPassword), '密码修改成功')
}

async function send(request, successText) {
  loading.value = true
  try {
    const result = await request()
    if (result.code !== 200) {
      setMessage('error', result.msg || '操作失败')
      return
    }

    const username = form.username.trim()
    form.password = ''
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''

    if (isLogin.value) {
      // 登录成功 → 跳转首页
      signIn(username)
      push('/home')
      return
    }

    if (isRegister.value) {
      // 注册成功 → 跳回登录页，用新账号登录
      switchTo('login')
      setMessage('success', `${result.msg || '注册成功'}，请登录`)
      return
    }

    setMessage('success', result.msg || successText)
  } catch (error) {
    setMessage('error', error.message || '操作失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 380px;
  padding: 32px 28px 24px;
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: 18px;
  box-shadow: 0 14px 40px rgba(91, 141, 239, 0.13), 0 2px 6px rgba(38, 48, 74, 0.04);
}

.auth-head {
  margin-bottom: 22px;
  text-align: center;
}

.brand {
  margin: 0;
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 8px;
  text-indent: 8px;
  color: #2b3856;
}

.subtitle {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--text-sub);
}

.field {
  margin-bottom: 14px;
}

.field-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.field-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--text-label);
}

.field-hint {
  font-size: 12px;
  color: #aab4c9;
}

.input {
  width: 100%;
  height: 42px;
  padding: 0 12px;
  font-size: 14px;
  color: var(--text-main);
  background: var(--input-bg);
  border: 1px solid var(--input-border);
  border-radius: 10px;
  outline: none;
  transition: border-color 0.18s, box-shadow 0.18s, background 0.18s;
}

.input::placeholder {
  color: var(--input-placeholder);
}

.input:focus {
  background: #fff;
  border-color: #9dbcfa;
  box-shadow: 0 0 0 3px var(--accent-glow);
}

.message {
  margin: 2px 0 12px;
  font-size: 13px;
  line-height: 1.5;
}

.message.error {
  color: var(--danger);
}

.message.success {
  color: var(--success);
}

.btn-primary {
  width: 100%;
  height: 44px;
  margin-top: 6px;
  font-size: 15px;
  letter-spacing: 2px;
  color: #fff;
  background: linear-gradient(135deg, #6ea3f5, #5b8def);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(91, 141, 239, 0.28);
  transition: filter 0.16s, box-shadow 0.16s, transform 0.16s;
}

.btn-primary:hover:not(:disabled) {
  filter: brightness(1.05);
  box-shadow: 0 8px 20px rgba(91, 141, 239, 0.34);
}

.btn-primary:active:not(:disabled) {
  transform: translateY(1px);
}

.btn-primary:disabled {
  cursor: not-allowed;
  opacity: 0.55;
  box-shadow: none;
}

.switch {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 16px;
  font-size: 13px;
}

.switch-text {
  color: var(--text-sub);
}

.divider {
  color: #c6cfe0;
}

.btn-link {
  padding: 0;
  font-size: 13px;
  color: var(--accent);
  background: none;
  border: none;
  cursor: pointer;
}

.btn-link:hover {
  color: var(--accent-dark);
  text-decoration: underline;
}
</style>
