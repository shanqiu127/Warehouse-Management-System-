package org.example.back.service;

import org.example.back.entity.AiConversation;
import org.example.back.entity.AiMessage;
import org.example.back.mapper.AiConversationMapper;
import org.example.back.mapper.AiMessageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiConversationServiceTest {

    @Mock
    private AiConversationMapper conversationMapper;

    @Mock
    private AiMessageMapper messageMapper;

    @InjectMocks
    private AiConversationService aiConversationService;

    @Test
    void saveMessagePair_shouldRejectInvalidSourcesJson() {
        AiConversation conversation = new AiConversation();
        conversation.setId(1L);
        conversation.setUserId(99L);
        when(conversationMapper.selectById(1L)).thenReturn(conversation);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> aiConversationService.saveMessagePair(1L, 99L, "用户问题", "助手回答", "{bad-json", "project-doc", null, null, null)
        );

        assertEquals("来源文档格式不正确", exception.getMessage());
        verify(messageMapper, never()).insert(any());
    }

    @Test
    void saveMessagePair_shouldPersistMetadata() {
        AiConversation conversation = new AiConversation();
        conversation.setId(1L);
        conversation.setUserId(99L);
        when(conversationMapper.selectById(1L)).thenReturn(conversation);

        doAnswer(invocation -> {
            AiMessage message = invocation.getArgument(0);
            if ("assistant".equals(message.getRole())) {
                message.setId(200L);
            } else {
                message.setId(100L);
            }
            return 1;
        }).when(messageMapper).insert(any(AiMessage.class));

        aiConversationService.saveMessagePair(
                1L,
                99L,
                "  用户问题\n需要摘要  ",
                "助手回答",
                "[]",
                "project-doc",
                "qwen",
                "qwen-plus",
                false
        );

        ArgumentCaptor<AiMessage> messageCaptor = ArgumentCaptor.forClass(AiMessage.class);
        verify(messageMapper, times(2)).insert(messageCaptor.capture());
        AiMessage assistantMessage = messageCaptor.getAllValues().get(1);
        assertEquals("qwen", assistantMessage.getProviderCode());
        assertEquals("qwen-plus", assistantMessage.getModelCode());
        assertEquals(Boolean.FALSE, assistantMessage.getFallbackUsed());
    }
}