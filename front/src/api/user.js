import request from '@/utils/request'

// 基础身份认证接口封装
export const loginAPI = (data) => request.post('/auth/login', data)
export const logoutAPI = () => request.post('/auth/logout')

// 注册接口（仅普通用户）
export const registerAPI = (data) => request.post('/auth/register', data)
export const getRegisterDeptOptionsAPI = () => request.get('/auth/depts')