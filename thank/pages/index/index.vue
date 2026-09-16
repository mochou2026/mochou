<template>
  <view class="index">
    <!-- 动态海水背景 -->
    <view class="sea-bg"></view>

    <!-- 鱼群画布 -->
    <canvas
      v-if="useCanvas"
      class="fish-canvas"
      canvas-id="fishCanvas"
      id="fishCanvas"
      @touchstart="onTouchCanvas"
      @touchmove="onTouchCanvas"
      @touchend="onTouchEnd"
    ></canvas>

    <!-- 贝壳卡片（字体布局保持不变） -->
    <view class="shell-card">
      <!-- 珍珠装饰（可选，不影响字体布局） -->
      <view class="pearl pearl-1"></view>
      <view class="pearl pearl-2"></view>
      <view class="pearl pearl-3"></view>

      <view class="shell-inner">
        <view class="card-header">
          <text class="emoji">🐚</text>
        </view>
        <view class="card-body">
          <text class="greeting">欢迎回来，</text>
          <text class="username">{{ username }}</text>
          <view class="wave-divider">
            <view class="wave"></view>
          </view>
          <button class="btn btn-primary" hover-class="btn-hover" @click="openLink">
            点击开始使用
          </button>
          <button class="btn btn-secondary" hover-class="btn-hover" @click="logout">
            退出登录
          </button>
        </view>
      </view>
      <!-- 漂浮气泡（装饰） -->
      <view class="bubble bubble-1"></view>
      <view class="bubble bubble-2"></view>
      <view class="bubble bubble-3"></view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      username: '',
      useCanvas: true,
      ctx: null,
      width: 0,
      height: 0,
      fishes: [],
      animationFrame: null,
      fishCount: 25,
      mouseX: null,
      mouseY: null,
      lastMoveTime: 0,
      time: 0,
    };
  },
  onShow() {
    const name = uni.getStorageSync('username');
    if (!name) {
      uni.redirectTo({ url: '/pages/login/login' });
    } else {
      this.username = name;
    }

    this.$nextTick(() => {
      this.initCanvas();
    });
  },
  onHide() {
    this.cancelAnimation();
  },
  methods: {
    openLink() {
      const url = 'https://www.thank.xn--6qq986b3xl/%E7%BD%91%E9%A1%B5%E4%BD%9C%E4%B8%9A/%E7%82%B9%E8%BF%99%E9%87%8C.html';
      // #ifdef H5
      window.location.href = url;
      // #endif
      // #ifdef APP-PLUS
      plus.runtime.openURL(url);
      // #endif
      // #ifdef MP
      uni.showToast({ title: '请使用浏览器打开', icon: 'none' });
      // #endif
    },
    logout() {
      uni.removeStorageSync('username');
      uni.redirectTo({ url: '/pages/login/login' });
    },

    // 初始化画布
    initCanvas() {
      const query = uni.createSelectorQuery().in(this);
      query.select('#fishCanvas').boundingClientRect(rect => {
        if (rect && rect.width > 0 && rect.height > 0) {
          this.width = rect.width;
          this.height = rect.height;
          this.initFishes();
          this.ctx = uni.createCanvasContext('fishCanvas', this);
          this.startAnimation();
        } else {
          console.warn('Canvas 尺寸获取失败，稍后重试');
          setTimeout(() => this.initCanvas(), 100);
        }
      }).exec();
    },

    // 初始化鱼群
    initFishes() {
      this.fishes = [];
      for (let i = 0; i < this.fishCount; i++) {
        const x = Math.random() * this.width;
        const y = Math.random() * this.height;
        const angle = Math.random() * 2 * Math.PI;
        const speed = 0.8 + Math.random() * 1.2;
        const vx = Math.cos(angle) * speed;
        const vy = Math.sin(angle) * speed;
        const size = 8 + Math.random() * 12;
        const hue = 180 + Math.random() * 30;
        const sat = 60 + Math.random() * 30;
        const light = 50 + Math.random() * 20;
        const color = `hsl(${hue}, ${sat}%, ${light}%)`;
        const phase = Math.random() * 2 * Math.PI;

        this.fishes.push({
          x, y, vx, vy,
          size,
          color,
          phase,
          bodyAngle: 0,
          tailPhase: 0,
        });
      }
    },

    startAnimation() {
      if (!this.ctx) return;
      const animate = () => {
        this.time += 0.05;
        this.updateFishes();
        this.drawFishes();
        this.animationFrame = requestAnimationFrame(animate);
      };
      animate();
    },

    updateFishes() {
      this.fishes.forEach(fish => {
        // 触摸排斥
        if (this.mouseX !== null && this.mouseY !== null) {
          const dx = fish.x - this.mouseX;
          const dy = fish.y - this.mouseY;
          const dist = Math.sqrt(dx * dx + dy * dy);
          const repelRadius = 120;
          if (dist < repelRadius && dist > 0.1) {
            const force = (repelRadius - dist) / repelRadius * 2;
            fish.vx += (dx / dist) * force * 0.2;
            fish.vy += (dy / dist) * force * 0.2;
          }
        }

        // 随机转向
        if (Math.random() < 0.01) {
          fish.vx += (Math.random() - 0.5) * 0.3;
          fish.vy += (Math.random() - 0.5) * 0.3;
        }

        // 限制速度
        const speed = Math.sqrt(fish.vx * fish.vx + fish.vy * fish.vy);
        const maxSpeed = 3.0;
        if (speed > maxSpeed) {
          fish.vx = (fish.vx / speed) * maxSpeed;
          fish.vy = (fish.vy / speed) * maxSpeed;
        }

        fish.x += fish.vx;
        fish.y += fish.vy;

        // 边界环绕
        if (fish.x < 0) fish.x = this.width;
        if (fish.x > this.width) fish.x = 0;
        if (fish.y < 0) fish.y = this.height;
        if (fish.y > this.height) fish.y = 0;

        fish.bodyAngle = Math.atan2(fish.vy, fish.vx);
        fish.tailPhase = Math.sin(this.time * 5 + fish.phase) * 0.5;
      });
    },

    drawFishes() {
      this.ctx.clearRect(0, 0, this.width, this.height);

      this.fishes.forEach(fish => {
        this.ctx.save();
        this.ctx.translate(fish.x, fish.y);
        this.ctx.rotate(fish.bodyAngle);

        const size = fish.size;
        const bodyLength = size * 1.5;
        const bodyHeight = size;

        // 绘制鱼身（使用缩放实现椭圆，兼容所有平台）
        this.ctx.save();
        this.ctx.scale(1, bodyHeight / bodyLength); // 压扁成椭圆
        this.ctx.beginPath();
        this.ctx.arc(0, 0, bodyLength / 2, 0, 2 * Math.PI);
        this.ctx.restore(); // 恢复缩放，保留旋转平移
        this.ctx.setFillStyle(fish.color);
        this.ctx.fill();

        // 眼睛
        this.ctx.beginPath();
        this.ctx.arc(bodyLength / 4, -bodyHeight / 4, size / 6, 0, 2 * Math.PI);
        this.ctx.setFillStyle('#ffffff');
        this.ctx.fill();
        this.ctx.beginPath();
        this.ctx.arc(bodyLength / 4 + 1, -bodyHeight / 4 - 1, size / 12, 0, 2 * Math.PI);
        this.ctx.setFillStyle('#000000');
        this.ctx.fill();

        // 尾巴
        this.ctx.save();
        this.ctx.translate(-bodyLength / 2, 0);
        this.ctx.rotate(fish.tailPhase * 0.5);
        this.ctx.beginPath();
        this.ctx.moveTo(0, -bodyHeight / 2);
        this.ctx.lineTo(-size / 2, 0);
        this.ctx.lineTo(0, bodyHeight / 2);
        this.ctx.closePath();
        this.ctx.setFillStyle(fish.color);
        this.ctx.fill();
        this.ctx.restore();

        this.ctx.restore();
      });

      this.ctx.draw();
    },

    cancelAnimation() {
      if (this.animationFrame) {
        cancelAnimationFrame(this.animationFrame);
        this.animationFrame = null;
      }
    },

    onTouchCanvas(e) {
      const touch = e.touches[0];
      if (!touch) return;
      const now = Date.now();
      if (now - this.lastMoveTime < 30) return;
      this.lastMoveTime = now;

      const query = uni.createSelectorQuery().in(this);
      query.select('#fishCanvas').boundingClientRect(rect => {
        if (rect) {
          this.mouseX = touch.clientX - rect.left;
          this.mouseY = touch.clientY - rect.top;
        }
      }).exec();
    },
    onTouchEnd() {
      this.mouseX = null;
      this.mouseY = null;
    },
  },
  beforeDestroy() {
    this.cancelAnimation();
  },
};
</script>

