package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.StorageQuota;
import io.coreplatform.storage.infrastructure.persistence.converter.StorageQuotaConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageQuotaEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class StorageQuotaRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageQuotaEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageQuotaEntity e = new StorageQuotaEntity();
        e.setId(rs.getLong("id"));
        e.setTenantId(rs.getString("tenant_id"));
        e.setResourceType(rs.getString("resource_type"));
        e.setLimitSize(rs.getLong("limit_size"));
        e.setUsedSize(rs.getLong("used_size"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("update_time");
        if (ut != null) e.setUpdateTime(ut.toLocalDateTime());
        return e;
    };

    public StorageQuotaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public StorageQuota save(StorageQuota domain) {
        StorageQuotaEntity entity = StorageQuotaConverter.toEntity(domain);
        if (entity.getCreateTime() == null) entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        String sql = "INSERT OR REPLACE INTO storage_quota (tenant_id, resource_type, limit_size, used_size, create_time, update_time) " +
                "VALUES (?,?,?,?,?,?)";
        jdbc.update(sql,
                entity.getTenantId(),
                entity.getResourceType(),
                entity.getLimitSize(),
                entity.getUsedSize(),
                Timestamp.valueOf(entity.getCreateTime()),
                Timestamp.valueOf(entity.getUpdateTime()));

        // re-fetch to get the id
        return findByTenantIdAndType(entity.getTenantId(), entity.getResourceType()).orElse(domain);
    }

    public Optional<StorageQuota> findByTenantIdAndType(String tenantId, String resourceType) {
        try {
            StorageQuotaEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_quota WHERE tenant_id = ? AND resource_type = ?",
                    ROW_MAPPER, tenantId, resourceType);
            return Optional.ofNullable(StorageQuotaConverter.toDomain(e));
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<StorageQuota> findByTenantId(String tenantId) {
        List<StorageQuotaEntity> entities = jdbc.query(
                "SELECT * FROM storage_quota WHERE tenant_id = ?", ROW_MAPPER, tenantId);
        return entities.stream().map(StorageQuotaConverter::toDomain).toList();
    }

    public List<StorageQuota> listAll() {
        List<StorageQuotaEntity> entities = jdbc.query(
                "SELECT * FROM storage_quota ORDER BY tenant_id, resource_type", ROW_MAPPER);
        return entities.stream().map(StorageQuotaConverter::toDomain).toList();
    }

    /** 原子增加已用配额（delta 可为正或负）。 */
    public void incrementUsed(String tenantId, String resourceType, long delta) {
        jdbc.update(
                "UPDATE storage_quota SET used_size = used_size + ?, update_time = ? WHERE tenant_id = ? AND resource_type = ?",
                delta, Timestamp.valueOf(LocalDateTime.now()), tenantId, resourceType);
    }

    public int updateLimit(String tenantId, String resourceType, long limitSize) {
        return jdbc.update(
                "UPDATE storage_quota SET limit_size = ?, update_time = ? WHERE tenant_id = ? AND resource_type = ?",
                limitSize, Timestamp.valueOf(LocalDateTime.now()), tenantId, resourceType);
    }

    public int deleteByTenantIdAndType(String tenantId, String resourceType) {
        return jdbc.update("DELETE FROM storage_quota WHERE tenant_id = ? AND resource_type = ?",
                tenantId, resourceType);
    }
}