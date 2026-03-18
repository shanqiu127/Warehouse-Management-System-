package org.example.back.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SalesTrendVO {

    private List<String> dateList;

    private List<BigDecimal> amountList;
}
