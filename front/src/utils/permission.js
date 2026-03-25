import { canAccessRoles, getRole } from '@/utils/auth'

/**
 * 自定义指令：v-permission
 * 作用：处理按钮级别的权限控制
 * 用法：<el-button v-permission="['admin']">删除</el-button>
 * 详情：如果当前用户的角色不在指令绑定的数组内，该 DOM 元素将被直接移除。
 */
export default {
  mounted(el, binding) {
    const { value } = binding
    const role = getRole()

    if (value && value instanceof Array && value.length > 0) {
      const permissionRoles = value
      const allowed = canAccessRoles(role, permissionRoles)

      if (!allowed) {
        // 对可交互元素优先禁用并提示，其他元素再移除。
        const tagName = (el.tagName || '').toUpperCase()
        if (["BUTTON", "INPUT", "SELECT", "TEXTAREA"].includes(tagName) || el.className?.includes("el-button")) {
          el.setAttribute('disabled', 'disabled')
          el.setAttribute('aria-disabled', 'true')
          el.style.pointerEvents = 'none'
          el.style.opacity = '0.5'
          if (!el.getAttribute('title')) {
            el.setAttribute('title', '无权限操作')
          }
          return
        }
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else {
      throw new Error(`需要指定权限角色，如 v-permission="['admin']"`)
    }
  }
}
