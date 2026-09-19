<template>
  <!-- 满屏居中容器：无论窗口怎么缩放，卡片都在正中间 -->
  <div class="page">
    <div class="backdrop" aria-hidden="true">
      <span class="glow glow-a"></span>
      <span class="glow glow-b"></span>
    </div>
    <AuthCard />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import AuthCard from '../components/AuthCard.vue'

onMounted(() => {
  document.title = '知己 · 登录'
})
</script>

<style scoped>
.page {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  min-height: 100dvh;
  width: 100%;
  padding: 24px 16px;
}

.backdrop {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
}

.glow {
  position: absolute;
  display: block;
  border-radius: 50%;
  filter: blur(90px);
}

.glow-a {
  width: 42vmin;
  height: 42vmin;
  top: -8vmin;
  left: -6vmin;
  background: rgba(122, 166, 246, 0.22);
}

.glow-b {
  width: 46vmin;
  height: 46vmin;
  right: -10vmin;
  bottom: -10vmin;
  background: rgba(180, 163, 245, 0.2);
}
</style>
