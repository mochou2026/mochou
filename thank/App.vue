<script>
	export default {
		onLaunch: function() {
		    // 生成/获取设备唯一ID（每台设备固定，用于限制注册）
		    let deviceId = uni.getStorageSync('deviceId');
		    if (!deviceId) {
		      // 生成简易UUID (v4风格)
		      deviceId = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
		        const r = Math.random() * 16 | 0;
		        const v = c == 'x' ? r : (r & 0x3 | 0x8);
		        return v.toString(16);
		      });
		      uni.setStorageSync('deviceId', deviceId);
		    }
		    // 挂载到全局对象，方便所有页面使用
		    uni.$deviceId = deviceId;
		    console.log('设备ID:', deviceId);
		  },
		onShow: function() {
			console.log('App Show')
		},
		onHide: function() {
			console.log('App Hide')
		}
	}
</script>

<style>
	/*每个页面公共css */
</style>
