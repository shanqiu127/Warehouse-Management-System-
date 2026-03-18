package org.example.back.service;

import org.example.back.dto.SalesChartQueryDTO;
import org.example.back.mapper.BizSalesMapper;
import org.example.back.mapper.BizSalesReturnMapper;
import org.example.back.vo.BrandRatioItemVO;
import org.example.back.vo.ChartOverviewVO;
import org.example.back.vo.SalesTop5VO;
import org.example.back.vo.SalesTrendVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long defaultCount(Long value) {
        return value == null ? 0L : value;
    }

    private record DateRange(LocalDate startDate, LocalDate endDate, LocalDateTime startTime, LocalDateTime endTime) {
    }
}
