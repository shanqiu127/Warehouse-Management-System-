<template>
  <component :is="activeHomeComponent" />
</template>

<script setup>
import { computed } from 'vue'
import AdminHome from './home/components/AdminHome.vue'
import EmployeeHome from './home/components/EmployeeHome.vue'
import SuperAdminHome from './home/components/SuperAdminHome.vue'
import { getRole, normalizeRole } from '@/utils/auth'

const activeHomeComponent = computed(() => {
  const role = normalizeRole(getRole())
  if (role === 'superadmin') return SuperAdminHome
  if (role === 'employee') return EmployeeHome
  return AdminHome
})
</script>