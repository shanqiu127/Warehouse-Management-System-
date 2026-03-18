package org.example.back.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.common.exception.BusinessException;
import org.example.back.common.result.PageResult;
import org.example.back.common.util.CodeGenerator;
import org.example.back.dto.GoodsQueryDTO;
import org.example.back.dto.GoodsSaveDTO;
import org.example.back.entity.BaseGoods;
import org.example.back.entity.BaseSupplier;
import org.example.back.mapper.BaseGoodsMapper;
import org.example.back.mapper.BaseSupplierMapper;
import org.example.back.vo.GoodsVO;
import org.example.back.vo.OptionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GoodsService {

    @Autowired
    private BaseGoodsMapper baseGoodsMapper;

    @Autowired
    private BaseSupplierMapper baseSupplierMapper;

    public PageResult<GoodsVO> page(GoodsQueryDTO queryDTO) {
        LambdaQueryWrapper<BaseGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getGoodsName()), BaseGoods::getGoodsName, queryDTO.getGoodsName())
                .eq(queryDTO.getSupplierId() != null, BaseGoods::getSupplierId, queryDTO.getSupplierId())
                .eq(queryDTO.getStatus() != null, BaseGoods::getStatus, queryDTO.getStatus())
                .orderByDesc(BaseGoods::getId);

        Page<BaseGoods> page = baseGoodsMapper.selectPage(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        Map<Long, BaseSupplier> supplierMap = buildSupplierMap(page.getRecords().stream().map(BaseGoods::getSupplierId).collect(Collectors.toSet()));
        List<GoodsVO> records = page.getRecords().stream().map(item -> toVO(item, supplierMap.get(item.getSupplierId()))).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public List<OptionVO> options() {
        LambdaQueryWrapper<BaseGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseGoods::getStatus, 1).orderByAsc(BaseGoods::getGoodsName);
        return baseGoodsMapper.selectList(wrapper).stream()
                .map(item -> new OptionVO(item.getId(), item.getGoodsName()))
                .toList();
    }

    public GoodsVO getById(Long id) {
        BaseGoods goods = requireGoods(id);
        BaseSupplier supplier = baseSupplierMapper.selectById(goods.getSupplierId());
        return toVO(goods, supplier);
    }

    public void create(GoodsSaveDTO dto) {
        checkGoodsNameUnique(dto.getGoodsName(), null);
        requireSupplier(dto.getSupplierId());
        validateGoodsPricing(dto.getPurchasePrice(), dto.getSalePrice());
        validateStock(dto.getStock());
        BaseGoods goods = new BaseGoods();
        BeanUtils.copyProperties(dto, goods);
        goods.setGoodsCode(CodeGenerator.goodsCode());
        goods.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        goods.setStock(dto.getStock() == null ? 0 : dto.getStock());
        baseGoodsMapper.insert(goods);
    }

    public void update(Long id, GoodsSaveDTO dto) {
        requireSupplier(dto.getSupplierId());
        BaseGoods goods = requireGoods(id);
        checkGoodsNameUnique(dto.getGoodsName(), id);
        validateGoodsPricing(dto.getPurchasePrice(), dto.getSalePrice());
        validateStock(dto.getStock());
        goods.setGoodsName(dto.getGoodsName());
        goods.setCategory(dto.getCategory());
        goods.setBrand(dto.getBrand());
        goods.setSupplierId(dto.getSupplierId());
        goods.setPurchasePrice(dto.getPurchasePrice());
        goods.setSalePrice(dto.getSalePrice());
        goods.setStock(dto.getStock() == null ? goods.getStock() : dto.getStock());
        goods.setUnit(dto.getUnit());
        goods.setStatus(dto.getStatus() == null ? goods.getStatus() : dto.getStatus());
        goods.setDescription(dto.getDescription());
        baseGoodsMapper.updateById(goods);
    }

    public void delete(Long id) {
        requireGoods(id);
        baseGoodsMapper.deleteById(id);
    }

    private void checkGoodsNameUnique(String goodsName, Long excludeId) {
        LambdaQueryWrapper<BaseGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseGoods::getGoodsName, goodsName)
                .ne(excludeId != null, BaseGoods::getId, excludeId);
        if (baseGoodsMapper.selectCount(wrapper) > 0) {
            throw BusinessException.validateFail("商品名称已存在");
        }
    }

    private BaseGoods requireGoods(Long id) {
        BaseGoods goods = baseGoodsMapper.selectById(id);
        if (goods == null) {
            throw BusinessException.notFound("商品不存在");
        }
        return goods;
    }

    private BaseSupplier requireSupplier(Long id) {
        BaseSupplier supplier = baseSupplierMapper.selectById(id);
        if (supplier == null) {
            throw BusinessException.validateFail("供应商不存在");
        }
        return supplier;
    }

    private void validateGoodsPricing(BigDecimal purchasePrice, BigDecimal salePrice) {
        if (purchasePrice == null || purchasePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.validateFail("进价必须大于0");
        }
        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.validateFail("售价必须大于0");
        }
    }

    private void validateStock(Integer stock) {
        if (stock != null && stock < 0) {
            throw BusinessException.validateFail("库存不能小于0");
        }
    }

    private Map<Long, BaseSupplier> buildSupplierMap(Set<Long> supplierIds) {
        if (supplierIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<BaseSupplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BaseSupplier::getId, supplierIds);
        return baseSupplierMapper.selectList(wrapper).stream().collect(Collectors.toMap(BaseSupplier::getId, Function.identity()));
    }

    private GoodsVO toVO(BaseGoods goods, BaseSupplier supplier) {
        GoodsVO vo = new GoodsVO();
        BeanUtils.copyProperties(goods, vo);
        vo.setSupplierName(supplier == null ? null : supplier.getSupplierName());
        vo.setPrice(goods.getSalePrice() == null ? BigDecimal.ZERO : goods.getSalePrice());
        return vo;
    }
}