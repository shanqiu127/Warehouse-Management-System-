package org.example.back.service;

import cn.dev33.satoken.stp.StpUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.common.exception.BusinessException;
import org.example.back.vo.UploadTokenVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WorkRequirementAttachmentStorageService {

    private static final String SESSION_KEY_TEMP_UPLOADS = "workRequirementTempUploads";

    @Value("${app.upload.base-path:./uploads}")
    private String basePath;

    @Value("${app.upload.allowed-types:image/jpeg,image/png,image/gif,image/webp}")
    private String allowedTypes;

    public UploadTokenVO storeTempImage(MultipartFile file) {
        validateImage(file);

        String originalFilename = sanitizeFileName(file.getOriginalFilename());
        String ext = extractExtension(originalFilename);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String storedFileName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path uploadRoot = Paths.get(basePath).toAbsolutePath().normalize();
        Path dir = uploadRoot.resolve(datePath).normalize();

        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(storedFileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            String storedPath = "/uploads/" + datePath + "/" + storedFileName;
            String token = UUID.randomUUID().toString().replace("-", "");

            Map<String, TempUploadMeta> uploads = getTempUploads();
            uploads.put(token, new TempUploadMeta(token, originalFilename, storedPath, file.getSize()));
            StpUtil.getSession().set(SESSION_KEY_TEMP_UPLOADS, uploads);
            return new UploadTokenVO(token, originalFilename, file.getSize());
        } catch (IOException e) {
            throw new BusinessException(500, "文件上传失败: " + e.getMessage());
        }
    }

    public TempUploadMeta consumeTempUpload(String token) {
        if (!StringUtils.hasText(token)) {
            throw BusinessException.validateFail("附件令牌不能为空");
        }
        Map<String, TempUploadMeta> uploads = getTempUploads();
        TempUploadMeta meta = uploads.remove(token.trim());
        StpUtil.getSession().set(SESSION_KEY_TEMP_UPLOADS, uploads);
        if (meta == null) {
            throw BusinessException.forbidden("附件令牌无效或已过期");
        }
        return meta;
    }

    public Resource loadAsResource(String storedPath) {
        Path filePath = resolveStoredPath(storedPath);
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw BusinessException.notFound("附件文件不存在");
        }
        return new FileSystemResource(filePath);
    }

    public void deleteStoredFileQuietly(String storedPath) {
        if (!StringUtils.hasText(storedPath)) {
            return;
        }
        try {
            Path filePath = resolveStoredPath(storedPath);
            Files.deleteIfExists(filePath);
        } catch (BusinessException ex) {
            log.warn("跳过非法附件路径删除: {}", ex.getMsg());
        } catch (IOException ex) {
            log.warn("删除附件文件失败: path={}, reason={}", storedPath, ex.getMessage());
        }
    }

    public MediaType resolveMediaType(String fileName) {
        String ext = extractExtension(fileName).toLowerCase();
        return switch (ext) {
            case ".jpg", ".jpeg" -> MediaType.IMAGE_JPEG;
            case ".png" -> MediaType.IMAGE_PNG;
            case ".gif" -> MediaType.IMAGE_GIF;
            case ".webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private void validateImage(MultipartFile file) {
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
        String ext = extractExtension(file.getOriginalFilename()).toLowerCase();
        Set<String> allowedExts = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
        if (!allowedExts.contains(ext)) {
            throw BusinessException.validateFail("不支持的文件扩展名");
        }
    }

    private Path resolveStoredPath(String storedPath) {
        if (!StringUtils.hasText(storedPath) || !storedPath.startsWith("/uploads/")) {
            throw BusinessException.validateFail("非法的附件路径");
        }
        Path uploadRoot = Paths.get(basePath).toAbsolutePath().normalize();
        String relativePath = storedPath.substring("/uploads/".length()).replace('/', java.io.File.separatorChar);
        Path resolved = uploadRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(uploadRoot)) {
            throw BusinessException.forbidden("非法的附件访问请求");
        }
        return resolved;
    }

    private Map<String, TempUploadMeta> getTempUploads() {
        Object sessionValue = StpUtil.getSession().get(SESSION_KEY_TEMP_UPLOADS);
        if (sessionValue instanceof Map<?, ?> sessionMap) {
            Map<String, TempUploadMeta> uploads = new LinkedHashMap<>();
            sessionMap.forEach((key, value) -> {
                if (key instanceof String strKey && value instanceof TempUploadMeta meta) {
                    uploads.put(strKey, meta);
                }
            });
            return uploads;
        }
        return new LinkedHashMap<>();
    }

    private String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "image" + UUID.randomUUID();
        }
        return Paths.get(fileName).getFileName().toString();
    }

    private String extractExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TempUploadMeta implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String token;
        private String fileName;
        private String storedPath;
        private Long fileSize;
    }
}