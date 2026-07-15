package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.StorageAccessPolicy;
import io.coreplatform.storage.infrastructure.persistence.converter.StorageAccessPolicyConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageAccessPolicyEntity;
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

@Repository
public class StorageAccessPolicyRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageAccessPolicyEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageAccessPolicyEntity e = new StorageAccessPolicyEntity();
        e.setId(rs.getLong("id"));
        e.setResourceUuid(rs.getString("resource_uuid"));
        e.setAccessMode(rs.getString("access_mode"));
        e.setRoleName(rs.getString("role_name"));
        e.setAllowDownload(rs.getInt("allow_download"));
        e.setAllowPreview(rs.getInt("allow_preview"));
        e.setAllowUpdate(rs.getInt("allow_update"));
        e.setAllowDelete(rs.getInt("allow_delete"));
        e.setAllowShare(rs.getInt("allow_share"));
        Timestamp exp = rs.getTimestamp("expire_time");
        if (exp != null) e.setExpireTime(exp.toLocalDateTime());
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("update_time");
        if (ut != null) e.setUpdateTime(ut.toLocalDateTime());
        e.setCreateUser(rs.getString("create_user"));
        e.setUpdateUser(rs.getString("update_user"));
        return e;
    };

    public StorageAccessPolicyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 插入一条策略。 */
    public StorageAccessPolicy save(StorageAccessPolicy domain) {
        StorageAccessPolicyEntity entity = StorageAccessPolicyConverter.toEntity(domain);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        String sql = "INSERT INTO storage_access_policy (" +
                "resource_uuid, access_mode, role_name, allow_download, allow_preview, " +
                "allow_update, allow_delete, allow_share, expire_time, " +
                "create_time, update_time, create_user, update_user" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, entity.getResourceUuid());
            ps.setString(i++, entity.getAccessMode());
            ps.setString(i++, entity.getRoleName());
            ps.setInt(i++, entity.getAllowDownload() != null ? entity.getAllowDownload() : 1);
            ps.setInt(i++, entity.getAllowPreview() != null ? entity.getAllowPreview() : 1);
            ps.setInt(i++, entity.getAllowUpdate() != null ? entity.getAllowUpdate() : 0);
            ps.setInt(i++, entity.getAllowDelete() != null ? entity.getAllowDelete() : 0);
            ps.setInt(i++, entity.getAllowShare() != null ? entity.getAllowShare() : 0);
            ps.setTimestamp(i++, entity.getExpireTime() != null ? Timestamp.valueOf(entity.getExpireTime()) : null);
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
        return StorageAccessPolicyConverter.toDomain(entity);
    }

    /** 查询某资源的所有策略。 */
    public List<StorageAccessPolicy> findByResourceUuid(String resourceUuid) {
        List<StorageAccessPolicyEntity> entities = jdbc.query(
                "SELECT * FROM storage_access_policy WHERE resource_uuid = ? ORDER BY id",
                ROW_MAPPER, resourceUuid);
        return entities.stream().map(StorageAccessPolicyConverter::toDomain).toList();
    }

    /** 按 resource_uuid + access_mode + role_name 精确查找一条策略。 */
    public StorageAccessPolicy findByResourceAndRole(String resourceUuid, String accessMode, String roleName) {
        List<StorageAccessPolicyEntity> entities = jdbc.query(
                "SELECT * FROM storage_access_policy WHERE resource_uuid = ? AND access_mode = ? AND role_name = ?",
                ROW_MAPPER, resourceUuid, accessMode, roleName);
        return entities.isEmpty() ? null : StorageAccessPolicyConverter.toDomain(entities.get(0));
    }

    /** 替换某资源的所有策略（先删后插）。 */
    public void replacePolicies(String resourceUuid, List<StorageAccessPolicy> policies) {
        jdbc.update("DELETE FROM storage_access_policy WHERE resource_uuid = ?", resourceUuid);
        for (StorageAccessPolicy p : policies) {
            p.setResourceUuid(resourceUuid);
            save(p);
        }
    }

    /** 获取某资源的默认策略（access_mode = resource 自己的 mode）。 */
    public List<StorageAccessPolicy> findDefaultPolicies(String resourceUuid) {
        List<StorageAccessPolicyEntity> entities = jdbc.query(
                "SELECT * FROM storage_access_policy WHERE resource_uuid = ? AND role_name IS NULL ORDER BY id",
                ROW_MAPPER, resourceUuid);
        return entities.stream().map(StorageAccessPolicyConverter::toDomain).toList();
    }
}