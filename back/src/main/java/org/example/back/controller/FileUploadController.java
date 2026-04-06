package org.example.back.controller;

import org.example.back.common.exception.BusinessException;
import org.example.back.common.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    @Value("${app.upload.base-path:./uploads}")
    private String basePath;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${app.upload.allowed-types:image/jpeg,image/png,image/gif,image/webp}")
    private String allowedTypes;

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.validateFail("请选择要上传的文件");
        }

        String contentType = file.getContentType();
        Set<String> allowed = Arrays.stream(allowedTypes.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
        if (contentType == null || !allowed.contains(contentType)) {
            throw BusinessException.validateFail("不支持的文件类型，仅允许上传图片(jpg/png/gif/webp)");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        // 白名单校验扩展名
        Set<String> allowedExts = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
        if (!allowedExts.contains(ext.toLowerCase())) {
            throw BusinessException.validateFail("不支持的文件扩展名");
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path uploadRoot = Paths.get(basePath).toAbsolutePath().normalize();
        Path dir = uploadRoot.resolve(datePath).normalize();

        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String relativePath = "/uploads/" + datePath + "/" + fileName;
            String normalizedContextPath = (contextPath == null || contextPath.isBlank()) ? "" : contextPath;
            return Result.success(normalizedContextPath + relativePath);
        } catch (IOException e) {
            throw new BusinessException(500, "文件上传失败: " + e.getMessage());
        }
    }
}
