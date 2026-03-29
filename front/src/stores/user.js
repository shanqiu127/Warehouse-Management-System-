import { defineStore } from "pinia"
import { ref } from "vue"
import {
  clearAuth,
  getAuthContext,
  getDeptCode,
  getDeptId,
  getDeptName,
  getRole,
  getToken,
  setToken as persistToken,
  setUserInfo as persistUserInfo
} from "@/utils/auth"

export const useUserStore = defineStore("user", () => {
  const initialContext = getAuthContext()
  const token = ref(getToken())
  const role = ref(getRole())
  const deptId = ref(getDeptId())
  const deptCode = ref(getDeptCode())
  const deptName = ref(getDeptName())
  const username = ref(initialContext.username)
  const realName = ref(initialContext.realName)

  const setToken = (newToken) => {
    token.value = newToken
    persistToken(newToken)
  }

  const setUserInfo = (userInfo = {}) => {
    persistUserInfo(userInfo)
    role.value = getRole()
    deptId.value = getDeptId()
    deptCode.value = getDeptCode()
    deptName.value = getDeptName()
    username.value = userInfo.username || ""
    realName.value = userInfo.realName || ""
  }

  const clearToken = () => {
    token.value = ""
    role.value = ""
    deptId.value = null
    deptCode.value = ""
    deptName.value = ""
    username.value = ""
    realName.value = ""
    clearAuth()
  }

  return { token, role, deptId, deptCode, deptName, username, realName, setToken, setUserInfo, clearToken }
})
