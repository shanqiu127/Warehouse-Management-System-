<template>
  <div class="register-page">
    <div class="paper-layer" aria-hidden="true">
      <span class="paper-grain"></span>
      <span class="paper-line"></span>
    </div>

    <main class="register-shell">
      <section class="brand-panel">
        <p class="brand-tag">WAREHOUSE MANAGEMENT SYSTEM</p>
        <h1>开始你的<br>仓储协作</h1>
        <p class="brand-desc">
          新账号仅用于普通员工协作接入，
          建立统一库存视图并参与标准业务流程。
        </p>
        <ul class="brand-metrics">
          <li>
            <strong>规范</strong>
            <span>账号权限边界清晰</span>
          </li>
          <li>
            <strong>统一</strong>
            <span>流程数据同源管理</span>
          </li>
          <li>
            <strong>可追溯</strong>
            <span>业务轨迹完整留痕</span>
          </li>
        </ul>
      </section>

      <section class="form-panel">
        <div class="panel-header">
          <p class="kicker">SIGN UP</p>
          <h2>创建员工账号</h2>
          <p class="subtitle">注册仅开放给普通用户（employee）</p>
        </div>

        <el-form :model="form" :rules="rules" ref="formRef" label-width="0" class="register-form">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入账号" size="large" autocomplete="username" />
          </el-form-item>

          <el-form-item prop="realName">
            <el-input v-model="form.realName" placeholder="请输入真实姓名" size="large" autocomplete="name" />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              size="large"
              autocomplete="new-password"
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请确认密码"
              show-password
              size="large"
              autocomplete="new-password"
              @keyup.enter="handleRegister"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" size="large" class="submit-btn" :loading="submitting" @click="handleRegister">
              创建账号
            </el-button>
          </el-form-item>

          <div class="action-row">
            <span>已有账号？</span>
            <el-link type="primary" @click="$router.push('/login')">返回登录</el-link>
          </div>
        </el-form>
      </section>
    </main>
  </div>
</template>
<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerAPI } from '@/api/user'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)

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
      submitting.value = true
      try {
        await registerAPI({ username: form.username, realName: form.realName, password: form.password })
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } catch (error) {
        ElMessage.error(error?.response?.data?.msg || '当前版本后端尚未开放注册接口，请联系管理员创建账号')
      } finally {
        submitting.value = false
      }
    }
  })
}
</script>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Cinzel:wght@500;700&family=Manrope:wght@400;500;700&display=swap');

:global(body) {
  margin: 0;
}

