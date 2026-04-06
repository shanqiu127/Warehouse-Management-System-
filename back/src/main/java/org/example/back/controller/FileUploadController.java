package org.example.back.controller;

import org.example.back.common.result.Result;
import org.example.back.service.WorkRequirementAttachmentStorageService;
import org.example.back.service.WorkRequirementService;
import org.example.back.vo.UploadTokenVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    @Autowired
    private WorkRequirementAttachmentStorageService attachmentStorageService;

    @Autowired
    private WorkRequirementService workRequirementService;

    @PostMapping("/image")
    public Result<UploadTokenVO> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.success(attachmentStorageService.storeTempImage(file));
    }

    @GetMapping("/work-requirement/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadWorkRequirementAttachment(@PathVariable Long attachmentId) {
        WorkRequirementService.AttachmentDownload attachment = workRequirementService.getAccessibleAttachment(attachmentId);
        Resource resource = attachmentStorageService.loadAsResource(attachment.getStoredPath());
        MediaType mediaType = attachmentStorageService.resolveMediaType(attachment.getFileName());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename*=UTF-8''" + java.net.URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8))
                .body(resource);
    }
}
