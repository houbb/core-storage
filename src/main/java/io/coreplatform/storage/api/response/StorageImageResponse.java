package io.coreplatform.storage.api.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Image 响应 DTO — 聚合图片信息和所有 Variants。
 */
public class StorageImageResponse {

    private Long id;
    private String imageUuid;
    private String metadataUuid;
    private int width;
    private int height;
    private String format;
    private String colorSpace;
    private boolean hasAlpha;
    private int orientation;
    private int dpi;
    private List<ImageVariantResponse> variants;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ---- getters & setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getImageUuid() { return imageUuid; }
    public void setImageUuid(String imageUuid) { this.imageUuid = imageUuid; }

    public String getMetadataUuid() { return metadataUuid; }
    public void setMetadataUuid(String metadataUuid) { this.metadataUuid = metadataUuid; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getColorSpace() { return colorSpace; }
    public void setColorSpace(String colorSpace) { this.colorSpace = colorSpace; }

    public boolean isHasAlpha() { return hasAlpha; }
    public void setHasAlpha(boolean hasAlpha) { this.hasAlpha = hasAlpha; }

    public int getOrientation() { return orientation; }
    public void setOrientation(int orientation) { this.orientation = orientation; }

    public int getDpi() { return dpi; }
    public void setDpi(int dpi) { this.dpi = dpi; }

    public List<ImageVariantResponse> getVariants() { return variants; }
    public void setVariants(List<ImageVariantResponse> variants) { this.variants = variants; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
