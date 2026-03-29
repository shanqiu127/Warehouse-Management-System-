package org.example.back.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.common.exception.BusinessException;
import org.example.back.common.result.PageResult;
import org.example.back.common.util.CodeGenerator;
import org.example.back.dto.SupplierQueryDTO;
import org.example.back.dto.SupplierSaveDTO;
import org.example.back.entity.BaseGoods;
import org.example.back.entity.BaseSupplier;
import org.example.back.mapper.BaseGoodsMapper;
import org.example.back.mapper.BaseSupplierMapper;
import org.example.back.vo.OptionVO;
import org.example.back.vo.SupplierVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SupplierService {

    @Autowired
    private BaseSupplierMapper baseSupplierMapper;

    @Autowired
    private BaseGoodsMapper baseGoodsMapper;

    @Autowired
    private AuthzService authzService;

    private void requireSupplierModuleAccess() {
        authzService.requireDeptAdminOrSuperAdmin(AuthzService.DEPT_WAREHOUSE, "仅仓储部门管理员可访问供应商资料");
    }

    public PageResult<SupplierVO> page(SupplierQueryDTO queryDTO) {
        requireSupplierModuleAccess();
        LambdaQueryWrapper<BaseSupplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getSupplierName()), BaseSupplier::getSupplierName, queryDTO.getSupplierName())
                .and(StringUtils.hasText(queryDTO.getContact()), w -> w.like(BaseSupplier::getContactPerson, queryDTO.getContact()))
                .eq(queryDTO.getStatus() != null, BaseSupplier::getStatus, queryDTO.getStatus())
                .orderByDesc(BaseSupplier::getId);

        Page<BaseSupplier> page = baseSupplierMapper.selectPage(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        List<SupplierVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public List<OptionVO> options() {
        requireSupplierModuleAccess();
        LambdaQueryWrapper<BaseSupplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseSupplier::getStatus, 1).orderByAsc(BaseSupplier::getSupplierName);
        return baseSupplierMapper.selectList(wrapper).stream()
                .map(item -> new OptionVO(item.getId(), item.getSupplierName()))
                .toList();
    }

    public SupplierVO getById(Long id) {
        requireSupplierModuleAccess();
        return toVO(requireSupplier(id));
    }

    public void create(SupplierSaveDTO dto) {
        requireSupplierModuleAccess();
        checkSupplierNameUnique(dto.getSupplierName(), null);
        BaseSupplier supplier = new BaseSupplier();
        BeanUtils.copyProperties(dto, supplier);
        supplier.setSupplierCode(CodeGenerator.supplierCode());
        supplier.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        baseSupplierMapper.insert(supplier);
    }

    public void update(Long id, SupplierSaveDTO dto) {
        requireSupplierModuleAccess();
        BaseSupplier supplier = requireSupplier(id);
        checkSupplierNameUnique(dto.getSupplierName(), id);
        supplier.setSupplierName(dto.getSupplierName());
        supplier.setContactPerson(dto.getContactPerson());
        supplier.setContactPhone(dto.getContactPhone());
        supplier.setAddress(dto.getAddress());
        supplier.setStatus(dto.getStatus() == null ? supplier.getStatus() : dto.getStatus());
        supplier.setDescription(dto.getDescription());
        baseSupplierMapper.updateById(supplier);
    }

    public void delete(Long id) {
        requireSupplierModuleAccess();
        requireSupplier(id);
        LambdaQueryWrapper<BaseGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseGoods::getSupplierId, id);
        if (baseGoodsMapper.selectCount(wrapper) > 0) {
            throw BusinessException.validateFail("该供应商下仍有关联商品，无法删除");
        }
        baseSupplierMapper.deleteById(id);
    }

    private void checkSupplierNameUnique(String supplierName, Long excludeId) {
        LambdaQueryWrapper<BaseSupplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseSupplier::getSupplierName, supplierName)
                .ne(excludeId != null, BaseSupplier::getId, excludeId);
        if (baseSupplierMapper.selectCount(wrapper) > 0) {
            throw BusinessException.validateFail("供应商名称已存在");
        }
    }

    private BaseSupplier requireSupplier(Long id) {
        BaseSupplier supplier = baseSupplierMapper.selectById(id);
        if (supplier == null) {
            throw BusinessException.notFound("供应商不存在");
        }
        return supplier;
    }

    private SupplierVO toVO(BaseSupplier supplier) {
        SupplierVO vo = new SupplierVO();
        BeanUtils.copyProperties(supplier, vo);
        vo.setContact(supplier.getContactPerson());
        vo.setPhone(supplier.getContactPhone());
        return vo;
    }
}