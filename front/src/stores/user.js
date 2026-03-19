import { defineStore } from "pinia"
import { ref } from "vue"
import { clearAuth, getRole, getToken, setRole as persistRole, setToken as persistToken } from "@/utils/auth"

export const useUserStore = defineStore("user", () => {
  const token = ref(getToken())
  const role = ref(getRole())

  const setToken = (newToken) => {
    token.value = newToken
    persistToken(newToken)
  }

  const setRole = (newRole) => {
    const roleText = String(newRole || "").trim().toLowerCase()
    const normalizedRole = roleText === "super_admin" ? "superadmin" : roleText
    role.value = normalizedRole
    persistRole(normalizedRole)
  }

  const clearToken = () => {
    token.value = ""
    role.value = ""
    clearAuth()
  }

  return { token, role, setToken, setRole, clearToken }
})
