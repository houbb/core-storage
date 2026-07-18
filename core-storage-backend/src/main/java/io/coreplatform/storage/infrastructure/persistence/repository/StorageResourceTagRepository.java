package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.infrastructure.persistence.entity.StorageResourceTagEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class StorageResourceTagRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageResourceTagEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageResourceTagEntity e = new StorageResourceTagEntity();
        e.setId(rs.getLong("id"));
        e.setResourceUuid(rs.getString("resource_uuid"));
        e.setTagName(rs.getString("tag_name"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("update_time");
        if (ut != null) e.setUpdateTime(ut.toLocalDateTime());
        e.setCreateUser(rs.getString("create_user"));
        e.setUpdateUser(rs.getString("update_user"));
        return e;
    };

    public StorageResourceTagRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 批量保存标签（先删后插）。
     */
    public void replaceTags(String resourceUuid, List<String> tags) {
        // 先删旧标签
        jdbc.update("DELETE FROM storage_resource_tag WHERE resource_uuid = ?", resourceUuid);
        // 再插新标签
        LocalDateTime now = LocalDateTime.now();
        for (String tag : tags) {
            if (tag == null || tag.isBlank()) continue;
            jdbc.update(
                    "INSERT INTO storage_resource_tag (resource_uuid, tag_name, create_time, update_time) VALUES (?,?,?,?)",
                    resourceUuid, tag.trim(), Timestamp.valueOf(now), Timestamp.valueOf(now));
        }
    }

    /**
     * 添加单个标签。
     */
    public void addTag(String resourceUuid, String tagName) {
        if (tagName == null || tagName.isBlank()) return;
        LocalDateTime now = LocalDateTime.now();
        jdbc.update(
                "INSERT INTO storage_resource_tag (resource_uuid, tag_name, create_time, update_time) VALUES (?,?,?,?)",
                resourceUuid, tagName.trim(), Timestamp.valueOf(now), Timestamp.valueOf(now));
    }

    /**
     * 查询资源的所有标签。
     */
    public List<String> findTagsByResourceUuid(String resourceUuid) {
        List<StorageResourceTagEntity> entities = jdbc.query(
                "SELECT * FROM storage_resource_tag WHERE resource_uuid = ? ORDER BY id",
                ROW_MAPPER, resourceUuid);
        return entities.stream().map(StorageResourceTagEntity::getTagName).toList();
    }

    /**
     * 删除资源全部标签。
     */
    public int deleteByResourceUuid(String resourceUuid) {
        return jdbc.update("DELETE FROM storage_resource_tag WHERE resource_uuid = ?", resourceUuid);
    }
}