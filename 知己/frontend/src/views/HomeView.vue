<template>
  <div ref="rootEl" class="home">
    <!-- 背景：莫愁前路无知己（7 个字居中，置于最底层，若隐若现） -->
    <div class="phrase" aria-hidden="true" :style="{ opacity: phraseOpacity }">
      <span
        v-for="(char, i) in PHRASE"
        :key="char + i"
        class="phrase-char"
        :style="{ animationDelay: `${i * 0.85}s` }"
      >{{ char }}</span>
    </div>

    <header class="topbar">
      <!-- 点「知己」从右侧拉出列表 -->
      <button class="logo-btn" type="button" aria-label="打开菜单" @click="drawerOpen = true">知己</button>
      <nav class="actions">
        <!-- 只保留问候语；退出登录在「知己」菜单里 -->
        <span v-if="currentUser" class="who">你好，{{ currentUser }}</span>
        <button v-else class="text-btn" type="button" @click="goLogin">去登录</button>
      </nav>
    </header>

    <!-- 首屏：图片轮播，向下滚动时整体逐渐消失 -->
    <section class="hero" :style="heroStyle">
      <PhotoCarousel :photos="photos" class="hero-carousel" />
      <div class="scroll-hint">
        <span>向下滑动</span>
        <span class="scroll-arrow">↓</span>
      </div>
    </section>

    <!-- 照片墙：数据库里上传的图片在前，本地图片在后 -->
    <section class="gallery">
      <h2 class="gallery-title">照片墙</h2>
      <div class="grid">
        <figure
          v-for="(photo, i) in wallPhotos"
          :key="photo.key"
          class="cell"
          :class="{ 'is-visible': visible[i] }"
          :style="{ transitionDelay: `${(i % 3) * 90}ms` }"
        >
          <img :src="photo.src" :alt="photo.name" loading="lazy" draggable="false" />
        </figure>
      </div>
    </section>

    <!-- 右侧拉出列表 -->
    <transition name="fade">
      <div v-if="drawerOpen" class="drawer-mask" @click.self="drawerOpen = false"></div>
    </transition>
    <transition name="slide">
      <aside v-if="drawerOpen" class="drawer" aria-label="菜单">
        <div class="drawer-head">
          <div class="drawer-avatar" aria-hidden="true">{{ avatarText }}</div>
          <div class="drawer-who">
            <p class="drawer-name">{{ currentUser || '未登录' }}</p>
            <p class="drawer-uid">{{ currentUser ? `知己号：zhiji_${currentUser}` : '登录后可以上传照片' }}</p>
          </div>
          <button class="drawer-close" type="button" aria-label="关闭菜单" @click="drawerOpen = false">×</button>
        </div>

        <nav class="drawer-menu">
          <button class="drawer-item" type="button" @click="goProfile">
            <span class="drawer-icon">👤</span>个人中心
          </button>
          <button v-if="currentUser" class="drawer-item" type="button" @click="handleSignOut">
            <span class="drawer-icon">⏻</span>退出登录
          </button>
          <button v-else class="drawer-item" type="button" @click="goLogin">
            <span class="drawer-icon">→</span>去登录
          </button>
          <button class="drawer-item" type="button" @click="openFeedback">
            <span class="drawer-icon">✉</span>反馈 / 投诉作者
          </button>
        </nav>
      </aside>
    </transition>

    <FeedbackDialog :open="feedbackOpen" @close="feedbackOpen = false" />

    <!-- 右下角背景音乐开关 -->
    <MusicToggle />
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import FeedbackDialog from '../components/FeedbackDialog.vue'
import MusicToggle from '../components/MusicToggle.vue'
import PhotoCarousel from '../components/PhotoCarousel.vue'
import { listPhotos } from '../api.js'
import { photos } from '../photos.js'
import { currentUser, signOut } from '../session.js'
import { push } from '../router.js'

/** 背景那 7 个字 */
const PHRASE = ['莫', '愁', '前', '路', '无', '知', '己']

const rootEl = ref(null)
const scrollY = ref(0)
const viewportHeight = ref(window.innerHeight)
const visible = ref([])

const drawerOpen = ref(false)
const feedbackOpen = ref(false)
/** 从数据库（chou 库）读来的照片 */
const remotePhotos = ref([])

let observer = null
let frame = 0

const avatarText = computed(() => ((currentUser.value || '知')[0] || '知').toUpperCase())

