package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.infrastructure.persistence.entity.StorageMetadataIndexEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Repository
public class StorageMetadataIndexRepository {

    private final JdbcTemplate jdbc;

    public StorageMetadataIndexRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 写入轻量索引。
     */
    public int save(StorageMetadataIndexEntity entity) {
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        return jdbc.update(
                "INSERT INTO storage_metadata_index (" +
                        "resource_uuid, owner_type, owner_id, resource_type, module_name, tag, status, " +
                        "create_time, update_time, create_user, update_user" +
                        ") VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                entity.getResourceUuid(),
                entity.getOwnerType(),
                entity.getOwnerId(),
                entity.getResourceType(),
                entity.getModuleName(),
                entity.getTag(),
                entity.getStatus(),
                Timestamp.valueOf(entity.getCreateTime()),
                Timestamp.valueOf(entity.getUpdateTime()),
                entity.getCreateUser(),
                entity.getUpdateUser()
        );
    }

    /**
     * 更新索引状态（资源状态变化时同步）。
     */
    public int updateStatus(String resourceUuid, String status) {
        return jdbc.update(
                "UPDATE storage_metadata_index SET status = ?, update_time = ? WHERE resource_uuid = ?",
                status, Timestamp.valueOf(LocalDateTime.now()), resourceUuid);
    }
}