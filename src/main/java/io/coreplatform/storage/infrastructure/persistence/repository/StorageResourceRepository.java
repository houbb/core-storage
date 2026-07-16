package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.StorageResource;
import io.coreplatform.storage.infrastructure.persistence.converter.StorageResourceConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageResourceEntity;
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
public class StorageResourceRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageResourceEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageResourceEntity e = new StorageResourceEntity();
        e.setId(rs.getLong("id"));
        e.setResourceUuid(rs.getString("resource_uuid"));
        e.setMetadataUuid(rs.getString("metadata_uuid"));
        e.setResourceName(rs.getString("resource_name"));
        e.setResourceType(rs.getString("resource_type"));
        e.setCategory(rs.getString("category"));
        e.setDescription(rs.getString("description"));
        e.setOwnerType(rs.getString("owner_type"));
        e.setOwnerId(rs.getString("owner_id"));
        e.setVisibility(rs.getString("visibility"));
        e.setStatus(rs.getString("status"));
        e.setAccessMode(rs.getString("access_mode"));
        e.setProfileName(rs.getString("profile_name"));
        e.setLifecycleStage(rs.getString("lifecycle_stage"));
        e.setTenantId(rs.getString("tenant_id"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("update_time");
        if (ut != null) e.setUpdateTime(ut.toLocalDateTime());
        e.setCreateUser(rs.getString("create_user"));
        e.setUpdateUser(rs.getString("update_user"));
        return e;
    };

    public StorageResourceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 创建资源记录。
     */
    public StorageResource save(StorageResource domain) {
        StorageResourceEntity entity = StorageResourceConverter.toEntity(domain);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        String sql = "INSERT INTO storage_resource (" +
                "resource_uuid, metadata_uuid, resource_name, resource_type, category, " +
                "description, owner_type, owner_id, visibility, access_mode, profile_name, lifecycle_stage, tenant_id, status, " +
                "create_time, update_time, create_user, update_user" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, entity.getResourceUuid());
            ps.setString(i++, entity.getMetadataUuid());
            ps.setString(i++, entity.getResourceName());
            ps.setString(i++, entity.getResourceType());
            ps.setString(i++, entity.getCategory());
            ps.setString(i++, entity.getDescription());
            ps.setString(i++, entity.getOwnerType());
            ps.setString(i++, entity.getOwnerId());
            ps.setString(i++, entity.getVisibility());
            ps.setString(i++, entity.getAccessMode() != null ? entity.getAccessMode() : "PUBLIC");
            ps.setString(i++, entity.getProfileName());
            ps.setString(i++, entity.getLifecycleStage() != null ? entity.getLifecycleStage() : "ACTIVE");
            ps.setString(i++, entity.getTenantId() != null ? entity.getTenantId() : "default");
            ps.setString(i++, entity.getStatus());
            ps.setTimestamp(i++, Timestamp.valueOf(entity.getCreateTime()));
            ps.setTimestamp(i++, Timestamp.valueOf(entity.getUpdateTime()));
            ps.setString(i++, entity.getCreateUser());
            ps.setString(i++, entity.getUpdateUser());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            entity.setId(key.longValue());
        }
        return StorageResourceConverter.toDomain(entity);
    }

    /**
     * 根据 resource_uuid 查询。
     */
    public Optional<StorageResource> findByResourceUuid(String resourceUuid) {
        try {
            StorageResourceEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_resource WHERE resource_uuid = ?", ROW_MAPPER, resourceUuid);
            return Optional.ofNullable(StorageResourceConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * 条件搜索 + 分页 + 排序。
     */
    public int update(String resourceUuid, String resourceName, String description,
                       String category, String visibility, String accessMode, List<String> tags) {
        return jdbc.update(
                "UPDATE storage_resource SET resource_name = ?, description = ?, category = ?, " +
                        "visibility = ?, access_mode = ?, update_time = ? WHERE resource_uuid = ?",
                resourceName, description, category, visibility,
                accessMode != null ? accessMode : "PUBLIC",
                Timestamp.valueOf(LocalDateTime.now()), resourceUuid);
    }

    /**
     * 更新资源状态。
     */
    public int updateStatus(String resourceUuid, String status) {
        return jdbc.update(
                "UPDATE storage_resource SET status = ?, update_time = ? WHERE resource_uuid = ?",
                status, Timestamp.valueOf(LocalDateTime.now()), resourceUuid);
    }

    /**
     * 根据 metadata_uuid 反向查找资源。
     */
    public Optional<StorageResource> findByMetadataUuid(String metadataUuid) {
        try {
            StorageResourceEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_resource WHERE metadata_uuid = ?", ROW_MAPPER, metadataUuid);
            return Optional.ofNullable(StorageResourceConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * 条件搜索 + 分页 + 排序。
     */
    public List<StorageResource> search(String keyword, String resourceType, String category,
                                         String visibility, String ownerType, String ownerId,
                                         String tag, String status,
                                         String sort, String order, int offset, int limit) {
        return search(keyword, resourceType, category, visibility, ownerType, ownerId, tag, status, null, sort, order, offset, limit);
    }

    /**
     * 条件搜索 + 分页 + 排序（含租户过滤）。
     */
    public List<StorageResource> search(String keyword, String resourceType, String category,
                                         String visibility, String ownerType, String ownerId,
                                         String tag, String status, String tenantId,
                                         String sort, String order, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT r.* FROM storage_resource r WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (r.resource_name LIKE ? OR r.resource_uuid LIKE ? OR r.description LIKE ?)");
            String kw = "%" + keyword + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (resourceType != null && !resourceType.isBlank()) {
            sql.append(" AND r.resource_type = ?");
            params.add(resourceType);
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND r.category = ?");
            params.add(category);
        }
        if (visibility != null && !visibility.isBlank()) {
            sql.append(" AND r.visibility = ?");
            params.add(visibility);
        }
        if (ownerType != null && !ownerType.isBlank()) {
            sql.append(" AND r.owner_type = ?");
            params.add(ownerType);
        }
        if (ownerId != null && !ownerId.isBlank()) {
            sql.append(" AND r.owner_id = ?");
            params.add(ownerId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND r.status = ?");
            params.add(status);
        }
        if (tenantId != null && !tenantId.isBlank()) {
            sql.append(" AND r.tenant_id = ?");
            params.add(tenantId);
        }
        // tag 过滤通过子查询
        if (tag != null && !tag.isBlank()) {
            sql.append(" AND EXISTS (SELECT 1 FROM storage_resource_tag t WHERE t.resource_uuid = r.resource_uuid AND t.tag_name = ?)");
            params.add(tag);
        }

        String orderBy = "r.create_time DESC";
        if (sort != null) {
            switch (sort) {
                case "name" -> orderBy = "r.resource_name";
                case "type" -> orderBy = "r.resource_type";
                default -> orderBy = "r.create_time";
            }
            orderBy += "DESC".equalsIgnoreCase(order) ? " DESC" : " ASC";
        }
        sql.append(" ORDER BY ").append(orderBy);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<StorageResourceEntity> entities = jdbc.query(sql.toString(), ROW_MAPPER, params.toArray());
        return entities.stream().map(StorageResourceConverter::toDomain).toList();
    }

    /**
     * 搜索总数。
     */
    public int countSearch(String keyword, String resourceType, String category,
                            String visibility, String ownerType, String ownerId,
                            String tag, String status) {
        return countSearch(keyword, resourceType, category, visibility, ownerType, ownerId, tag, status, null);
    }

    /**
     * 搜索总数（含租户过滤）。
     */
    public int countSearch(String keyword, String resourceType, String category,
                            String visibility, String ownerType, String ownerId,
                            String tag, String status, String tenantId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM storage_resource r WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (r.resource_name LIKE ? OR r.resource_uuid LIKE ? OR r.description LIKE ?)");
            String kw = "%" + keyword + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (resourceType != null && !resourceType.isBlank()) {
            sql.append(" AND r.resource_type = ?");
            params.add(resourceType);
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND r.category = ?");
            params.add(category);
        }
        if (visibility != null && !visibility.isBlank()) {
            sql.append(" AND r.visibility = ?");
            params.add(visibility);
        }
        if (ownerType != null && !ownerType.isBlank()) {
            sql.append(" AND r.owner_type = ?");
            params.add(ownerType);
        }
        if (ownerId != null && !ownerId.isBlank()) {
            sql.append(" AND r.owner_id = ?");
            params.add(ownerId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND r.status = ?");
            params.add(status);
        }
        if (tenantId != null && !tenantId.isBlank()) {
            sql.append(" AND r.tenant_id = ?");
            params.add(tenantId);
        }
        if (tag != null && !tag.isBlank()) {
            sql.append(" AND EXISTS (SELECT 1 FROM storage_resource_tag t WHERE t.resource_uuid = r.resource_uuid AND t.tag_name = ?)");
            params.add(tag);
        }

        Integer count = jdbc.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    /**
     * 更新资源的生命周期阶段。
     */
    public int updateLifecycleStage(String resourceUuid, String lifecycleStage) {
        return jdbc.update(
                "UPDATE storage_resource SET lifecycle_stage = ?, update_time = ? WHERE resource_uuid = ?",
                lifecycleStage, Timestamp.valueOf(LocalDateTime.now()), resourceUuid);
    }

    /**
     * 按生命周期阶段统计资源数量。
     */
    public int countByLifecycleStage(String lifecycleStage) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM storage_resource WHERE lifecycle_stage = ?",
                Integer.class, lifecycleStage);
        return count != null ? count : 0;
    }

    /**
     * 查询所有非 DELETED 状态的资源（用于调度器扫描）。
     */
    public List<StorageResource> findActiveForLifecycle() {
        List<StorageResourceEntity> entities = jdbc.query(
                "SELECT * FROM storage_resource WHERE lifecycle_stage != 'DELETED' AND status != 'DELETED'",
                ROW_MAPPER);
        return entities.stream().map(StorageResourceConverter::toDomain).toList();
    }
}