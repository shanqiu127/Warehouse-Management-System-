import request from '@/utils/request'

export const createConversationAPI = (title) =>
  request.post('/assistant/conversation', { title })

export const listConversationsAPI = (pageNum = 1, pageSize = 20) =>
  request.get('/assistant/conversation/list', { params: { pageNum, pageSize } })

export const getConversationMessagesAPI = (id) =>
  request.get(`/assistant/conversation/${id}/messages`)

export const deleteConversationAPI = (id) =>
  request.delete(`/assistant/conversation/${id}`)

export const clearAllConversationsAPI = () =>
  request.delete('/assistant/conversation/all')

export const saveConversationMessagesAPI = (id, data) =>
  request.post(`/assistant/conversation/${id}/messages`, data)
