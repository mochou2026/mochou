<template>
  <view class="login-container">
    <view class="login-card">
      <view class="logo-area">
        <text class="logo-icon">✨</text>
        <text class="logo-text">欢迎回来</text>
      </view>
      <view class="form">
        <view class="input-group">
          <text class="input-icon">👤</text>
          <input type="text" v-model="username" placeholder="用户名 / 手机号" placeholder-class="placeholder" />
        </view>
        <view class="input-group">
          <text class="input-icon">🔒</text>
          <input type="password" v-model="password" placeholder="密码" placeholder-class="placeholder" />
        </view>
        <button class="login-btn" @click="handleLogin" :disabled="!username || !password">登 录</button>
        <view class="links">
          <text class="link" @click="goRegister">注册账号</text>
          <text class="link" @click="goResetPwd">忘记密码?</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      username: '',
      password: ''
    };
  },
  methods: {
    async handleLogin() {
      if (!this.username || !this.password) {
        uni.showToast({ title: '请填写完整', icon: 'none' });
        return;
      }
      uni.showLoading({ title: '登录中' });
      try {
        const res = await uni.request({
          url: this.$apiBase + '/login',
          method: 'POST',
          data: {
            username: this.username,
            password: this.password
          }
        });
        uni.hideLoading();
        if (res.data && res.data.code === 200) {
          uni.showToast({ title: '登录成功', icon: 'success' });
          uni.setStorageSync('username', this.username);
          setTimeout(() => {
           uni.reLaunch({ url: '/pages/index/index' });
          }, 1500);
        } else {
          const msg = (res.data && res.data.msg) ? res.data.msg : '登录失败';
          uni.showToast({ title: msg, icon: 'none' });
        }
      } catch (err) {
        uni.hideLoading();
        uni.showToast({ title: '网络错误', icon: 'none' });
        console.error(err);
      }
    },
    goRegister() {
      uni.navigateTo({ url: '/pages/register/register' });
    },
    goResetPwd() {
      uni.navigateTo({ url: '/pages/resetpwd/resetpwd' });
    }
  }
};
</script>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #1a2a6c, #b21f1f, #fdbb4d);
  background-size: 200% 200%;
  animation: gradientShift 8s ease infinite;
  padding: 30rpx;
}

@keyframes gradientShift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.login-card {
  width: 100%;
  max-width: 600rpx;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border-radius: 48rpx;
  padding: 60rpx 40rpx;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.logo-area {
  text-align: center;
  margin-bottom: 60rpx;
}
.logo-icon {
  font-size: 80rpx;
  display: block;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));
}
.logo-text {
  font-size: 48rpx;
  font-weight: bold;
  color: #fff;
  margin-top: 20rpx;
  display: block;
  text-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.input-group {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 60rpx;
  margin-bottom: 30rpx;
  padding: 0 30rpx;
  border: 1px solid rgba(255, 255, 255, 0.3);
  transition: all 0.3s;
}
.input-group:focus-within {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.8);
  box-shadow: 0 0 0 4rpx rgba(255, 255, 255, 0.2);
}
.input-icon {
  font-size: 40rpx;
  color: rgba(255, 255, 255, 0.9);
  margin-right: 20rpx;
}
.input-group input {
  flex: 1;
  height: 90rpx;
  font-size: 32rpx;
  color: #fff;
}
.placeholder {
  color: rgba(255, 255, 255, 0.6);
  font-size: 28rpx;
}
.login-btn {
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(5px);
  color: white;
  border-radius: 60rpx;
  height: 90rpx;
  line-height: 90rpx;
  font-size: 36rpx;
  font-weight: bold;
  margin-top: 40rpx;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 8rpx 16rpx rgba(0, 0, 0, 0.2);
}
.login-btn:active {
  transform: scale(0.98);
  background: rgba(255, 255, 255, 0.5);
}
.links {
  display: flex;
  justify-content: space-between;
  margin-top: 40rpx;
}
.link {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
  text-shadow: 0 1px 2px rgba(0,0,0,0.2);
}
</style>