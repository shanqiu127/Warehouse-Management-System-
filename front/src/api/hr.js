import request from '@/utils/request'

export const getHrEmployeeDistributionAPI = () => request.get('/system/hr-charts/employee-distribution')