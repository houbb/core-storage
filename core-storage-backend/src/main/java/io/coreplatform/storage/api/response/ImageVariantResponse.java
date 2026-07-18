package io.coreplatform.storage.api.response;

import java.time.LocalDateTime;

/**
 * Image Variant 响应 DTO。
 */
public class ImageVariantResponse {

    private Long id;
    private String variantName;
    private String metadataUuid;
    private int width;
    private int height;
    private String format;
    private long fileSize;
    private String downloadUrl;
    private LocalDateTime createTime;

    // ---- getters & setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVariantName() { return variantName; }
    public void setVariantName(String variantName) { this.variantName = variantName; }

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

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
