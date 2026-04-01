package org.example.back.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.back.common.exception.BusinessException;
import org.example.back.entity.SysDept;
import org.example.back.entity.SysUser;
import org.example.back.mapper.SysDeptMapper;
import org.example.back.mapper.SysUserMapper;
import org.example.back.vo.HrDeptEmployeeStatVO;
import org.example.back.vo.HrEmployeeChartVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HrChartService {

    private static final int DEPT_STATUS_APPROVED = 2;
    private static final int USER_STATUS_ENABLED = 1;

    @Autowired
    private SysDeptMapper sysDeptMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private AuthzService authzService;

    public HrEmployeeChartVO employeeDistribution() {
        requireHrChartAccess();

        LambdaQueryWrapper<SysDept> deptWrapper = new LambdaQueryWrapper<>();
        deptWrapper.eq(SysDept::getStatus, DEPT_STATUS_APPROVED)
                .orderByAsc(SysDept::getDeptName);
        List<SysDept> depts = sysDeptMapper.selectList(deptWrapper);

        HrEmployeeChartVO vo = new HrEmployeeChartVO();
        if (depts.isEmpty()) {
            vo.setTotalEmployeeCount(0L);
            vo.setDeptCount(0L);
            vo.setOccupiedDeptCount(0L);
            vo.setTopDeptEmployeeCount(0L);
            vo.setDeptStats(List.of());
            return vo;
        }

        Set<Long> deptIds = depts.stream().map(SysDept::getId).collect(Collectors.toSet());
        Long systemManagementDeptId = depts.stream()
            .filter(dept -> AuthzService.DEPT_SYSTEM_MANAGEMENT.equals(authzService.normalizeDeptCode(dept.getDeptCode())))
            .map(SysDept::getId)
            .findFirst()
            .orElse(null);

        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getStatus, USER_STATUS_ENABLED);
        List<SysUser> users = sysUserMapper.selectList(userWrapper);

        Map<Long, Long> countMap = users.stream()
            .map(user -> resolveStatDeptId(user, deptIds, systemManagementDeptId))
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        long totalEmployeeCount = countMap.values().stream().mapToLong(Long::longValue).sum();

        List<HrDeptEmployeeStatVO> deptStats = depts.stream().map(dept -> {
            long employeeCount = countMap.getOrDefault(dept.getId(), 0L);
            HrDeptEmployeeStatVO item = new HrDeptEmployeeStatVO();
            item.setDeptId(dept.getId());
            item.setDeptName(dept.getDeptName());
            item.setEmployeeCount(employeeCount);
            item.setRatio(calculateRatio(employeeCount, totalEmployeeCount));
            return item;
        }).toList();

        HrDeptEmployeeStatVO topDept = deptStats.stream()
                .max(Comparator.comparingLong(item -> item.getEmployeeCount() == null ? 0L : item.getEmployeeCount()))
                .orElse(null);

        vo.setTotalEmployeeCount(totalEmployeeCount);
        vo.setDeptCount((long) deptStats.size());
        vo.setOccupiedDeptCount(deptStats.stream().filter(item -> item.getEmployeeCount() != null && item.getEmployeeCount() > 0).count());
        vo.setTopDeptName(topDept == null ? "-" : topDept.getDeptName());
        vo.setTopDeptEmployeeCount(topDept == null || topDept.getEmployeeCount() == null ? 0L : topDept.getEmployeeCount());
        vo.setDeptStats(deptStats);
        return vo;
    }

    private void requireHrChartAccess() {
        if (!authzService.isHrAdmin()) {
            throw BusinessException.forbidden("仅人事部门管理员可访问员工图表");
        }
    }

    private Long resolveStatDeptId(SysUser user, Set<Long> deptIds, Long systemManagementDeptId) {
        if (user == null) {
            return null;
        }
        Long deptId = user.getDeptId();
        if (deptId != null && deptIds.contains(deptId)) {
            return deptId;
        }
        if (AuthzService.ROLE_SUPERADMIN.equals(authzService.normalizeRole(user.getRole()))) {
            return systemManagementDeptId;
        }
        return null;
    }

    private BigDecimal calculateRatio(long employeeCount, long totalEmployeeCount) {
        if (totalEmployeeCount <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(employeeCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalEmployeeCount), 2, RoundingMode.HALF_UP);
    }
}