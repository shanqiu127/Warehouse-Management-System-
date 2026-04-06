package org.example.back.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadTokenVO {
    private String token;
    private String fileName;
    private Long fileSize;
}