package org.example.back.vo;

import lombok.Data;

import java.util.List;

@Data
public class SalesTop5VO {

    private List<String> nameList;

    private List<Long> dataList;
}
