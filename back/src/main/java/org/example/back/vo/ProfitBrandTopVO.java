package org.example.back.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProfitBrandTopVO {

    private List<String> nameList;

    private List<BigDecimal> dataList;
}
