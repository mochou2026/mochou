<template>
  <figure class="carousel" @mouseenter="paused = true" @mouseleave="paused = false">
    <div class="viewport">
      <!-- 所有图片叠在一起，切换时交叉淡入淡出 -->
      <img
        v-for="(photo, i) in photos"
        :key="photo.src"
        :src="photo.src"
        :alt="photo.name"
        class="slide"
        :class="{ 'is-active': i === index }"
        draggable="false"
      />

      <template v-if="hasMany">
        <button class="arrow arrow-prev" type="button" aria-label="上一张" @click="prev">‹</button>
        <button class="arrow arrow-next" type="button" aria-label="下一张" @click="next">›</button>
      </template>
    </div>

    <div v-if="hasMany" class="dots">
      <button
        v-for="(photo, i) in photos"
        :key="'dot-' + photo.src"
        class="dot"
        :class="{ 'is-active': i === index }"
        type="button"
        :aria-label="`第 ${i + 1} 张`"
        @click="go(i)"
      />
    </div>

    <figcaption v-if="hasMany" class="counter">{{ index + 1 }} / {{ photos.length }}</figcaption>
  </figure>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  photos: { type: Array, default: () => [] },
  /** 自动播放间隔（毫秒） */
  interval: { type: Number, default: 5000 }
})

const index = ref(0)
const paused = ref(false)
let timer = null

const hasMany = computed(() => props.photos.length > 1)

function go(target) {
  const total = props.photos.length
  if (!total) return
  index.value = ((target % total) + total) % total
}

function next() {
  go(index.value + 1)
}

function prev() {
  go(index.value - 1)
}

function start() {
  stop()
  if (!hasMany.value) return
  timer = setInterval(() => {
    if (!paused.value) next()
  }, props.interval)
}

function stop() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onMounted(start)
onBeforeUnmount(stop)
watch(hasMany, start)
</script>

<style scoped>
.carousel {
  margin: 0;
}

.viewport {
  position: relative;
  width: 100%;
  aspect-ratio: 3 / 4;
  max-height: 54vh;
  margin: 0 auto;
  overflow: hidden;
  background: #eef3fd;
  border: 1px solid var(--card-border);
  border-radius: 20px;
  box-shadow: 0 22px 50px rgba(64, 96, 160, 0.18);
}

.slide {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transform: scale(1.045);
  transition: opacity 0.9s ease, transform 6s ease;
  user-select: none;
}

.slide.is-active {
  opacity: 1;
  transform: scale(1);
}

.arrow {
  position: absolute;
  top: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  margin-top: -17px;
  font-size: 22px;
  line-height: 1;
  color: #45608f;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.25s ease, background 0.2s ease;
}

.viewport:hover .arrow {
  opacity: 1;
}

.arrow:hover {
  background: #fff;
}

.arrow-prev {
  left: 12px;
}

.arrow-next {
  right: 12px;
}

.dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 14px;
}

.dot {
  width: 7px;
  height: 7px;
  padding: 0;
  cursor: pointer;
  background: #cfd9ee;
  border: none;
  border-radius: 50%;
  transition: background 0.25s ease, transform 0.25s ease;
}

.dot.is-active {
  background: var(--accent);
  transform: scale(1.3);
}

.counter {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-sub);
  text-align: center;
}
</style>
