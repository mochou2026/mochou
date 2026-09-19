<template>
  <div class="music">
    <audio
      ref="audioEl"
      :src="src"
      loop
      preload="auto"
      @play="handlePlay"
      @pause="playing = false"
    />

    <transition name="hint">
      <span v-if="showHint" class="music-hint">点击播放背景音乐</span>
    </transition>

    <button
      class="music-btn"
      :class="{ 'is-playing': playing }"
      type="button"
      :title="playing ? '暂停背景音乐' : '播放背景音乐'"
      :aria-label="playing ? '暂停背景音乐' : '播放背景音乐'"
      @click="toggle"
    >
      <span class="disc" aria-hidden="true">♪</span>
    </button>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { backgroundTrack } from '../music.js'

const src = backgroundTrack
const audioEl = ref(null)
const playing = ref(false)
/** 自动播放被浏览器拦截时给个提示 */
const blocked = ref(false)
/** 用户主动暂停过，就不再自动续播 */
let userPaused = false

const showHint = computed(() => Boolean(src) && blocked.value && !playing.value)

async function tryPlay() {
  const el = audioEl.value
  if (!el || !src) return false
  try {
    await el.play()
    return true
  } catch (error) {
    return false
  }
}

function toggle() {
  const el = audioEl.value
  if (!el) return
  if (el.paused) {
    userPaused = false
    tryPlay()
  } else {
    userPaused = true
    el.pause()
  }
}

/** 浏览器默认拦截「无用户交互」的自动播放：首次交互后再尝试一次 */
function handleFirstGesture() {
  if (userPaused || playing.value) return
  tryPlay()
}

/** 用户离开页面（切标签 / 窗口失焦）时暂停，回来再续上 */
let resumeAfterReturn = false

function pauseForLeave() {
  const el = audioEl.value
  if (!el || el.paused) return
  resumeAfterReturn = true
  el.pause()
}

function resumeWhenBack() {
  if (!resumeAfterReturn || userPaused) return
  resumeAfterReturn = false
  tryPlay()
}

function handleVisibilityChange() {
  if (document.hidden) {
    pauseForLeave()
  } else {
    resumeWhenBack()
  }
}

function detachGestureListeners() {
  window.removeEventListener('pointerdown', handleFirstGesture)
  window.removeEventListener('keydown', handleFirstGesture)
}

function handlePlay() {
  playing.value = true
  blocked.value = false
  detachGestureListeners()
}

onMounted(async () => {
  const el = audioEl.value
  if (!el || !src) return
  el.volume = 0.45

  // 不在页面时停止播放
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('blur', pauseForLeave)
  window.addEventListener('focus', resumeWhenBack)

  const started = await tryPlay()
  blocked.value = !started
  if (!started) {
    window.addEventListener('pointerdown', handleFirstGesture)
    window.addEventListener('keydown', handleFirstGesture)
  }
})

onBeforeUnmount(() => {
  detachGestureListeners()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('blur', pauseForLeave)
  window.removeEventListener('focus', resumeWhenBack)
  const el = audioEl.value
  if (el) el.pause()
})
</script>

<style scoped>
.music {
  position: fixed;
  right: 18px;
  bottom: 18px;
  z-index: 4;
  display: flex;
  gap: 8px;
  align-items: center;
}

.music-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  padding: 0;
  color: var(--accent);
  cursor: pointer;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid var(--card-border);
  border-radius: 50%;
  box-shadow: 0 8px 20px rgba(64, 96, 160, 0.16);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  transition: transform 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.music-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(64, 96, 160, 0.22);
}

.music-btn.is-playing {
  color: var(--accent-dark);
}

.disc {
  display: block;
  font-size: 18px;
  line-height: 1;
}

.music-btn.is-playing .disc {
  animation: spin 3.6s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.music-hint {
  padding: 6px 12px;
  font-size: 12px;
  color: #55627f;
  white-space: nowrap;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid var(--card-border);
  border-radius: 999px;
  box-shadow: 0 8px 20px rgba(64, 96, 160, 0.14);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.hint-enter-active,
.hint-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.hint-enter-from,
.hint-leave-to {
  opacity: 0;
  transform: translateX(8px);
}

@media (max-width: 520px) {
  .music {
    right: 12px;
    bottom: 12px;
  }

  .music-hint {
    display: none;
  }
}
</style>
