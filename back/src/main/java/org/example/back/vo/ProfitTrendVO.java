package org.example.back.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProfitTrendVO {

    private List<String> dateList;

    private List<BigDecimal> amountList;
}
