package io.coreplatform.storage.api.controller;

import io.coreplatform.storage.api.response.ImageVariantResponse;
import io.coreplatform.storage.api.response.StorageImageResponse;
import io.coreplatform.storage.application.domain.enums.Variant;
import io.coreplatform.storage.application.service.StorageImageService;
import io.coreplatform.storage.application.service.StorageService;
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
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/storage/images")
@Tag(name = "Image Runtime", description = "统一图片处理（上传、变体、转换、压缩、裁剪）")
public class StorageImageController {

    private final StorageImageService imageService;
    private final StorageService storageService;

    public StorageImageController(StorageImageService imageService,
                                   StorageService storageService) {
        this.imageService = imageService;
        this.storageService = storageService;
    }

    @PostMapping
    @Operation(summary = "上传图片（自动分析 + 生成默认 Variants：THUMBNAIL + WEBP）")
    public StorageImageResponse uploadImage(@RequestParam("file") MultipartFile file,
                                             @RequestParam(required = false) String ownerType,
                                             @RequestParam(required = false) String ownerId,
                                             @RequestParam(required = false) String tags,
                                             @RequestParam(required = false) String remark) throws IOException {
        // 使用 StorageService 的上传流程（会自动触发 P2 Resource 创建 + P4 Image Runtime）
        StorageService.FileDownloadResult result = storageService.download(
                storageService.upload(file, ownerType, ownerId, "image", "upload",
                        null, null, tags, remark).getId());
        // Actually, we need uploadResource with resourceType=IMAGE
        // The dedicated endpoint reuses existing upload flow
        var response = storageService.uploadResource(file, ownerType, ownerId, "image", "upload",
                null, null, tags, remark, "IMAGE", null, null, "PUBLIC", null, null, null);
        // After upload, the image uuid is derived from metadataUuid
        // Return the image info using the uploaded file's metadataUuid
        return imageService.getImageWithVariants(response.getId().toString());
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "获取图片信息 + 所有 Variants")
    public StorageImageResponse getImage(@PathVariable String uuid) {
        return imageService.getImageWithVariants(uuid);
    }

    @GetMapping("/{uuid}/thumbnail")
    @Operation(summary = "获取缩略图 Variant（200×200）")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String uuid) throws IOException {
        StorageService.FileDownloadResult result = imageService.getVariantFile(uuid, Variant.THUMBNAIL.name());

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(result.metadata().getOriginalName(), StandardCharsets.UTF_8)
                .build();

        String mimeType = result.metadata().getMimeType();
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = MediaType.IMAGE_JPEG_VALUE;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(mimeType))
                .body(new InputStreamResource(result.stream()));
    }

    @GetMapping("/{uuid}/variant/{name}")
    @Operation(summary = "获取指定 Variant 文件")
    public ResponseEntity<Resource> getVariant(@PathVariable String uuid,
                                                @PathVariable String name) throws IOException {
        StorageService.FileDownloadResult result = imageService.getVariantFile(uuid, name.toUpperCase());

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(result.metadata().getOriginalName(), StandardCharsets.UTF_8)
                .build();

        String mimeType = result.metadata().getMimeType();
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(mimeType))
                .body(new InputStreamResource(result.stream()));
    }

    @PostMapping("/{uuid}/convert")
    @Operation(summary = "格式转换（生成新 Variant）")
    public ImageVariantResponse convert(@PathVariable String uuid,
                                         @RequestBody ConvertRequest request) throws IOException {
        Variant variant = Variant.valueOf(request.format.toUpperCase());
        float quality = request.quality != null ? request.quality : 0.9f;
        return imageService.generateVariant(uuid, variant,
                0, 0, request.format, quality);
    }

    @PostMapping("/{uuid}/compress")
    @Operation(summary = "压缩图片（生成 WEB Variant）")
    public ImageVariantResponse compress(@PathVariable String uuid,
                                          @RequestBody CompressRequest request) throws IOException {
        float quality = request.quality / 100f;
        return imageService.generateVariant(uuid, Variant.WEB,
                0, 0, null, quality);
    }

    @PostMapping("/{uuid}/crop")
    @Operation(summary = "裁剪并缩放图片（生成自定义 Variant）")
    public ImageVariantResponse crop(@PathVariable String uuid,
                                      @RequestBody CropRequest request) throws IOException {
        String customName = "CROP_" + request.width + "x" + request.height;
        Variant variant;
        try {
            variant = Variant.valueOf(customName);
        } catch (IllegalArgumentException e) {
            // 自定义变体名称——使用 WEB 作为 fallback variant type，实际尺寸由参数控制
            variant = Variant.WEB;
        }
        Float quality = request.quality != null ? request.quality / 100f : null;
        return imageService.generateVariant(uuid, variant,
                request.width, request.height, null, quality);
    }

    // ---- request DTOs ----

    public record ConvertRequest(String format, Float quality) {
    }

    public record CompressRequest(int quality) {
    }

    public record CropRequest(int width, int height, Integer quality, String gravity) {
    }
}
