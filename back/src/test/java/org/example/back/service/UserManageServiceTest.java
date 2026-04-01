package org.example.back.service;

import org.example.back.common.exception.BusinessException;
import org.example.back.dto.LoginResponse;
import org.example.back.dto.UserSaveDTO;
import org.example.back.entity.SysDept;
import org.example.back.entity.SysEmployee;
import org.example.back.entity.SysUser;
import org.example.back.mapper.SysDeptMapper;
import org.example.back.mapper.SysEmployeeMapper;
import org.example.back.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManageServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysDeptMapper sysDeptMapper;

    @Mock
    private SysEmployeeMapper sysEmployeeMapper;

    @Mock
    private AuthzService authzService;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private UserManageService userManageService;

    @Test
    void create_shouldCreateEmployeeProfileWhenTargetRoleIsEmployee() {
        UserSaveDTO dto = new UserSaveDTO();
        dto.setUsername("employee_from_user_page");
        dto.setRealName("用户页员工");
        dto.setRole("employee");
        dto.setDeptId(4L);
        dto.setStatus(1);
        dto.setPhone("13800000011");
        dto.setEmail("employee_from_user_page@test.com");

        LoginResponse.UserInfoVO operator = new LoginResponse.UserInfoVO();
        operator.setRole("superadmin");

        SysDept dept = new SysDept();
        dept.setId(4L);
        dept.setDeptCode("sales");

        when(authzService.currentUser()).thenReturn(operator);
        when(authzService.isAdmin()).thenReturn(false);
        when(authzService.isSuperAdmin()).thenReturn(true);
        when(authzService.currentOperatorLabel()).thenReturn("超级管理员");
        when(authzService.requireDept(4L)).thenReturn(dept);
        when(authzService.normalizeRole(any())).thenAnswer(invocation -> {
            Object value = invocation.getArgument(0);
            return value == null ? "" : String.valueOf(value).trim().toLowerCase();
        });
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(77L);
            return 1;
        }).when(sysUserMapper).insert(any(SysUser.class));

        userManageService.create(dto);

        ArgumentCaptor<SysEmployee> employeeCaptor = ArgumentCaptor.forClass(SysEmployee.class);
        verify(sysEmployeeMapper, times(1)).insert(employeeCaptor.capture());
        SysEmployee savedEmployee = employeeCaptor.getValue();
        assertEquals(77L, savedEmployee.getUserId());
        assertEquals("用户页员工", savedEmployee.getEmpName());
        assertEquals(4L, savedEmployee.getDeptId());
        assertEquals("普通员工", savedEmployee.getPosition());

        verify(messageService, times(1)).sendNewEmployeePasswordReminder("用户页员工", 4L, "超级管理员");
    }

    @Test
    void delete_shouldDeleteLinkedEmployeeProfileForEmployeeUser() {
        SysUser user = new SysUser();
        user.setId(77L);
        user.setRealName("待删除员工");
        user.setRole("employee");
        user.setDeptId(4L);

        when(sysUserMapper.selectById(77L)).thenReturn(user);
        when(authzService.isSuperAdmin()).thenReturn(true);
        when(authzService.currentOperatorLabel()).thenReturn("超级管理员");
        when(authzService.normalizeRole(any())).thenAnswer(invocation -> {
            Object value = invocation.getArgument(0);
            return value == null ? "" : String.valueOf(value).trim().toLowerCase();
        });

        userManageService.delete(77L);

        verify(sysEmployeeMapper, times(1)).delete(any());
        verify(sysUserMapper, times(1)).deleteById(77L);
        verify(messageService, times(1)).sendEmployeeDeletedReminder("待删除员工", 4L, "超级管理员");
    }

    @Test
    void create_shouldRejectHrAdminAcrossDepartmentsForEmployeeUsers() {
        UserSaveDTO dto = new UserSaveDTO();
        dto.setUsername("employee_cross_dept");
        dto.setRealName("跨部门员工");
        dto.setRole("employee");
        dto.setDeptId(5L);
        dto.setStatus(1);

        LoginResponse.UserInfoVO operator = new LoginResponse.UserInfoVO();
        operator.setRole("admin");
        operator.setDeptId(2L);
        operator.setDeptCode("hr");

        SysDept dept = new SysDept();
        dept.setId(5L);
        dept.setDeptCode("purchase");

        when(authzService.currentUser()).thenReturn(operator);
        when(authzService.isAdmin()).thenReturn(true);
        when(authzService.requireDept(5L)).thenReturn(dept);
        when(authzService.normalizeRole(any())).thenAnswer(invocation -> {
            Object value = invocation.getArgument(0);
            return value == null ? "" : String.valueOf(value).trim().toLowerCase();
        });
        when(authzService.normalizeDeptCode(any())).thenAnswer(invocation -> {
            Object value = invocation.getArgument(0);
            return value == null ? "" : String.valueOf(value).trim().toLowerCase();
        });

        assertThrows(BusinessException.class, () -> userManageService.create(dto));

        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }
}