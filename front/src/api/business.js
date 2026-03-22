import request from '@/utils/request'

export const getGoodsOptionsAPI = () => request.get('/base/goods/options')

export const getPurchasePageAPI = (params) => request.get('/business/purchases/page', { params })
export const getPurchaseDetailAPI = (id) => request.get(`/business/purchases/${id}`)
export const getReturnablePurchaseOptionsAPI = (params) => request.get('/business/purchases/options/returnable', { params })
export const createPurchaseAPI = (data) => request.post('/business/purchases', data)
export const deletePurchaseAPI = (id) => request.delete(`/business/purchases/${id}`)
export const voidPurchaseAPI = (id, data) => request.put(`/business/purchases/${id}/void`, data)

export const getPurchaseReturnPageAPI = (params) => request.get('/business/purchase-returns/page', { params })
export const getPurchaseReturnDetailAPI = (id) => request.get(`/business/purchase-returns/${id}`)
export const createPurchaseReturnAPI = (data) => request.post('/business/purchase-returns', data)
export const deletePurchaseReturnAPI = (id) => request.delete(`/business/purchase-returns/${id}`)
export const voidPurchaseReturnAPI = (id, data) => request.put(`/business/purchase-returns/${id}/void`, data)

export const getSalesPageAPI = (params) => request.get('/business/sales/page', { params })
export const getSalesDetailAPI = (id) => request.get(`/business/sales/${id}`)
export const getReturnableSalesOptionsAPI = (params) => request.get('/business/sales/options/returnable', { params })
export const createSalesAPI = (data) => request.post('/business/sales', data)
export const deleteSalesAPI = (id) => request.delete(`/business/sales/${id}`)
export const voidSalesAPI = (id, data) => request.put(`/business/sales/${id}/void`, data)

export const getSalesReturnPageAPI = (params) => request.get('/business/sales-returns/page', { params })
export const getSalesReturnDetailAPI = (id) => request.get(`/business/sales-returns/${id}`)
export const createSalesReturnAPI = (data) => request.post('/business/sales-returns', data)
export const deleteSalesReturnAPI = (id) => request.delete(`/business/sales-returns/${id}`)
export const voidSalesReturnAPI = (id, data) => request.put(`/business/sales-returns/${id}/void`, data)

export const getChartOverviewAPI = (params) => request.get('/business/charts/overview', { params })
export const getChartTop5API = (params) => request.get('/business/charts/top5', { params })
export const getChartBrandRatioAPI = (params) => request.get('/business/charts/brand-ratio', { params })
export const getChartDailyTrendAPI = (params) => request.get('/business/charts/daily-trend', { params })
