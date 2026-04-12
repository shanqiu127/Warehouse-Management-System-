package org.example.back.service;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.back.config.ProjectAssistantProperties;
import org.example.back.entity.ProjectDocChunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
public class ProjectKnowledgeBaseService {

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,3})\\s+(.+)$");
    private static final int MAX_CHUNK_LENGTH = 600;

    private static final Map<String, DocMeta> DOC_META_MAP = new LinkedHashMap<>();

    static {
        DOC_META_MAP.put("README.md", new DocMeta("readme", 10, "public", List.of("admin", "superadmin", "employee")));
        DOC_META_MAP.put("project.md", new DocMeta("project", 9, "public", List.of("admin", "superadmin", "employee")));
        DOC_META_MAP.put("front.md", new DocMeta("front", 9, "public", List.of("admin", "superadmin", "employee")));
        DOC_META_MAP.put("back.md", new DocMeta("back", 9, "public", List.of("admin", "superadmin", "employee")));
        DOC_META_MAP.put("superadmin.md", new DocMeta("role", 10, "superadmin", List.of("superadmin")));
        DOC_META_MAP.put("hr_admin.md", new DocMeta("role", 10, "admin", List.of("admin", "superadmin")));
        DOC_META_MAP.put("purchase_admin.md", new DocMeta("role", 10, "admin", List.of("admin", "superadmin")));
        DOC_META_MAP.put("sales_admin.md", new DocMeta("role", 10, "admin", List.of("admin", "superadmin")));
        DOC_META_MAP.put("warehouse_admin.md", new DocMeta("role", 10, "admin", List.of("admin", "superadmin")));
        DOC_META_MAP.put("finance_admin.md", new DocMeta("role", 10, "admin", List.of("admin", "superadmin")));
        DOC_META_MAP.put("employee.md", new DocMeta("role", 10, "employee_only", List.of("employee")));
    }

    @Autowired
    private ProjectAssistantProperties properties;

    private final CopyOnWriteArrayList<ProjectDocChunk> chunks = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<String> missingPaths = new CopyOnWriteArrayList<>();
    private volatile long lastBuildTimeMs;
    private volatile int documentCount;

    @PostConstruct
    public void init() {
        if (properties.isEnabled()) {
            rebuild();
        }
    }

    public synchronized Map<String, Object> rebuild() {
        long start = System.currentTimeMillis();
        List<ProjectDocChunk> newChunks = new ArrayList<>();
        AtomicInteger docCount = new AtomicInteger(0);
        List<String> newMissingPaths = new ArrayList<>();
        List<Path> markdownFiles = resolveConfiguredMarkdownFiles(newMissingPaths);

        for (Path resolved : markdownFiles) {
            try {
                String content = Files.readString(resolved, StandardCharsets.UTF_8);
                String fileName = resolved.getFileName().toString();
                DocMeta meta = DOC_META_MAP.getOrDefault(fileName, new DocMeta("project", 5, "public", List.of("admin", "superadmin", "employee")));

                List<ProjectDocChunk> fileChunks = parseMarkdown(content, fileName, meta);
                newChunks.addAll(fileChunks);
                docCount.incrementAndGet();
            } catch (IOException e) {
                log.error("读取知识库文档失败: {}", resolved, e);
            }
        }

        chunks.clear();
        chunks.addAll(newChunks);
        missingPaths.clear();
        missingPaths.addAll(newMissingPaths);
        lastBuildTimeMs = System.currentTimeMillis() - start;
        documentCount = docCount.get();

        if (!missingPaths.isEmpty()) {
            log.error("知识库配置路径不存在: {}", missingPaths);
        }
        if (chunks.isEmpty()) {
            log.warn("知识库未加载到任何 Markdown 文档，请检查 assistant.project.doc-paths 配置");
        }
        log.info("知识库重建完成: {} 个文档, {} 个chunk, 耗时 {}ms", documentCount, chunks.size(), lastBuildTimeMs);

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("success", missingPaths.isEmpty() && !chunks.isEmpty());
        status.put("documentCount", documentCount);
        status.put("chunkCount", chunks.size());
        status.put("buildTimeMs", lastBuildTimeMs);
        status.put("missingPaths", List.copyOf(missingPaths));
        return status;
    }

    public List<ProjectDocChunk> getAllChunks() {
        return Collections.unmodifiableList(chunks);
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("loaded", !chunks.isEmpty());
        status.put("success", missingPaths.isEmpty() && !chunks.isEmpty());
        status.put("documentCount", documentCount);
        status.put("chunkCount", chunks.size());
        status.put("lastBuildTimeMs", lastBuildTimeMs);
        status.put("configuredDocPaths", properties.getDocPaths());
        status.put("missingPaths", List.copyOf(missingPaths));
        return status;
    }

    private List<Path> resolveConfiguredMarkdownFiles(List<String> newMissingPaths) {
        List<String> configuredPaths = properties.getDocPaths();
        if (configuredPaths == null || configuredPaths.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Path> markdownFiles = new LinkedHashSet<>();
        for (String configuredPath : configuredPaths) {
            if (configuredPath == null || configuredPath.isBlank()) {
                continue;
            }

            Path resolved = Paths.get(configuredPath.trim()).toAbsolutePath().normalize();
            if (!Files.exists(resolved)) {
                newMissingPaths.add(resolved.toString());
                continue;
            }

            if (Files.isDirectory(resolved)) {
                try (Stream<Path> stream = Files.walk(resolved)) {
                    stream.filter(Files::isRegularFile)
                            .filter(this::isMarkdownFile)
                            .sorted()
                            .forEach(markdownFiles::add);
                } catch (IOException e) {
                    log.error("扫描知识库目录失败: {}", resolved, e);
                }
                continue;
            }

            if (isMarkdownFile(resolved)) {
                markdownFiles.add(resolved);
            }
        }
        return new ArrayList<>(markdownFiles);
    }

    private boolean isMarkdownFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".md");
    }

    private List<ProjectDocChunk> parseMarkdown(String content, String fileName, DocMeta meta) {
        List<ProjectDocChunk> result = new ArrayList<>();
        String[] lines = content.split("\n");

        List<String> currentTitlePath = new ArrayList<>();
        StringBuilder currentContent = new StringBuilder();
        String currentChunkId = fileName;

        for (String line : lines) {
            Matcher matcher = HEADING_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                // Flush previous chunk
                if (currentContent.length() > 0) {
                    addChunk(result, currentChunkId, fileName, meta, new ArrayList<>(currentTitlePath), currentContent.toString().trim());
                    currentContent.setLength(0);
                }

                int level = matcher.group(1).length();
                String title = matcher.group(2).trim();

                // Update title path
                while (currentTitlePath.size() >= level) {
                    currentTitlePath.remove(currentTitlePath.size() - 1);
                }
                currentTitlePath.add(title);

                currentChunkId = fileName + "#" + String.join("#", currentTitlePath);
            } else {
                currentContent.append(line).append("\n");
            }
        }

        // Flush last chunk
        if (currentContent.length() > 0) {
            addChunk(result, currentChunkId, fileName, meta, new ArrayList<>(currentTitlePath), currentContent.toString().trim());
        }

        return result;
    }

    private void addChunk(List<ProjectDocChunk> result, String chunkId, String fileName,
                          DocMeta meta, List<String> titlePath, String content) {
        if (content.isBlank()) {
            return;
        }

        // If content is too long, split by paragraphs
        if (content.length() > MAX_CHUNK_LENGTH) {
            String[] paragraphs = content.split("\n\n");
            StringBuilder buffer = new StringBuilder();
            int subIndex = 0;
            for (String para : paragraphs) {
                if (buffer.length() + para.length() > MAX_CHUNK_LENGTH && buffer.length() > 0) {
                    createChunk(result, chunkId + "_" + subIndex, fileName, meta, titlePath, buffer.toString().trim());
                    buffer.setLength(0);
                    subIndex++;
                }
                buffer.append(para).append("\n\n");
            }
            if (buffer.length() > 0) {
                createChunk(result, chunkId + "_" + subIndex, fileName, meta, titlePath, buffer.toString().trim());
            }
        } else {
            createChunk(result, chunkId, fileName, meta, titlePath, content);
        }
    }

    private void createChunk(List<ProjectDocChunk> result, String chunkId, String fileName,
                             DocMeta meta, List<String> titlePath, String content) {
        ProjectDocChunk chunk = new ProjectDocChunk();
        chunk.setChunkId(chunkId);
        chunk.setFileName(fileName);
        chunk.setSourceType(meta.getSourceType());
        chunk.setRoleScope(meta.getRoleScope());
        chunk.setTitlePath(new ArrayList<>(titlePath));
        chunk.setContent(content);
        chunk.setKeywords(extractKeywords(content, titlePath));
        chunk.setPriority(meta.getPriority());
        chunk.setVisibility(meta.getVisibility());
        result.add(chunk);
    }

    private List<String> extractKeywords(String content, List<String> titlePath) {
        Set<String> keywords = new LinkedHashSet<>();

        // Add title path words as keywords
        for (String title : titlePath) {
            String cleaned = title.replaceAll("[\\d.#*]", "").trim();
            if (!cleaned.isEmpty()) {
                keywords.add(cleaned);
            }
        }

        // Extract key terms from content
        String[] terms = content.replaceAll("[|\\-#*`>]", " ").split("\\s+");
        Map<String, Integer> freq = new LinkedHashMap<>();
        for (String term : terms) {
            String t = term.trim();
            if (t.length() >= 2 && t.length() <= 20) {
                freq.merge(t, 1, Integer::sum);
            }
        }

        freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> keywords.add(e.getKey()));

        return new ArrayList<>(keywords);
    }

    @Data
    static class DocMeta {
        private final String sourceType;
        private final int priority;
        private final String visibility;
        private final List<String> roleScope;
    }
}
