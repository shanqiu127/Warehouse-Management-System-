package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_message")
public class AiMessage {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private String role;

    private String content;

    private String sourcesJson;

    private String hitType;

    private String providerCode;

    private String modelCode;

    private Boolean fallbackUsed;

    private LocalDateTime createdAt;
}
