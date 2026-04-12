import request from '@/utils/request'

export const queryAssistantAPI = (data) =>
  request.post('/assistant/project/query', data, { timeout: 60000 })

export const getAssistantModelsAPI = () =>
  request.get('/assistant/project/models')

export const getSuggestionsAPI = () =>
  request.get('/assistant/project/suggestions')

export const rebuildKnowledgeBaseAPI = () =>
  request.post('/assistant/project/rebuild')

export const getKnowledgeBaseStatusAPI = () =>
  request.get('/assistant/project/status')
