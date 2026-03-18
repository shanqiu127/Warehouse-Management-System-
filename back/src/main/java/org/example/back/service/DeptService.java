package org.example.back.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.common.exception.BusinessException;
import org.example.back.common.result.PageResult;
import org.example.back.common.util.CodeGenerator;
import org.example.back.dto.DeptQueryDTO;
import org.example.back.dto.DeptSaveDTO;
import org.example.back.entity.SysDept;
import org.example.back.entity.SysEmployee;
import org.example.back.mapper.SysDeptMapper;
import org.example.back.mapper.SysEmployeeMapper;
import org.example.back.vo.DeptVO;
import org.example.back.vo.OptionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DeptService {

    @Autowired
    private SysDeptMapper sysDeptMapper;

    @Autowired
    private SysEmployeeMapper sysEmployeeMapper;

    public PageResult<DeptVO> page(DeptQueryDTO queryDTO) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getDeptName()), SysDept::getDeptName, queryDTO.getDeptName())
                .orderByDesc(SysDept::getId);

        Page<SysDept> page = sysDeptMapper.selectPage(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        List<DeptVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public List<OptionVO> options() {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysDept::getDeptName);
        return sysDeptMapper.selectList(wrapper).stream()
                .map(item -> new OptionVO(item.getId(), item.getDeptName()))
                .toList();
    }

    public DeptVO getById(Long id) {
        return toVO(requireDept(id));
    }

    public void create(DeptSaveDTO dto) {
        SysDept dept = new SysDept();
        BeanUtils.copyProperties(dto, dept);
        dept.setDeptCode(CodeGenerator.deptCode());
        checkDeptNameUnique(dto.getDeptName(), null);
        sysDeptMapper.insert(dept);
    }

    public void update(Long id, DeptSaveDTO dto) {
        SysDept dept = requireDept(id);
        checkDeptNameUnique(dto.getDeptName(), id);
        dept.setDeptName(dto.getDeptName());
        dept.setLeader(dto.getLeader());
        dept.setPhone(dto.getPhone());
        dept.setDescription(dto.getDescription());
        sysDeptMapper.updateById(dept);
    }

    public void delete(Long id) {
        requireDept(id);
        LambdaQueryWrapper<SysEmployee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEmployee::getDeptId, id);
        if (sysEmployeeMapper.selectCount(wrapper) > 0) {
            throw BusinessException.validateFail("该部门下仍有关联员工，无法删除");
        }
        sysDeptMapper.deleteById(id);
    }

    private SysDept requireDept(Long id) {
        SysDept dept = sysDeptMapper.selectById(id);
        if (dept == null) {
            throw BusinessException.notFound("部门不存在");
        }
        return dept;
    }

    private void checkDeptNameUnique(String deptName, Long excludeId) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getDeptName, deptName)
                .ne(excludeId != null, SysDept::getId, excludeId);
        if (sysDeptMapper.selectCount(wrapper) > 0) {
            throw BusinessException.validateFail("部门名称已存在");
        }
    }

    private DeptVO toVO(SysDept dept) {
        DeptVO vo = new DeptVO();
        BeanUtils.copyProperties(dept, vo);
        vo.setManager(dept.getLeader());
        vo.setContactPhone(dept.getPhone());
        return vo;
    }
}