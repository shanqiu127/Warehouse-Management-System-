package org.example.back.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkRequirementVO {

    private Long id;

    private String content;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String targetScope;

    private String creatorName;

    private LocalDateTime createTime;

    /** 汇总状态: 未完成/待审核/已完成 */
    private String summaryStatus;

    /** 总分配人数 */
    private Integer totalCount;

    /** 已完成人数 */
    private Integer completedCount;

    /** 待审核人数 */
    private Integer pendingReviewCount;

    /** 拒收人数 */
    private Integer rejectedCount;

    /** 超时中人数 */
    private Integer overdueCount;

    /** 逾期提交人数 */
    private Integer overdueSubmitCount;
}
