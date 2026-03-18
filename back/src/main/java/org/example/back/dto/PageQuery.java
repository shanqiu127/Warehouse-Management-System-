package org.example.back.dto;

import lombok.Data;

/**
 * 通用分页查询参数
 */
@Data
public class PageQuery {

    /**
     * 页码
     */
    private Long pageNum = 1L;

    /**
     * 每页条数
     */
    private Long pageSize = 10L;
}