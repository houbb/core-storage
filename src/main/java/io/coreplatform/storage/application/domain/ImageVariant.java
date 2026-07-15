package io.coreplatform.storage.application.domain;

import io.coreplatform.storage.application.domain.enums.Variant;

import java.time.LocalDateTime;

/**
 * Image Variant 领域对象 — 对应 storage_image_variant 表。
 * 每个 Variant 对应一个独立的 StorageFile 实体（通过 metadata_uuid 关联）。
 */
public class ImageVariant {

    private Long id;
    private String imageUuid;
    private Variant variantName;
    private String metadataUuid;
    private int width;
    private int height;
    private String format;
    private long fileSize;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createUser;
    private String updateUser;

    public ImageVariant() {
    }

    // ---- getters & setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getImageUuid() { return imageUuid; }
    public void setImageUuid(String imageUuid) { this.imageUuid = imageUuid; }

    public Variant getVariantName() { return variantName; }
    public void setVariantName(Variant variantName) { this.variantName = variantName; }

    public String getMetadataUuid() { return metadataUuid; }
    public void setMetadataUuid(String metadataUuid) { this.metadataUuid = metadataUuid; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public String getCreateUser() { return createUser; }
    public void setCreateUser(String createUser) { this.createUser = createUser; }

    public String getUpdateUser() { return updateUser; }
    public void setUpdateUser(String updateUser) { this.updateUser = updateUser; }
}
