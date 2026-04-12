package org.example.back.service;

import org.example.back.config.ProjectAssistantProperties;
import org.example.back.entity.ProjectDocChunk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectKnowledgeBaseServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void rebuild_shouldLoadMarkdownFilesFromConfiguredDocPaths() throws IOException {
        Path readme = tempDir.resolve("README.md");
        Path docsDir = Files.createDirectories(tempDir.resolve("projectmd"));
        Path nestedDoc = docsDir.resolve("project.md");

        Files.writeString(readme, "# README\n\n项目说明");
        Files.writeString(nestedDoc, "# 项目结构\n\n项目模块说明");

        ProjectAssistantProperties properties = new ProjectAssistantProperties();
        properties.setDocPaths(List.of(readme.toString(), docsDir.toString()));

        ProjectKnowledgeBaseService service = new ProjectKnowledgeBaseService();
        ReflectionTestUtils.setField(service, "properties", properties);

        Map<String, Object> status = service.rebuild();
        List<ProjectDocChunk> chunks = service.getAllChunks();

        assertEquals(Boolean.TRUE, status.get("success"));
        assertEquals(2, status.get("documentCount"));
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().anyMatch(chunk -> "README.md".equals(chunk.getFileName())));
        assertTrue(chunks.stream().anyMatch(chunk -> "project.md".equals(chunk.getFileName())));
    }
}