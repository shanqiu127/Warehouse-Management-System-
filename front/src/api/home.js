import request from '@/utils/request'

export const getHomeSummaryAPI = () => request.get('/home/summary')

export const getEmployeeWorkbenchAPI = () => request.get('/home/employee-workbench')

export const updateEmployeeContactAPI = (data) => request.put('/home/employee-workbench/contact', data)
