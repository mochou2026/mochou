import { ref } from 'vue'

/**
 * 登录状态：只记住用户名，存在 sessionStorage（刷新保留、关掉标签页失效）。
 * 登录页和首页共用这一份状态。
 */
const KEY = 'zhiji_user'

export const currentUser = ref(sessionStorage.getItem(KEY) || '')

export function signIn(username) {
  currentUser.value = username
  sessionStorage.setItem(KEY, username)
}

export function signOut() {
  currentUser.value = ''
  sessionStorage.removeItem(KEY)
}
