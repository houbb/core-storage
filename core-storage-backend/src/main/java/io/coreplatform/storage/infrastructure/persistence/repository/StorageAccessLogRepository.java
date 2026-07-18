package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.infrastructure.persistence.entity.StorageAccessLogEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 访问日志仓储 — 只写入，不提供查询（审计数据由外部工具消费）。
 */
@Repository
public class StorageAccessLogRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageAccessLogEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageAccessLogEntity e = new StorageAccessLogEntity();
        e.setId(rs.getLong("id"));
        e.setResourceUuid(rs.getString("resource_uuid"));
        e.setAccessType(rs.getString("access_type"));
        e.setAccessDetail(rs.getString("access_detail"));
        e.setOperatorId(rs.getString("operator_id"));
        e.setOperatorRoles(rs.getString("operator_roles"));
        e.setClientIp(rs.getString("client_ip"));
        e.setUserAgent(rs.getString("user_agent"));
        e.setResult(rs.getString("result"));
        e.setReason(rs.getString("reason"));
        e.setDurationMs(rs.getInt("duration_ms"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("update_time");
        if (ut != null) e.setUpdateTime(ut.toLocalDateTime());
        e.setCreateUser(rs.getString("create_user"));
        e.setUpdateUser(rs.getString("update_user"));
        return e;
    };

    public StorageAccessLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 插入一条访问日志（异步调用，不应抛异常阻塞主流程）。 */
    public void save(StorageAccessLogEntity entity) {
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        String sql = "INSERT INTO storage_access_log (" +
                "resource_uuid, access_type, access_detail, operator_id, operator_roles, " +
                "client_ip, user_agent, result, reason, duration_ms, " +
                "create_time, update_time, create_user, update_user" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, entity.getResourceUuid());
            ps.setString(2, entity.getAccessType());
            ps.setString(3, entity.getAccessDetail());
            ps.setString(4, entity.getOperatorId());
            ps.setString(5, entity.getOperatorRoles());
            ps.setString(6, entity.getClientIp());
            ps.setString(7, entity.getUserAgent());
            ps.setString(8, entity.getResult());
            ps.setString(9, entity.getReason());
            ps.setInt(10, entity.getDurationMs() != null ? entity.getDurationMs() : 0);
            ps.setTimestamp(11, Timestamp.valueOf(entity.getCreateTime()));
            ps.setTimestamp(12, Timestamp.valueOf(entity.getUpdateTime()));
            ps.setString(13, entity.getCreateUser());
            ps.setString(14, entity.getUpdateUser());
            return ps;
        }, keyHolder);
    }
}