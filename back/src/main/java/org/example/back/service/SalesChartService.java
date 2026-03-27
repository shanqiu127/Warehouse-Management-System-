package org.example.back.service;

import org.example.back.dto.SalesChartQueryDTO;
import org.example.back.mapper.BizSalesMapper;
import org.example.back.mapper.BizSalesReturnMapper;
import org.example.back.vo.BrandRatioItemVO;
import org.example.back.vo.ChartOverviewVO;
import org.example.back.vo.ProfitBrandTopVO;
import org.example.back.vo.ProfitOverviewVO;
import org.example.back.vo.ProfitTrendVO;
import org.example.back.vo.SalesTop5VO;
import org.example.back.vo.SalesTrendVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesChartService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    @Autowired
    private BizSalesMapper bizSalesMapper;

    @Autowired
    private BizSalesReturnMapper bizSalesReturnMapper;

    public ChartOverviewVO getOverview(SalesChartQueryDTO queryDTO) {
        DateRange range = resolveRange(queryDTO);

        BigDecimal salesAmount = defaultAmount(bizSalesMapper.sumSalesAmount(range.startTime(), range.endTime()));
        BigDecimal returnAmount = defaultAmount(bizSalesReturnMapper.sumReturnAmount(range.startTime(), range.endTime()));
        Long salesQuantity = defaultCount(bizSalesMapper.sumSalesQuantity(range.startTime(), range.endTime()));
        Long returnQuantity = defaultCount(bizSalesReturnMapper.sumReturnQuantity(range.startTime(), range.endTime()));

        ChartOverviewVO vo = new ChartOverviewVO();
        vo.setSalesAmount(salesAmount);
        vo.setReturnAmount(returnAmount);
        vo.setNetSalesAmount(salesAmount.subtract(returnAmount));
        vo.setSalesQuantity(salesQuantity);
        vo.setReturnQuantity(returnQuantity);
        return vo;
    }

    public SalesTop5VO getTop5(SalesChartQueryDTO queryDTO) {
        DateRange range = resolveRange(queryDTO);
        List<BizSalesMapper.TopGoodsAgg> rows = bizSalesMapper.topGoods(range.startTime(), range.endTime());

        List<String> nameList = new ArrayList<>();
        List<Long> dataList = new ArrayList<>();
        for (BizSalesMapper.TopGoodsAgg row : rows) {
            nameList.add(row.getName());
            dataList.add(defaultCount(row.getQuantity()));
        }

        SalesTop5VO vo = new SalesTop5VO();
        vo.setNameList(nameList);
        vo.setDataList(dataList);
        return vo;
    }

    public List<BrandRatioItemVO> getBrandRatio(SalesChartQueryDTO queryDTO) {
        DateRange range = resolveRange(queryDTO);
        List<BizSalesMapper.BrandAmountAgg> rows = bizSalesMapper.brandSalesAmount(range.startTime(), range.endTime());

        List<BrandRatioItemVO> result = new ArrayList<>();
        for (BizSalesMapper.BrandAmountAgg row : rows) {
            BrandRatioItemVO item = new BrandRatioItemVO();
            item.setName(row.getName());
            item.setValue(defaultAmount(row.getAmount()));
            result.add(item);
        }
        return result;
    }

    public SalesTrendVO getDailyTrend(SalesChartQueryDTO queryDTO) {
        DateRange range = resolveRange(queryDTO);
        List<BizSalesMapper.DailyAmountAgg> rows = bizSalesMapper.dailySalesAmount(range.startTime(), range.endTime());

        Map<String, BigDecimal> amountMap = new HashMap<>();
        for (BizSalesMapper.DailyAmountAgg row : rows) {
            amountMap.put(row.getStatDate(), defaultAmount(row.getAmount()));
        }

        List<String> dateList = new ArrayList<>();
        List<BigDecimal> amountList = new ArrayList<>();
        LocalDate current = range.startDate();
        while (!current.isAfter(range.endDate())) {
            String fullDate = current.toString();
            dateList.add(current.format(DATE_FORMATTER));
            amountList.add(amountMap.getOrDefault(fullDate, BigDecimal.ZERO));
            current = current.plusDays(1);
        }

        SalesTrendVO vo = new SalesTrendVO();
        vo.setDateList(dateList);
        vo.setAmountList(amountList);
        return vo;
    }

    public ProfitOverviewVO getProfitOverview(SalesChartQueryDTO queryDTO) {
        DateRange range = resolveValidRange(queryDTO);

        BigDecimal salesAmount = defaultAmount(bizSalesMapper.sumValidSalesAmount(range.startTime(), range.endTime()));
        BigDecimal returnAmount = defaultAmount(bizSalesReturnMapper.sumValidReturnAmount(range.startTime(), range.endTime()));
        Long salesQuantity = defaultCount(bizSalesMapper.sumValidSalesQuantity(range.startTime(), range.endTime()));
        Long returnQuantity = defaultCount(bizSalesReturnMapper.sumValidReturnQuantity(range.startTime(), range.endTime()));
        BigDecimal salesCost = defaultAmount(bizSalesMapper.sumEstimatedSalesCost(range.startTime(), range.endTime()));
        BigDecimal returnCost = defaultAmount(bizSalesReturnMapper.sumEstimatedReturnCost(range.startTime(), range.endTime()));

        BigDecimal netSalesAmount = salesAmount.subtract(returnAmount);
        long netQuantity = salesQuantity - returnQuantity;
        BigDecimal estimatedCost = salesCost.subtract(returnCost);
        BigDecimal grossProfitAmount = netSalesAmount.subtract(estimatedCost);

        ProfitOverviewVO vo = new ProfitOverviewVO();
        vo.setSalesAmount(salesAmount);
        vo.setReturnAmount(returnAmount);
        vo.setNetSalesAmount(netSalesAmount);
        vo.setSalesQuantity(salesQuantity);
        vo.setReturnQuantity(returnQuantity);
        vo.setNetQuantity(netQuantity);
        vo.setEstimatedCost(estimatedCost);
        vo.setGrossProfitAmount(grossProfitAmount);
        vo.setGrossProfitRate(calculateRate(grossProfitAmount, netSalesAmount));
        return vo;
    }

    public ProfitBrandTopVO getProfitBrandTop(SalesChartQueryDTO queryDTO) {
        DateRange range = resolveValidRange(queryDTO);

        Map<String, BigDecimal> brandProfitMap = new HashMap<>();
        for (BizSalesMapper.BrandAmountAgg row : bizSalesMapper.brandGrossProfitPart(range.startTime(), range.endTime())) {
            mergeAmount(brandProfitMap, row.getName(), row.getAmount());
        }
        for (BizSalesMapper.BrandAmountAgg row : bizSalesReturnMapper.brandGrossProfitPart(range.startTime(), range.endTime())) {
            mergeAmount(brandProfitMap, row.getName(), row.getAmount());
        }

        List<Map.Entry<String, BigDecimal>> sortedRows = new ArrayList<>(brandProfitMap.entrySet());
        sortedRows.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        List<String> nameList = new ArrayList<>();
        List<BigDecimal> dataList = new ArrayList<>();
        int limit = Math.min(5, sortedRows.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, BigDecimal> entry = sortedRows.get(i);
            nameList.add(entry.getKey());
            dataList.add(entry.getValue());
        }

        ProfitBrandTopVO vo = new ProfitBrandTopVO();
        vo.setNameList(nameList);
        vo.setDataList(dataList);
        return vo;
    }

    public ProfitTrendVO getProfitDailyTrend(SalesChartQueryDTO queryDTO) {
        DateRange range = resolveValidRange(queryDTO);

        Map<String, BigDecimal> salesAmountMap = toAmountMap(bizSalesMapper.dailyValidSalesAmount(range.startTime(), range.endTime()));
        Map<String, BigDecimal> returnAmountMap = toAmountMap(bizSalesReturnMapper.dailyValidReturnAmount(range.startTime(), range.endTime()));
        Map<String, BigDecimal> salesCostMap = toAmountMap(bizSalesMapper.dailyEstimatedSalesCost(range.startTime(), range.endTime()));
        Map<String, BigDecimal> returnCostMap = toAmountMap(bizSalesReturnMapper.dailyEstimatedReturnCost(range.startTime(), range.endTime()));

        List<String> dateList = new ArrayList<>();
        List<BigDecimal> amountList = new ArrayList<>();
        LocalDate current = range.startDate();
        while (!current.isAfter(range.endDate())) {
            String fullDate = current.toString();
            BigDecimal netSalesAmount = salesAmountMap.getOrDefault(fullDate, BigDecimal.ZERO)
                    .subtract(returnAmountMap.getOrDefault(fullDate, BigDecimal.ZERO));
            BigDecimal estimatedCost = salesCostMap.getOrDefault(fullDate, BigDecimal.ZERO)
                    .subtract(returnCostMap.getOrDefault(fullDate, BigDecimal.ZERO));
            BigDecimal grossProfit = netSalesAmount.subtract(estimatedCost);

            dateList.add(current.format(DATE_FORMATTER));
            amountList.add(grossProfit);
            current = current.plusDays(1);
        }

        ProfitTrendVO vo = new ProfitTrendVO();
        vo.setDateList(dateList);
        vo.setAmountList(amountList);
        return vo;
    }

    // 解析查询条件中的日期范围，返回包含开始日期、结束日期及其对应的 LocalDateTime 的 DateRange 对象
    private DateRange resolveRange(SalesChartQueryDTO queryDTO) {
        LocalDate startDate = queryDTO.getStartDate();
        LocalDate endDate = queryDTO.getEndDate();

        if (startDate == null && endDate == null) {
            LocalDateTime minTime = bizSalesMapper.minOperationTime();
            LocalDateTime maxTime = bizSalesMapper.maxOperationTime();
            if (minTime != null && maxTime != null) {
                startDate = minTime.toLocalDate();
                endDate = maxTime.toLocalDate();
            } else {
                endDate = LocalDate.now();
                startDate = endDate.minusDays(6);
            }
        } else {
            endDate = endDate == null ? LocalDate.now() : endDate;
            startDate = startDate == null ? endDate.minusDays(6) : startDate;
        }

        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        return new DateRange(startDate, endDate, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    }

    private DateRange resolveValidRange(SalesChartQueryDTO queryDTO) {
        LocalDate startDate = queryDTO.getStartDate();
        LocalDate endDate = queryDTO.getEndDate();

        if (startDate == null && endDate == null) {
            LocalDateTime minTime = bizSalesMapper.minValidOperationTime();
            LocalDateTime maxTime = bizSalesMapper.maxValidOperationTime();
            if (minTime != null && maxTime != null) {
                startDate = minTime.toLocalDate();
                endDate = maxTime.toLocalDate();
            } else {
                endDate = LocalDate.now();
                startDate = endDate.minusDays(6);
            }
        } else {
            endDate = endDate == null ? LocalDate.now() : endDate;
            startDate = startDate == null ? endDate.minusDays(6) : startDate;
        }

        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        return new DateRange(startDate, endDate, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    }

    private Map<String, BigDecimal> toAmountMap(List<BizSalesMapper.DailyAmountAgg> rows) {
        Map<String, BigDecimal> amountMap = new HashMap<>();
        for (BizSalesMapper.DailyAmountAgg row : rows) {
            amountMap.put(row.getStatDate(), defaultAmount(row.getAmount()));
        }
        return amountMap;
    }

    private void mergeAmount(Map<String, BigDecimal> target, String name, BigDecimal amount) {
        String key = (name == null || name.isBlank()) ? "未标注品牌" : name;
        target.merge(key, defaultAmount(amount), BigDecimal::add);
    }

    private BigDecimal calculateRate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return numerator
                .divide(denominator, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    // 辅助方法：如果金额值为 null，则返回 BigDecimal.ZERO；否则返回原值
    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long defaultCount(Long value) {
        return value == null ? 0L : value;
    }

    private record DateRange(LocalDate startDate, LocalDate endDate, LocalDateTime startTime, LocalDateTime endTime) {
    }
}
