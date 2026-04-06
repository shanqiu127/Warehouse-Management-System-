package org.example.back.service;

import org.example.back.common.exception.BusinessException;
import org.example.back.dto.LoginResponse;
import org.example.back.dto.WorkRequirementExecuteDTO;
import org.example.back.entity.WorkRequirement;
import org.example.back.entity.WorkRequirementAssign;
import org.example.back.entity.WorkRequirementAttachment;
import org.example.back.mapper.SysUserMapper;
import org.example.back.mapper.WorkRequirementAssignMapper;
import org.example.back.mapper.WorkRequirementAttachmentMapper;
import org.example.back.mapper.WorkRequirementMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkRequirementServiceTest {

    @Mock
    private WorkRequirementMapper workRequirementMapper;

    @Mock
    private WorkRequirementAssignMapper assignMapper;

    @Mock
    private WorkRequirementAttachmentMapper attachmentMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private AuthzService authzService;

    @Mock
    private MessageService messageService;

    @Mock
    private WorkRequirementAttachmentStorageService attachmentStorageService;

    @InjectMocks
    private WorkRequirementService workRequirementService;

    @Test
    void submitExecution_shouldRejectForeignAttachmentId() {
        LoginResponse.UserInfoVO currentUser = new LoginResponse.UserInfoVO();
        currentUser.setId(10L);

        WorkRequirementAssign assign = new WorkRequirementAssign();
        assign.setId(1L);
        assign.setRequirementId(2L);
        assign.setEmployeeUserId(10L);
        assign.setStatus(1);

        WorkRequirement requirement = new WorkRequirement();
        requirement.setId(2L);
        requirement.setEndTime(LocalDateTime.now().plusDays(1));

        WorkRequirementAttachment currentAttachment = new WorkRequirementAttachment();
        currentAttachment.setId(100L);
        currentAttachment.setAssignId(1L);
        currentAttachment.setFileName("proof.png");
        currentAttachment.setFilePath("/uploads/2026/04/06/proof.png");

        WorkRequirementExecuteDTO dto = new WorkRequirementExecuteDTO();
        dto.setAssignId(1L);
        dto.setExecuteResult("已完成");
        dto.setExistingAttachmentIds(List.of(999L));

        when(authzService.currentUser()).thenReturn(currentUser);
        when(assignMapper.selectById(1L)).thenReturn(assign);
        when(workRequirementMapper.selectById(2L)).thenReturn(requirement);
        when(attachmentMapper.selectList(any())).thenReturn(List.of(currentAttachment));

        BusinessException ex = assertThrows(BusinessException.class, () -> workRequirementService.submitExecution(dto));

        assertEquals(403, ex.getCode());
        assertEquals("附件引用无效或不属于当前工作要求", ex.getMsg());
        verify(assignMapper).updateById(any(WorkRequirementAssign.class));
        verify(attachmentMapper, never()).delete(any());
        verifyNoInteractions(attachmentStorageService);
    }

    @Test
    void delete_shouldRemoveAttachmentRowsAndFiles() {
        LoginResponse.UserInfoVO currentUser = new LoginResponse.UserInfoVO();
        currentUser.setId(99L);

        WorkRequirement requirement = new WorkRequirement();
        requirement.setId(8L);
        requirement.setCreatorId(99L);

        WorkRequirementAssign assign = new WorkRequirementAssign();
        assign.setId(18L);
        assign.setRequirementId(8L);

        WorkRequirementAttachment attachment = new WorkRequirementAttachment();
        attachment.setId(28L);
        attachment.setAssignId(18L);
        attachment.setFilePath("/uploads/2026/04/06/delete-proof.png");

        when(authzService.currentUser()).thenReturn(currentUser);
        when(workRequirementMapper.selectById(8L)).thenReturn(requirement);
        when(assignMapper.selectList(any())).thenReturn(List.of(assign));
        when(attachmentMapper.selectList(any())).thenReturn(List.of(attachment));

        workRequirementService.delete(8L);

        verify(attachmentMapper).delete(any());
        verify(attachmentStorageService).deleteStoredFileQuietly("/uploads/2026/04/06/delete-proof.png");
        verify(workRequirementMapper).deleteById(8L);
        verify(assignMapper).deleteById(18L);
    }
}