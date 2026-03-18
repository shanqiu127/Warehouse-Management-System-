<template>
  <div class="login-container">
    <el-card class="login-box">
      <h2 style="text-align: center; margin-bottom: 20px;">系统登录</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" size="large"></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password size="large"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%" @click="handleLogin">登录</el-button>
        </el-form-item>
        <div style="text-align: right;">
          <el-link type="primary" @click="$router.push('/register')">立即注册</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>
<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { loginAPI } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '账号不可为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不可为空', trigger: 'blur' }]
}

const handleLogin = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const res = await loginAPI(form)
        if (res.code !== 200 || !res.data?.token) {
          throw new Error(res.msg || '登录失败')
        }

        userStore.setToken(res.data.token)
        userStore.setRole(res.data.userInfo?.role || '')

        const roleText = userStore.role === 'superadmin'
          ? '超级管理员'
          : (userStore.role === 'admin' ? '管理员' : '普通员工')
        ElMessage.success(`登录成功，当前角色：${roleText}`)
        router.push('/')
      } catch (error) {
        ElMessage.error(error.message || '登录失败')
      }
    }
  })
}
</script>
<style scoped>
.login-container { 
  display: flex; 
  justify-content: center; 
  align-items: center; 
  height: 100vh; 
  background: linear-gradient(135deg, #74ebd5 0%, #acb6e5 100%); 
}
.login-box { 
  width: 400px; 
  padding: 30px; 
  box-sizing: border-box; 
  border-radius: 10px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}
</style>