package io.coreplatform.storage.api.response;

/**
 * API 返回 DTO — 业务方只保存 id，通过 downloadUrl 访问文件。
 */
public class StorageFileResponse {

    private Long id;
    private String downloadUrl;
    private String filename;
    private long size;

    public StorageFileResponse() {
    }

    public StorageFileResponse(Long id, String downloadUrl, String filename, long size) {
        this.id = id;
        this.downloadUrl = downloadUrl;
        this.filename = filename;
        this.size = size;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
}