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
// 部门 Service
@Service
public class DeptService {

    @Autowired
    private SysDeptMapper sysDeptMapper;

    @Autowired
    private SysEmployeeMapper sysEmployeeMapper;
    // 部门分页查询
    public PageResult<DeptVO> page(DeptQueryDTO queryDTO) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getDeptName()), SysDept::getDeptName, queryDTO.getDeptName())
                .orderByDesc(SysDept::getId);

        Page<SysDept> page = sysDeptMapper.selectPage(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        List<DeptVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }
    // 部门选项列表
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
    // 根据 ID 获取部门，若不存在则抛出异常
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
    // 将部门实体转换为 VO 对象
    private DeptVO toVO(SysDept dept) {
        DeptVO vo = new DeptVO();
        BeanUtils.copyProperties(dept, vo);
        vo.setManager(dept.getLeader());
        vo.setContactPhone(dept.getPhone());
        return vo;
    }
}