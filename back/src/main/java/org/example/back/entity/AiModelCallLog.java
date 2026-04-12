package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_model_call_log")
public class AiModelCallLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String roleCode;

    private String deptCode;

    private Long conversationId;

    private Long assistantMessageId;

    private String sceneCode;

    private String questionType;

    private String requestedModelCode;

    private String providerCode;

    private String modelCode;

    private Boolean fallbackUsed;

    private String hitType;

    private String resultStatus;

    private Long latencyMs;

    private String questionExcerpt;

    private LocalDateTime createdAt;
}