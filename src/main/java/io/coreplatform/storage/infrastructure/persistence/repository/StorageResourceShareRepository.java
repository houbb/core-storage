package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.StorageResourceShare;
import io.coreplatform.storage.infrastructure.persistence.converter.StorageResourceShareConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageResourceShareEntity;
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
public class StorageResourceShareRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageResourceShareEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageResourceShareEntity e = new StorageResourceShareEntity();
        e.setId(rs.getLong("id"));
        e.setShareToken(rs.getString("share_token"));
        e.setResourceUuid(rs.getString("resource_uuid"));
        e.setExpireSeconds(rs.getInt("expire_seconds"));
        Timestamp exp = rs.getTimestamp("expire_time");
        if (exp != null) e.setExpireTime(exp.toLocalDateTime());
        e.setCreatorId(rs.getString("creator_id"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("update_time");
        if (ut != null) e.setUpdateTime(ut.toLocalDateTime());
        e.setCreateUser(rs.getString("create_user"));
        e.setUpdateUser(rs.getString("update_user"));
        return e;
    };

    public StorageResourceShareRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 创建分享记录。 */
    public StorageResourceShare save(StorageResourceShare domain) {
        StorageResourceShareEntity entity = StorageResourceShareConverter.toEntity(domain);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        String sql = "INSERT INTO storage_resource_share (" +
                "share_token, resource_uuid, expire_seconds, expire_time, creator_id, " +
                "create_time, update_time, create_user, update_user" +
                ") VALUES (?,?,?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, entity.getShareToken());
            ps.setString(2, entity.getResourceUuid());
            ps.setInt(3, entity.getExpireSeconds() != null ? entity.getExpireSeconds() : 86400);
            ps.setTimestamp(4, Timestamp.valueOf(entity.getExpireTime()));
            ps.setString(5, entity.getCreatorId());
            ps.setTimestamp(6, Timestamp.valueOf(entity.getCreateTime()));
            ps.setTimestamp(7, Timestamp.valueOf(entity.getUpdateTime()));
            ps.setString(8, entity.getCreateUser());
            ps.setString(9, entity.getUpdateUser());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) entity.setId(key.longValue());
        return StorageResourceShareConverter.toDomain(entity);
    }

    /** 根据 token 查询分享。 */
    public Optional<StorageResourceShare> findByToken(String shareToken) {
        try {
            StorageResourceShareEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_resource_share WHERE share_token = ?", ROW_MAPPER, shareToken);
            return Optional.ofNullable(StorageResourceShareConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /** 查询某资源的分享列表。 */
    public List<StorageResourceShare> findByResourceUuid(String resourceUuid) {
        List<StorageResourceShareEntity> entities = jdbc.query(
                "SELECT * FROM storage_resource_share WHERE resource_uuid = ? ORDER BY create_time DESC",
                ROW_MAPPER, resourceUuid);
        return entities.stream().map(StorageResourceShareConverter::toDomain).toList();
    }

    /** 按 ID 删除分享。 */
    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM storage_resource_share WHERE id = ?", id);
    }

    /** 清理已过期的分享。 */
    public int deleteExpired() {
        return jdbc.update("DELETE FROM storage_resource_share WHERE expire_time < ?",
                Timestamp.valueOf(LocalDateTime.now()));
    }
}