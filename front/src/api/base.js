import request from '@/utils/request'

export const getSupplierPageAPI = (params) => request.get('/base/suppliers/page', { params })
export const getSupplierOptionsAPI = () => request.get('/base/suppliers/options')
export const getSupplierDetailAPI = (id) => request.get(`/base/suppliers/${id}`)
export const createSupplierAPI = (data) => request.post('/base/suppliers', data)
export const updateSupplierAPI = (id, data) => request.put(`/base/suppliers/${id}`, data)
export const deleteSupplierAPI = (id) => request.delete(`/base/suppliers/${id}`)

export const getGoodsPageAPI = (params) => request.get('/base/goods/page', { params })
export const getStockWarningPageAPI = (params) => request.get('/base/goods/page', {
	params: { ...params, warningOnly: true }
})
export const getGoodsOptionsAPI = () => request.get('/base/goods/options')
export const getGoodsDetailAPI = (id) => request.get(`/base/goods/${id}`)
export const createGoodsAPI = (data) => request.post('/base/goods', data)
export const updateGoodsAPI = (id, data) => request.put(`/base/goods/${id}`, data)
export const deleteGoodsAPI = (id) => request.delete(`/base/goods/${id}`)