<style scoped>
/* 页面容器 */
.index {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #0a1a2a;
  position: relative;
  overflow: hidden;
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
}

/* 动态海水背景 */
.sea-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: radial-gradient(ellipse at 30% 40%, #1e5f70, #0a2f44, #05202b);
  z-index: 0;
  animation: seaWave 20s infinite alternate, hueShift 30s infinite linear;
}

@keyframes seaWave {
  0% { transform: scale(1) translate(0, 0); }
  100% { transform: scale(1.1) translate(-2%, -2%); }
}

@keyframes hueShift {
  0% { filter: hue-rotate(0deg) brightness(1); }
  50% { filter: hue-rotate(15deg) brightness(1.1); }
  100% { filter: hue-rotate(0deg) brightness(1); }
}

/* 鱼群画布 */
.fish-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: auto;
}

/* 贝壳卡片（保持字体布局不变） */
.shell-card {
  width: 650rpx;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(25rpx);
  -webkit-backdrop-filter: blur(25rpx);
  border-radius: 120rpx 120rpx 80rpx 80rpx;
  box-shadow: 0 40rpx 80rpx rgba(0, 20, 30, 0.6), inset 0 0 30rpx rgba(255, 255, 240, 0.3);
  border: 2rpx solid rgba(255, 255, 255, 0.3);
  position: relative;
  z-index: 20;
  overflow: hidden;
  animation: floatCard 6s infinite ease-in-out;
}

