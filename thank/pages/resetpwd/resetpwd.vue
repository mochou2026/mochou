<template>
  <view class="reset-container">
    <view class="reset-card">
      <view class="logo-area">
        <text class="logo-icon">🔄</text>
        <text class="logo-text">修改密码</text>
      </view>
      <view class="form">
        <view class="input-group">
          <text class="input-icon">👤</text>
          <input type="text" v-model="username" placeholder="用户名" placeholder-class="placeholder" />
        </view>
        <view class="input-group">
          <text class="input-icon">🔒</text>
          <input type="password" v-model="oldPassword" placeholder="旧密码" placeholder-class="placeholder" />
        </view>
        <view class="input-group">
          <text class="input-icon">🔑</text>
          <input type="password" v-model="newPassword" placeholder="新密码 (至少6位)" placeholder-class="placeholder" />
        </view>
        <view class="input-group">
          <text class="input-icon">✓</text>
          <input type="password" v-model="confirmNew" placeholder="确认新密码" placeholder-class="placeholder" />
        </view>
        <button class="reset-btn" @click="handleReset" :disabled="!username || !oldPassword || !newPassword || !confirmNew">确 认 修 改</button>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      username: '',
      oldPassword: '',
      newPassword: '',
      confirmNew: ''
    };
  },
  methods: {
    async handleReset() {
      if (!this.username || !this.oldPassword || !this.newPassword || !this.confirmNew) {
        uni.showToast({ title: '请填写完整', icon: 'none' });
        return;
      }
      if (this.newPassword !== this.confirmNew) {
        uni.showToast({ title: '新密码不一致', icon: 'none' });
        return;
      }
      if (this.newPassword.length < 6) {
        uni.showToast({ title: '新密码至少6位', icon: 'none' });
        return;
      }
      uni.showLoading({ title: '修改中' });
      try {
        const res = await uni.request({
          url: this.$apiBase + '/resetPwd',
          method: 'POST',
          data: {
            username: this.username,
            oldPassword: this.oldPassword,
            newPassword: this.newPassword
          }
        });
        uni.hideLoading();
        if (res.data && res.data.code === 200) {
          uni.showToast({ title: '修改成功', icon: 'success' });
          setTimeout(() => {
            uni.navigateBack();
          }, 1500);
        } else {
          const msg = (res.data && res.data.msg) ? res.data.msg : '修改失败';
          uni.showToast({ title: msg, icon: 'none' });
        }
      } catch (err) {
        uni.hideLoading();
        uni.showToast({ title: '网络错误', icon: 'none' });
        console.error(err);
      }
    }
  }
};
</script>

<style scoped>
.reset-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #2b2d42, #8d99ae, #edf2f4);
  background-size: 200% 200%;
  animation: gradientShift 8s ease infinite;
  padding: 30rpx;
}

@keyframes gradientShift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.reset-card {
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
}
.logo-text {
  font-size: 48rpx;
  font-weight: bold;
  color: #fff;
  margin-top: 20rpx;
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
}
.reset-btn {
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
.reset-btn:active {
  transform: scale(0.98);
  background: rgba(255, 255, 255, 0.5);
}
</style>