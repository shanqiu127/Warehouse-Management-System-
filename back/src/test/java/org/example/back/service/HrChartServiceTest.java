package org.example.back.service;

import org.example.back.common.exception.BusinessException;
import org.example.back.entity.SysDept;
import org.example.back.entity.SysUser;
import org.example.back.mapper.SysDeptMapper;
import org.example.back.mapper.SysUserMapper;
import org.example.back.vo.HrEmployeeChartVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrChartServiceTest {

    @Mock
    private SysDeptMapper sysDeptMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private AuthzService authzService;

    @InjectMocks
    private HrChartService hrChartService;

    @Test
    void employeeDistribution_shouldCountActiveUsersIncludingAdminAndSuperadmin() {
        SysDept salesDept = new SysDept();
        salesDept.setId(1L);
        salesDept.setDeptName("销售部");
        salesDept.setDeptCode("sales");

        SysDept purchaseDept = new SysDept();
        purchaseDept.setId(2L);
        purchaseDept.setDeptName("采购部");
        purchaseDept.setDeptCode("purchase");

        SysDept systemDept = new SysDept();
        systemDept.setId(3L);
        systemDept.setDeptName("系统管理部");
        systemDept.setDeptCode("system_management");

        SysUser salesAdmin = new SysUser();
        salesAdmin.setDeptId(1L);
        salesAdmin.setRole("admin");
        salesAdmin.setStatus(1);

        SysUser salesEmployee = new SysUser();
        salesEmployee.setDeptId(1L);
        salesEmployee.setRole("employee");
        salesEmployee.setStatus(1);

        SysUser purchaseEmployee = new SysUser();
        purchaseEmployee.setDeptId(2L);
        purchaseEmployee.setRole("employee");
        purchaseEmployee.setStatus(1);

        SysUser superadmin = new SysUser();
        superadmin.setDeptId(null);
        superadmin.setRole("superadmin");
        superadmin.setStatus(1);

        when(authzService.isHrAdmin()).thenReturn(true);
        when(authzService.normalizeDeptCode(any())).thenAnswer(invocation -> {
            Object value = invocation.getArgument(0);
            return value == null ? "" : String.valueOf(value).trim().toLowerCase();
        });
        when(authzService.normalizeRole(any())).thenAnswer(invocation -> {
            Object value = invocation.getArgument(0);
            return value == null ? "" : String.valueOf(value).trim().toLowerCase();
        });
        when(sysDeptMapper.selectList(any())).thenReturn(List.of(salesDept, purchaseDept, systemDept));
        when(sysUserMapper.selectList(any())).thenReturn(List.of(salesAdmin, salesEmployee, purchaseEmployee, superadmin));

        HrEmployeeChartVO result = hrChartService.employeeDistribution();

        Map<String, BigDecimal> ratioMap = result.getDeptStats().stream()
                .collect(Collectors.toMap(item -> item.getDeptName(), item -> item.getRatio()));

        assertEquals(4L, result.getTotalEmployeeCount());
        assertEquals(3L, result.getDeptCount());
        assertEquals(3L, result.getOccupiedDeptCount());
        assertEquals("销售部", result.getTopDeptName());
        assertEquals(2L, result.getTopDeptEmployeeCount());
        assertEquals(3, result.getDeptStats().size());
        assertEquals(0, BigDecimal.valueOf(50.00).compareTo(ratioMap.get("销售部")));
        assertEquals(0, BigDecimal.valueOf(25.00).compareTo(ratioMap.get("采购部")));
        assertEquals(0, BigDecimal.valueOf(25.00).compareTo(ratioMap.get("系统管理部")));
    }

    @Test
    void employeeDistribution_shouldRejectNonHrAdmin() {
        when(authzService.isHrAdmin()).thenReturn(false);

        assertThrows(BusinessException.class, () -> hrChartService.employeeDistribution());
    }
}