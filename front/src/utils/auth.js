const TOKEN_KEY = "token"
const ROLE_KEY = "role"
const DEPT_ID_KEY = "deptId"
const DEPT_CODE_KEY = "deptCode"
const DEPT_NAME_KEY = "deptName"
const USERNAME_KEY = "username"
const REAL_NAME_KEY = "realName"

export const normalizeRole = (role) => String(role || "").trim().toLowerCase()

export const normalizeDeptCode = (deptCode) => String(deptCode || "").trim().toLowerCase()

const setOptionalStorage = (key, value) => {
  if (value === undefined || value === null || value === "") {
    localStorage.removeItem(key)
    return
  }
  localStorage.setItem(key, String(value))
}

export const getToken = () => localStorage.getItem(TOKEN_KEY) || ""

export const getRole = () => normalizeRole(localStorage.getItem(ROLE_KEY))

export const getDeptId = () => {
  const value = localStorage.getItem(DEPT_ID_KEY)
  if (value === null || value === "") {
    return null
  }
  const parsed = Number(value)
  return Number.isNaN(parsed) ? null : parsed
}

export const getDeptCode = () => normalizeDeptCode(localStorage.getItem(DEPT_CODE_KEY))

export const getDeptName = () => localStorage.getItem(DEPT_NAME_KEY) || ""

export const getAuthContext = () => ({
  role: getRole(),
  deptId: getDeptId(),
  deptCode: getDeptCode(),
  deptName: getDeptName(),
  username: localStorage.getItem(USERNAME_KEY) || "",
  realName: localStorage.getItem(REAL_NAME_KEY) || ""
})

export const setToken = (token) => {
  localStorage.setItem(TOKEN_KEY, token || "")
}

export const setRole = (role) => {
  localStorage.setItem(ROLE_KEY, normalizeRole(role))
}

export const setUserInfo = (userInfo = {}) => {
  setRole(userInfo.role)
  setOptionalStorage(USERNAME_KEY, userInfo.username)
  setOptionalStorage(REAL_NAME_KEY, userInfo.realName)
  setOptionalStorage(DEPT_ID_KEY, userInfo.deptId)
  setOptionalStorage(DEPT_CODE_KEY, normalizeDeptCode(userInfo.deptCode))
  setOptionalStorage(DEPT_NAME_KEY, userInfo.deptName)
}

export const clearAuth = () => {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(ROLE_KEY)
  localStorage.removeItem(DEPT_ID_KEY)
  localStorage.removeItem(DEPT_CODE_KEY)
  localStorage.removeItem(DEPT_NAME_KEY)
  localStorage.removeItem(USERNAME_KEY)
  localStorage.removeItem(REAL_NAME_KEY)
}

export const hasRole = (currentRole, allowRoles = []) => {
  if (!currentRole || !Array.isArray(allowRoles)) {
    return false
  }

  const normalizedRole = normalizeRole(currentRole)
  const normalizedAllowRoles = allowRoles.map((item) => normalizeRole(item))

  return normalizedAllowRoles.includes(normalizedRole)
    || (normalizedRole === "superadmin" && normalizedAllowRoles.includes("admin"))
}

export const hasDeptAccess = (currentDeptCode, allowDeptCodes = [], currentRole = getRole()) => {
  if (!Array.isArray(allowDeptCodes) || allowDeptCodes.length === 0) {
    return true
  }
  if (isSuperAdmin(currentRole)) {
    return true
  }
  const normalizedDeptCode = normalizeDeptCode(currentDeptCode)
  const normalizedAllowDeptCodes = allowDeptCodes.map((item) => normalizeDeptCode(item))
  return normalizedAllowDeptCodes.includes(normalizedDeptCode)
}

export const isSuperAdmin = (role) => normalizeRole(role) === "superadmin"

export const isAdminRole = (role) => normalizeRole(role) === "admin"

export const isEmployeeRole = (role) => normalizeRole(role) === "employee"

export const isDeptAdmin = (role, deptCode) => isAdminRole(role) && Boolean(normalizeDeptCode(deptCode))

export const isDeptEmployee = (role, deptCode) => isEmployeeRole(role) && Boolean(normalizeDeptCode(deptCode))

export const canAccessRoles = (currentRole, allowRoles = []) => hasRole(currentRole, allowRoles)
