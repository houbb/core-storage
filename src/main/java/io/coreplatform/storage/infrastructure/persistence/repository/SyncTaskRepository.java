package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.SyncTask;
import io.coreplatform.storage.application.domain.enums.SyncTaskStatus;
import io.coreplatform.storage.infrastructure.persistence.converter.SyncTaskConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.SyncTaskEntity;
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
public class SyncTaskRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<SyncTaskEntity> ROW_MAPPER = (rs, rowNum) -> {
        SyncTaskEntity e = new SyncTaskEntity();
        e.setId(rs.getLong("id"));
        e.setTaskType(rs.getString("task_type"));
        e.setResourceUuid(rs.getString("resource_uuid"));
        e.setSourceProfile(rs.getString("source_profile"));
        e.setTargetProfile(rs.getString("target_profile"));
        e.setStatus(rs.getString("status"));
        e.setProgress(rs.getInt("progress"));
        e.setErrorMessage(rs.getString("error_message"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("update_time");
        if (ut != null) e.setUpdateTime(ut.toLocalDateTime());
        return e;
    };

    public SyncTaskRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 创建任务记录。
     */
    public SyncTask save(SyncTask domain) {
        SyncTaskEntity entity = SyncTaskConverter.toEntity(domain);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        String sql = "INSERT INTO storage_sync_task (" +
                "task_type, resource_uuid, source_profile, target_profile, " +
                "status, progress, error_message, create_time, update_time" +
                ") VALUES (?,?,?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, entity.getTaskType());
            ps.setString(i++, entity.getResourceUuid());
            ps.setString(i++, entity.getSourceProfile());
            ps.setString(i++, entity.getTargetProfile());
            ps.setString(i++, entity.getStatus());
            ps.setInt(i++, entity.getProgress() != null ? entity.getProgress() : 0);
            ps.setString(i++, entity.getErrorMessage());
            ps.setTimestamp(i++, Timestamp.valueOf(entity.getCreateTime()));
            ps.setTimestamp(i++, Timestamp.valueOf(entity.getUpdateTime()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            entity.setId(key.longValue());
        }
        return SyncTaskConverter.toDomain(entity);
    }

    /**
     * 根据 ID 查询。
     */
    public Optional<SyncTask> findById(Long id) {
        try {
            SyncTaskEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_sync_task WHERE id = ?", ROW_MAPPER, id);
            return Optional.ofNullable(SyncTaskConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * 查询 Resource 的所有任务。
     */
    public List<SyncTask> findByResourceUuid(String resourceUuid) {
        List<SyncTaskEntity> entities = jdbc.query(
                "SELECT * FROM storage_sync_task WHERE resource_uuid = ? ORDER BY create_time DESC",
                ROW_MAPPER, resourceUuid);
        return entities.stream().map(SyncTaskConverter::toDomain).toList();
    }

    /**
     * 查找所有 PENDING 任务（Scheduler 用），限制返回数量。
     */
    public List<SyncTask> findPending(int limit) {
        List<SyncTaskEntity> entities = jdbc.query(
                "SELECT * FROM storage_sync_task WHERE status = 'PENDING' ORDER BY create_time ASC LIMIT ?",
                ROW_MAPPER, limit);
        return entities.stream().map(SyncTaskConverter::toDomain).toList();
    }

    /**
     * 查找指定 Resource 是否有未完成的任务（RUNNING 或 PENDING）。
     */
    public List<SyncTask> findActiveByResourceUuid(String resourceUuid) {
        List<SyncTaskEntity> entities = jdbc.query(
                "SELECT * FROM storage_sync_task WHERE resource_uuid = ? AND status IN ('PENDING', 'RUNNING')",
                ROW_MAPPER, resourceUuid);
        return entities.stream().map(SyncTaskConverter::toDomain).toList();
    }

    /**
     * 条件搜索任务。
     */
    public List<SyncTask> search(String resourceUuid, String taskType, String status,
                                  String sort, String order, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM storage_sync_task WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (resourceUuid != null && !resourceUuid.isBlank()) {
            sql.append(" AND resource_uuid = ?");
            params.add(resourceUuid);
        }
        if (taskType != null && !taskType.isBlank()) {
            sql.append(" AND task_type = ?");
            params.add(taskType);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        String orderBy = "create_time DESC";
        if (sort != null) {
            switch (sort) {
                case "taskType" -> orderBy = "task_type";
                case "status" -> orderBy = "status";
                default -> orderBy = "create_time";
            }
            orderBy += "DESC".equalsIgnoreCase(order) ? " DESC" : " ASC";
        }
        sql.append(" ORDER BY ").append(orderBy);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<SyncTaskEntity> entities = jdbc.query(sql.toString(), ROW_MAPPER, params.toArray());
        return entities.stream().map(SyncTaskConverter::toDomain).toList();
    }

    /**
     * 搜索总数。
     */
    public int countSearch(String resourceUuid, String taskType, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM storage_sync_task WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (resourceUuid != null && !resourceUuid.isBlank()) {
            sql.append(" AND resource_uuid = ?");
            params.add(resourceUuid);
        }
        if (taskType != null && !taskType.isBlank()) {
            sql.append(" AND task_type = ?");
            params.add(taskType);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        Integer count = jdbc.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    /**
     * 更新任务状态。
     */
    public int updateStatus(Long id, SyncTaskStatus status) {
        return jdbc.update(
                "UPDATE storage_sync_task SET status = ?, update_time = ? WHERE id = ?",
                status.name(), Timestamp.valueOf(LocalDateTime.now()), id);
    }

    /**
     * 更新任务进度。
     */
    public int updateProgress(Long id, int progress) {
        return jdbc.update(
                "UPDATE storage_sync_task SET progress = ?, update_time = ? WHERE id = ?",
                progress, Timestamp.valueOf(LocalDateTime.now()), id);
    }

    /**
     * 更新错误信息。
     */
    public int updateError(Long id, String errorMessage) {
        return jdbc.update(
                "UPDATE storage_sync_task SET status = ?, error_message = ?, update_time = ? WHERE id = ?",
                SyncTaskStatus.FAILED.name(), errorMessage, Timestamp.valueOf(LocalDateTime.now()), id);
    }
}