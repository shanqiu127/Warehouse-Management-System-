package org.example.back.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.common.exception.BusinessException;
import org.example.back.common.result.PageResult;
import org.example.back.common.util.CodeGenerator;
import org.example.back.dto.LoginResponse;
import org.example.back.dto.DocumentVoidDTO;
import org.example.back.dto.SalesReturnQueryDTO;
import org.example.back.dto.SalesReturnSaveDTO;
import org.example.back.entity.BaseGoods;
import org.example.back.entity.BizSales;
import org.example.back.entity.BizSalesReturn;
import org.example.back.mapper.BaseGoodsMapper;
import org.example.back.mapper.BizPurchaseMapper;
import org.example.back.mapper.BizSalesMapper;
import org.example.back.mapper.BizSalesReturnMapper;
import org.example.back.vo.SalesReturnVO;
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
public class SalesReturnService {

    @Autowired
    private BizSalesReturnMapper bizSalesReturnMapper;

    @Autowired
    private BaseGoodsMapper baseGoodsMapper;

    @Autowired
    private BizSalesMapper bizSalesMapper;

    @Autowired
    private BizPurchaseMapper bizPurchaseMapper;

    @Autowired
    private AuthService authService;

    public PageResult<SalesReturnVO> page(SalesReturnQueryDTO queryDTO) {
        LocalDateTime startTime = queryDTO.getStartDate() == null ? null : queryDTO.getStartDate().atStartOfDay();
        LocalDateTime endTime = queryDTO.getEndDate() == null ? null : queryDTO.getEndDate().plusDays(1).atStartOfDay();

        LambdaQueryWrapper<BizSalesReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getReturnNo()), BizSalesReturn::getReturnNo, queryDTO.getReturnNo())
                .like(StringUtils.hasText(queryDTO.getGoodsName()), BizSalesReturn::getGoodsName, queryDTO.getGoodsName())
                .eq(queryDTO.getGoodsId() != null, BizSalesReturn::getGoodsId, queryDTO.getGoodsId())
            .ge(startTime != null, BizSalesReturn::getOperationTime, startTime)
            .lt(endTime != null, BizSalesReturn::getOperationTime, endTime)
                .orderByDesc(BizSalesReturn::getId);

        Page<BizSalesReturn> page = bizSalesReturnMapper.selectPage(new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize()), wrapper);
        List<SalesReturnVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public SalesReturnVO getById(Long id) {
        BizSalesReturn entity = requireEntity(id);
        return toVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(SalesReturnSaveDTO dto) {
        validateQuantity(dto.getQuantity());

        BizSales sourceSales = requireSourceSales(dto.getSourceSalesId());
        ensureSourceSalesNormal(sourceSales);
        validateReturnableQuantity(sourceSales, dto.getQuantity());

        BaseGoods goods = requireGoods(sourceSales.getGoodsId());
        ensureGoodsEnabled(goods);
        BigDecimal unitPrice = resolveUnitPrice(dto.getUnitPrice(), sourceSales.getUnitPrice(), "来源销售单缺少单价，请传入客退单价");
        LocalDateTime operationTime = dto.getOperationTime() == null ? LocalDateTime.now() : dto.getOperationTime();
        CostSnapshot costSnapshot = buildReturnCostSnapshot(sourceSales, goods, operationTime);

        LoginResponse.UserInfoVO loginUser = authService.getUserInfo();

        BizSalesReturn entity = new BizSalesReturn();
        entity.setReturnNo(CodeGenerator.salesReturnNo());
        entity.setSourceSalesId(sourceSales.getId());
        entity.setSourceSalesNo(sourceSales.getSalesNo());
        entity.setGoodsId(goods.getId());
        entity.setGoodsName(goods.getGoodsName());
        entity.setQuantity(dto.getQuantity());
        entity.setUnitPrice(unitPrice);
        entity.setCostUnitPrice(costSnapshot.unitPrice());
        entity.setCostTotalPrice(costSnapshot.unitPrice().multiply(BigDecimal.valueOf(dto.getQuantity())));
        entity.setCostSource(costSnapshot.source());
        entity.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(dto.getQuantity())));
        entity.setOperatorId(loginUser.getId());
        entity.setOperatorName(loginUser.getRealName());
        entity.setOperationTime(operationTime);
        entity.setRemark(dto.getRemark());
        entity.setBizStatus(1);

        bizSalesReturnMapper.insert(entity);
        increaseStock(goods.getId(), dto.getQuantity());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        BizSalesReturn entity = requireEntity(id);
        ensureNormalStatus(entity.getBizStatus(), "客退单");
        validateDeleteWindow(entity.getOperationTime(), "客退单");
        decreaseStock(entity.getGoodsId(), entity.getQuantity(), "当前库存不足，无法删除该客退单");
        bizSalesReturnMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    // 作废单据，更新单据状态并记录作废信息，同时根据前端请求决定是否创建对应的红冲单
    public void voidDocument(Long id, DocumentVoidDTO dto) {
        BizSalesReturn entity = requireEntity(id);
        ensureNormalStatus(entity.getBizStatus(), "客退单");

        String reason = normalizeReason(dto == null ? null : dto.getReason());
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<BizSalesReturn> voidWrapper = new LambdaUpdateWrapper<>();
        voidWrapper.eq(BizSalesReturn::getId, entity.getId())
                .eq(BizSalesReturn::getBizStatus, 1)
                .set(BizSalesReturn::getBizStatus, 2)
                .set(BizSalesReturn::getVoidTime, now)
                .set(BizSalesReturn::getVoidReason, reason);
        int rows = bizSalesReturnMapper.update(null, voidWrapper);
        if (rows != 1) {
            throw BusinessException.validateFail("客退单已被处理，禁止重复作废");
        }

        decreaseStock(entity.getGoodsId(), entity.getQuantity(), "当前库存不足，无法作废该客退单");

        if (dto != null && Boolean.TRUE.equals(dto.getCreateRedFlush())) {
            LoginResponse.UserInfoVO loginUser = authService.getUserInfo();
            BizSalesReturn redFlushDoc = new BizSalesReturn();
            redFlushDoc.setReturnNo(CodeGenerator.salesReturnNo());
            redFlushDoc.setSourceSalesId(entity.getSourceSalesId());
            redFlushDoc.setSourceSalesNo(entity.getSourceSalesNo());
            redFlushDoc.setGoodsId(entity.getGoodsId());
            redFlushDoc.setGoodsName(entity.getGoodsName());
            redFlushDoc.setQuantity(-entity.getQuantity());
            redFlushDoc.setUnitPrice(entity.getUnitPrice());
            redFlushDoc.setCostUnitPrice(entity.getCostUnitPrice());
            redFlushDoc.setCostTotalPrice(entity.getCostTotalPrice() == null ? null : entity.getCostTotalPrice().negate());
            redFlushDoc.setCostSource(entity.getCostSource());
            redFlushDoc.setTotalPrice(entity.getTotalPrice().negate());
            redFlushDoc.setOperatorId(loginUser.getId());
            redFlushDoc.setOperatorName(loginUser.getRealName());
            redFlushDoc.setOperationTime(now);
            redFlushDoc.setRemark("红冲来源:" + entity.getReturnNo());
            redFlushDoc.setBizStatus(3);
            redFlushDoc.setSourceId(entity.getId());
            redFlushDoc.setVoidReason(reason);
            bizSalesReturnMapper.insert(redFlushDoc);
        }
    }
    
    private BizSalesReturn requireEntity(Long id) {
        BizSalesReturn entity = bizSalesReturnMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("客退单不存在");
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

    private BizSales requireSourceSales(Long sourceSalesId) {
        BizSales sales = bizSalesMapper.selectById(sourceSalesId);
        if (sales == null) {
            throw BusinessException.validateFail("来源销售单不存在");
        }
        return sales;
    }

    private void ensureSourceSalesNormal(BizSales sourceSales) {
        if (sourceSales.getBizStatus() == null || sourceSales.getBizStatus() != 1) {
            throw BusinessException.validateFail("来源销售单非正常状态，禁止退货");
        }
    }

    private void validateReturnableQuantity(BizSales sourceSales, Integer returnQty) {
        LambdaQueryWrapper<BizSalesReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizSalesReturn::getSourceSalesId, sourceSales.getId())
                .eq(BizSalesReturn::getBizStatus, 1);
        List<BizSalesReturn> linkedReturns = bizSalesReturnMapper.selectList(wrapper);
        int returnedQty = linkedReturns.stream().map(BizSalesReturn::getQuantity).reduce(0, Integer::sum);
        int availableQty = sourceSales.getQuantity() - returnedQty;
        if (availableQty <= 0) {
            throw BusinessException.validateFail("来源销售单已无可退数量");
        }
        if (returnQty > availableQty) {
            throw BusinessException.validateFail("退货数量超出可退数量，当前最多可退: " + availableQty);
        }
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

    private CostSnapshot buildReturnCostSnapshot(BizSales sourceSales, BaseGoods goods, LocalDateTime returnTime) {
        BigDecimal sourceCost = sourceSales.getCostUnitPrice();
        if (sourceCost != null && sourceCost.compareTo(BigDecimal.ZERO) > 0) {
            return new CostSnapshot(sourceCost, "SOURCE_SALE");
        }

        LocalDateTime lookupTime = sourceSales.getOperationTime() == null ? returnTime : sourceSales.getOperationTime();
        BigDecimal purchasePrice = bizPurchaseMapper.latestValidUnitPrice(goods.getId(), lookupTime);
        if (purchasePrice != null && purchasePrice.compareTo(BigDecimal.ZERO) > 0) {
            return new CostSnapshot(purchasePrice, "RECENT_PURCHASE");
        }
        if (goods.getPurchasePrice() != null && goods.getPurchasePrice().compareTo(BigDecimal.ZERO) > 0) {
            return new CostSnapshot(goods.getPurchasePrice(), "GOODS_PRICE");
        }
        return new CostSnapshot(BigDecimal.ZERO, "ZERO_FALLBACK");
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

    private SalesReturnVO toVO(BizSalesReturn entity) {
        SalesReturnVO vo = new SalesReturnVO();
        BeanUtils.copyProperties(entity, vo);
        LocalDateTime bizTime = entity.getOperationTime() == null ? entity.getCreateTime() : entity.getOperationTime();
        vo.setSourceSalesId(entity.getSourceSalesId());
        vo.setSourceSalesNo(entity.getSourceSalesNo());
        vo.setOrderNo(entity.getSourceSalesNo());
        vo.setRefundAmount(entity.getTotalPrice());
        vo.setOperationTime(bizTime);
        vo.setReturnDate(bizTime);
        vo.setOperator(entity.getOperatorName());
        vo.setReason(entity.getRemark());
        vo.setBizStatus(entity.getBizStatus());
        vo.setSourceId(entity.getSourceId());
        vo.setVoidTime(entity.getVoidTime());
        vo.setVoidReason(entity.getVoidReason());
        return vo;
    }

    private record CostSnapshot(BigDecimal unitPrice, String source) {
    }
}
