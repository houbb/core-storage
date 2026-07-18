package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.ResourceHold;
import io.coreplatform.storage.infrastructure.persistence.converter.ResourceHoldConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.ResourceHoldEntity;
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
public class ResourceHoldRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<ResourceHoldEntity> ROW_MAPPER = (rs, rowNum) -> {
        ResourceHoldEntity e = new ResourceHoldEntity();
        e.setId(rs.getLong("id"));
        e.setResourceUuid(rs.getString("resource_uuid"));
        e.setHoldType(rs.getString("hold_type"));
        e.setReason(rs.getString("reason"));
        e.setOperatorId(rs.getString("operator_id"));
        Timestamp exp = rs.getTimestamp("expire_time");
        if (exp != null) e.setExpireTime(exp.toLocalDateTime());
        e.setReleased(rs.getInt("released"));
        Timestamp rt = rs.getTimestamp("released_time");
        if (rt != null) e.setReleasedTime(rt.toLocalDateTime());
        e.setReleaseOperatorId(rs.getString("release_operator_id"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        return e;
    };

    public ResourceHoldRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ResourceHold save(ResourceHold domain) {
        ResourceHoldEntity entity = ResourceHoldConverter.toEntity(domain);
        entity.setCreateTime(LocalDateTime.now());

        String sql = "INSERT INTO storage_resource_hold (" +
                "resource_uuid, hold_type, reason, operator_id, expire_time, " +
                "released, released_time, release_operator_id, create_time" +
                ") VALUES (?,?,?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, entity.getResourceUuid());
            ps.setString(i++, entity.getHoldType());
            ps.setString(i++, entity.getReason());
            ps.setString(i++, entity.getOperatorId());
            ps.setTimestamp(i++, entity.getExpireTime() != null ? Timestamp.valueOf(entity.getExpireTime()) : null);
            ps.setInt(i++, entity.getReleased() != null ? entity.getReleased() : 0);
            ps.setTimestamp(i++, entity.getReleasedTime() != null ? Timestamp.valueOf(entity.getReleasedTime()) : null);
            ps.setString(i++, entity.getReleaseOperatorId());
            ps.setTimestamp(i++, Timestamp.valueOf(entity.getCreateTime()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            entity.setId(key.longValue());
        }
        return ResourceHoldConverter.toDomain(entity);
    }

    public Optional<ResourceHold> findById(Long id) {
        try {
            ResourceHoldEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_resource_hold WHERE id = ?", ROW_MAPPER, id);
            return Optional.ofNullable(ResourceHoldConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<ResourceHold> findByResourceUuid(String resourceUuid) {
        return jdbc.query(
                "SELECT * FROM storage_resource_hold WHERE resource_uuid = ? ORDER BY create_time DESC",
                ROW_MAPPER, resourceUuid)
                .stream().map(ResourceHoldConverter::toDomain).toList();
    }

    public List<ResourceHold> findActiveByResourceUuid(String resourceUuid) {
        return jdbc.query(
                "SELECT * FROM storage_resource_hold WHERE resource_uuid = ? AND released = 0 " +
                        "AND (expire_time IS NULL OR expire_time > ?) ORDER BY create_time DESC",
                ROW_MAPPER, resourceUuid, Timestamp.valueOf(LocalDateTime.now()))
                .stream().map(ResourceHoldConverter::toDomain).toList();
    }

    public boolean hasActiveHold(String resourceUuid) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM storage_resource_hold WHERE resource_uuid = ? AND released = 0 " +
                        "AND (expire_time IS NULL OR expire_time > ?)",
                Integer.class, resourceUuid, Timestamp.valueOf(LocalDateTime.now()));
        return count != null && count > 0;
    }

    public int countActiveHolds() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM storage_resource_hold WHERE released = 0 " +
                        "AND (expire_time IS NULL OR expire_time > ?)",
                Integer.class, Timestamp.valueOf(LocalDateTime.now()));
        return count != null ? count : 0;
    }

    public int release(Long id, String operatorId) {
        return jdbc.update(
                "UPDATE storage_resource_hold SET released = 1, released_time = ?, release_operator_id = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now()), operatorId, id);
    }
}