/** 照片墙 = 数据库里的照片（新的在前）+ 本地 assets/gallery 里的照片 */
const wallPhotos = computed(() => [
  ...remotePhotos.value.map((photo) => ({
    key: 'db-' + photo.id,
    src: photo.url,
    name: photo.filename
  })),
  ...photos.map((photo) => ({
    key: 'local-' + photo.src,
    src: photo.src,
    name: photo.name
  }))
])

/** 0 → 1：滚过约 85% 屏高时首屏完全淡出 */
const progress = computed(() => {
  const span = viewportHeight.value * 0.85
  return span <= 0 ? 0 : Math.min(Math.max(scrollY.value / span, 0), 1)
})

const heroStyle = computed(() => ({
  opacity: String(1 - progress.value),
  transform: `translateY(${(-progress.value * 70).toFixed(1)}px) scale(${(1 - progress.value * 0.05).toFixed(3)})`
}))

const phraseOpacity = computed(() => String(1 - progress.value * 0.9))

function handleScroll() {
  if (frame) return
  frame = requestAnimationFrame(() => {
    frame = 0
    scrollY.value = window.scrollY
  })
}

function handleResize() {
  viewportHeight.value = window.innerHeight
}

function handleSignOut() {
  drawerOpen.value = false
  signOut()
  push('/login')
}

function goLogin() {
  drawerOpen.value = false
  push('/login')
}

function goProfile() {
  drawerOpen.value = false
  push('/profile')
}

function openFeedback() {
  drawerOpen.value = false
  feedbackOpen.value = true
}

/** 登记照片墙的渐显监听（滚入视口淡入、滚出淡出） */
function observeCells() {
  if (observer) {
    observer.disconnect()
    observer = null
  }
  const cells = rootEl.value ? Array.from(rootEl.value.querySelectorAll('.cell')) : []
  visible.value = cells.map(() => false)
  if (!cells.length) return

  if (typeof IntersectionObserver === 'undefined') {
    visible.value = cells.map(() => true)
    return
  }
  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        const index = Number(entry.target.dataset.index)
        if (!Number.isNaN(index)) visible.value[index] = entry.isIntersecting
      })
    },
    { threshold: 0.2, rootMargin: '-8% 0px -12% 0px' }
  )
  cells.forEach((el, index) => {
    el.dataset.index = String(index)
    observer.observe(el)
  })
}

/** 读取数据库里的照片（所有用户上传的都会出现在照片墙） */
async function loadRemotePhotos() {
  try {
    const result = await listPhotos()
    remotePhotos.value = result.code === 200 && Array.isArray(result.data) ? result.data : []
  } catch (error) {
    remotePhotos.value = []
  }
}

onMounted(() => {
  document.title = '知己 · 首页'
  window.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener('resize', handleResize)
  observeCells()
  loadRemotePhotos()
})

// 数据库照片到达（或增删）后重新登记监听
watch(wallPhotos, () => {
  nextTick(observeCells)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', handleResize)
  if (frame) cancelAnimationFrame(frame)
  if (observer) observer.disconnect()
})
</script>

<style scoped>
.home {
  position: relative;
  min-height: 100dvh;
}

/* ---------- 背景文字：7 个字居中且若隐若现（最底层） ---------- */
.phrase {
  position: fixed;
  inset: 0;
  z-index: 0;
  display: flex;
  gap: clamp(2px, 1.4vw, 18px);
  /* 水平 + 垂直都居中 */
  align-items: center;
  justify-content: center;
  pointer-events: none;
  will-change: opacity;
}

.phrase-char {
  font-size: clamp(28px, 5.2vw, 66px);
  font-weight: 600;
  color: #4a6cb4;
  opacity: 0.08;
  animation: breath 6.4s ease-in-out infinite;
}

@keyframes breath {
  0%,
  100% {
    opacity: 0.06;
    transform: translateY(0);
  }
  50% {
    opacity: 0.24;
    transform: translateY(-6px);
  }
}

