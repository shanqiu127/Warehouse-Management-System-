package org.example.back.service;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.back.common.AssistantAnswerMode;
import org.example.back.config.ProjectAssistantProperties;
import org.example.back.entity.ProjectDocChunk;
import org.example.back.llm.dto.LlmChatResponse;
import org.example.back.llm.dto.LlmInvocationContext;
import org.example.back.llm.dto.LlmModelOption;
import org.example.back.llm.exception.LlmBudgetExceededException;
import org.example.back.llm.service.LlmAuditService;
import org.example.back.llm.service.LlmGovernanceService;
import org.example.back.llm.service.LlmRoutingService;
import org.example.back.vo.ProjectAssistantAnswerVO;
import org.example.back.vo.ProjectAssistantModelOptionVO;
import org.example.back.vo.ProjectAssistantSourceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProjectAssistantService {

    private static final String NO_EVIDENCE_MSG = "抱歉，我没有找到相关的知识库，请您提供更多的信息或者尝试其他的关键词。接下来采用大模型进行搜索为你解答。";
    private static final String STRICT_MODE_NO_EVIDENCE_MSG = "仅按项目文档回答模式下，当前未找到足够依据回答这个问题。你可以补充更具体的项目关键词，或切换到“文档优先，不足时允许模型补充”。";
    private static final String NON_PROJECT_MSG = "我目前只负责说明当前仓库管理系统项目，暂时不回答项目范围外的问题。你可以问我项目功能、技术栈、部署方式、模块职责、接口范围、角色权限或业务流程。";
    private static final String GENERAL_QUERY_LIMIT_MSG = "这个问题可能触发较长推理或 token 消耗过高，已暂停回答。请缩小范围，或改成更直接的问题。";

    private static final Map<String, String> SOURCE_TYPE_LABELS = Map.of(
            "readme", "README",
            "project", "项目结构",
            "front", "前端",
            "back", "后端",
            "role", "角色知识"
    );

    private static final Map<String, String> DEPT_LABEL_MAP = Map.of(
            "hr", "人事",
            "purchase", "采购",
            "sales", "销售",
            "warehouse", "仓储",
            "finance", "财务"
    );

    private static final Map<String, String> MODEL_LABEL_MAP = Map.of(
            "qwen-plus", "千问 Plus",
            "glm-4-flash", "智谱 GLM-4-Flash",
            "deepseek-chat", "DeepSeek Chat",
            "kimi-8k", "Kimi 8K"
    );

    @Autowired
    private ProjectAssistantProperties properties;

    @Autowired
    private ProjectKnowledgeBaseService knowledgeBaseService;

    @Autowired
    private ProjectKnowledgeRetrievalService retrievalService;

    @Autowired
    private LlmRoutingService llmRoutingService;

    @Autowired
    private LlmGovernanceService llmGovernanceService;

    @Autowired
    private LlmAuditService llmAuditService;

    /**
     * 推荐问题缓存: role -> list of suggestions
     */
    private final ConcurrentHashMap<String, List<String>> suggestionsCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (properties.isEnabled()) {
            buildSuggestions();
        }
    }

    /**
     * 处理用户提问
     */
    public ProjectAssistantAnswerVO query(String question, String rawMode, String requestedModelCode, Long conversationId) {
        AssistantAnswerMode mode = AssistantAnswerMode.resolve(rawMode);
        String role = getCurrentRole();
        String deptCode = getCurrentDeptCode();
        Long userId = getCurrentUserId();
        String selectedModelCode = llmRoutingService.canSelectModel(role) ? normalizeRequestedModelCode(requestedModelCode) : null;

        List<ProjectDocChunk> chunks = retrievalService.retrieve(question, role, deptCode);

        if (retrievalService.isBelowThreshold(chunks)) {
            if (!retrievalService.isLikelyProjectQuestion(question)) {
                if (mode == AssistantAnswerMode.STRICT) {
                    return buildRefusalAnswer(question, STRICT_MODE_NO_EVIDENCE_MSG, "kb-miss-strict", mode);
                }
                return handleGeneralQuestion(question, mode, selectedModelCode, userId, role, deptCode, conversationId);
            }
            if (mode == AssistantAnswerMode.STRICT) {
                return buildRefusalAnswer(question, STRICT_MODE_NO_EVIDENCE_MSG, "kb-miss-strict", mode);
            }
            if (llmRoutingService.isProjectModelConfigured()) {
                return handleKnowledgeBaseMissWithLLM(question, mode, selectedModelCode, userId, role, deptCode, conversationId);
            }
            return buildRefusalAnswer(question, NO_EVIDENCE_MSG, "no-hit", mode);
        }

        if (!llmRoutingService.isProjectModelConfigured()) {
            return buildLocalAnswer(question, chunks, mode);
        }

        String roleLabel = buildRoleLabel(role, deptCode);
        String systemPrompt = buildSystemPrompt(role, deptCode, roleLabel, mode);
        String userPrompt = buildUserPrompt(question, chunks);
        LlmInvocationContext invocationContext = buildInvocationContext(userId, role, deptCode, conversationId, question, selectedModelCode, "project", "project-doc");

        LlmGovernanceService.RateLimitDecision decision = llmGovernanceService.checkUserRateLimit(userId);
        if (!decision.isAllowed()) {
            llmAuditService.record(invocationContext, null, null, null, false, "rate_limited", 0L);
            return buildLocalAnswer(question, chunks, mode);
        }

        LlmChatResponse aiResponse;
        try {
            aiResponse = llmRoutingService.chatWithProjectModel(systemPrompt, userPrompt, selectedModelCode, invocationContext);
        } catch (LlmBudgetExceededException e) {
            log.warn("项目模型预算已达上限，回退为本地知识库回答", e);
            return buildLocalAnswer(question, chunks, mode);
        } catch (Exception e) {
            log.warn("项目模型调用失败，已切换为本地知识库回答", e);
            return buildLocalAnswer(question, chunks, mode);
        }

        return buildAnswer(question, aiResponse, chunks, mode);
    }

    public List<ProjectAssistantModelOptionVO> listAvailableModels() {
        String role = getCurrentRole();
        return llmRoutingService.listSelectableModels(role).stream()
                .map(this::toModelOption)
                .toList();
    }

    /**
     * 获取推荐问题
     */
    public List<String> getSuggestions() {
        String role = getCurrentRole();
        String deptCode = getCurrentDeptCode();
        String cacheKey = buildCacheKey(role, deptCode);

        List<String> cached = suggestionsCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        cached = suggestionsCache.get("general");
        return cached != null ? cached : List.of(
            "这个角色当前最核心的业务是什么？",
            "这个角色当前能处理哪些事情？",
            "这个角色的权限边界是什么？",
            "这个角色相关流程应该怎么走？"
        );
    }

    /**
     * Build suggestions from knowledge base
     */
    public void buildSuggestions() {
        List<ProjectDocChunk> allChunks = knowledgeBaseService.getAllChunks();
        if (allChunks.isEmpty()) {
            return;
        }

        suggestionsCache.put("general", List.of(
                "这个角色当前最核心的业务是什么？",
                "这个角色当前能处理哪些事情？",
                "这个角色的权限边界是什么？",
                "这个角色相关流程应该怎么走？"
        ));

        buildRoleSuggestions("superadmin", null);
        buildRoleSuggestions("employee", null);
        for (String dept : List.of("hr", "purchase", "sales", "warehouse", "finance")) {
            buildRoleSuggestions("admin", dept);
        }
    }

    private void buildRoleSuggestions(String role, String deptCode) {
        String cacheKey = buildCacheKey(role, deptCode);
        suggestionsCache.put(cacheKey, getRoleBusinessSuggestions(role, deptCode));
    }

    private List<String> getRoleBusinessSuggestions(String role, String deptCode) {
        if ("superadmin".equals(role)) {
            return List.of(
                    "超管总览页面有哪些指标和快捷入口？",
                    "部门审批页面怎么通过或驳回申请？",
                    "安全策略页面怎么新增和管理IP白名单？",
                    "登录日志页面记录哪些信息？",
                    "公告管理页面怎么发布公告？",
                    "跨部门业务归口是怎么划分的？"
            );
        }

        if ("employee".equals(role)) {
            return List.of(
                    "员工首页都展示哪些功能模块？",
                    "工作要求详情页怎么查看和接受任务？",
                    "任务状态流程有哪些阶段？",
                    "站内消息中心可以收到哪些通知？",
                    "员工首页的浮动提醒会显示什么？",
                    "部门场景说明里有哪些部门介绍？"
            );
        }

        if (!"admin".equals(role) || deptCode == null) {
            return List.of(
                    "这个角色当前最核心的业务是什么？",
                    "这个角色当前能处理哪些事情？",
                    "这个角色的权限边界是什么？",
                    "这个角色相关流程应该怎么走？"
            );
        }

        return switch (deptCode) {
            case "hr" -> List.of(
                    "全部门管理页面怎么新增和编辑部门？",
                    "全员工管理页面怎么新增员工？",
                    "员工分布图表页面展示哪些统计数据？",
                    "工作要求页面怎么发布和审核任务？",
                    "部门审批流程是怎样的？",
                    "首页功能有哪些快捷入口和提醒？"
            );
            case "purchase" -> List.of(
                    "商品进货页面怎么新增进货单？",
                    "进货退货页面怎么操作退货？",
                    "历史单据作废流程是怎样的？",
                    "预警中心页面显示哪些库存预警？",
                    "工作要求页面怎么发布和跟踪任务？",
                    "首页功能有哪些预警指标和快捷入口？"
            );
            case "sales" -> List.of(
                    "商品销售页面怎么新增销售单？",
                    "销售退货页面怎么操作退货？",
                    "历史单据作废流程是怎样的？",
                    "预警中心页面显示哪些库存预警？",
                    "工作要求页面怎么发布和跟踪任务？",
                    "首页功能有哪些预警指标和快捷入口？"
            );
            case "warehouse" -> List.of(
                    "供应商管理页面怎么新增和编辑供应商？",
                    "商品资料管理页面怎么设置预警阈值？",
                    "作废审批页面怎么审批通过或驳回？",
                    "预警中心页面的零库存和低库存有什么区别？",
                    "工作要求页面怎么发布和跟踪任务？",
                    "首页功能有哪些预警指标和快捷入口？"
            );
            case "finance" -> List.of(
                    "销售统计图表页面有哪些指标和图表？",
                    "毛利视角指标卡展示哪些数据？",
                    "毛利计算口径说明是怎样的？",
                    "图表页面的时间筛选怎么操作？",
                    "工作要求页面怎么发布和跟踪任务？",
                    "首页功能有哪些快捷入口和提醒？"
            );
            default -> List.of(
                    DEPT_LABEL_MAP.getOrDefault(deptCode, deptCode) + "管理员当前最核心的业务是什么？",
                    DEPT_LABEL_MAP.getOrDefault(deptCode, deptCode) + "管理员当前能处理哪些事情？",
                    DEPT_LABEL_MAP.getOrDefault(deptCode, deptCode) + "管理员的权限边界是什么？",
                    DEPT_LABEL_MAP.getOrDefault(deptCode, deptCode) + "管理员相关流程应该怎么走？"
            );
        };
    }

    private String buildSystemPrompt(String role, String deptCode, String roleLabel, AssistantAnswerMode mode) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是当前仓库管理系统项目的说明助手。\n");
        sb.append("你只能根据提供的项目文档片段回答。\n");
        sb.append("你必须遵守当前角色的知识边界。\n");
        sb.append("如果文档片段不足，请只给出能够确认的信息，不要编造不存在的细节。\n");
        if (mode == AssistantAnswerMode.STRICT) {
            sb.append("当前模式为仅按项目文档回答。文档没写到的内容必须明确说明无法确认，不能补充推测。\n");
        } else {
            sb.append("当前模式为文档优先，不足时允许模型补充，但必须明确区分哪些信息来自文档、哪些是谨慎补充。\n");
        }
        sb.append("不要补充文档中不存在的实现细节。\n");
        sb.append("不要输出当前角色不可见的内容。\n");
        sb.append("只回答项目相关问题。\n\n");

        sb.append("当前登录身份：").append(roleLabel).append("\n");

        if ("superadmin".equals(role)) {
            sb.append("当前身份可见范围：全部管理员知识、公共文档。\n");
        } else if ("admin".equals(role)) {
            sb.append("当前身份可见范围：本部门管理员知识、其他管理员知识、公共文档。不可见员工独立视角文档。\n");
        } else {
            sb.append("当前身份可见范围：员工知识、公共文档。不可见管理员专属知识。\n");
        }

        sb.append("\n请按以下格式输出：\n");
        sb.append("1. 先给出简明直接的答案\n");
        sb.append("2. 如果用户是在问怎么做、怎么操作、怎么新增、怎么审批、怎么发布，请优先给出可执行步骤\n");
        sb.append("3. 如果有明确文档依据，再补充简短的依据说明\n");

        return sb.toString();
    }

    private String buildUserPrompt(String question, List<ProjectDocChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是检索到的项目文档片段：\n\n");
        for (int i = 0; i < chunks.size(); i++) {
            ProjectDocChunk c = chunks.get(i);
            sb.append("--- 片段 ").append(i + 1).append(" ---\n");
            sb.append("来源文件：").append(c.getFileName()).append("\n");
            sb.append("标题路径：").append(String.join(" > ", c.getTitlePath())).append("\n");
            sb.append("内容：\n").append(truncate(c.getContent(), properties.getMaxSnippetLength())).append("\n\n");
        }
        sb.append("用户问题：").append(question).append("\n");
        return sb.toString();
    }

    private ProjectAssistantAnswerVO handleKnowledgeBaseMissWithLLM(String question,
                                                                    AssistantAnswerMode mode,
                                                                    String selectedModelCode,
                                                                    Long userId,
                                                                    String role,
                                                                    String deptCode,
                                                                    Long conversationId) {
        LlmInvocationContext invocationContext = buildInvocationContext(userId, role, deptCode, conversationId, question, selectedModelCode, "project", "kb-miss-model");
        LlmGovernanceService.RateLimitDecision decision = llmGovernanceService.checkUserRateLimit(userId);
        if (!decision.isAllowed()) {
            llmAuditService.record(invocationContext, null, null, null, false, "rate_limited", 0L);
            return buildRateLimitAnswer(question, decision.getRetryAfterSeconds(), mode);
        }

        try {
            String systemPrompt = "你是一个智能助手。用户的问题在项目知识库中未找到相关内容，请根据你的通用知识为用户提供有帮助的回答。"
                    + "回答应当简明扼要，控制在3段以内，优先给直接结论与最多3条要点。";
            LlmChatResponse aiResponse = llmRoutingService.chatWithProjectModel(systemPrompt, question, selectedModelCode, invocationContext);

            ProjectAssistantAnswerVO vo = new ProjectAssistantAnswerVO();
            vo.setQuestion(question);
            vo.setAnswer(aiResponse == null ? "" : aiResponse.getContent().trim());
            vo.setReasoning("该回答由大模型直接生成，知识库中未检索到相关内容。");
            vo.setSources(Collections.emptyList());
            vo.setHitType("kb-miss-model");
            vo.setMode(mode.getCode());
            applyLlmMetadata(vo, aiResponse);
            return vo;
        } catch (LlmBudgetExceededException e) {
            return buildBudgetBlockedAnswer(question, selectedModelCode, mode);
        } catch (Exception e) {
            log.warn("知识库未命中后调用大模型失败", e);
            return buildRefusalAnswer(question, NO_EVIDENCE_MSG, "no-hit", mode);
        }
    }

    private ProjectAssistantAnswerVO handleGeneralQuestion(String question,
                                                          AssistantAnswerMode mode,
                                                          String selectedModelCode,
                                                          Long userId,
                                                          String role,
                                                          String deptCode,
                                                          Long conversationId) {
        if (!llmRoutingService.canUseGeneralQuery(role)) {
            return buildRefusalAnswer(question, NON_PROJECT_MSG, "no-hit", mode);
        }
        if (!properties.getGeneral().isEnabled()) {
            return buildRefusalAnswer(question, NON_PROJECT_MSG, "no-hit", mode);
        }
        if (question == null || question.trim().isEmpty()) {
            return buildRefusalAnswer(question, NON_PROJECT_MSG, "no-hit", mode);
        }

        String normalized = question.trim();
        if (normalized.length() > properties.getGeneral().getMaxQuestionLength()) {
            return buildGeneralLimitAnswer(question, GENERAL_QUERY_LIMIT_MSG, mode);
        }

        int estimatedTokens = estimateTokens(normalized) + 180;
        if (estimatedTokens > properties.getGeneral().getMaxEstimatedTokens()) {
            return buildGeneralLimitAnswer(question, GENERAL_QUERY_LIMIT_MSG, mode);
        }

        LlmInvocationContext invocationContext = buildInvocationContext(userId, role, deptCode, conversationId, normalized, selectedModelCode, "general", "general-model");
        LlmGovernanceService.RateLimitDecision decision = llmGovernanceService.checkUserRateLimit(userId);
        if (!decision.isAllowed()) {
            llmAuditService.record(invocationContext, null, null, null, false, "rate_limited", 0L);
            return buildRateLimitAnswer(question, decision.getRetryAfterSeconds(), mode);
        }

        try {
            LlmChatResponse answer = llmRoutingService.chatGeneralQuestion(
                    buildGeneralSystemPrompt(),
                    normalized,
                    properties.getGeneral().getMaxResponseTokens(),
                    selectedModelCode,
                    invocationContext
            );
            return buildGeneralAnswer(question, answer, mode);
        } catch (LlmBudgetExceededException e) {
            return buildBudgetBlockedAnswer(question, selectedModelCode, mode);
        } catch (Exception e) {
            log.warn("项目外问题调用大模型失败", e);
            return buildRefusalAnswer(question, "项目外问题回答暂时不可用，请稍后再试。你也可以把问题改短一些再提问。", "no-hit", mode);
        }
    }

    private String buildGeneralSystemPrompt() {
        return "你可以回答项目范围外的问题，但必须严格控制输出长度。"
                + "请直接给结论，不要展示思维链，不要分步长推理，不要输出隐藏推理过程。"
                + "如果问题需要很长推理、背景铺垫或多轮分析，请直接回复：这个问题需要较长推理，已暂停，请缩小范围后再问。"
                + "回答控制在3段以内，优先给直接结论与最多3条要点。";
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int chineseLike = text.replaceAll("\\s+", "").length();
        return Math.max(1, (int) Math.ceil(chineseLike / 1.6));
    }

    private ProjectAssistantAnswerVO buildGeneralLimitAnswer(String question, String message, AssistantAnswerMode mode) {
        ProjectAssistantAnswerVO vo = new ProjectAssistantAnswerVO();
        vo.setQuestion(question);
        vo.setAnswer(message);
        vo.setReasoning("为避免长链推理和过高 token 消耗，系统对项目外问题设置了长度与预算上限。");
        vo.setSources(Collections.emptyList());
        vo.setHitType("general-limited");
        vo.setMode(mode.getCode());
        return vo;
    }

    private ProjectAssistantAnswerVO buildRateLimitAnswer(String question, long retryAfterSeconds, AssistantAnswerMode mode) {
        String message = "请求过于频繁，请 " + Math.max(1L, retryAfterSeconds) + " 秒后再试。";
        ProjectAssistantAnswerVO vo = new ProjectAssistantAnswerVO();
        vo.setQuestion(question);
        vo.setAnswer(message);
        vo.setReasoning("系统已触发按用户限流保护，以避免高频滥用。请稍后重试。\n恢复条件：等待限流窗口结束后再次提问。");
        vo.setSources(Collections.emptyList());
        vo.setHitType("rate-limited");
        vo.setMode(mode.getCode());
        return vo;
    }

    private ProjectAssistantAnswerVO buildBudgetBlockedAnswer(String question, String selectedModelCode, AssistantAnswerMode mode) {
        String modelLabel = selectedModelCode == null ? "当前可用模型" : MODEL_LABEL_MAP.getOrDefault(selectedModelCode, selectedModelCode);
        ProjectAssistantAnswerVO vo = new ProjectAssistantAnswerVO();
        vo.setQuestion(question);
        vo.setAnswer(modelLabel + " 今日预算已达上限，请稍后再试。");
        vo.setReasoning("系统已触发按模型预算控制，当前模型暂不可继续调用。恢复条件：等待预算窗口重置，或切换到其他仍可用模型。\n");
        vo.setSources(Collections.emptyList());
        vo.setHitType("budget-blocked");
        vo.setMode(mode.getCode());
        return vo;
    }

    private ProjectAssistantAnswerVO buildGeneralAnswer(String question, LlmChatResponse answer, AssistantAnswerMode mode) {
        ProjectAssistantAnswerVO vo = new ProjectAssistantAnswerVO();
        vo.setQuestion(question);
        String content = answer == null ? "" : answer.getContent();
        vo.setAnswer(content == null || content.isBlank() ? "项目外问题未返回有效结果，请换个更直接的问题。" : content.trim());
        vo.setReasoning("该回答由大模型直接生成，未使用项目知识库来源。系统已限制回答长度与推理开销。");
        vo.setSources(Collections.emptyList());
        vo.setHitType("general-model");
        vo.setMode(mode.getCode());
        applyLlmMetadata(vo, answer);
        return vo;
    }

    private ProjectAssistantAnswerVO buildLocalAnswer(String question, List<ProjectDocChunk> chunks, AssistantAnswerMode mode) {
        ProjectAssistantAnswerVO vo = new ProjectAssistantAnswerVO();
        vo.setQuestion(question);

        String conclusion = buildConclusion(chunks);
        String reasoning = buildReasoning(chunks);
        List<ProjectAssistantSourceVO> sources = buildSources(chunks);

        StringBuilder answer = new StringBuilder();
        answer.append("结论：").append(conclusion).append("\n\n");
        answer.append("依据：").append(reasoning).append("\n\n");
        answer.append("当前为本地知识库整理结果，未依赖外部 AI 服务。");

        vo.setAnswer(answer.toString().trim());
        vo.setReasoning(reasoning);
        vo.setSources(sources);
        vo.setHitType("project-doc");
        vo.setMode(mode.getCode());
        return vo;
    }

    private String buildConclusion(List<ProjectDocChunk> chunks) {
        ProjectDocChunk topChunk = chunks.get(0);
        String content = topChunk.getContent();
        String[] lines = content.split("\\n");
        List<String> bulletLines = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                bulletLines.add(trimmed.substring(2).trim());
            }
            if (bulletLines.size() >= 3) {
                break;
            }
        }

        if (!bulletLines.isEmpty()) {
            return String.join("；", bulletLines) + "。";
        }

        String normalized = content.replace("\n", " ").replaceAll("\\s+", " ").trim();
        int sentenceEnd = findSentenceEnd(normalized, 120);
        return normalized.substring(0, Math.min(sentenceEnd, normalized.length())).trim();
    }

    private String buildReasoning(List<ProjectDocChunk> chunks) {
        return chunks.stream()
                .limit(3)
                .map(chunk -> {
                    String sourceLabel = SOURCE_TYPE_LABELS.getOrDefault(chunk.getSourceType(), chunk.getSourceType());
                    String titlePath = chunk.getTitlePath().isEmpty()
                            ? chunk.getFileName()
                            : String.join(" > ", chunk.getTitlePath());
                    return sourceLabel + "中的“" + titlePath + "”提到了“" + truncate(compact(chunk.getContent()), 60) + "”";
                })
                .collect(Collectors.joining("；"));
    }

    private List<ProjectAssistantSourceVO> buildSources(List<ProjectDocChunk> chunks) {
        List<ProjectAssistantSourceVO> sources = new ArrayList<>();
        for (ProjectDocChunk chunk : chunks) {
            ProjectAssistantSourceVO source = new ProjectAssistantSourceVO();
            source.setFileName(chunk.getFileName());
            source.setTitlePath(String.join(" > ", chunk.getTitlePath()));
            source.setSnippet(truncate(compact(chunk.getContent()), 150));
            source.setSourceType(chunk.getSourceType());
            sources.add(source);
        }
        return sources;
    }

    private int findSentenceEnd(String text, int limit) {
        int max = Math.min(limit, text.length());
        for (int i = 0; i < max; i++) {
            char current = text.charAt(i);
            if (current == '。' || current == '！' || current == '？' || current == '.') {
                return i + 1;
            }
        }
        return max;
    }

    private String compact(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\n", " ").replaceAll("\\s+", " ").trim();
    }

    private ProjectAssistantAnswerVO buildAnswer(String question, LlmChatResponse aiResponse, List<ProjectDocChunk> chunks, AssistantAnswerMode mode) {
        ProjectAssistantAnswerVO vo = new ProjectAssistantAnswerVO();
        vo.setQuestion(question);

        vo.setAnswer(aiResponse == null ? "" : aiResponse.getContent().trim());
        vo.setReasoning("");

        vo.setSources(buildSources(chunks));
        vo.setHitType("project-doc");
        vo.setMode(mode.getCode());
        applyLlmMetadata(vo, aiResponse);
        return vo;
    }

    private ProjectAssistantAnswerVO buildRefusalAnswer(String question, String message, String hitType, AssistantAnswerMode mode) {
        ProjectAssistantAnswerVO vo = new ProjectAssistantAnswerVO();
        vo.setQuestion(question);
        vo.setAnswer(message);
        vo.setReasoning("");
        vo.setSources(Collections.emptyList());
        vo.setHitType(hitType);
        vo.setMode(mode.getCode());
        return vo;
    }

    private void applyLlmMetadata(ProjectAssistantAnswerVO vo, LlmChatResponse response) {
        if (vo == null || response == null) {
            return;
        }
        vo.setProviderCode(response.getProviderCode());
        vo.setModelCode(response.getModelCode());
        vo.setFallbackUsed(response.isFallbackUsed());
        vo.setLatencyMs(response.getLatencyMs());
    }

    private String buildRoleLabel(String role, String deptCode) {
        String r = normalize(role);
        if ("superadmin".equals(r)) return "超级管理员";
        if ("employee".equals(r)) return "普通员工";
        if ("admin".equals(r) && deptCode != null) {
            String deptLabel = DEPT_LABEL_MAP.getOrDefault(normalize(deptCode), deptCode);
            return deptLabel + "管理员";
        }
        return "管理员";
    }

    private String buildCacheKey(String role, String deptCode) {
        String r = normalize(role);
        if ("admin".equals(r) && deptCode != null) {
            return r + "_" + normalize(deptCode);
        }
        return r;
    }

    private ProjectAssistantModelOptionVO toModelOption(LlmModelOption option) {
        ProjectAssistantModelOptionVO vo = new ProjectAssistantModelOptionVO();
        vo.setProviderCode(option.getProviderCode());
        vo.setModelCode(option.getModelCode());
        vo.setLabel(MODEL_LABEL_MAP.getOrDefault(option.getModelCode(), option.getModelCode()));
        vo.setDefaultSelected(option.isDefaultSelected());
        return vo;
    }

    private String normalizeRequestedModelCode(String requestedModelCode) {
        if (requestedModelCode == null) {
            return null;
        }
        String normalized = requestedModelCode.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String getCurrentRole() {
        Object role = StpUtil.getSession().get("role");
        return role != null ? role.toString().trim().toLowerCase() : "employee";
    }

    private String getCurrentDeptCode() {
        Object deptCode = StpUtil.getSession().get("deptCode");
        return deptCode != null ? deptCode.toString().trim().toLowerCase() : null;
    }

    private Long getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    private LlmInvocationContext buildInvocationContext(Long userId,
                                                        String role,
                                                        String deptCode,
                                                        Long conversationId,
                                                        String question,
                                                        String requestedModelCode,
                                                        String questionType,
                                                        String hitType) {
        LlmInvocationContext context = new LlmInvocationContext();
        context.setUserId(userId);
        context.setRoleCode(role);
        context.setDeptCode(deptCode);
        context.setConversationId(conversationId);
        context.setSceneCode("project-assistant");
        context.setQuestionType(questionType);
        context.setHitType(hitType);
        context.setRequestedModelCode(requestedModelCode);
        context.setQuestionExcerpt(buildQuestionExcerpt(question));
        return context;
    }

    private String buildQuestionExcerpt(String question) {
        if (question == null) {
            return null;
        }
        String compacted = question.replaceAll("\\s+", " ").trim();
        if (compacted.isEmpty()) {
            return null;
        }
        return compacted.length() > 200 ? compacted.substring(0, 200) : compacted;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
