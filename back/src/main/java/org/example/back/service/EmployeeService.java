package org.example.back.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.common.exception.BusinessException;
import org.example.back.common.result.PageResult;
import org.example.back.common.util.CodeGenerator;
import org.example.back.dto.EmployeeQueryDTO;
import org.example.back.dto.EmployeeSaveDTO;
import org.example.back.entity.SysDept;
import org.example.back.entity.SysEmployee;
import org.example.back.mapper.SysDeptMapper;
import org.example.back.mapper.SysEmployeeMapper;
import org.example.back.vo.EmployeeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    @Autowired
    private SysEmployeeMapper sysEmployeeMapper;

    @Autowired
    private SysDeptMapper sysDeptMapper;

    public PageResult<EmployeeVO> page(EmployeeQueryDTO queryDTO) {
        LambdaQueryWrapper<SysEmployee> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getEmpName()), SysEmployee::getEmpName, queryDTO.getEmpName())
                .eq(queryDTO.getDeptId() != null, SysEmployee::getDeptId, queryDTO.getDeptId())
                .eq(queryDTO.getStatus() != null, SysEmployee::getStatus, queryDTO.getStatus())
                .orderByDesc(SysEmployee::getId);

        Page<SysEmployee> page = sysEmployeeMapper.selectPage(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        Map<Long, SysDept> deptMap = buildDeptMap(page.getRecords().stream().map(SysEmployee::getDeptId).collect(Collectors.toSet()));
        List<EmployeeVO> records = page.getRecords().stream().map(item -> toVO(item, deptMap.get(item.getDeptId()))).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public EmployeeVO getById(Long id) {
        SysEmployee employee = requireEmployee(id);
        SysDept dept = sysDeptMapper.selectById(employee.getDeptId());
        return toVO(employee, dept);
    }

    public void create(EmployeeSaveDTO dto) {
        requireDept(dto.getDeptId());
        SysEmployee employee = new SysEmployee();
        BeanUtils.copyProperties(dto, employee);
        employee.setEmpCode(CodeGenerator.employeeCode());
        employee.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        sysEmployeeMapper.insert(employee);
    }

    public void update(Long id, EmployeeSaveDTO dto) {
        requireDept(dto.getDeptId());
        SysEmployee employee = requireEmployee(id);
        employee.setEmpName(dto.getEmpName());
        employee.setDeptId(dto.getDeptId());
        employee.setPosition(dto.getPosition());
        employee.setPhone(dto.getPhone());
        employee.setEmail(dto.getEmail());
        employee.setStatus(dto.getStatus() == null ? employee.getStatus() : dto.getStatus());
        sysEmployeeMapper.updateById(employee);
    }

    public void delete(Long id) {
        requireEmployee(id);
        sysEmployeeMapper.deleteById(id);
    }

    private SysEmployee requireEmployee(Long id) {
        SysEmployee employee = sysEmployeeMapper.selectById(id);
        if (employee == null) {
            throw BusinessException.notFound("员工不存在");
        }
        return employee;
    }

    private SysDept requireDept(Long deptId) {
        SysDept dept = sysDeptMapper.selectById(deptId);
        if (dept == null) {
            throw BusinessException.validateFail("所属部门不存在");
        }
        return dept;
    }

    private Map<Long, SysDept> buildDeptMap(Set<Long> deptIds) {
        if (deptIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysDept::getId, deptIds);
        return sysDeptMapper.selectList(wrapper).stream().collect(Collectors.toMap(SysDept::getId, Function.identity()));
    }

    private EmployeeVO toVO(SysEmployee employee, SysDept dept) {
        EmployeeVO vo = new EmployeeVO();
        BeanUtils.copyProperties(employee, vo);
        vo.setDeptName(dept == null ? null : dept.getDeptName());
        return vo;
    }
}