package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.StorageAudit;
import io.coreplatform.storage.infrastructure.persistence.converter.StorageAuditConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageAuditEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

@Repository
public class StorageAuditRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageAuditEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageAuditEntity e = new StorageAuditEntity();
        e.setId(rs.getLong("id"));
        e.setTenantId(rs.getString("tenant_id"));
        e.setResourceUuid(rs.getString("resource_uuid"));
        e.setOperatorId(rs.getString("operator_id"));
        e.setAction(rs.getString("action"));
        e.setTarget(rs.getString("target"));
        e.setResult(rs.getString("result"));
        e.setDetail(rs.getString("detail"));
        e.setClientIp(rs.getString("client_ip"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        return e;
    };

    public StorageAuditRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 插入审计日志（fire-and-forget，不应抛异常阻塞主流程）。 */
    public void save(StorageAudit domain) {
        StorageAuditEntity entity = StorageAuditConverter.toEntity(domain);
        entity.setCreateTime(LocalDateTime.now());

        String sql = "INSERT INTO storage_audit (tenant_id, resource_uuid, operator_id, action, target, result, detail, client_ip, create_time) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, entity.getTenantId());
            ps.setString(2, entity.getResourceUuid());
            ps.setString(3, entity.getOperatorId());
            ps.setString(4, entity.getAction());
            ps.setString(5, entity.getTarget());
            ps.setString(6, entity.getResult() != null ? entity.getResult() : "SUCCESS");
            ps.setString(7, entity.getDetail());
            ps.setString(8, entity.getClientIp());
            ps.setTimestamp(9, Timestamp.valueOf(entity.getCreateTime()));
            return ps;
        }, keyHolder);
    }

    public Optional<StorageAudit> findById(Long id) {
        try {
            StorageAuditEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_audit WHERE id = ?", ROW_MAPPER, id);
            return Optional.ofNullable(StorageAuditConverter.toDomain(e));
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /** 多条件搜索审计日志（分页）。 */
    public List<StorageAudit> search(String tenantId, String resourceUuid, String action,
                                      String operatorId, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM storage_audit WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (tenantId != null && !tenantId.isBlank()) {
            sql.append(" AND tenant_id = ?");
            params.add(tenantId);
        }
        if (resourceUuid != null && !resourceUuid.isBlank()) {
            sql.append(" AND resource_uuid = ?");
            params.add(resourceUuid);
        }
        if (action != null && !action.isBlank()) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        if (operatorId != null && !operatorId.isBlank()) {
            sql.append(" AND operator_id = ?");
            params.add(operatorId);
        }

        sql.append(" ORDER BY create_time DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<StorageAuditEntity> entities = jdbc.query(sql.toString(), ROW_MAPPER, params.toArray());
        return entities.stream().map(StorageAuditConverter::toDomain).toList();
    }

    /** 搜索总数。 */
    public int countSearch(String tenantId, String resourceUuid, String action, String operatorId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM storage_audit WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (tenantId != null && !tenantId.isBlank()) {
            sql.append(" AND tenant_id = ?");
            params.add(tenantId);
        }
        if (resourceUuid != null && !resourceUuid.isBlank()) {
            sql.append(" AND resource_uuid = ?");
            params.add(resourceUuid);
        }
        if (action != null && !action.isBlank()) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        if (operatorId != null && !operatorId.isBlank()) {
            sql.append(" AND operator_id = ?");
            params.add(operatorId);
        }

        Integer count = jdbc.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }
}