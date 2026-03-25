import request from '@/utils/request'

// 登录日志查询
export const getLoginLogPageAPI = (params) => request.get('/system/audit/login-logs/page', { params })
export const getLoginLogDetailAPI = (id) => request.get(`/system/audit/login-logs/${id}`)

// 操作日志查询
export const getOperationLogPageAPI = (params) => request.get('/system/audit/operation-logs/page', { params })
export const getOperationLogDetailAPI = (id) => request.get(`/system/audit/operation-logs/${id}`)
