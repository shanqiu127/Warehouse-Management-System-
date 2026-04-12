package org.example.back.entity;

import lombok.Data;

import java.util.List;

@Data
public class ProjectDocChunk {

    private String chunkId;
    private String fileName;
    private String sourceType;
    private List<String> roleScope;
    private List<String> titlePath;
    private String content;
    private List<String> keywords;
    private int priority;
    private String visibility;
}
