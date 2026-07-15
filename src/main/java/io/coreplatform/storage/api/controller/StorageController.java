package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.StorageFileResponse;
import io.coreplatform.storage.application.service.StorageService;
import io.coreplatform.storage.application.service.StorageService.FileDownloadResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/storage")
@Tag(name = "Unified File Runtime", description = "统一文件上传、下载、删除、查询")
public class StorageController {

    private final StorageService service;

    public StorageController(StorageService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public StorageFileResponse upload(@RequestParam("file") MultipartFile file) throws IOException {
        return service.upload(file);
    }

    @GetMapping("/file/{id}")
    @Operation(summary = "下载文件")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException {
        FileDownloadResult result = service.download(id);

        String encodedFilename = URLEncoder.encode(result.metadata().getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(result.metadata().getOriginalName(), StandardCharsets.UTF_8)
                .build();

        String contentType = result.metadata().getMimeType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(result.stream()));
    }

    @DeleteMapping("/file/{id}")
    @Operation(summary = "删除文件（软删除）")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/file/{id}/info")
    @Operation(summary = "获取文件元数据")
    public StorageFileResponse getInfo(@PathVariable Long id) {
        return service.getInfo(id);
    }
}