@keyframes floatCard {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20rpx); }
}

/* 珍珠装饰（不影响字体布局） */
.pearl {
  position: absolute;
  width: 30rpx;
  height: 30rpx;
  background: radial-gradient(circle at 30% 30%, #fff9e6, #ffdbb5);
  border-radius: 50%;
  box-shadow: 0 0 20rpx rgba(255, 255, 200, 0.8);
  z-index: 2;
}
.pearl-1 {
  top: 40rpx;
  left: 60rpx;
  width: 40rpx;
  height: 40rpx;
}
.pearl-2 {
  bottom: 50rpx;
  right: 70rpx;
  width: 50rpx;
  height: 50rpx;
}
.pearl-3 {
  top: 120rpx;
  right: 40rpx;
  width: 25rpx;
  height: 25rpx;
}

/* 卡片内部 */
.shell-inner {
  position: relative;
  z-index: 5;
  padding: 40rpx 30rpx 60rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.card-header {
  text-align: center;
  margin-bottom: 20rpx;
}
.emoji {
  font-size: 100rpx;
  filter: drop-shadow(0 8rpx 12rpx rgba(0, 50, 70, 0.5));
  animation: rotateEmoji 8s infinite alternate;
}
@keyframes rotateEmoji {
  0% { transform: rotate(-5deg) scale(1); }
  100% { transform: rotate(5deg) scale(1.1); }
}

/* 字体布局保持不变 */
.greeting {
  font-size: 32rpx;
  color: rgba(230, 255, 255, 0.9);
  letter-spacing: 2rpx;
  text-shadow: 0 2rpx 10rpx #00aabb;
}
.username {
  font-size: 72rpx;
  font-weight: 700;
  margin-top: 10rpx;
  background: linear-gradient(135deg, #c0f0ff, #a0d8ff, #80c0ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 20rpx #0088aa;
}

/* 波浪分割线 */
.wave-divider {
  width: 100%;
  margin: 40rpx 0;
  display: flex;
  justify-content: center;
}
.wave {
  width: 300rpx;
  height: 20rpx;
  background: repeating-linear-gradient( -45deg, rgba(255,255,255,0.4) 0rpx, rgba(255,255,255,0.4) 20rpx, transparent 20rpx, transparent 40rpx );
  background-size: 80rpx 80rpx;
  animation: waveMove 10s infinite linear;
  opacity: 0.7;
  border-radius: 10rpx;
}
@keyframes waveMove {
  0% { background-position: 0 0; }
  100% { background-position: 80rpx 0; }
}

/* 按钮样式（与之前一致） */
.btn {
  width: 100%;
  height: 96rpx;
  border-radius: 60rpx;
  font-size: 34rpx;
  font-weight: 600;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 30rpx;
  transition: all 0.3s ease;
  border: none;
  box-shadow: 0 10rpx 30rpx rgba(0, 30, 40, 0.5);
  letter-spacing: 2rpx;
}
.btn-primary {
  background: linear-gradient(145deg, #3d9eb3, #1d6f8c);
  color: #ffffff;
  border: 1rpx solid rgba(200, 240, 255, 0.6);
}
.btn-secondary {
  background: rgba(20, 60, 80, 0.5);
  color: #d0f0ff;
  border: 1rpx solid rgba(100, 200, 255, 0.4);
  backdrop-filter: blur(10rpx);
}
.btn-primary::after, .btn-secondary::after {
  border: none;
}
.btn-hover {
  transform: scale(0.96);
  opacity: 0.9;
  box-shadow: 0 4rpx 15rpx rgba(0, 150, 200, 0.6);
}
.btn:last-child {
  margin-bottom: 0;
}

/* 漂浮气泡 */
.bubble {
  position: absolute;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 50%;
  border: 1rpx solid rgba(255, 255, 255, 0.4);
  box-shadow: inset -5rpx -5rpx 10rpx rgba(0,0,0,0.1), inset 5rpx 5rpx 20rpx rgba(255,255,255,0.6);
  pointer-events: none;
  z-index: 3;
  animation: bubbleFloat 8s infinite ease-in-out;
}
.bubble-1 {
  width: 60rpx;
  height: 60rpx;
  bottom: 20rpx;
  left: 20rpx;
  animation-duration: 7s;
}
.bubble-2 {
  width: 100rpx;
  height: 100rpx;
  top: 40rpx;
  right: 10rpx;
  animation-duration: 10s;
  animation-delay: 2s;
}
.bubble-3 {
  width: 40rpx;
  height: 40rpx;
  bottom: 100rpx;
  right: 60rpx;
  animation-duration: 6s;
  animation-delay: 1s;
}
@keyframes bubbleFloat {
  0% { transform: translateY(0) scale(1); opacity: 0.3; }
  50% { transform: translateY(-60rpx) scale(1.1); opacity: 0.7; }
  100% { transform: translateY(0) scale(1); opacity: 0.3; }
}

/* 降级方案 */
@supports not (backdrop-filter: blur(25rpx)) {
  .shell-card {
    background: rgba(0, 30, 40, 0.8);
  }
}
</style>