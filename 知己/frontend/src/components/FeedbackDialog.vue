<template>
  <transition name="dialog">
    <div v-if="open" class="mask" @click.self="close">
      <section class="dialog" role="dialog" aria-modal="true" aria-label="反馈 / 投诉作者">
        <h3 class="dialog-title">反馈 / 投诉作者</h3>
        <p class="dialog-tip">遇到问题或有建议，写在这里提交给作者。</p>

        <textarea
          v-model="content"
          class="dialog-input"
          rows="5"
          maxlength="1000"
          placeholder="请描述你的问题或建议（最多 1000 字）"
          @input="clearMessage"
        ></textarea>

        <div class="dialog-foot">
          <span class="dialog-count">{{ content.length }}/1000</span>
          <div class="dialog-actions">
            <button class="btn-ghost" type="button" @click="close">取消</button>
            <button class="btn-primary" type="button" :disabled="loading" @click="submit">
              {{ loading ? '提交中…' : '提 交' }}
            </button>
          </div>
        </div>

        <p v-if="message.text" class="dialog-message" :class="message.type">{{ message.text }}</p>
      </section>
    </div>
  </transition>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { sendFeedback } from '../api.js'
import { currentUser } from '../session.js'

const props = defineProps({
  open: { type: Boolean, default: false }
})
const emit = defineEmits(['close'])

const content = ref('')
const loading = ref(false)
const message = reactive({ type: '', text: '' })

function clearMessage() {
  message.type = ''
  message.text = ''
}

function close() {
  if (loading.value) return
  emit('close')
}

async function submit() {
  if (loading.value) return
  clearMessage()

  const text = content.value.trim()
  if (!text) {
    message.type = 'error'
    message.text = '请填写反馈内容'
    return
  }

  loading.value = true
  try {
    const result = await sendFeedback(text, currentUser.value)
    if (result.code === 200) {
      message.type = 'success'
      message.text = result.msg || '感谢你的反馈'
      content.value = ''
      setTimeout(() => emit('close'), 900)
    } else {
      message.type = 'error'
      message.text = result.msg || '提交失败'
    }
  } catch (error) {
    message.type = 'error'
    message.text = error.message || '提交失败'
  } finally {
    loading.value = false
  }
}

// 每次打开时重置
watch(
  () => props.open,
  (value) => {
    if (value) {
      content.value = ''
      clearMessage()
    }
  }
)
</script>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(38, 48, 74, 0.35);
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
}

.dialog {
  width: 100%;
  max-width: 420px;
  padding: 22px 22px 18px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(38, 48, 74, 0.24);
}

.dialog-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #2b3856;
}

.dialog-tip {
  margin: 8px 0 14px;
  font-size: 12px;
  color: var(--text-sub);
}

.dialog-input {
  width: 100%;
  padding: 10px 12px;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-main);
  resize: vertical;
  background: var(--input-bg);
  border: 1px solid var(--input-border);
  border-radius: 10px;
  outline: none;
  transition: border-color 0.18s, box-shadow 0.18s, background 0.18s;
}

.dialog-input:focus {
  background: #fff;
  border-color: #9dbcfa;
  box-shadow: 0 0 0 3px var(--accent-glow);
}

.dialog-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

.dialog-count {
  font-size: 12px;
  color: #aab4c9;
}

.dialog-actions {
  display: flex;
  gap: 10px;
}

.btn-ghost {
  height: 36px;
  padding: 0 16px;
  font-size: 13px;
  color: #66738f;
  cursor: pointer;
  background: #f4f7fd;
  border: 1px solid var(--input-border);
  border-radius: 9px;
}

.btn-ghost:hover {
  background: #eaf0fb;
}

.btn-primary {
  height: 36px;
  padding: 0 20px;
  font-size: 13px;
  letter-spacing: 1px;
  color: #fff;
  cursor: pointer;
  background: linear-gradient(135deg, #6ea3f5, #5b8def);
  border: none;
  border-radius: 9px;
  box-shadow: 0 6px 14px rgba(91, 141, 239, 0.26);
}

.btn-primary:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
}

.dialog-message {
  margin: 10px 0 0;
  font-size: 12px;
}

.dialog-message.error {
  color: var(--danger);
}

.dialog-message.success {
  color: var(--success);
}

.dialog-enter-active,
.dialog-leave-active {
  transition: opacity 0.22s ease;
}

.dialog-enter-active .dialog,
.dialog-leave-active .dialog {
  transition: transform 0.22s ease;
}

.dialog-enter-from,
.dialog-leave-to {
  opacity: 0;
}

.dialog-enter-from .dialog,
.dialog-leave-to .dialog {
  transform: translateY(10px) scale(0.98);
}
</style>
