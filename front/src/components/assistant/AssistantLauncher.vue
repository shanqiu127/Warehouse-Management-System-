<template>
  <Teleport to="body">
    <button
      class="assistant-fab"
      title="项目助手"
      @click="goAssistant"
    >
      <svg class="fab-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z" fill="currentColor" opacity="0.15"/>
        <path d="M9 9h6M9 12h4M9 15h5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        <rect x="5" y="5" width="14" height="14" rx="3" stroke="currentColor" stroke-width="1.5"/>
      </svg>
      <span class="fab-label">AI</span>
    </button>
  </Teleport>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const goAssistant = () => {
  if (route.path !== '/assistant/project') {
    router.push('/assistant/project')
  }
}
</script>

<style scoped>
.assistant-fab {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 2000;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 18px;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  border: none;
  border-radius: 50px;
  color: #fff;
  font-family: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 24px rgba(37, 99, 235, 0.35);
  transition: all 0.3s ease;
  letter-spacing: 1px;
}

.assistant-fab:hover {
  transform: translateY(-2px) scale(1.03);
  box-shadow: 0 8px 32px rgba(37, 99, 235, 0.45);
}

.assistant-fab:active {
  transform: translateY(0) scale(0.98);
}

.fab-icon {
  width: 20px;
  height: 20px;
}

@media screen and (max-width: 768px) {
  .assistant-fab {
    right: 16px;
    bottom: 16px;
    padding: 10px 14px;
    font-size: 12px;
  }
}
</style>
