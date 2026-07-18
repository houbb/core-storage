package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.StorageReplica;
import io.coreplatform.storage.application.domain.enums.ReplicaRole;
import io.coreplatform.storage.application.domain.enums.ReplicaStatus;
import io.coreplatform.storage.infrastructure.persistence.converter.StorageReplicaConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageReplicaEntity;
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
public class StorageReplicaRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageReplicaEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageReplicaEntity e = new StorageReplicaEntity();
        e.setId(rs.getLong("id"));
        e.setResourceUuid(rs.getString("resource_uuid"));
        e.setProfileName(rs.getString("profile_name"));
        e.setDriverName(rs.getString("driver_name"));
        e.setReplicaRole(rs.getString("replica_role"));
        e.setReplicaStatus(rs.getString("replica_status"));
        e.setVersion(rs.getLong("version"));
        e.setChecksum(rs.getString("checksum"));
        Timestamp st = rs.getTimestamp("sync_time");
        if (st != null) e.setSyncTime(st.toLocalDateTime());
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        return e;
    };

    public StorageReplicaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 创建副本记录。
     */
    public StorageReplica save(StorageReplica domain) {
        StorageReplicaEntity entity = StorageReplicaConverter.toEntity(domain);
        entity.setCreateTime(LocalDateTime.now());

        String sql = "INSERT INTO storage_replica (" +
                "resource_uuid, profile_name, driver_name, replica_role, replica_status, " +
                "version, checksum, sync_time, create_time" +
                ") VALUES (?,?,?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, entity.getResourceUuid());
            ps.setString(i++, entity.getProfileName());
            ps.setString(i++, entity.getDriverName());
            ps.setString(i++, entity.getReplicaRole());
            ps.setString(i++, entity.getReplicaStatus());
            ps.setLong(i++, entity.getVersion() != null ? entity.getVersion() : 1L);
            ps.setString(i++, entity.getChecksum());
            ps.setTimestamp(i++, entity.getSyncTime() != null ? Timestamp.valueOf(entity.getSyncTime()) : null);
            ps.setTimestamp(i++, Timestamp.valueOf(entity.getCreateTime()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            entity.setId(key.longValue());
        }
        return StorageReplicaConverter.toDomain(entity);
    }

    /**
     * 根据 ID 查询。
     */
    public Optional<StorageReplica> findById(Long id) {
        try {
            StorageReplicaEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_replica WHERE id = ?", ROW_MAPPER, id);
            return Optional.ofNullable(StorageReplicaConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * 查询 Resource 下所有副本。
     */
    public List<StorageReplica> findByResourceUuid(String resourceUuid) {
        List<StorageReplicaEntity> entities = jdbc.query(
                "SELECT * FROM storage_replica WHERE resource_uuid = ? ORDER BY create_time",
                ROW_MAPPER, resourceUuid);
        return entities.stream().map(StorageReplicaConverter::toDomain).toList();
    }

    /**
     * 查询 Resource 下指定 Profile 的副本（唯一）。
     */
    public Optional<StorageReplica> findByResourceUuidAndProfile(String resourceUuid, String profileName) {
        try {
            StorageReplicaEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_replica WHERE resource_uuid = ? AND profile_name = ?",
                    ROW_MAPPER, resourceUuid, profileName);
            return Optional.ofNullable(StorageReplicaConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * 查询 Resource 下指定角色的副本。
     */
    public Optional<StorageReplica> findByResourceUuidAndRole(String resourceUuid, ReplicaRole role) {
        try {
            StorageReplicaEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_replica WHERE resource_uuid = ? AND replica_role = ?",
                    ROW_MAPPER, resourceUuid, role.name());
            return Optional.ofNullable(StorageReplicaConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * 统计 Resource 的副本数。
     */
    public int countByResourceUuid(String resourceUuid) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM storage_replica WHERE resource_uuid = ?",
                Integer.class, resourceUuid);
        return count != null ? count : 0;
    }

    /**
     * 更新副本状态。
     */
    public int updateStatus(Long id, ReplicaStatus status) {
        return jdbc.update(
                "UPDATE storage_replica SET replica_status = ? WHERE id = ?",
                status.name(), id);
    }

    /**
     * 更新副本角色。
     */
    public int updateRole(Long id, ReplicaRole role) {
        return jdbc.update(
                "UPDATE storage_replica SET replica_role = ? WHERE id = ?",
                role.name(), id);
    }

    /**
     * 更新副本校验信息（同步完成后调用）。
     */
    public int updateChecksum(Long id, String checksum, LocalDateTime syncTime) {
        return jdbc.update(
                "UPDATE storage_replica SET checksum = ?, version = version + 1, sync_time = ?, replica_status = ? WHERE id = ?",
                checksum, syncTime != null ? Timestamp.valueOf(syncTime) : null, ReplicaStatus.READY.name(), id);
    }

    /**
     * 更新副本 profile 绑定（迁移时使用）。
     */
    public int updateProfile(Long id, String profileName, String driverName) {
        return jdbc.update(
                "UPDATE storage_replica SET profile_name = ?, driver_name = ? WHERE id = ?",
                profileName, driverName, id);
    }

    /**
     * 删除副本记录。
     */
    public int delete(Long id) {
        return jdbc.update("DELETE FROM storage_replica WHERE id = ?", id);
    }
}