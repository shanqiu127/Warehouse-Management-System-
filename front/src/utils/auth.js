const TOKEN_KEY = "token"
const ROLE_KEY = "role"

export const normalizeRole = (role) => {
  return String(role || "").trim().toLowerCase()
}

export const getToken = () => localStorage.getItem(TOKEN_KEY) || ""

export const getRole = () => normalizeRole(localStorage.getItem(ROLE_KEY))

export const setToken = (token) => {
  localStorage.setItem(TOKEN_KEY, token || "")
}

export const setRole = (role) => {
  localStorage.setItem(ROLE_KEY, normalizeRole(role))
}

export const clearAuth = () => {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(ROLE_KEY)
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

export const isSuperAdmin = (role) => normalizeRole(role) === "superadmin"

export const canAccessRoles = (currentRole, allowRoles = []) => hasRole(currentRole, allowRoles)
