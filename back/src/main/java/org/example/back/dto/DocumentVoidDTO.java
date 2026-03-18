package org.example.back.dto;

import lombok.Data;

@Data
public class DocumentVoidDTO {

    private String reason;

    /**
     * 是否生成红冲单据记录（仅记录，不重复影响库存）。
     */
    private Boolean createRedFlush;
}
