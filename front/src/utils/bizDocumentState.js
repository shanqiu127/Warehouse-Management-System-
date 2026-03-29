const APPROVAL_PENDING = 1
const APPROVAL_APPROVED = 2
const APPROVAL_REJECTED = 3
const APPROVAL_PROCESSING = 4

const ACTION_VOID_RED = 'void_red'

export const isBizDocumentDeleted = (row = {}) => Boolean(row?.__uiDeleted || Number(row?.isDeleted || 0) === 1)

export const resolveBizDocumentState = (row = {}) => {
  if (isBizDocumentDeleted(row)) {
    return { label: '已删除', type: 'danger' }
  }

  const approvalStatus = Number(row?.approvalStatus || 0)
  const approvalAction = String(row?.approvalRequestAction || '').trim().toLowerCase()

  if (approvalStatus === APPROVAL_PENDING || approvalStatus === APPROVAL_PROCESSING) {
    return { label: '待审批', type: 'warning' }
  }

  if (approvalStatus === APPROVAL_REJECTED) {
    return { label: '驳回', type: 'danger' }
  }

  if (approvalStatus === APPROVAL_APPROVED) {
    return {
      label: approvalAction === ACTION_VOID_RED ? '作废并红冲成功' : '作废成功',
      type: 'success'
    }
  }

  if (Number(row?.bizStatus || 0) === 2) {
    return { label: '作废成功', type: 'success' }
  }

  if (Number(row?.bizStatus || 0) === 3) {
    return { label: '作废并红冲成功', type: 'success' }
  }

  return null
}

export const hasBizDocumentWorkflowState = (row = {}) => Boolean(resolveBizDocumentState(row))