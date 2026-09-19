<template>
  <div class="profile-page">
    <!-- 顶部：左上角返回 + 标题 -->
    <header class="bar">
      <button class="back-btn" type="button" aria-label="返回" @click="goBack">‹</button>
      <h1 class="bar-title">个人中心</h1>
      <span class="bar-side" aria-hidden="true"></span>
    </header>

    <div class="wrap">
      <!-- 未登录 -->
      <section v-if="!currentUser" class="card center">
        <p class="tip">请先登录后再进入个人中心</p>
        <button class="btn-primary" type="button" @click="goLogin">去登录</button>
      </section>

      <template v-else>
        <!-- 个人信息 -->
        <section class="card identity">
          <div class="avatar" aria-hidden="true">{{ avatarText }}</div>
          <div class="names">
            <p class="name">{{ currentUser }}</p>
            <p class="uid">知己号：{{ uid }}</p>
          </div>
        </section>

        <!-- 修改密码 -->
        <section class="card">
          <h2 class="card-title">修改密码</h2>
          <div class="field">
            <label class="field-label" for="oldPassword">原密码</label>
            <input id="oldPassword" v-model="pwd.oldPassword" class="input" type="password" placeholder="请输入原密码" autocomplete="current-password" @input="clearPwdMessage" />
          </div>
          <div class="field">
            <label class="field-label" for="newPassword">新密码</label>
            <input id="newPassword" v-model="pwd.newPassword" class="input" type="password" placeholder="请输入新密码" autocomplete="new-password" @input="clearPwdMessage" />
          </div>
          <div class="field">
            <label class="field-label" for="confirmPassword">确认新密码</label>
            <input id="confirmPassword" v-model="pwd.confirmPassword" class="input" type="password" placeholder="请再次输入新密码" autocomplete="new-password" @input="clearPwdMessage" />
          </div>
          <p v-if="pwdMessage.text" class="message" :class="pwdMessage.type">{{ pwdMessage.text }}</p>
          <button class="btn-primary wide" type="button" :disabled="pwdLoading" @click="submitPassword">
            {{ pwdLoading ? '提交中…' : '确认修改' }}
          </button>
        </section>

        <!-- 上传图片 -->
        <section class="card">
          <h2 class="card-title">上传图片</h2>
          <p class="tip">支持 jpg / png / webp，单张不超过 12MB。图片保存进数据库（<code>chou</code> 库），并展示在下面的照片墙。</p>
          <label class="upload-btn" :class="{ 'is-busy': uploading }">
            <input ref="fileInput" type="file" accept="image/*" :disabled="uploading" @change="handleFile" />
            <span>{{ uploading ? '上传中…' : '选择图片上传' }}</span>
          </label>
          <p v-if="uploadMessage.text" class="message" :class="uploadMessage.type">{{ uploadMessage.text }}</p>
        </section>

        <!-- 我的照片墙 -->
        <section class="card">
          <h2 class="card-title">我的照片墙<span class="count">{{ photos.length }} 张</span></h2>
          <div v-if="photos.length" class="grid">
            <figure v-for="photo in photos" :key="photo.id" class="cell">
              <img :src="photo.url" :alt="photo.filename" loading="lazy" draggable="false" />
              <figcaption>{{ photo.createTime }}</figcaption>
            </figure>
          </div>
          <p v-else class="empty">{{ loadingPhotos ? '加载中…' : '还没有上传过图片' }}</p>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { changePassword, listPhotos, uploadPhoto } from '../api.js'
import { currentUser } from '../session.js'
import { push } from '../router.js'

const photos = ref([])
const loadingPhotos = ref(false)
const uploading = ref(false)
const fileInput = ref(null)

const pwd = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdMessage = reactive({ type: '', text: '' })
const pwdLoading = ref(false)
const uploadMessage = reactive({ type: '', text: '' })

const uid = computed(() => (currentUser.value ? `zhiji_${currentUser.value}` : 'zhiji_0000'))
const avatarText = computed(() => (currentUser.value[0] || '知').toUpperCase())

function goBack() {
  push('/home')
}

function goLogin() {
  push('/login')
}

function clearPwdMessage() {
  pwdMessage.type = ''
  pwdMessage.text = ''
}

function clearUploadMessage() {
  uploadMessage.type = ''
  uploadMessage.text = ''
}

/** 读取当前用户的图片（存在 chou 库） */
async function loadPhotos() {
  if (!currentUser.value) return
  loadingPhotos.value = true
  try {
    const result = await listPhotos(currentUser.value)
    photos.value = result.code === 200 && Array.isArray(result.data) ? result.data : []
  } catch (error) {
    photos.value = []
  } finally {
    loadingPhotos.value = false
  }
}

async function submitPassword() {
  if (pwdLoading.value) return
  clearPwdMessage()

  if (!pwd.oldPassword) {
    pwdMessage.type = 'error'
    pwdMessage.text = '请输入原密码'
    return
  }
  if (!pwd.newPassword) {
    pwdMessage.type = 'error'
    pwdMessage.text = '请输入新密码'
    return
  }
  if (pwd.newPassword !== pwd.confirmPassword) {
    pwdMessage.type = 'error'
    pwdMessage.text = '两次输入的新密码不一致'
    return
  }

  pwdLoading.value = true
  try {
    const result = await changePassword(currentUser.value, pwd.oldPassword, pwd.newPassword)
    if (result.code === 200) {
      pwd.oldPassword = ''
      pwd.newPassword = ''
      pwd.confirmPassword = ''
      pwdMessage.type = 'success'
      pwdMessage.text = result.msg || '密码修改成功'
    } else {
      pwdMessage.type = 'error'
      pwdMessage.text = result.msg || '修改失败'
    }
  } catch (error) {
    pwdMessage.type = 'error'
    pwdMessage.text = error.message || '修改失败'
  } finally {
    pwdLoading.value = false
  }
}

