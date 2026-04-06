package org.example.back.service;

import org.example.back.common.exception.BusinessException;
import org.example.back.dto.ApprovalCreateDTO;
import org.example.back.dto.LoginResponse;
import org.example.back.mapper.BizApprovalOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock
    private BizApprovalOrderMapper bizApprovalOrderMapper;

    @Mock
    private AuthService authService;

    @InjectMocks
    private ApprovalService approvalService;

    @Test
    void create_shouldRejectEmployeeRequester() {
        LoginResponse.UserInfoVO requester = new LoginResponse.UserInfoVO();
        requester.setId(100L);
        requester.setRole("employee");
        requester.setDeptCode("sales");

        ApprovalCreateDTO dto = new ApprovalCreateDTO();
        dto.setBizType("purchase");
        dto.setBizId(1L);
        dto.setRequestAction("void");
        dto.setReason("test");

        when(authService.getUserInfo()).thenReturn(requester);

        BusinessException ex = assertThrows(BusinessException.class, () -> approvalService.create(dto));

        assertEquals(403, ex.getCode());
        assertEquals("仅采购或销售部门管理员可提交该类作废审批申请", ex.getMsg());
        verifyNoInteractions(bizApprovalOrderMapper);
    }
}