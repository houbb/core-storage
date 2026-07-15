package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.StorageFile;
import io.coreplatform.storage.infrastructure.persistence.converter.StorageFileConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageFileEntity;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class StorageFileRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageFileEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageFileEntity e = new StorageFileEntity();
        e.setId(rs.getLong("id"));
        e.setUuid(rs.getString("uuid"));
        e.setOriginalName(rs.getString("original_name"));
        e.setStorageName(rs.getString("storage_name"));
        e.setExtension(rs.getString("extension"));
        e.setMimeType(rs.getString("mime_type"));
        e.setSize(rs.getLong("size"));
        e.setStorageType(rs.getString("storage_type"));
        e.setRelativePath(rs.getString("relative_path"));
        e.setHash(rs.getString("hash"));
        e.setStatus(rs.getString("status"));
        e.setDeleted(rs.getInt("deleted"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("update_time");
        if (ut != null) e.setUpdateTime(ut.toLocalDateTime());
        e.setCreateUser(rs.getString("create_user"));
        e.setUpdateUser(rs.getString("update_user"));
        return e;
    };

    public StorageFileRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 插入新记录，返回自增 ID。
     */
    public StorageFile save(StorageFile domain) {
        StorageFileEntity entity = StorageFileConverter.toEntity(domain);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        String sql = "INSERT INTO storage_file (" +
                "uuid, original_name, storage_name, extension, mime_type, " +
                "size, storage_type, relative_path, hash, status, deleted, " +
                "create_time, update_time, create_user, update_user" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, entity.getUuid());
            ps.setString(2, entity.getOriginalName());
            ps.setString(3, entity.getStorageName());
            ps.setString(4, entity.getExtension());
            ps.setString(5, entity.getMimeType());
            ps.setLong(6, entity.getSize() != null ? entity.getSize() : 0);
            ps.setString(7, entity.getStorageType());
            ps.setString(8, entity.getRelativePath());
            ps.setString(9, entity.getHash());
            ps.setString(10, entity.getStatus());
            ps.setInt(11, entity.getDeleted() != null ? entity.getDeleted() : 0);
            ps.setTimestamp(12, Timestamp.valueOf(entity.getCreateTime()));
            ps.setTimestamp(13, Timestamp.valueOf(entity.getUpdateTime()));
            ps.setString(14, entity.getCreateUser());
            ps.setString(15, entity.getUpdateUser());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            entity.setId(key.longValue());
        }
        return StorageFileConverter.toDomain(entity);
    }

    public Optional<StorageFile> findById(Long id) {
        try {
            StorageFileEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_file WHERE id = ?", ROW_MAPPER, id);
            return Optional.ofNullable(StorageFileConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * 软删除：标记 deleted=1, status='DELETED'
     */
    public int softDelete(Long id) {
        return jdbc.update(
                "UPDATE storage_file SET deleted = 1, status = 'DELETED', update_time = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now()), id);
    }

    /**
     * 更新文件状态（如上传失败标记为 FAILED）
     */
    public int updateStatus(Long id, String status) {
        return jdbc.update(
                "UPDATE storage_file SET status = ?, update_time = ? WHERE id = ?",
                status, Timestamp.valueOf(LocalDateTime.now()), id);
    }
}