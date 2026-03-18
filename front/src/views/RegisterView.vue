<template>
  <div class="register-container">
    <el-card class="register-box">
      <h2 style="text-align: center; margin-bottom: 20px;">系统注册</h2>
      <el-alert title="注册仅开放给普通用户（employee）" type="info" :closable="false" style="margin-bottom: 16px;" />
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" size="large"></el-input>
        </el-form-item>
        <el-form-item prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" size="large"></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password size="large"></el-input>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请确认密码" show-password size="large"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%" @click="handleRegister">注册</el-button>
        </el-form-item>
        <div style="text-align: right;">
          <el-link type="primary" @click="$router.push('/login')">返回登录</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>
<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerAPI } from '@/api/user'

const router = useRouter()
const formRef = ref(null)

const form = reactive({ username: '', realName: '', password: '', confirmPassword: '' })
const validatePass = (rule, value, callback) => {
  if (value === '') { callback(new Error('请再次输入密码')) }
  else if (value !== form.password) { callback(new Error('两次输入密码不一致!')) }
  else { callback() }
}
const rules = {
  username: [{ required: true, message: '账号不可为空', trigger: 'blur' }],
  realName: [{ required: true, message: '真实姓名不可为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不可为空', trigger: 'blur' }],
  confirmPassword: [{ validator: validatePass, trigger: 'blur' }]
}

const handleRegister = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await registerAPI({ username: form.username, realName: form.realName, password: form.password })
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } catch (error) {
        ElMessage.error(error?.response?.data?.msg || '当前版本后端尚未开放注册接口，请联系管理员创建账号')
      }
    }
  })
}
</script>
<style scoped>
.register-container { 
  display: flex; 
  justify-content: center; 
  align-items: center; 
  height: 100vh; 
  background: linear-gradient(135deg, #74ebd5 0%, #acb6e5 100%); 
}
.register-box { 
  width: 400px; 
  padding: 30px; 
  box-sizing: border-box; 
  border-radius: 10px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}
</style>