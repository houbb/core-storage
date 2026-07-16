package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.StorageMetadata;
import io.coreplatform.storage.infrastructure.persistence.converter.StorageMetadataConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageMetadataEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class StorageMetadataRepository {

    private static final Logger log = LoggerFactory.getLogger(StorageMetadataRepository.class);

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageMetadataEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageMetadataEntity e = new StorageMetadataEntity();
        e.setId(rs.getLong("id"));
        e.setUuid(rs.getString("uuid"));
        e.setResourceName(rs.getString("resource_name"));
        e.setOriginalName(rs.getString("original_name"));
        e.setExtension(rs.getString("extension"));
        e.setMimeType(rs.getString("mime_type"));
        e.setFileSize(rs.getLong("file_size"));
        e.setHashSha256(rs.getString("hash_sha256"));
        e.setStorageDriver(rs.getString("storage_driver"));
        e.setStorageKey(rs.getString("storage_key"));
        e.setRelativePath(rs.getString("relative_path"));
        e.setStorageName(rs.getString("storage_name"));
        e.setStorageType(rs.getString("storage_type"));
        e.setOwnerType(rs.getString("owner_type"));
        e.setOwnerId(rs.getString("owner_id"));
        e.setSystemName(rs.getString("system_name"));
        e.setModuleName(rs.getString("module_name"));
        e.setTags(rs.getString("tags"));
        e.setRemark(rs.getString("remark"));
        e.setStatus(rs.getString("status"));
        e.setDeleted(rs.getInt("deleted"));
        e.setTenantId(rs.getString("tenant_id"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("update_time");
        if (ut != null) e.setUpdateTime(ut.toLocalDateTime());
        e.setCreateUser(rs.getString("create_user"));
        e.setUpdateUser(rs.getString("update_user"));
        return e;
    };

    public StorageMetadataRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 插入新元数据，返回自增 ID。
     */
    public StorageMetadata save(StorageMetadata domain) {
        StorageMetadataEntity entity = StorageMetadataConverter.toEntity(domain);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        String sql = "INSERT INTO storage_metadata (" +
                "uuid, resource_name, original_name, extension, mime_type, " +
                "file_size, hash_sha256, storage_driver, storage_key, " +
                "relative_path, storage_name, storage_type, " +
                "owner_type, owner_id, system_name, module_name, tags, remark, " +
                "status, deleted, tenant_id, create_time, update_time, create_user, update_user" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, entity.getUuid());
            ps.setString(i++, entity.getResourceName());
            ps.setString(i++, entity.getOriginalName());
            ps.setString(i++, entity.getExtension());
            ps.setString(i++, entity.getMimeType());
            ps.setLong(i++, entity.getFileSize() != null ? entity.getFileSize() : 0);
            ps.setString(i++, entity.getHashSha256());
            ps.setString(i++, entity.getStorageDriver());
            ps.setString(i++, entity.getStorageKey());
            ps.setString(i++, entity.getRelativePath());
            ps.setString(i++, entity.getStorageName());
            ps.setString(i++, entity.getStorageType());
            ps.setString(i++, entity.getOwnerType());
            ps.setString(i++, entity.getOwnerId());
            ps.setString(i++, entity.getSystemName());
            ps.setString(i++, entity.getModuleName());
            ps.setString(i++, entity.getTags());
            ps.setString(i++, entity.getRemark());
            ps.setString(i++, entity.getStatus());
            ps.setInt(i++, entity.getDeleted() != null ? entity.getDeleted() : 0);
            ps.setString(i++, entity.getTenantId() != null ? entity.getTenantId() : "default");
            ps.setTimestamp(i++, Timestamp.valueOf(entity.getCreateTime()));
            ps.setTimestamp(i++, Timestamp.valueOf(entity.getUpdateTime()));
            ps.setString(i++, entity.getCreateUser());
            ps.setString(i++, entity.getUpdateUser());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            entity.setId(key.longValue());
        }
        return StorageMetadataConverter.toDomain(entity);
    }

    /**
     * 根据 UUID 查询（不含已删除）。
     */
    public Optional<StorageMetadata> findByUuid(String uuid) {
        try {
            StorageMetadataEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_metadata WHERE uuid = ? AND deleted = 0", ROW_MAPPER, uuid);
            return Optional.ofNullable(StorageMetadataConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * 条件搜索 + 分页 + 排序。
     * 返回结果中 referenceCount 需要额外查询填充（由 Service 层完成）。
     */
    public List<StorageMetadata> search(String keyword, String mimeType, String status, String hash,
                                         String ownerType, String ownerId, String system, String module,
                                         String tag, LocalDateTime startTime, LocalDateTime endTime,
                                         String sort, String order, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM storage_metadata WHERE deleted = 0");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (original_name LIKE ? OR uuid LIKE ? OR resource_name LIKE ?)");
            String kw = "%" + keyword + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (mimeType != null && !mimeType.isBlank()) {
            sql.append(" AND mime_type LIKE ?");
            params.add("%" + mimeType + "%");
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (hash != null && !hash.isBlank()) {
            sql.append(" AND hash_sha256 = ?");
            params.add(hash);
        }
        if (ownerType != null && !ownerType.isBlank()) {
            sql.append(" AND owner_type = ?");
            params.add(ownerType);
        }
        if (ownerId != null && !ownerId.isBlank()) {
            sql.append(" AND owner_id = ?");
            params.add(ownerId);
        }
        if (system != null && !system.isBlank()) {
            sql.append(" AND system_name = ?");
            params.add(system);
        }
        if (module != null && !module.isBlank()) {
            sql.append(" AND module_name = ?");
            params.add(module);
        }
        if (tag != null && !tag.isBlank()) {
            sql.append(" AND tags LIKE ?");
            params.add("%" + tag + "%");
        }
        if (startTime != null) {
            sql.append(" AND create_time >= ?");
            params.add(Timestamp.valueOf(startTime));
        }
        if (endTime != null) {
            sql.append(" AND create_time <= ?");
            params.add(Timestamp.valueOf(endTime));
        }

        // 排序（白名单防注入）
        String orderBy = "create_time DESC";
        if (sort != null) {
            switch (sort) {
                case "size" -> orderBy = "file_size";
                case "name" -> orderBy = "original_name";
                case "uuid" -> orderBy = "uuid";
                case "mime" -> orderBy = "mime_type";
                default -> orderBy = "create_time";
            }
            orderBy += "DESC".equalsIgnoreCase(order) ? " DESC" : " ASC";
        } else {
            orderBy = "create_time DESC";
        }
        sql.append(" ORDER BY ").append(orderBy);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<StorageMetadataEntity> entities = jdbc.query(sql.toString(), ROW_MAPPER, params.toArray());
        return entities.stream().map(StorageMetadataConverter::toDomain).toList();
    }

    /**
     * 搜索总数（用于分页）。
     */
    public int countSearch(String keyword, String mimeType, String status, String hash,
                            String ownerType, String ownerId, String system, String module,
                            String tag, LocalDateTime startTime, LocalDateTime endTime) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM storage_metadata WHERE deleted = 0");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (original_name LIKE ? OR uuid LIKE ? OR resource_name LIKE ?)");
            String kw = "%" + keyword + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (mimeType != null && !mimeType.isBlank()) {
            sql.append(" AND mime_type LIKE ?");
            params.add("%" + mimeType + "%");
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (hash != null && !hash.isBlank()) {
            sql.append(" AND hash_sha256 = ?");
            params.add(hash);
        }
        if (ownerType != null && !ownerType.isBlank()) {
            sql.append(" AND owner_type = ?");
            params.add(ownerType);
        }
        if (ownerId != null && !ownerId.isBlank()) {
            sql.append(" AND owner_id = ?");
            params.add(ownerId);
        }
        if (system != null && !system.isBlank()) {
            sql.append(" AND system_name = ?");
            params.add(system);
        }
        if (module != null && !module.isBlank()) {
            sql.append(" AND module_name = ?");
            params.add(module);
        }
        if (tag != null && !tag.isBlank()) {
            sql.append(" AND tags LIKE ?");
            params.add("%" + tag + "%");
        }
        if (startTime != null) {
            sql.append(" AND create_time >= ?");
            params.add(Timestamp.valueOf(startTime));
        }
        if (endTime != null) {
            sql.append(" AND create_time <= ?");
            params.add(Timestamp.valueOf(endTime));
        }

        Integer count = jdbc.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    /**
     * 软删除。
     */
    public int softDelete(String uuid) {
        return jdbc.update(
                "UPDATE storage_metadata SET deleted = 1, status = 'SOFT_DELETED', update_time = ? WHERE uuid = ?",
                Timestamp.valueOf(LocalDateTime.now()), uuid);
    }

    /**
     * 更新状态。
     */
    public int updateStatus(String uuid, String status) {
        return jdbc.update(
                "UPDATE storage_metadata SET status = ?, update_time = ? WHERE uuid = ?",
                status, Timestamp.valueOf(LocalDateTime.now()), uuid);
    }
}