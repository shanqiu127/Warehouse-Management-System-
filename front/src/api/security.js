import request from '@/utils/request'

// 安全策略（IP白名单）
export const getIpPolicyPageAPI = (params) => request.get('/system/security/ip-policies/page', { params })
export const getIpPolicyDetailAPI = (id) => request.get(`/system/security/ip-policies/${id}`)
export const createIpPolicyAPI = (data) => request.post('/system/security/ip-policies', data)
export const updateIpPolicyAPI = (id, data) => request.put(`/system/security/ip-policies/${id}`, data)
export const deleteIpPolicyAPI = (id) => request.delete(`/system/security/ip-policies/${id}`)

// 启停策略（enabled: true/false）
export const updateIpPolicyStatusAPI = (id, enabled) => request.put(`/system/security/ip-policies/${id}/status`, { enabled })

// 便于下游页面直接复用的简表接口
export const getEnabledIpPoliciesAPI = () => request.get('/system/security/ip-policies/enabled')