.register-page {
  --paper: #f4efe6;
  --paper-soft: #f8f3eb;
  --ink: #1f1f1d;
  --ink-soft: #6d675f;
  --line: rgba(34, 31, 27, 0.2);
  --accent: #2e4e66;

  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 28px;
  font-family: 'Manrope', 'Segoe UI', sans-serif;
  background:
    radial-gradient(circle at 10% 12%, rgba(198, 176, 147, 0.18), transparent 38%),
    radial-gradient(circle at 86% 16%, rgba(160, 183, 201, 0.14), transparent 36%),
    linear-gradient(135deg, #f8f4ec 0%, #f2ece2 58%, #ede6db 100%);
}

.paper-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.paper-grain {
  position: absolute;
  inset: 0;
  opacity: 0.26;
  background-image: radial-gradient(rgba(0, 0, 0, 0.12) 0.45px, transparent 0.45px);
  background-size: 4px 4px;
}

.paper-line {
  position: absolute;
  inset: 20px;
  border: 1px solid rgba(51, 44, 35, 0.12);
}

.register-shell {
  width: min(1080px, 100%);
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  border: 1px solid rgba(51, 44, 35, 0.2);
  border-radius: 6px;
  overflow: hidden;
  background: linear-gradient(145deg, var(--paper) 0%, var(--paper-soft) 100%);
  box-shadow:
    0 14px 34px rgba(63, 49, 38, 0.12),
    0 2px 0 rgba(255, 255, 255, 0.45) inset;
}

.brand-panel {
  position: relative;
  padding: clamp(30px, 5vw, 64px);
  color: var(--ink);
  border-right: 1px solid rgba(51, 44, 35, 0.18);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.brand-tag {
  font-size: 11px;
  letter-spacing: 0.32em;
  color: var(--accent);
  margin: 0 0 16px;
}

.brand-panel h1 {
  margin: 0;
  font-family: 'Cinzel', 'Times New Roman', serif;
  font-weight: 700;
  line-height: 1.04;
  letter-spacing: 0.01em;
  font-size: clamp(2.2rem, 4.6vw, 4rem);
  text-wrap: balance;
}

.brand-desc {
  margin: 26px 0 34px;
  max-width: 44ch;
  line-height: 1.9;
  color: var(--ink-soft);
}

.brand-metrics {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.brand-metrics li {
  min-width: 120px;
  padding: 14px 16px;
  border: 1px solid rgba(44, 37, 31, 0.16);
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.3);
}

.brand-metrics strong {
  display: block;
  color: var(--ink);
  font-size: 1.02rem;
  margin-bottom: 6px;
}

.brand-metrics span {
  font-size: 0.82rem;
  color: var(--ink-soft);
}

.form-panel {
  padding: clamp(24px, 5vw, 58px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: var(--ink);
}

.panel-header {
  margin-bottom: 28px;
}

.kicker {
  margin: 0 0 12px;
  font-size: 11px;
  letter-spacing: 0.3em;
  color: var(--accent);
}

.panel-header h2 {
  margin: 0 0 8px;
  font-family: 'Cinzel', 'Times New Roman', serif;
  font-size: clamp(1.7rem, 3vw, 2.4rem);
  font-weight: 700;
}

.subtitle {
  margin: 0;
  color: var(--ink-soft);
}

.register-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.register-form :deep(.el-input__wrapper) {
  border-radius: 2px;
  border: 1px solid rgba(34, 31, 27, 0.18);
  box-shadow: none;
  background: rgba(255, 255, 255, 0.58);
  transition: border-color 0.28s ease, box-shadow 0.28s ease, transform 0.28s ease;
}

.register-form :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(46, 78, 102, 0.58);
  box-shadow: 0 0 0 3px rgba(46, 78, 102, 0.13);
  transform: translateY(-1px);
}

.register-form :deep(.el-input__inner) {
  color: #1f1f1d;
  height: 46px;
}

.register-form :deep(.el-input__inner::placeholder) {
  color: #8a847c;
}

.submit-btn {
  width: 100%;
  border: 1px solid #1d2e3c;
  height: 46px;
  border-radius: 2px;
  font-weight: 700;
  letter-spacing: 0.08em;
  background: linear-gradient(90deg, #263f54 0%, #355670 100%);
  color: #f9f5ee;
  box-shadow: 0 8px 16px rgba(39, 60, 77, 0.18);
  transition: transform 0.28s ease, box-shadow 0.28s ease, filter 0.28s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 22px rgba(39, 60, 77, 0.22);
  filter: brightness(1.03);
}

.submit-btn:active {
  transform: translateY(0);
}

.action-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  font-size: 0.92rem;
  color: var(--ink-soft);
}

.action-row :deep(.el-link__inner) {
  font-weight: 700;
}

@media (max-width: 980px) {
  .register-shell {
    grid-template-columns: 1fr;
    max-width: 680px;
  }

  .brand-panel {
    border-right: none;
    border-bottom: 1px solid rgba(51, 44, 35, 0.18);
  }
}

@media (max-width: 640px) {
  .register-page {
    padding: 14px;
  }

  .brand-metrics {
    gap: 10px;
  }

  .brand-metrics li {
    flex: 1 1 100%;
  }

  .action-row {
    justify-content: space-between;
  }
}

@media (prefers-reduced-motion: reduce) {
  .submit-btn,
  .register-form :deep(.el-input__wrapper) {
    animation: none;
    transition: none;
  }
}
</style>