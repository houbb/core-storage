package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.infrastructure.persistence.entity.StorageResourcePropertyEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class StorageResourcePropertyRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageResourcePropertyEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageResourcePropertyEntity e = new StorageResourcePropertyEntity();
        e.setId(rs.getLong("id"));
        e.setResourceUuid(rs.getString("resource_uuid"));
        e.setPropKey(rs.getString("prop_key"));
        e.setPropValue(rs.getString("prop_value"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("update_time");
        if (ut != null) e.setUpdateTime(ut.toLocalDateTime());
        e.setCreateUser(rs.getString("create_user"));
        e.setUpdateUser(rs.getString("update_user"));
        return e;
    };

    public StorageResourcePropertyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 设置资源属性（幂等 upsert）— 存在则更新，不存在则插入。
     */
    public void setProperty(String resourceUuid, String key, String value) {
        if (key == null || key.isBlank()) return;
        LocalDateTime now = LocalDateTime.now();

        int updated = jdbc.update(
                "UPDATE storage_resource_property SET prop_value = ?, update_time = ? WHERE resource_uuid = ? AND prop_key = ?",
                value, Timestamp.valueOf(now), resourceUuid, key);
        if (updated == 0) {
            jdbc.update(
                    "INSERT INTO storage_resource_property (resource_uuid, prop_key, prop_value, create_time, update_time) VALUES (?,?,?,?,?)",
                    resourceUuid, key, value, Timestamp.valueOf(now), Timestamp.valueOf(now));
        }
    }

    /**
     * 批量设置属性。
     */
    public void setProperties(String resourceUuid, Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) return;
        properties.forEach((k, v) -> setProperty(resourceUuid, k, v));
    }

    /**
     * 查询资源的所有属性。
     */
    public List<StorageResourcePropertyEntity> findByResourceUuid(String resourceUuid) {
        return jdbc.query(
                "SELECT * FROM storage_resource_property WHERE resource_uuid = ? ORDER BY id",
                ROW_MAPPER, resourceUuid);
    }

    /**
     * 获取单个属性值。
     */
    public String getPropertyValue(String resourceUuid, String key) {
        try {
            return jdbc.queryForObject(
                    "SELECT prop_value FROM storage_resource_property WHERE resource_uuid = ? AND prop_key = ?",
                    String.class, resourceUuid, key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 删除指定属性。
     */
    public int deleteProperty(String resourceUuid, String key) {
        return jdbc.update(
                "DELETE FROM storage_resource_property WHERE resource_uuid = ? AND prop_key = ?",
                resourceUuid, key);
    }
}