/* ---------- 顶栏 ---------- */
.topbar {
  position: fixed;
  top: 0;
  right: 0;
  left: 0;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 14px 0 8px;
  background: rgba(255, 255, 255, 0.72);
  border-bottom: 1px solid rgba(231, 237, 249, 0.9);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.logo-btn {
  padding: 8px 12px;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 4px;
  color: #2b3856;
  cursor: pointer;
  background: none;
  border: none;
  border-radius: 10px;
  transition: background 0.2s ease;
}

.logo-btn:hover {
  background: rgba(91, 141, 239, 0.1);
}

.actions {
  display: flex;
  gap: 12px;
  align-items: center;
  font-size: 13px;
}

.who {
  color: var(--text-sub);
}

.text-btn {
  padding: 0;
  font-size: 13px;
  color: var(--accent);
  cursor: pointer;
  background: none;
  border: none;
}

.text-btn:hover {
  color: var(--accent-dark);
  text-decoration: underline;
}

/* ---------- 首屏 ---------- */
.hero {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
  justify-content: center;
  min-height: 100dvh;
  padding: 64px 20px 56px;
  will-change: opacity, transform;
}

.hero-carousel {
  width: min(400px, 82vw);
}

.scroll-hint {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
  font-size: 12px;
  color: #93a0b8;
}

.scroll-arrow {
  font-size: 15px;
  animation: float 1.9s ease-in-out infinite;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  50% {
    transform: translateY(6px);
    opacity: 1;
  }
}

/* ---------- 照片墙 ---------- */
.gallery {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  max-width: 1040px;
  /* 至少占满一屏，保证首屏能完整地随滚动淡出 */
  min-height: 100dvh;
  margin: 0 auto;
  padding: 8px 20px 96px;
}

.gallery-title {
  margin: 0 0 22px;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #2b3856;
  text-align: center;
}

.grid {
  display: grid;
  /* auto-fit + 固定列宽：图片少的时候空轨道会塌缩，整体居中不贴边 */
  grid-template-columns: repeat(auto-fit, 220px);
  gap: 16px;
  justify-content: center;
}

.cell {
  aspect-ratio: 1;
  margin: 0;
  overflow: hidden;
  background: #eef3fd;
  border: 1px solid var(--card-border);
  border-radius: 14px;
  box-shadow: 0 8px 22px rgba(64, 96, 160, 0.1);
  opacity: 0;
  transform: translateY(26px) scale(0.96);
  transition: opacity 0.75s ease, transform 0.75s ease;
}

.cell.is-visible {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.cell img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

/* 悬停放大 */
.cell:hover img {
  transform: scale(1.06);
}

/* ---------- 右侧拉出列表 ---------- */
.drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 8;
  background: rgba(38, 48, 74, 0.32);
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
}

.drawer {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 9;
  display: flex;
  flex-direction: column;
  gap: 18px;
  width: min(280px, 78vw);
  padding: 20px 18px;
  background: #fff;
  box-shadow: -14px 0 40px rgba(38, 48, 74, 0.18);
}

.drawer-head {
  display: flex;
  gap: 12px;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #eef2fa;
}

.drawer-avatar {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #7fa9f7, #5b8def);
  border-radius: 50%;
}

.drawer-who {
  min-width: 0;
  flex: 1;
}

.drawer-name {
  margin: 0;
  overflow: hidden;
  font-size: 15px;
  font-weight: 600;
  color: #1f2a44;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drawer-uid {
  margin: 4px 0 0;
  overflow: hidden;
  font-size: 12px;
  color: var(--text-sub);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drawer-close {
  flex: none;
  width: 30px;
  height: 30px;
  font-size: 20px;
  line-height: 1;
  color: #93a0b8;
  cursor: pointer;
  background: none;
  border: none;
  border-radius: 50%;
}

.drawer-close:hover {
  background: #f2f6fd;
}

.drawer-menu {
  display: flex;
  flex-direction: column;
}

.drawer-item {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 14px 10px;
  font-size: 14px;
  color: #33405e;
  text-align: left;
  cursor: pointer;
  background: none;
  border: none;
  border-bottom: 1px solid #f3f6fc;
  transition: background 0.18s ease, color 0.18s ease;
}

.drawer-item:hover {
  color: var(--accent-dark);
  background: #f6f9ff;
}

.drawer-icon {
  width: 18px;
  font-size: 14px;
  text-align: center;
  opacity: 0.7;
}

.slide-enter-active,
.slide-leave-active {
  transition: transform 0.26s ease;
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.26s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 520px) {
  .hero {
    padding: 64px 16px 40px;
  }

  .grid {
    grid-template-columns: repeat(auto-fit, 148px);
    gap: 12px;
  }
}
</style>
