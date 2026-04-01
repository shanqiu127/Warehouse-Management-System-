package org.example.back.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.back.common.exception.BusinessException;
import org.example.back.common.result.PageResult;
import org.example.back.common.util.CodeGenerator;
import org.example.back.dto.EmployeeQueryDTO;
import org.example.back.dto.EmployeeSaveDTO;
import org.example.back.entity.SysDept;
import org.example.back.entity.SysEmployee;
import org.example.back.entity.SysUser;
import org.example.back.mapper.SysDeptMapper;
import org.example.back.mapper.SysEmployeeMapper;
import org.example.back.mapper.SysUserMapper;
import org.example.back.vo.EmployeeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private static final String DEFAULT_PASSWORD = "123456";
    private static final int DEPT_STATUS_APPROVED = 2;
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_EMPLOYEE = "employee";
    private static final String ROLE_SUPERADMIN = "superadmin";
    private static final String SYSTEM_MANAGEMENT_DEPT_NAME = "系统管理部";
    private static final String SYSTEM_ADMIN_SUFFIX = "系统管理员";
    private static final String SUPER_ADMIN_POSITION = "系统超级管理员";
    private static final String DEFAULT_EMPLOYEE_POSITION = "普通员工";

    @Autowired
    private SysEmployeeMapper sysEmployeeMapper;

    @Autowired
    private SysDeptMapper sysDeptMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private AuthzService authzService;

    @Autowired
    private MessageService messageService;

    private void requireEmployeeModuleAccess() {
        authzService.requireDeptAdminOrSuperAdmin(AuthzService.DEPT_HR, "仅人事部门管理员可访问员工档案");
    }

    public PageResult<EmployeeVO> page(EmployeeQueryDTO queryDTO) {
        requireEmployeeModuleAccess();
        syncRegisteredEmployeeProfiles();
        SysDept systemDept = resolveSystemManagementDept();
        List<EmployeeVO> records = new ArrayList<>();
        records.addAll(listEmployeeRecords(queryDTO));
        records.addAll(listManagementUserRecords(queryDTO, systemDept));
        records.sort(Comparator
                .comparing(EmployeeVO::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(EmployeeVO::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        long total = records.size();
        long pageNum = queryDTO.getPageNum() == null || queryDTO.getPageNum() < 1 ? 1L : queryDTO.getPageNum();
        long pageSize = queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1 ? 10L : queryDTO.getPageSize();
        int fromIndex = (int) Math.min((pageNum - 1) * pageSize, total);
        int toIndex = (int) Math.min(fromIndex + pageSize, total);
        long pages = pageSize == 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResult<>(records.subList(fromIndex, toIndex), total, pageNum, pageSize, pages);
    }

    private List<EmployeeVO> listEmployeeRecords(EmployeeQueryDTO queryDTO) {
        LambdaQueryWrapper<SysEmployee> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getEmpName()), SysEmployee::getEmpName, queryDTO.getEmpName())
                .eq(queryDTO.getDeptId() != null, SysEmployee::getDeptId, queryDTO.getDeptId())
                .eq(queryDTO.getStatus() != null, SysEmployee::getStatus, queryDTO.getStatus())
                .orderByDesc(SysEmployee::getCreateTime)
                .orderByDesc(SysEmployee::getId);

        List<SysEmployee> employees = sysEmployeeMapper.selectList(wrapper);
        Map<Long, SysDept> deptMap = buildDeptMap(employees.stream().map(SysEmployee::getDeptId).collect(Collectors.toSet()));
        Map<Long, SysUser> userMap = buildUserMap(employees.stream()
                .map(SysEmployee::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return employees.stream().map(item -> toVO(item, deptMap.get(item.getDeptId()), userMap.get(item.getUserId()))).toList();
    }

    public EmployeeVO getById(Long id) {
        requireEmployeeModuleAccess();
        assertEditableEmployeeId(id);
        SysEmployee employee = requireEmployee(id);
        SysDept dept = sysDeptMapper.selectById(employee.getDeptId());
        SysUser user = employee.getUserId() == null ? null : sysUserMapper.selectById(employee.getUserId());
        return toVO(employee, dept, user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(EmployeeSaveDTO dto) {
        requireEmployeeModuleAccess();
        SysDept dept = requireEditableDept(dto.getDeptId());
        validateUsernameUnique(dto.getUsername(), null);

        SysUser user = buildEmployeeUser(dto, dept, dto.getStatus() == null ? 1 : dto.getStatus());
        sysUserMapper.insert(user);
        sysEmployeeMapper.insert(buildEmployeeProfile(user, dto.getPosition()));
        messageService.sendNewEmployeePasswordReminder(user.getRealName(), dept.getId(), authzService.currentOperatorLabel());
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, EmployeeSaveDTO dto) {
        requireEmployeeModuleAccess();
        assertEditableEmployeeId(id);
        SysDept dept = requireEditableDept(dto.getDeptId());
        SysEmployee employee = requireEmployee(id);
        Long previousDeptId = employee.getDeptId();
        Integer previousStatus = employee.getStatus();
        SysUser user = resolveLinkedUserForUpdate(employee, dto, dept);
        validateUsernameUnique(dto.getUsername(), user.getId());
        assertEmployeeUser(user);

        copySharedFieldsToUser(user, dto, dept, dto.getStatus() == null ? user.getStatus() : dto.getStatus());
        sysUserMapper.updateById(user);

        employee.setUserId(user.getId());
        employee.setEmpName(user.getRealName());
        employee.setDeptId(user.getDeptId());
        employee.setPosition(resolveEmployeePosition(dto.getPosition()));
        employee.setPhone(user.getPhone());
        employee.setEmail(user.getEmail());
        employee.setStatus(user.getStatus());
        sysEmployeeMapper.updateById(employee);

        if (previousDeptId != null && !previousDeptId.equals(employee.getDeptId())) {
            messageService.sendEmployeeTransferReminders(user.getRealName(), previousDeptId, employee.getDeptId(), authzService.currentOperatorLabel());
        }
        if (!Integer.valueOf(0).equals(previousStatus) && Integer.valueOf(0).equals(employee.getStatus())) {
            messageService.sendEmployeeLeftReminder(user.getRealName(), employee.getDeptId(), authzService.currentOperatorLabel());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireEmployeeModuleAccess();
        assertEditableEmployeeId(id);
        SysEmployee employee = requireEmployee(id);
        Long userId = employee.getUserId();
        messageService.sendEmployeeDeletedReminder(employee.getEmpName(), employee.getDeptId(), authzService.currentOperatorLabel());
        sysEmployeeMapper.deleteById(id);
        if (userId != null) {
            sysUserMapper.deleteById(userId);
        }
    }

    private List<EmployeeVO> listManagementUserRecords(EmployeeQueryDTO queryDTO, SysDept systemDept) {
        if (queryDTO.getDeptId() != null && (systemDept == null || !queryDTO.getDeptId().equals(systemDept.getId()))) {
            return List.of();
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUser::getRole, ROLE_ADMIN, ROLE_SUPERADMIN)
                .like(StringUtils.hasText(queryDTO.getEmpName()), SysUser::getRealName, queryDTO.getEmpName())
                .eq(queryDTO.getStatus() != null, SysUser::getStatus, queryDTO.getStatus())
                .orderByDesc(SysUser::getCreateTime)
                .orderByDesc(SysUser::getId);

        List<SysUser> users = sysUserMapper.selectList(wrapper);
        Map<Long, SysDept> originDeptMap = buildDeptMap(users.stream()
                .map(SysUser::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Long displayDeptId = systemDept == null ? null : systemDept.getId();
        String displayDeptName = systemDept == null ? SYSTEM_MANAGEMENT_DEPT_NAME : systemDept.getDeptName();
        return users.stream()
                .map(item -> toManagementVO(item, displayDeptId, displayDeptName, originDeptMap.get(item.getDeptId())))
                .toList();
    }

    private SysEmployee requireEmployee(Long id) {
        SysEmployee employee = sysEmployeeMapper.selectById(id);
        if (employee == null) {
            throw BusinessException.notFound("员工不存在");
        }
        return employee;
    }

    private SysDept requireEditableDept(Long deptId) {
        SysDept dept = sysDeptMapper.selectById(deptId);
        if (dept == null) {
            throw BusinessException.validateFail("所属部门不存在");
        }
        if (!Integer.valueOf(DEPT_STATUS_APPROVED).equals(dept.getStatus())) {
            throw BusinessException.validateFail("仅已生效部门允许维护员工");
        }
        if (AuthzService.DEPT_SYSTEM_MANAGEMENT.equals(authzService.normalizeDeptCode(dept.getDeptCode()))) {
            throw BusinessException.validateFail("系统管理部人员仅用于展示，不允许维护");
        }
        return dept;
    }

    private void syncRegisteredEmployeeProfiles() {
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getRole, ROLE_EMPLOYEE)
                .isNotNull(SysUser::getDeptId)
                .orderByAsc(SysUser::getId);

        List<SysUser> employeeUsers = sysUserMapper.selectList(userWrapper);
        if (employeeUsers.isEmpty()) {
            return;
        }

        List<SysEmployee> existingEmployees = sysEmployeeMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, SysEmployee> linkedProfiles = existingEmployees.stream()
                .filter(item -> item.getUserId() != null)
                .collect(Collectors.toMap(SysEmployee::getUserId, Function.identity(), (left, right) -> left));
        List<SysEmployee> orphanProfiles = existingEmployees.stream()
                .filter(item -> item.getUserId() == null)
                .collect(Collectors.toCollection(ArrayList::new));

        for (SysUser user : employeeUsers) {
            SysDept dept = sysDeptMapper.selectById(user.getDeptId());
            if (dept == null || AuthzService.DEPT_SYSTEM_MANAGEMENT.equals(authzService.normalizeDeptCode(dept.getDeptCode()))) {
                continue;
            }

            SysEmployee linkedProfile = linkedProfiles.get(user.getId());
            if (linkedProfile != null) {
                if (syncEmployeeSnapshot(linkedProfile, user)) {
                    sysEmployeeMapper.updateById(linkedProfile);
                }
                continue;
            }

            SysEmployee legacyProfile = findLegacyEmployeeProfile(orphanProfiles, user);
            if (legacyProfile != null) {
                legacyProfile.setUserId(user.getId());
                syncEmployeeSnapshot(legacyProfile, user);
                sysEmployeeMapper.updateById(legacyProfile);
                orphanProfiles.remove(legacyProfile);
                linkedProfiles.put(user.getId(), legacyProfile);
                continue;
            }

            SysEmployee created = buildEmployeeProfile(user, DEFAULT_EMPLOYEE_POSITION);
            sysEmployeeMapper.insert(created);
            linkedProfiles.put(user.getId(), created);
        }
    }

    private SysEmployee findLegacyEmployeeProfile(List<SysEmployee> orphanProfiles, SysUser user) {
        List<SysEmployee> matches = orphanProfiles.stream()
                .filter(item -> Objects.equals(item.getDeptId(), user.getDeptId()))
                .filter(item -> normalizeText(item.getEmpName()).equals(normalizeText(user.getRealName())))
                .toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private boolean syncEmployeeSnapshot(SysEmployee employee, SysUser user) {
        boolean changed = false;
        if (!Objects.equals(employee.getUserId(), user.getId())) {
            employee.setUserId(user.getId());
            changed = true;
        }
        if (!Objects.equals(employee.getEmpName(), user.getRealName())) {
            employee.setEmpName(user.getRealName());
            changed = true;
        }
        if (!Objects.equals(employee.getDeptId(), user.getDeptId())) {
            employee.setDeptId(user.getDeptId());
            changed = true;
        }
        if (!Objects.equals(employee.getPhone(), user.getPhone())) {
            employee.setPhone(user.getPhone());
            changed = true;
        }
        if (!Objects.equals(employee.getEmail(), user.getEmail())) {
            employee.setEmail(user.getEmail());
            changed = true;
        }
        if (!Objects.equals(employee.getStatus(), user.getStatus())) {
            employee.setStatus(user.getStatus());
            changed = true;
        }
        if (!StringUtils.hasText(employee.getPosition())) {
            employee.setPosition(DEFAULT_EMPLOYEE_POSITION);
            changed = true;
        }
        return changed;
    }

    private SysUser resolveLinkedUserForUpdate(SysEmployee employee, EmployeeSaveDTO dto, SysDept dept) {
        if (employee.getUserId() != null) {
            return requireEmployeeUser(employee.getUserId());
        }

        SysUser existingUser = findUserByUsername(dto.getUsername());
        if (existingUser != null) {
            assertEmployeeUser(existingUser);
            assertUserNotLinkedToAnotherEmployee(existingUser.getId(), employee.getId());
            return existingUser;
        }

        SysUser user = buildEmployeeUser(dto, dept, dto.getStatus() == null ? employee.getStatus() : dto.getStatus());
        sysUserMapper.insert(user);
        return user;
    }

    private void assertUserNotLinkedToAnotherEmployee(Long userId, Long currentEmployeeId) {
        LambdaQueryWrapper<SysEmployee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEmployee::getUserId, userId)
                .ne(currentEmployeeId != null, SysEmployee::getId, currentEmployeeId);
        if (sysEmployeeMapper.selectCount(wrapper) > 0) {
            throw BusinessException.validateFail("该账号已绑定其他员工档案");
        }
    }

    private void validateUsernameUnique(String username, Long excludeUserId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, normalizeRequired(username, "用户名不能为空"))
                .ne(excludeUserId != null, SysUser::getId, excludeUserId);
        if (sysUserMapper.selectCount(wrapper) > 0) {
            throw BusinessException.validateFail("用户名已存在");
        }
    }

    private SysUser requireEmployeeUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.validateFail("员工关联账号不存在");
        }
        assertEmployeeUser(user);
        return user;
    }

    private SysUser findUserByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, normalizeRequired(username, "用户名不能为空"));
        return sysUserMapper.selectOne(wrapper);
    }

    private void assertEmployeeUser(SysUser user) {
        if (!ROLE_EMPLOYEE.equals(authzService.normalizeRole(user.getRole()))) {
            throw BusinessException.validateFail("员工档案仅能关联普通员工账号");
        }
    }

    private SysUser buildEmployeeUser(EmployeeSaveDTO dto, SysDept dept, Integer status) {
        SysUser user = new SysUser();
        copySharedFieldsToUser(user, dto, dept, status == null ? 1 : status);
        user.setRole(ROLE_EMPLOYEE);
        user.setPassword(BCrypt.hashpw(DEFAULT_PASSWORD));
        return user;
    }

    private void copySharedFieldsToUser(SysUser user, EmployeeSaveDTO dto, SysDept dept, Integer status) {
        user.setUsername(normalizeRequired(dto.getUsername(), "用户名不能为空"));
        user.setRealName(normalizeRequired(dto.getEmpName(), "员工姓名不能为空"));
        user.setDeptId(dept.getId());
        user.setStatus(status == null ? 1 : status);
        user.setPhone(normalizeNullable(dto.getPhone()));
        user.setEmail(normalizeNullable(dto.getEmail()));
        user.setRole(ROLE_EMPLOYEE);
    }

    private SysEmployee buildEmployeeProfile(SysUser user, String position) {
        SysEmployee employee = new SysEmployee();
        employee.setUserId(user.getId());
        employee.setEmpCode(CodeGenerator.employeeCode());
        employee.setEmpName(user.getRealName());
        employee.setDeptId(user.getDeptId());
        employee.setPosition(resolveEmployeePosition(position));
        employee.setPhone(user.getPhone());
        employee.setEmail(user.getEmail());
        employee.setStatus(user.getStatus());
        return employee;
    }

    private String resolveEmployeePosition(String position) {
        return StringUtils.hasText(position) ? position.trim() : DEFAULT_EMPLOYEE_POSITION;
    }

    private Map<Long, SysDept> buildDeptMap(Set<Long> deptIds) {
        if (deptIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysDept::getId, deptIds);
        return sysDeptMapper.selectList(wrapper).stream().collect(Collectors.toMap(SysDept::getId, Function.identity()));
    }

    private Map<Long, SysUser> buildUserMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUser::getId, userIds);
        return sysUserMapper.selectList(wrapper).stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }

    private SysDept resolveSystemManagementDept() {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getDeptCode, AuthzService.DEPT_SYSTEM_MANAGEMENT);
        return sysDeptMapper.selectOne(wrapper);
    }

    private void assertEditableEmployeeId(Long id) {
        if (id == null || id <= 0) {
            throw BusinessException.validateFail("系统管理部人员仅用于展示，不允许维护");
        }
    }

    private EmployeeVO toVO(SysEmployee employee, SysDept dept, SysUser user) {
        EmployeeVO vo = new EmployeeVO();
        BeanUtils.copyProperties(employee, vo);
        vo.setUsername(user == null ? null : user.getUsername());
        vo.setDeptName(dept == null ? null : dept.getDeptName());
        vo.setReadOnly(Boolean.FALSE);
        return vo;
    }

    private EmployeeVO toManagementVO(SysUser user, Long deptId, String deptName, SysDept originDept) {
        EmployeeVO vo = new EmployeeVO();
        vo.setId(-user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmpCode("USR-" + user.getUsername());
        vo.setEmpName(user.getRealName());
        vo.setDeptId(deptId);
        vo.setDeptName(deptName);
        vo.setPosition(resolveManagementPosition(user, originDept));
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setReadOnly(Boolean.TRUE);
        return vo;
    }

    private String resolveManagementPosition(SysUser user, SysDept originDept) {
        String role = authzService.normalizeRole(user.getRole());
        if (ROLE_SUPERADMIN.equals(role)) {
            return SUPER_ADMIN_POSITION;
        }
        String deptName = originDept == null ? "部门" : originDept.getDeptName();
        return deptName + SYSTEM_ADMIN_SUFFIX;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw BusinessException.validateFail(message);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}