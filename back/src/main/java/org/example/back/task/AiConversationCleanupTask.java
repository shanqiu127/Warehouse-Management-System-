package org.example.back.task;

import org.example.back.service.AiConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AiConversationCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(AiConversationCleanupTask.class);

    @Autowired
    private AiConversationService conversationService;

    /**
     * 每天凌晨3点清理过期会话（超过30天）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredConversations() {
        try {
            int count = conversationService.cleanupExpired();
            if (count > 0) {
                log.info("已清理 {} 个过期AI会话", count);
            }
        } catch (Exception e) {
            log.error("清理过期AI会话失败", e);
        }
    }
}
