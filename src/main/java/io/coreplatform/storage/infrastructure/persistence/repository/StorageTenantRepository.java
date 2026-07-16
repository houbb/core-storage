package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.StorageTenant;
import io.coreplatform.storage.infrastructure.persistence.converter.StorageTenantConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageTenantEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class StorageTenantRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageTenantEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageTenantEntity e = new StorageTenantEntity();
        e.setTenantId(rs.getString("tenant_id"));
        e.setTenantName(rs.getString("tenant_name"));
        e.setStatus(rs.getString("status"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("update_time");
        if (ut != null) e.setUpdateTime(ut.toLocalDateTime());
        return e;
    };

    public StorageTenantRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public StorageTenant save(StorageTenant domain) {
        StorageTenantEntity entity = StorageTenantConverter.toEntity(domain);
        entity.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime() : LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        String sql = "INSERT OR REPLACE INTO storage_tenant (tenant_id, tenant_name, status, create_time, update_time) " +
                "VALUES (?,?,?,?,?)";
        jdbc.update(sql,
                entity.getTenantId(),
                entity.getTenantName(),
                entity.getStatus(),
                Timestamp.valueOf(entity.getCreateTime()),
                Timestamp.valueOf(entity.getUpdateTime()));
        return StorageTenantConverter.toDomain(entity);
    }

    public Optional<StorageTenant> findByTenantId(String tenantId) {
        try {
            StorageTenantEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_tenant WHERE tenant_id = ?", ROW_MAPPER, tenantId);
            return Optional.ofNullable(StorageTenantConverter.toDomain(e));
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<StorageTenant> listAll() {
        List<StorageTenantEntity> entities = jdbc.query(
                "SELECT * FROM storage_tenant ORDER BY create_time DESC", ROW_MAPPER);
        return entities.stream().map(StorageTenantConverter::toDomain).toList();
    }

    public int update(StorageTenant domain) {
        return jdbc.update(
                "UPDATE storage_tenant SET tenant_name = ?, status = ?, update_time = ? WHERE tenant_id = ?",
                domain.getTenantName(),
                domain.getStatus() != null ? domain.getStatus().name() : "ACTIVE",
                Timestamp.valueOf(LocalDateTime.now()),
                domain.getTenantId());
    }

    public int deleteByTenantId(String tenantId) {
        // soft delete: set status = 'DELETED'
        return jdbc.update(
                "UPDATE storage_tenant SET status = 'DELETED', update_time = ? WHERE tenant_id = ?",
                Timestamp.valueOf(LocalDateTime.now()), tenantId);
    }

    public boolean existsByTenantId(String tenantId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM storage_tenant WHERE tenant_id = ?", Integer.class, tenantId);
        return count != null && count > 0;
    }
}