async function handleFile(event) {
  const input = event.target
  const file = input.files && input.files[0]
  if (!file || uploading.value) return

  clearUploadMessage()
  if (!file.type.startsWith('image/')) {
    uploadMessage.type = 'error'
    uploadMessage.text = '只能上传图片文件'
    input.value = ''
    return
  }
  if (file.size > 12 * 1024 * 1024) {
    uploadMessage.type = 'error'
    uploadMessage.text = '图片不能超过 12MB'
    input.value = ''
    return
  }

  uploading.value = true
  try {
    const result = await uploadPhoto(file, currentUser.value)
    if (result.code === 200) {
      uploadMessage.type = 'success'
      uploadMessage.text = '上传成功，已保存到照片墙'
      await loadPhotos()
    } else {
      uploadMessage.type = 'error'
      uploadMessage.text = result.msg || '上传失败'
    }
  } catch (error) {
    uploadMessage.type = 'error'
    uploadMessage.text = error.message || '上传失败'
  } finally {
    uploading.value = false
    input.value = ''
  }
}

onMounted(() => {
  document.title = '知己 · 个人中心'
  loadPhotos()
})
</script>

<style scoped>
.profile-page {
  min-height: 100dvh;
}

/* 顶部栏 */
.bar {
  position: sticky;
  top: 0;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.86);
  border-bottom: 1px solid rgba(231, 237, 249, 0.95);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  font-size: 26px;
  line-height: 1;
  color: #2b3856;
  cursor: pointer;
  background: none;
  border: none;
  border-radius: 50%;
  transition: background 0.2s ease;
}

.back-btn:hover {
  background: rgba(91, 141, 239, 0.1);
}

.bar-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #2b3856;
}

.bar-side {
  width: 36px;
}

/* 内容 */
.wrap {
  max-width: 620px;
  margin: 0 auto;
  padding: 16px 12px 72px;
}

.card {
  padding: 18px 18px 20px;
  margin-bottom: 14px;
  background: #fff;
  border: 1px solid var(--card-border);
  border-radius: 16px;
  box-shadow: 0 10px 28px rgba(64, 96, 160, 0.08);
}

.card.center {
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-items: center;
  padding: 40px 18px;
}

.card-title {
  display: flex;
  gap: 8px;
  align-items: baseline;
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
  color: #2b3856;
}

.count {
  font-size: 12px;
  font-weight: 400;
  color: #9aa5bb;
}

/* 个人信息 */
.identity {
  display: flex;
  gap: 14px;
  align-items: center;
}

.avatar {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  font-size: 24px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #7fa9f7, #5b8def);
  border-radius: 50%;
  box-shadow: 0 8px 20px rgba(91, 141, 239, 0.28);
}

.name {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: #1f2a44;
}

.uid {
  margin: 5px 0 0;
  font-size: 12px;
  color: var(--text-sub);
}

/* 表单 */
.field {
  margin-bottom: 12px;
}

.field-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--text-label);
}

.input {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  font-size: 14px;
  color: var(--text-main);
  background: var(--input-bg);
  border: 1px solid var(--input-border);
  border-radius: 10px;
  outline: none;
  transition: border-color 0.18s, box-shadow 0.18s, background 0.18s;
}

.input:focus {
  background: #fff;
  border-color: #9dbcfa;
  box-shadow: 0 0 0 3px var(--accent-glow);
}

.input::placeholder {
  color: var(--input-placeholder);
}

.tip {
  margin: 0 0 12px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--text-sub);
}

.tip code {
  padding: 1px 6px;
  background: rgba(91, 141, 239, 0.1);
  border-radius: 5px;
}

.message {
  margin: 2px 0 10px;
  font-size: 13px;
}

.message.error {
  color: var(--danger);
}

.message.success {
  color: var(--success);
}

.btn-primary {
  height: 40px;
  padding: 0 22px;
  font-size: 14px;
  letter-spacing: 1px;
  color: #fff;
  cursor: pointer;
  background: linear-gradient(135deg, #6ea3f5, #5b8def);
  border: none;
  border-radius: 10px;
  box-shadow: 0 8px 18px rgba(91, 141, 239, 0.26);
}

.btn-primary.wide {
  width: 100%;
  margin-top: 4px;
}

.btn-primary:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
}

/* 上传 */
.upload-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  padding: 0 20px;
  font-size: 14px;
  color: var(--accent);
  cursor: pointer;
  background: rgba(91, 141, 239, 0.08);
  border: 1px dashed #a9c4f6;
  border-radius: 10px;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.upload-btn:hover {
  background: rgba(91, 141, 239, 0.14);
  border-color: #7fa9f7;
}

.upload-btn.is-busy {
  cursor: progress;
  opacity: 0.7;
}

.upload-btn input {
  display: none;
}

/* 照片墙 */
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
}

.cell {
  margin: 0;
  overflow: hidden;
  background: #eef3fd;
  border: 1px solid var(--card-border);
  border-radius: 12px;
}

.cell img {
  display: block;
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  transition: transform 0.5s ease;
}

/* 悬停放大（与首页照片墙一致） */
.cell:hover img {
  transform: scale(1.06);
}

.cell figcaption {
  padding: 6px 8px;
  font-size: 11px;
  color: #9aa5bb;
}

.empty {
  padding: 34px 0;
  font-size: 13px;
  color: #9aa5bb;
  text-align: center;
}

@media (max-width: 520px) {
  .wrap {
    padding: 12px 8px 56px;
  }

  .card {
    padding: 16px 14px 18px;
  }

  .grid {
    grid-template-columns: repeat(auto-fill, minmax(96px, 1fr));
  }
}
</style>
