package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.LifecycleTask;
import io.coreplatform.storage.infrastructure.persistence.converter.LifecycleTaskConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.LifecycleTaskEntity;
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
public class LifecycleTaskRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<LifecycleTaskEntity> ROW_MAPPER = (rs, rowNum) -> {
        LifecycleTaskEntity e = new LifecycleTaskEntity();
        e.setId(rs.getLong("id"));
        e.setResourceUuid(rs.getString("resource_uuid"));
        Long pid = rs.getLong("policy_id");
        if (!rs.wasNull()) e.setPolicyId(pid);
        e.setAction(rs.getString("action"));
        e.setTargetStage(rs.getString("target_stage"));
        e.setStatus(rs.getString("status"));
        Timestamp et = rs.getTimestamp("execute_time");
        if (et != null) e.setExecuteTime(et.toLocalDateTime());
        Timestamp ft = rs.getTimestamp("finish_time");
        if (ft != null) e.setFinishTime(ft.toLocalDateTime());
        e.setErrorMessage(rs.getString("error_message"));
        e.setRetryCount(rs.getInt("retry_count"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("update_time");
        if (ut != null) e.setUpdateTime(ut.toLocalDateTime());
        return e;
    };

    public LifecycleTaskRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public LifecycleTask save(LifecycleTask domain) {
        LifecycleTaskEntity entity = LifecycleTaskConverter.toEntity(domain);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        String sql = "INSERT INTO storage_lifecycle_task (" +
                "resource_uuid, policy_id, action, target_stage, status, " +
                "execute_time, finish_time, error_message, retry_count, create_time, update_time" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, entity.getResourceUuid());
            if (entity.getPolicyId() != null) {
                ps.setLong(i++, entity.getPolicyId());
            } else {
                ps.setNull(i++, java.sql.Types.BIGINT);
            }
            ps.setString(i++, entity.getAction());
            ps.setString(i++, entity.getTargetStage());
            ps.setString(i++, entity.getStatus() != null ? entity.getStatus() : "PENDING");
            ps.setTimestamp(i++, entity.getExecuteTime() != null ? Timestamp.valueOf(entity.getExecuteTime()) : null);
            ps.setTimestamp(i++, entity.getFinishTime() != null ? Timestamp.valueOf(entity.getFinishTime()) : null);
            ps.setString(i++, entity.getErrorMessage());
            ps.setInt(i++, entity.getRetryCount() != null ? entity.getRetryCount() : 0);
            ps.setTimestamp(i++, Timestamp.valueOf(entity.getCreateTime()));
            ps.setTimestamp(i++, Timestamp.valueOf(entity.getUpdateTime()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            entity.setId(key.longValue());
        }
        return LifecycleTaskConverter.toDomain(entity);
    }

    public Optional<LifecycleTask> findById(Long id) {
        try {
            LifecycleTaskEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_lifecycle_task WHERE id = ?", ROW_MAPPER, id);
            return Optional.ofNullable(LifecycleTaskConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<LifecycleTask> findByResourceUuid(String resourceUuid) {
        return jdbc.query(
                "SELECT * FROM storage_lifecycle_task WHERE resource_uuid = ? ORDER BY create_time DESC",
                ROW_MAPPER, resourceUuid)
                .stream().map(LifecycleTaskConverter::toDomain).toList();
    }

    public List<LifecycleTask> findPending(int limit) {
        return jdbc.query(
                "SELECT * FROM storage_lifecycle_task WHERE status = 'PENDING' ORDER BY create_time ASC LIMIT ?",
                ROW_MAPPER, limit)
                .stream().map(LifecycleTaskConverter::toDomain).toList();
    }

    public Optional<LifecycleTask> findActiveByResourceUuid(String resourceUuid) {
        try {
            LifecycleTaskEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_lifecycle_task WHERE resource_uuid = ? AND status IN ('PENDING','RUNNING') LIMIT 1",
                    ROW_MAPPER, resourceUuid);
            return Optional.ofNullable(LifecycleTaskConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<LifecycleTask> search(String status, String resourceUuid, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM storage_lifecycle_task WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (resourceUuid != null && !resourceUuid.isBlank()) {
            sql.append(" AND resource_uuid = ?");
            params.add(resourceUuid);
        }

        sql.append(" ORDER BY create_time DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbc.query(sql.toString(), ROW_MAPPER, params.toArray())
                .stream().map(LifecycleTaskConverter::toDomain).toList();
    }

    public int countSearch(String status, String resourceUuid) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM storage_lifecycle_task WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (resourceUuid != null && !resourceUuid.isBlank()) {
            sql.append(" AND resource_uuid = ?");
            params.add(resourceUuid);
        }

        Integer count = jdbc.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    public int updateStatus(Long id, String status) {
        return jdbc.update(
                "UPDATE storage_lifecycle_task SET status = ?, update_time = ? WHERE id = ?",
                status, Timestamp.valueOf(LocalDateTime.now()), id);
    }

    public int updateError(Long id, String errorMessage) {
        return jdbc.update(
                "UPDATE storage_lifecycle_task SET status = 'FAILED', error_message = ?, " +
                        "finish_time = ?, update_time = ? WHERE id = ?",
                errorMessage, Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now()), id);
    }

    public int markCompleted(Long id) {
        return jdbc.update(
                "UPDATE storage_lifecycle_task SET status = 'COMPLETED', finish_time = ?, update_time = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now()), id);
    }
}