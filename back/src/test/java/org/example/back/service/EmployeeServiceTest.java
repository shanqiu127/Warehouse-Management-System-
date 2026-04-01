package org.example.back.service;

import cn.hutool.crypto.digest.BCrypt;
import org.example.back.dto.EmployeeSaveDTO;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private SysEmployeeMapper sysEmployeeMapper;

    @Mock
    private SysDeptMapper sysDeptMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private AuthzService authzService;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void create_shouldCreateLinkedUserAndEmployeeProfile() {
        EmployeeSaveDTO dto = new EmployeeSaveDTO();
        dto.setUsername("employee_a");
        dto.setEmpName("员工甲");
        dto.setDeptId(2L);
        dto.setPosition("仓管员");
        dto.setPhone("13800000001");
        dto.setEmail("employee_a@test.com");
        dto.setStatus(1);

        SysDept dept = new SysDept();
        dept.setId(2L);
        dept.setDeptCode("warehouse");
        dept.setStatus(2);

        when(sysDeptMapper.selectById(2L)).thenReturn(dept);
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(authzService.currentOperatorLabel()).thenReturn("人事部管理员");
        doAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(88L);
            return 1;
        }).when(sysUserMapper).insert(any(SysUser.class));

        employeeService.create(dto);

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper, times(1)).insert(userCaptor.capture());
        SysUser savedUser = userCaptor.getValue();
        assertEquals("employee_a", savedUser.getUsername());
        assertEquals("员工甲", savedUser.getRealName());
        assertEquals("employee", savedUser.getRole());
        assertEquals(2L, savedUser.getDeptId());
        assertEquals(1, savedUser.getStatus());
        assertTrue(BCrypt.checkpw("123456", savedUser.getPassword()));

        ArgumentCaptor<SysEmployee> employeeCaptor = ArgumentCaptor.forClass(SysEmployee.class);
        verify(sysEmployeeMapper, times(1)).insert(employeeCaptor.capture());
        SysEmployee savedEmployee = employeeCaptor.getValue();
        assertEquals(88L, savedEmployee.getUserId());
        assertEquals("员工甲", savedEmployee.getEmpName());
        assertEquals("仓管员", savedEmployee.getPosition());
        assertEquals("13800000001", savedEmployee.getPhone());
        assertEquals("employee_a@test.com", savedEmployee.getEmail());

        verify(messageService, times(1)).sendNewEmployeePasswordReminder("员工甲", 2L, "人事部管理员");
    }

    @Test
    void update_shouldSyncUserAndEmployeeProfile() {
        EmployeeSaveDTO dto = new EmployeeSaveDTO();
        dto.setUsername("employee_b_new");
        dto.setEmpName("员工乙-更新");
        dto.setDeptId(3L);
        dto.setPosition("采购专员");
        dto.setPhone("13800000002");
        dto.setEmail("employee_b_new@test.com");
        dto.setStatus(0);

        SysDept dept = new SysDept();
        dept.setId(3L);
        dept.setDeptCode("purchase");
        dept.setStatus(2);

        SysEmployee employee = new SysEmployee();
        employee.setId(5L);
        employee.setUserId(99L);
        employee.setEmpCode("EMP001");
        employee.setEmpName("员工乙");
        employee.setDeptId(2L);
        employee.setPosition("普通员工");
        employee.setStatus(1);

        SysUser user = new SysUser();
        user.setId(99L);
        user.setUsername("employee_b");
        user.setRealName("员工乙");
        user.setRole("employee");
        user.setDeptId(2L);
        user.setStatus(1);

        when(sysDeptMapper.selectById(3L)).thenReturn(dept);
        when(sysEmployeeMapper.selectById(5L)).thenReturn(employee);
        when(sysUserMapper.selectById(99L)).thenReturn(user);
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(authzService.normalizeRole(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(authzService.currentOperatorLabel()).thenReturn("人事部管理员");

        employeeService.update(5L, dto);

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper, times(1)).updateById(userCaptor.capture());
        SysUser updatedUser = userCaptor.getValue();
        assertEquals("employee_b_new", updatedUser.getUsername());
        assertEquals("员工乙-更新", updatedUser.getRealName());
        assertEquals(3L, updatedUser.getDeptId());
        assertEquals(0, updatedUser.getStatus());

        ArgumentCaptor<SysEmployee> employeeCaptor = ArgumentCaptor.forClass(SysEmployee.class);
        verify(sysEmployeeMapper, times(1)).updateById(employeeCaptor.capture());
        SysEmployee updatedEmployee = employeeCaptor.getValue();
        assertEquals(99L, updatedEmployee.getUserId());
        assertEquals("员工乙-更新", updatedEmployee.getEmpName());
        assertEquals(3L, updatedEmployee.getDeptId());
        assertEquals("采购专员", updatedEmployee.getPosition());
        assertEquals(0, updatedEmployee.getStatus());

        verify(messageService, times(1)).sendEmployeeTransferReminders("员工乙-更新", 2L, 3L, "人事部管理员");
        verify(messageService, times(1)).sendEmployeeLeftReminder("员工乙-更新", 3L, "人事部管理员");
    }

    @Test
    void delete_shouldDeleteLinkedUserAndSendReminder() {
        SysEmployee employee = new SysEmployee();
        employee.setId(6L);
        employee.setUserId(101L);
        employee.setEmpName("员工丙");
        employee.setDeptId(5L);

        when(sysEmployeeMapper.selectById(6L)).thenReturn(employee);
        when(authzService.currentOperatorLabel()).thenReturn("人事部管理员");

        employeeService.delete(6L);

        verify(messageService, times(1)).sendEmployeeDeletedReminder("员工丙", 5L, "人事部管理员");
        verify(sysEmployeeMapper, times(1)).deleteById(6L);
        verify(sysUserMapper, times(1)).deleteById(101L);
    }
}