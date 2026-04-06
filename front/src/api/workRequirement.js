import request from '@/utils/request'

// ========== 管理员端 ==========
export const getWorkRequirementPageAPI = (params) => request.get('/system/work-requirements/page', { params })
export const getWorkRequirementDetailAPI = (id) => request.get(`/system/work-requirements/${id}`)
export const getWorkRequirementPendingReviewReminderAPI = () => request.get('/system/work-requirements/pending-review-reminder')
export const getWorkRequirementOverdueReminderAPI = () => request.get('/system/work-requirements/overdue-reminder')
export const createWorkRequirementAPI = (data) => request.post('/system/work-requirements', data)
export const deleteWorkRequirementAPI = (id) => request.delete(`/system/work-requirements/${id}`)
export const reviewWorkRequirementAPI = (assignId, data) => request.put(`/system/work-requirements/assign/${assignId}/review`, data)
export const getDeptEmployeesAPI = () => request.get('/system/work-requirements/dept-employees')

// ========== 员工端 ==========
export const getMyWorkRequirementsAPI = () => request.get('/home/work-requirements')
export const getWorkRequirementAssignDetailAPI = (assignId) => request.get(`/home/work-requirements/${assignId}`)
export const acceptWorkRequirementAPI = (assignId) => request.put(`/home/work-requirements/${assignId}/accept`)
export const rejectWorkRequirementAPI = (assignId) => request.put(`/home/work-requirements/${assignId}/reject`)
export const submitWorkRequirementAPI = (assignId, data) => request.put(`/home/work-requirements/${assignId}/submit`, data)
