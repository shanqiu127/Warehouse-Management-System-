import request from '@/utils/request'

export const getMessagePageAPI = (params) => request.get('/system/messages/page', { params })

export const getUnreadMessageCountAPI = () => request.get('/system/messages/unread-count')

export const markMessageReadAPI = (id) => request.put(`/system/messages/${id}/read`)

export const markAllMessagesReadAPI = () => request.put('/system/messages/read-all')

export const deleteAllReadMessagesAPI = () => request.delete('/system/messages/read')