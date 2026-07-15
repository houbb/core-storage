package io.coreplatform.storage.application.domain;

import java.time.LocalDateTime;

/**
 * Image 领域对象 — 对应 storage_image 表。
 * Image Runtime 是 Resource Runtime 的垂直扩展，不替代 Storage。
 */
public class StorageImage {

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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createUser;
    private String updateUser;

    public StorageImage() {
    }

    /** 判断是否为横向图片 */
    public boolean isLandscape() {
        return width > height;
    }

    /** 判断是否为纵向图片 */
    public boolean isPortrait() {
        return height > width;
    }

    /** 获取宽高比 */
    public double aspectRatio() {
        if (height == 0) return 0;
        return (double) width / height;
    }

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

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public String getCreateUser() { return createUser; }
    public void setCreateUser(String createUser) { this.createUser = createUser; }

    public String getUpdateUser() { return updateUser; }
    public void setUpdateUser(String updateUser) { this.updateUser = updateUser; }
}
