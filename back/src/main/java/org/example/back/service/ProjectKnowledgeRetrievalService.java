package org.example.back.service;

import lombok.extern.slf4j.Slf4j;
import org.example.back.config.ProjectAssistantProperties;
import org.example.back.entity.ProjectDocChunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProjectKnowledgeRetrievalService {

    private static final String CATEGORY_OVERVIEW = "项目概览";
    private static final String CATEGORY_DEPLOY = "部署启动";
    private static final String CATEGORY_FRONT = "前端页面";
    private static final String CATEGORY_BACK = "后端接口";
    private static final String CATEGORY_ROLE = "角色权限";
    private static final String CATEGORY_FLOW = "业务流程";

    private static final Map<String, List<String>> DEPT_QUERY_KEYWORDS = new LinkedHashMap<>();

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = new LinkedHashMap<>();

        private static final List<String> PROJECT_CONTEXT_KEYWORDS = List.of(
            "登录", "消息", "审批", "公告", "页面", "菜单", "路由", "接口", "权限", "角色",
            "模块", "功能", "会话", "知识库", "助手", "部署", "启动", "数据库", "报错", "异常",
            "首页", "工作要求", "预警", "库存", "订单", "供应商", "商品", "员工", "管理员", "部门"
        );

    static {
        CATEGORY_KEYWORDS.put(CATEGORY_OVERVIEW, List.of("项目", "简介", "介绍", "什么系统", "概览", "总览", "亮点", "特点", "功能", "模块"));
        CATEGORY_KEYWORDS.put(CATEGORY_DEPLOY, List.of("启动", "部署", "安装", "运行", "配置", "端口", "数据库", "环境", "命令", "npm", "maven", "vite", "build"));
        CATEGORY_KEYWORDS.put(CATEGORY_FRONT, List.of("前端", "页面", "菜单", "路由", "组件", "Vue", "Element", "侧边栏", "首页", "工作台", "界面", "UI"));
        CATEGORY_KEYWORDS.put(CATEGORY_BACK, List.of("后端", "接口", "API", "Controller", "Service", "数据库", "表", "字段", "MyBatis", "Sa-Token", "权限校验"));
        CATEGORY_KEYWORDS.put(CATEGORY_ROLE, List.of("角色", "权限", "管理员", "员工", "超级管理员", "部门", "可见", "能做什么", "不能", "边界", "访问", "仓储", "采购", "销售", "财务", "人事"));
        CATEGORY_KEYWORDS.put(CATEGORY_FLOW, List.of("流程", "审批", "退货", "进货", "销售", "作废", "红冲", "工作要求", "公告", "预警", "通知", "闭环", "任务"));

        DEPT_QUERY_KEYWORDS.put("hr", List.of("人事", "员工", "部门", "组织", "分布图表", "员工管理", "部门管理"));
        DEPT_QUERY_KEYWORDS.put("purchase", List.of("采购", "进货", "进货退货", "采购退货", "补货"));
        DEPT_QUERY_KEYWORDS.put("sales", List.of("销售", "销售退货", "订单", "出货"));
        DEPT_QUERY_KEYWORDS.put("warehouse", List.of("仓储", "库存", "预警", "供应商", "商品资料", "作废审批", "红冲审批"));
        DEPT_QUERY_KEYWORDS.put("finance", List.of("财务", "统计", "毛利", "利润", "经营分析", "销售统计"));
    }

    /**
     * 角色主文档映射：deptCode -> fileName
     */
    private static final Map<String, String> DEPT_PRIMARY_DOC = Map.of(
            "hr", "hr_admin.md",
            "purchase", "purchase_admin.md",
            "sales", "sales_admin.md",
            "warehouse", "warehouse_admin.md",
            "finance", "finance_admin.md"
    );

    @Autowired
    private ProjectKnowledgeBaseService knowledgeBaseService;

    @Autowired
    private ProjectAssistantProperties properties;

    /**
     * 判断问题是否为非项目问题
     */
    public boolean isNonProjectQuestion(String question) {
        return !isLikelyProjectQuestion(question);
    }

    public boolean isLikelyProjectQuestion(String question) {
        String q = normalize(question);
        if (q.isEmpty()) {
            return false;
        }

        for (List<String> keywords : CATEGORY_KEYWORDS.values()) {
            for (String kw : keywords) {
                if (q.contains(normalize(kw))) {
                    return true;
                }
            }
        }
        for (List<String> keywords : DEPT_QUERY_KEYWORDS.values()) {
            for (String keyword : keywords) {
                if (q.contains(normalize(keyword))) {
                    return true;
                }
            }
        }
        if (q.contains("这个项目") || q.contains("这个系统") || q.contains("仓库管理系统") || q.contains("企业协同运营管理系统") || q.contains("ecs")) {
            return true;
        }
        return PROJECT_CONTEXT_KEYWORDS.stream().anyMatch(q::contains);
    }

    /**
     * 分类问题
     */
    public String classifyQuestion(String question) {
        String q = question.toLowerCase();
        int maxScore = 0;
        String bestCategory = CATEGORY_OVERVIEW;

        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String kw : entry.getValue()) {
                if (q.contains(kw.toLowerCase())) {
                    score++;
                }
            }
            if (score > maxScore) {
                maxScore = score;
                bestCategory = entry.getKey();
            }
        }

        return bestCategory;
    }

    /**
     * 构建角色候选文档集并进行关键词检索
     */
    public List<ProjectDocChunk> retrieve(String question, String role, String deptCode) {
        List<ProjectDocChunk> allChunks = knowledgeBaseService.getAllChunks();
        if (allChunks.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> candidateFiles = buildCandidateFiles(question, role, deptCode);
        List<ProjectDocChunk> candidates = allChunks.stream()
                .filter(chunk -> candidateFiles.contains(chunk.getFileName()))
                .filter(chunk -> canAccessChunk(chunk, role))
                .collect(Collectors.toList());

        String primaryDoc = getPrimaryDoc(role, deptCode);
        String category = classifyQuestion(question);
        String preferredSourceType = categoryToSourceType(category);

        List<ScoredChunk> scored = new ArrayList<>();
        for (ProjectDocChunk chunk : candidates) {
            int score = computeScore(chunk, question, primaryDoc, preferredSourceType);
            if (score > 0) {
                scored.add(new ScoredChunk(chunk, score));
            }
        }

        scored.sort(Comparator.comparingInt(ScoredChunk::score).reversed());

        int maxChunks = properties.getMaxChunks();
        return scored.stream()
                .limit(maxChunks)
                .map(ScoredChunk::chunk)
                .collect(Collectors.toList());
    }

    public Set<String> getSuggestionFiles(String role, String deptCode) {
        String normalizedRole = normalize(role);
        LinkedHashSet<String> files = new LinkedHashSet<>();
        if ("employee".equals(normalizedRole)) {
            files.add("employee.md");
            return files;
        }

        if ("superadmin".equals(normalizedRole)) {
            files.add("superadmin.md");
            files.add("project.md");
            return files;
        }

        String primaryDoc = getPrimaryDoc(role, deptCode);
        if (primaryDoc != null) {
            files.add(primaryDoc);
        }
        return files;
    }

    private Set<String> buildCandidateFiles(String question, String role, String deptCode) {
        String normalizedRole = normalize(role);
        String category = classifyQuestion(question);
        LinkedHashSet<String> candidateFiles = new LinkedHashSet<>();

        if ("employee".equals(normalizedRole)) {
            candidateFiles.add("employee.md");
            return candidateFiles;
        }

        if ("superadmin".equals(normalizedRole)) {
            candidateFiles.add("superadmin.md");
            String targetDept = detectTargetDept(question);
            if (targetDept != null) {
                candidateFiles.add(DEPT_PRIMARY_DOC.get(targetDept));
            }
            addSupplementFiles(candidateFiles, category);
            candidateFiles.add("project.md");
            return candidateFiles;
        }

        String primaryDoc = getPrimaryDoc(role, deptCode);
        if (primaryDoc != null) {
            candidateFiles.add(primaryDoc);
        }
        if (CATEGORY_BACK.equals(category) || CATEGORY_FRONT.equals(category) || CATEGORY_DEPLOY.equals(category)) {
            addSupplementFiles(candidateFiles, category);
        }
        return candidateFiles;
    }

    private void addSupplementFiles(Set<String> candidateFiles, String category) {
        if (CATEGORY_DEPLOY.equals(category) || CATEGORY_OVERVIEW.equals(category)) {
            candidateFiles.add("README.md");
            candidateFiles.add("project.md");
            return;
        }
        if (CATEGORY_FRONT.equals(category)) {
            candidateFiles.add("project.md");
            candidateFiles.add("front.md");
            return;
        }
        if (CATEGORY_BACK.equals(category)) {
            candidateFiles.add("project.md");
            candidateFiles.add("back.md");
            return;
        }
        if (CATEGORY_FLOW.equals(category)) {
            candidateFiles.add("project.md");
        }
    }

    private String detectTargetDept(String question) {
        String q = normalize(question);
        for (Map.Entry<String, List<String>> entry : DEPT_QUERY_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (q.contains(normalize(keyword))) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private boolean canAccessChunk(ProjectDocChunk chunk, String role) {
        String normalizedRole = normalize(role);
        String visibility = chunk.getVisibility();
        if ("employee_only".equals(visibility)) {
            return "employee".equals(normalizedRole);
        }
        if ("superadmin".equals(visibility)) {
            return "superadmin".equals(normalizedRole);
        }
        if ("admin".equals(visibility)) {
            return "admin".equals(normalizedRole) || "superadmin".equals(normalizedRole);
        }
        return true;
    }

    private int computeScore(ProjectDocChunk chunk, String question, String primaryDoc, String preferredSourceType) {
        int score = 0;
        String q = question.toLowerCase();
        String content = chunk.getContent().toLowerCase();
        String fileName = chunk.getFileName().toLowerCase();

        // Primary doc bonus
        if (primaryDoc != null && fileName.equals(primaryDoc.toLowerCase())) {
            score += 12;
        }

        // Title match
        for (String title : chunk.getTitlePath()) {
            String t = title.toLowerCase();
            String tClean = t.replaceAll("^[\\d.]+\\s*", "").replaceAll("[（(].+[）)]$", "").trim();
            if (q.contains(t) || (tClean.length() >= 2 && q.contains(tClean))) {
                score += 10;
            } else {
                // Partial match: check each word in question against title
                String[] qWords = q.split("[\\s，。？！、]+");
                for (String w : qWords) {
                    if (w.length() >= 2 && (t.contains(w) || tClean.contains(w))) {
                        score += 7;
                        break;
                    }
                }
            }
        }

        // Title path match
        String titlePathStr = String.join(" ", chunk.getTitlePath()).toLowerCase()
                .replaceAll("[\\d.]+", " ").replaceAll("[（(][^）)]+[）)]", " ");
        String[] questionWords = q.split("[\\s，。？！、]+");
        for (String w : questionWords) {
            if (w.length() >= 2 && titlePathStr.contains(w)) {
                score += 6;
                break;
            }
        }

        // Source type match
        if (preferredSourceType != null && preferredSourceType.equals(chunk.getSourceType())) {
            score += 5;
        }

        // Content keyword match
        int contentHits = 0;
        for (String w : questionWords) {
            if (w.length() >= 2 && content.contains(w)) {
                contentHits++;
            }
        }
        score += Math.min(contentHits, 5) * 3;

        // Priority bonus
        score += chunk.getPriority();

        return score;
    }

    private String getPrimaryDoc(String role, String deptCode) {
        String r = normalize(role);
        if ("superadmin".equals(r)) {
            return "superadmin.md";
        }
        if ("employee".equals(r)) {
            return "employee.md";
        }
        if ("admin".equals(r) && deptCode != null) {
            return DEPT_PRIMARY_DOC.get(normalize(deptCode));
        }
        return null;
    }

    private String categoryToSourceType(String category) {
        return switch (category) {
            case CATEGORY_OVERVIEW -> "readme";
            case CATEGORY_DEPLOY -> "readme";
            case CATEGORY_FRONT -> "front";
            case CATEGORY_BACK -> "back";
            case CATEGORY_ROLE -> "role";
            case CATEGORY_FLOW -> "project";
            default -> null;
        };
    }

    /**
     * Check if recall score is below threshold (no useful results)
     */
    public boolean isBelowThreshold(List<ProjectDocChunk> chunks) {
        return chunks.isEmpty();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    record ScoredChunk(ProjectDocChunk chunk, int score) {}
}
