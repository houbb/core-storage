package io.coreplatform.storage.infrastructure.persistence.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 数据库驱动 BLOB 存储 — 对应 storage_driver_blob 表。
 * 仅供 DatabaseDriver 内部使用，不对外暴露。
 */
@Repository
public class StorageDriverBlobRepository {

    private final JdbcTemplate jdbc;

    public StorageDriverBlobRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 插入或替换 BLOB 内容。
     */
    public void insert(String storageKey, byte[] content, String contentType) {
        String blobId = UUID.randomUUID().toString().replace("-", "");
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbc.update(
                "INSERT OR REPLACE INTO storage_driver_blob (blob_id, storage_key, content, size, content_type, create_time, update_time) VALUES (?,?,?,?,?,?,?)",
                blobId, storageKey, content, content.length, contentType, now, now
        );
    }

    /**
     * 根据 storage_key 查询 BLOB 行。
     */
    public BlobRow findByStorageKey(String storageKey) {
        try {
            return jdbc.queryForObject(
                    "SELECT blob_id, storage_key, content, size, content_type, create_time, update_time FROM storage_driver_blob WHERE storage_key = ?",
                    (rs, rowNum) -> {
                        BlobRow row = new BlobRow();
                        row.blobId = rs.getString("blob_id");
                        row.storageKey = rs.getString("storage_key");
                        row.content = rs.getBytes("content");
                        row.size = rs.getLong("size");
                        row.contentType = rs.getString("content_type");
                        Timestamp ct = rs.getTimestamp("create_time");
                        if (ct != null) row.createTime = ct.toLocalDateTime();
                        Timestamp ut = rs.getTimestamp("update_time");
                        if (ut != null) row.updateTime = ut.toLocalDateTime();
                        return row;
                    }, storageKey
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    /**
     * 删除 BLOB 行。
     */
    public boolean deleteByStorageKey(String storageKey) {
        int rows = jdbc.update("DELETE FROM storage_driver_blob WHERE storage_key = ?", storageKey);
        return rows > 0;
    }

    /**
     * 更新 BLOB 的 storage_key（用于 move 操作）。
     */
    public boolean updateStorageKey(String oldKey, String newKey, byte[] content, String contentType) {
        int rows = jdbc.update(
                "UPDATE storage_driver_blob SET storage_key = ?, content = ?, size = ?, content_type = ?, update_time = ? WHERE storage_key = ?",
                newKey, content, content != null ? content.length : 0, contentType,
                Timestamp.valueOf(LocalDateTime.now()), oldKey
        );
        return rows > 0;
    }

    /**
     * 检查 key 是否存在。
     */
    public boolean exists(String storageKey) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM storage_driver_blob WHERE storage_key = ?", Integer.class, storageKey);
        return count != null && count > 0;
    }

    /**
     * 获取指定前缀的所有 BLOB（如需批量操作）。
     */
    public List<BlobRow> findByStorageKeyPrefix(String keyPrefix) {
        return jdbc.query(
                "SELECT blob_id, storage_key, content, size, content_type, create_time, update_time FROM storage_driver_blob WHERE storage_key LIKE ?",
                (rs, rowNum) -> {
                    BlobRow row = new BlobRow();
                    row.blobId = rs.getString("blob_id");
                    row.storageKey = rs.getString("storage_key");
                    row.content = rs.getBytes("content");
                    row.size = rs.getLong("size");
                    row.contentType = rs.getString("content_type");
                    Timestamp ct = rs.getTimestamp("create_time");
                    if (ct != null) row.createTime = ct.toLocalDateTime();
                    Timestamp ut = rs.getTimestamp("update_time");
                    if (ut != null) row.updateTime = ut.toLocalDateTime();
                    return row;
                },
                keyPrefix + "%"
        );
    }

    // --- inner types ---

    public static class BlobRow {
        public String blobId;
        public String storageKey;
        public byte[] content;
        public long size;
        public String contentType;
        public LocalDateTime createTime;
        public LocalDateTime updateTime;
    }
}
