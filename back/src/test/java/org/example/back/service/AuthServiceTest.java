package org.example.back.service;

import cn.hutool.crypto.digest.BCrypt;
import org.example.back.dto.RegisterRequest;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysDeptMapper sysDeptMapper;

    @Mock
    private SysEmployeeMapper sysEmployeeMapper;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldCreateUserAndEmployeeProfile() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new_employee");
        request.setPassword("123456");
        request.setRealName("新员工");
        request.setDeptId(2L);

        SysDept dept = new SysDept();
        dept.setId(2L);
        dept.setDeptCode("sales");
        dept.setStatus(2);

        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(sysDeptMapper.selectById(2L)).thenReturn(dept);
        doAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(100L);
            return 1;
        }).when(sysUserMapper).insert(any(SysUser.class));

        authService.register(request);

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper, times(1)).insert(userCaptor.capture());
        SysUser savedUser = userCaptor.getValue();
        assertEquals("new_employee", savedUser.getUsername());
        assertEquals("新员工", savedUser.getRealName());
        assertEquals("employee", savedUser.getRole());
        assertEquals(2L, savedUser.getDeptId());
        assertEquals(1, savedUser.getStatus());
        assertTrue(BCrypt.checkpw("123456", savedUser.getPassword()));

        ArgumentCaptor<SysEmployee> employeeCaptor = ArgumentCaptor.forClass(SysEmployee.class);
        verify(sysEmployeeMapper, times(1)).insert(employeeCaptor.capture());
        SysEmployee savedEmployee = employeeCaptor.getValue();
        assertEquals(100L, savedEmployee.getUserId());
        assertNotNull(savedEmployee.getEmpCode());
        assertTrue(savedEmployee.getEmpCode().startsWith("EMP"));
        assertEquals("新员工", savedEmployee.getEmpName());
        assertEquals(2L, savedEmployee.getDeptId());
        assertEquals("普通员工", savedEmployee.getPosition());
        assertEquals(1, savedEmployee.getStatus());
    }

    @Test
    void register_shouldRejectNonApprovedDept() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("pending_user");
        request.setPassword("123456");
        request.setRealName("待审批员工");
        request.setDeptId(9L);

        SysDept dept = new SysDept();
        dept.setId(9L);
        dept.setDeptCode("pending_dept");
        dept.setStatus(1);

        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(sysDeptMapper.selectById(9L)).thenReturn(dept);

        org.junit.jupiter.api.Assertions.assertThrows(
                org.example.back.common.exception.BusinessException.class,
                () -> authService.register(request)
        );
    }
}