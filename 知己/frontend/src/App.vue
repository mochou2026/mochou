<template>
  <component :is="view" />
</template>

<script setup>
import { computed, watchEffect } from 'vue'
import { replace, route } from './router.js'
import { currentUser } from './session.js'
import HomeView from './views/HomeView.vue'
import LoginView from './views/LoginView.vue'
import ProfileView from './views/ProfileView.vue'

/** 入口 '/'：已登录直接进首页，否则进登录页 */
watchEffect(() => {
  if (route.value === '/') {
    replace(currentUser.value ? '/home' : '/login')
  }
})

const view = computed(() => {
  if (route.value === '/profile') return ProfileView
  return route.value === '/login' ? LoginView : HomeView
})
</script>
