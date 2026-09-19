import { ref } from 'vue'

/**
 * 极简路由：本站只有「登录页 /login」和「首页 /home」两个页面，
 * 用一个响应式变量 + History API 就够了，不额外引入依赖。
 *
 * 新增页面：把路径加进 ROUTES，再到 App.vue 里映射组件即可。
 */
const ROUTES = ['/login', '/home', '/profile']

function normalize(path) {
  const clean = (path || '/').split('?')[0].replace(/\/+$/, '') || '/'
  if (clean === '/') return '/'
  return ROUTES.includes(clean) ? clean : '/login'
}

export const route = ref(normalize(window.location.pathname))

function apply(path, mode) {
  const next = normalize(path)
  if (next === route.value) return
  window.history[mode === 'replace' ? 'replaceState' : 'pushState']({}, '', next)
  route.value = next
}

/** 跳转（新增一条历史记录，浏览器可以后退） */
export function push(path) {
  apply(path, 'push')
}

/** 替换当前地址（不增加历史记录） */
export function replace(path) {
  apply(path, 'replace')
}

window.addEventListener('popstate', () => {
  route.value = normalize(window.location.pathname)
})
