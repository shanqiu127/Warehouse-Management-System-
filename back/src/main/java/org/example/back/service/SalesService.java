package org.example.back.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.common.exception.BusinessException;
import org.example.back.common.result.PageResult;
import org.example.back.common.util.CodeGenerator;
import org.example.back.dto.LoginResponse;
import org.example.back.dto.DocumentVoidDTO;
import org.example.back.dto.SalesQueryDTO;
import org.example.back.dto.SalesSaveDTO;
import org.example.back.entity.BaseGoods;
import org.example.back.entity.BizSales;
import org.example.back.mapper.BaseGoodsMapper;
import org.example.back.mapper.BizSalesMapper;
import org.example.back.vo.SalesVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SalesService {

    @Autowired
    private BizSalesMapper bizSalesMapper;

    @Autowired
    private BaseGoodsMapper baseGoodsMapper;

    @Autowired
    private AuthService authService;

    public PageResult<SalesVO> page(SalesQueryDTO queryDTO) {
        LocalDateTime startTime = queryDTO.getStartDate() == null ? null : queryDTO.getStartDate().atStartOfDay();
        LocalDateTime endTime = queryDTO.getEndDate() == null ? null : queryDTO.getEndDate().plusDays(1).atStartOfDay();

        LambdaQueryWrapper<BizSales> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getSalesNo()), BizSales::getSalesNo, queryDTO.getSalesNo())
                .like(StringUtils.hasText(queryDTO.getGoodsName()), BizSales::getGoodsName, queryDTO.getGoodsName())
                .eq(queryDTO.getGoodsId() != null, BizSales::getGoodsId, queryDTO.getGoodsId())
            .ge(startTime != null, BizSales::getOperationTime, startTime)
            .lt(endTime != null, BizSales::getOperationTime, endTime)
                .orderByDesc(BizSales::getId);

        Page<BizSales> page = bizSalesMapper.selectPage(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        List<SalesVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public SalesVO getById(Long id) {
        BizSales entity = requireEntity(id);
        return toVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(SalesSaveDTO dto) {
        validateQuantity(dto.getQuantity());

        BaseGoods goods = requireGoods(dto.getGoodsId());
        ensureGoodsEnabled(goods);
        BigDecimal unitPrice = resolveUnitPrice(dto.getUnitPrice(), goods.getSalePrice(), "商品售价为空，请传入销售单价");
        LocalDateTime operationTime = dto.getOperationTime() == null ? LocalDateTime.now() : dto.getOperationTime();

        LoginResponse.UserInfoVO loginUser = authService.getUserInfo();

        BizSales entity = new BizSales();
        entity.setSalesNo(CodeGenerator.salesNo());
        entity.setGoodsId(goods.getId());
        entity.setGoodsName(goods.getGoodsName());
        entity.setQuantity(dto.getQuantity());
        entity.setUnitPrice(unitPrice);
        entity.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(dto.getQuantity())));
        entity.setOperatorId(loginUser.getId());
        entity.setOperatorName(loginUser.getRealName());
        entity.setOperationTime(operationTime);
        entity.setRemark(dto.getRemark());
        entity.setBizStatus(1);

        bizSalesMapper.insert(entity);
        decreaseStock(goods.getId(), dto.getQuantity(), "库存不足，销售出库失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        BizSales entity = requireEntity(id);
        ensureNormalStatus(entity.getBizStatus(), "销售单");
        validateDeleteWindow(entity.getOperationTime(), "销售单");
        increaseStock(entity.getGoodsId(), entity.getQuantity());
        bizSalesMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void voidDocument(Long id, DocumentVoidDTO dto) {
        BizSales entity = requireEntity(id);
        ensureNormalStatus(entity.getBizStatus(), "销售单");
        validateHistoricalRedFlushOnly(entity.getOperationTime(), dto, "销售单");

        increaseStock(entity.getGoodsId(), entity.getQuantity());

        String reason = normalizeReason(dto == null ? null : dto.getReason());
        LocalDateTime now = LocalDateTime.now();
        entity.setBizStatus(2);
        entity.setVoidTime(now);
        entity.setVoidReason(reason);
        bizSalesMapper.updateById(entity);

        if (dto != null && Boolean.TRUE.equals(dto.getCreateRedFlush())) {
            LoginResponse.UserInfoVO loginUser = authService.getUserInfo();
            BizSales redFlushDoc = new BizSales();
            redFlushDoc.setSalesNo(CodeGenerator.salesNo());
            redFlushDoc.setGoodsId(entity.getGoodsId());
            redFlushDoc.setGoodsName(entity.getGoodsName());
            redFlushDoc.setQuantity(-entity.getQuantity());
            redFlushDoc.setUnitPrice(entity.getUnitPrice());
            redFlushDoc.setTotalPrice(entity.getTotalPrice().negate());
            redFlushDoc.setOperatorId(loginUser.getId());
            redFlushDoc.setOperatorName(loginUser.getRealName());
            redFlushDoc.setOperationTime(now);
            redFlushDoc.setRemark("红冲来源:" + entity.getSalesNo());
            redFlushDoc.setBizStatus(3);
            redFlushDoc.setSourceId(entity.getId());
            redFlushDoc.setVoidReason(reason);
            bizSalesMapper.insert(redFlushDoc);
        }
    }

    private BizSales requireEntity(Long id) {
        BizSales entity = bizSalesMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("销售单不存在");
        }
        return entity;
    }

    private BaseGoods requireGoods(Long goodsId) {
        BaseGoods goods = baseGoodsMapper.selectById(goodsId);
        if (goods == null) {
            throw BusinessException.validateFail("商品不存在");
        }
        return goods;
    }

    private void ensureGoodsEnabled(BaseGoods goods) {
        if (goods.getStatus() == null || goods.getStatus() != 1) {
            throw BusinessException.validateFail("商品已下架，无法创建业务单据");
        }
    }

    private void validateDeleteWindow(LocalDateTime operationTime, String docName) {
        if (operationTime == null) {
            return;
        }
        if (!operationTime.toLocalDate().equals(LocalDate.now())) {
            throw BusinessException.validateFail("仅允许删除当天" + docName + "，历史单据请走作废/红冲流程");
        }
    }

    private void ensureNormalStatus(Integer bizStatus, String docName) {
        if (bizStatus == null || bizStatus == 1) {
            return;
        }
        if (bizStatus == 2) {
            throw BusinessException.validateFail(docName + "已作废，禁止重复操作");
        }
        throw BusinessException.validateFail(docName + "为红冲单，禁止删除或再次作废");
    }

    private void validateHistoricalRedFlushOnly(LocalDateTime operationTime, DocumentVoidDTO dto, String docName) {
        if (operationTime == null) {
            return;
        }
        boolean isToday = operationTime.toLocalDate().equals(LocalDate.now());
        boolean createRedFlush = dto != null && Boolean.TRUE.equals(dto.getCreateRedFlush());
        if (!isToday && !createRedFlush) {
            throw BusinessException.validateFail("历史" + docName + "仅支持作废红冲");
        }
    }

    private String normalizeReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return "手工作废";
        }
        return reason.trim();
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw BusinessException.validateFail("数量必须大于0");
        }
    }

    private BigDecimal resolveUnitPrice(BigDecimal inputPrice, BigDecimal fallbackPrice, String emptyPriceMsg) {
        BigDecimal finalPrice = inputPrice == null ? fallbackPrice : inputPrice;
        if (finalPrice == null) {
            throw BusinessException.validateFail(emptyPriceMsg);
        }
        if (finalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.validateFail("单价必须大于0");
        }
        return finalPrice;
    }

    private void increaseStock(Long goodsId, Integer quantity) {
        LambdaUpdateWrapper<BaseGoods> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BaseGoods::getId, goodsId)
                .setSql("stock = stock + " + quantity);
        int rows = baseGoodsMapper.update(null, wrapper);
        if (rows == 0) {
            throw BusinessException.validateFail("商品不存在");
        }
    }

    private void decreaseStock(Long goodsId, Integer quantity, String msg) {
        LambdaUpdateWrapper<BaseGoods> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BaseGoods::getId, goodsId)
                .ge(BaseGoods::getStock, quantity)
                .setSql("stock = stock - " + quantity);
        int rows = baseGoodsMapper.update(null, wrapper);
        if (rows == 0) {
            throw BusinessException.stockInsufficient(msg);
        }
    }

    private SalesVO toVO(BizSales entity) {
        SalesVO vo = new SalesVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setSalesPrice(entity.getUnitPrice());
        vo.setTotalAmount(entity.getTotalPrice());
        vo.setSalesDate(entity.getOperationTime());
        vo.setOperator(entity.getOperatorName());
        vo.setBizStatus(entity.getBizStatus());
        vo.setSourceId(entity.getSourceId());
        vo.setVoidTime(entity.getVoidTime());
        vo.setVoidReason(entity.getVoidReason());
        return vo;
    }
}
