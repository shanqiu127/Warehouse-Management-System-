<template>
  <div class="forbidden-page">
    <div class="glow glow-left"></div>
    <div class="glow glow-right"></div>
    <div class="forbidden-card">
      <div class="illustration-wrap">
        <img class="illustration" :src="forbiddenIllustration" alt="403 无权限访问" />
      </div>
      <p class="code">403</p>
      <h1>当前页面暂未授权</h1>
      <p class="desc">当前账号没有访问该页面的权限。你可以返回继续操作，或提交权限申请给管理员处理。</p>
      <div class="actions">
        <el-button @click="goBack">返回上一页</el-button>
        <el-button @click="requestAccess">申请权限</el-button>
        <el-button type="primary" @click="goHome">回到首页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import forbiddenIllustration from '@/assets/illustrations/403.svg'

const router = useRouter()

const goBack = () => {
  router.back()
}

const goHome = () => {
  router.push('/home')
}

const requestAccess = () => {
  ElMessageBox.alert('请联系系统管理员，为当前账号开通对应菜单权限。', '申请权限说明', {
    confirmButtonText: '我知道了',
    type: 'warning'
  })
}
</script>

<style scoped>
.forbidden-page {
  position: relative;
  overflow: hidden;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: radial-gradient(circle at 15% 20%, #eaf4ff 0%, transparent 35%),
    radial-gradient(circle at 82% 78%, #fff1e1 0%, transparent 36%),
    linear-gradient(135deg, #f8fbff 0%, #f7f9fc 100%);
}

.glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(50px);
  opacity: 0.6;
  pointer-events: none;
}

.glow-left {
  width: 260px;
  height: 260px;
  background: #bfdbfe;
  top: -80px;
  left: -60px;
}

.glow-right {
  width: 300px;
  height: 300px;
  background: #fed7aa;
  right: -90px;
  bottom: -110px;
}

.forbidden-card {
  position: relative;
  z-index: 1;
  width: min(560px, 100%);
  padding: 36px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(8px);
  border: 1px solid #e2e8f0;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.12);
  text-align: center;
  animation: reveal 420ms ease-out;
}

.illustration-wrap {
  width: 118px;
  height: 118px;
  margin: 0 auto 12px;
  border-radius: 999px;
  background: linear-gradient(145deg, #fff5f5 0%, #ffe4e6 100%);
  border: 1px solid #fecdd3;
  display: grid;
  place-items: center;
  box-shadow: 0 8px 22px rgba(220, 38, 38, 0.14);
}

.illustration {
  width: 86px;
  height: 86px;
  display: block;
}

.code {
  margin: 0;
  font-size: 52px;
  line-height: 1;
  font-weight: 800;
  letter-spacing: 2px;
  color: #d0021b;
}

h1 {
  margin: 14px 0 10px;
  font-size: 30px;
  color: #0f172a;
}

.desc {
  margin: 0 auto;
  max-width: 420px;
  color: #475569;
  line-height: 1.7;
}

.actions {
  margin-top: 26px;
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

@keyframes reveal {
  from {
    opacity: 0;
    transform: translateY(8px) scale(0.99);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 640px) {
  .forbidden-card {
    padding: 26px 20px;
    border-radius: 18px;
  }

  .illustration-wrap {
    width: 104px;
    height: 104px;
  }

  .illustration {
    width: 76px;
    height: 76px;
  }

  .code {
    font-size: 44px;
  }

  h1 {
    font-size: 24px;
  }
}
</style>
