<template>
  <div class="forbidden-page">
    <div class="forbidden-card">
      <p class="code">403</p>
      <h1>无权限访问</h1>
      <p class="desc">当前账号没有访问该页面的权限，请联系管理员开通对应角色权限。</p>
      <div class="actions">
        <el-button @click="goBack">返回上一页</el-button>
        <el-button type="primary" @click="goHome">回到首页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goBack = () => {
  router.back()
}

const goHome = () => {
  router.push('/home')
}
</script>

<style scoped>
.forbidden-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #eef2ff 100%);
}

.forbidden-card {
  width: min(560px, 100%);
  padding: 36px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 12px 36px rgba(15, 23, 42, 0.08);
  text-align: center;
}

.code {
  margin: 0;
  font-size: 56px;
  line-height: 1;
  font-weight: 700;
  color: #ef4444;
}

h1 {
  margin: 14px 0 10px;
  font-size: 28px;
  color: #111827;
}

.desc {
  margin: 0 auto;
  max-width: 420px;
  color: #4b5563;
  line-height: 1.7;
}

.actions {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
