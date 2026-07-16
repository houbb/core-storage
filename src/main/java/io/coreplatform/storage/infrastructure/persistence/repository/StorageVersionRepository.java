package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.StorageVersion;
import io.coreplatform.storage.infrastructure.persistence.converter.StorageVersionConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageVersionEntity;
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
import java.util.List;
import java.util.Optional;

@Repository
public class StorageVersionRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageVersionEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageVersionEntity e = new StorageVersionEntity();
        e.setId(rs.getLong("id"));
        e.setVersionUuid(rs.getString("version_uuid"));
        e.setResourceUuid(rs.getString("resource_uuid"));
        e.setMetadataUuid(rs.getString("metadata_uuid"));
        e.setVersionName(rs.getString("version_name"));
        e.setVersionCode(rs.getInt("version_code"));
        e.setStatus(rs.getString("status"));
        e.setPublished(rs.getInt("published"));
        e.setLatest(rs.getInt("latest"));
        e.setChecksum(rs.getString("checksum"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        Timestamp pt = rs.getTimestamp("publish_time");
        if (pt != null) e.setPublishTime(pt.toLocalDateTime());
        return e;
    };

    public StorageVersionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 创建版本记录 */
    public StorageVersion save(StorageVersion domain) {
        StorageVersionEntity entity = StorageVersionConverter.toEntity(domain);
        entity.setCreateTime(LocalDateTime.now());

        String sql = "INSERT INTO storage_version (" +
                "version_uuid, resource_uuid, metadata_uuid, version_name, version_code, " +
                "status, published, latest, checksum, create_time, publish_time" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, entity.getVersionUuid());
            ps.setString(i++, entity.getResourceUuid());
            ps.setString(i++, entity.getMetadataUuid());
            ps.setString(i++, entity.getVersionName());
            ps.setInt(i++, entity.getVersionCode() != null ? entity.getVersionCode() : 1);
            ps.setString(i++, entity.getStatus());
            ps.setInt(i++, entity.getPublished() != null ? entity.getPublished() : 0);
            ps.setInt(i++, entity.getLatest() != null ? entity.getLatest() : 0);
            ps.setString(i++, entity.getChecksum());
            ps.setTimestamp(i++, entity.getCreateTime() != null ? Timestamp.valueOf(entity.getCreateTime()) : null);
            ps.setTimestamp(i++, entity.getPublishTime() != null ? Timestamp.valueOf(entity.getPublishTime()) : null);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            entity.setId(key.longValue());
        }
        return StorageVersionConverter.toDomain(entity);
    }

    /** 根据 version_uuid 查询 */
    public Optional<StorageVersion> findByVersionUuid(String versionUuid) {
        try {
            StorageVersionEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_version WHERE version_uuid = ?", ROW_MAPPER, versionUuid);
            return Optional.ofNullable(StorageVersionConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /** 查询 Resource 下所有版本（按 version_code 降序） */
    public List<StorageVersion> findByResourceUuid(String resourceUuid) {
        List<StorageVersionEntity> entities = jdbc.query(
                "SELECT * FROM storage_version WHERE resource_uuid = ? ORDER BY version_code DESC",
                ROW_MAPPER, resourceUuid);
        return entities.stream().map(StorageVersionConverter::toDomain).toList();
    }

    /** 查询 Resource 的 Latest 版本 */
    public Optional<StorageVersion> findLatestByResourceUuid(String resourceUuid) {
        try {
            StorageVersionEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_version WHERE resource_uuid = ? AND latest = 1",
                    ROW_MAPPER, resourceUuid);
            return Optional.ofNullable(StorageVersionConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /** 根据 metadata_uuid 反向查找版本 */
    public Optional<StorageVersion> findByMetadataUuid(String metadataUuid) {
        try {
            StorageVersionEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_version WHERE metadata_uuid = ?", ROW_MAPPER, metadataUuid);
            return Optional.ofNullable(StorageVersionConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /** 根据 resourceUuid + versionCode 查找 */
    public Optional<StorageVersion> findByResourceUuidAndVersionCode(String resourceUuid, int versionCode) {
        try {
            StorageVersionEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_version WHERE resource_uuid = ? AND version_code = ?",
                    ROW_MAPPER, resourceUuid, versionCode);
            return Optional.ofNullable(StorageVersionConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /** 设置 latest 标记 */
    public int setLatest(String versionUuid, boolean latest) {
        return jdbc.update(
                "UPDATE storage_version SET latest = ? WHERE version_uuid = ?",
                latest ? 1 : 0, versionUuid);
    }

    /** 清除 Resource 所有版本的 latest 标记 */
    public int clearLatestForResource(String resourceUuid) {
        return jdbc.update(
                "UPDATE storage_version SET latest = 0 WHERE resource_uuid = ?",
                resourceUuid);
    }

    /** 更新版本状态 */
    public int updateStatus(String versionUuid, String status) {
        return jdbc.update(
                "UPDATE storage_version SET status = ? WHERE version_uuid = ?",
                status, versionUuid);
    }

    /** 更新发布时间 */
    public int updatePublishTime(String versionUuid) {
        return jdbc.update(
                "UPDATE storage_version SET publish_time = ? WHERE version_uuid = ?",
                Timestamp.valueOf(LocalDateTime.now()), versionUuid);
    }

    /** 统计 Resource 的版本数 */
    public int countByResourceUuid(String resourceUuid) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM storage_version WHERE resource_uuid = ?",
                Integer.class, resourceUuid);
        return count != null ? count : 0;
    }

    /** 删除版本记录 */
    public int deleteByVersionUuid(String versionUuid) {
        return jdbc.update("DELETE FROM storage_version WHERE version_uuid = ?", versionUuid);
    }
}
