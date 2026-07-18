package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.StorageReference;
import io.coreplatform.storage.infrastructure.persistence.converter.StorageReferenceConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageReferenceEntity;
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
public class StorageReferenceRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageReferenceEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageReferenceEntity e = new StorageReferenceEntity();
        e.setId(rs.getLong("id"));
        e.setMetadataUuid(rs.getString("metadata_uuid"));
        e.setSystemName(rs.getString("system_name"));
        e.setModuleName(rs.getString("module_name"));
        e.setBusinessType(rs.getString("business_type"));
        e.setBusinessId(rs.getString("business_id"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("update_time");
        if (ut != null) e.setUpdateTime(ut.toLocalDateTime());
        e.setCreateUser(rs.getString("create_user"));
        e.setUpdateUser(rs.getString("update_user"));
        return e;
    };

    public StorageReferenceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 创建引用。
     */
    public StorageReference save(StorageReference domain) {
        StorageReferenceEntity entity = StorageReferenceConverter.toEntity(domain);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        String sql = "INSERT INTO storage_reference (" +
                "metadata_uuid, system_name, module_name, business_type, business_id, " +
                "create_time, update_time, create_user, update_user" +
                ") VALUES (?,?,?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, entity.getMetadataUuid());
            ps.setString(2, entity.getSystemName());
            ps.setString(3, entity.getModuleName());
            ps.setString(4, entity.getBusinessType());
            ps.setString(5, entity.getBusinessId());
            ps.setTimestamp(6, Timestamp.valueOf(entity.getCreateTime()));
            ps.setTimestamp(7, Timestamp.valueOf(entity.getUpdateTime()));
            ps.setString(8, entity.getCreateUser());
            ps.setString(9, entity.getUpdateUser());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            entity.setId(key.longValue());
        }
        return StorageReferenceConverter.toDomain(entity);
    }

    /**
     * 查询某资源的所有引用。
     */
    public List<StorageReference> findByMetadataUuid(String metadataUuid) {
        List<StorageReferenceEntity> entities = jdbc.query(
                "SELECT * FROM storage_reference WHERE metadata_uuid = ? ORDER BY create_time DESC",
                ROW_MAPPER, metadataUuid);
        return entities.stream().map(StorageReferenceConverter::toDomain).toList();
    }

    /**
     * 统计某资源的引用数。
     */
    public int countByMetadataUuid(String metadataUuid) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM storage_reference WHERE metadata_uuid = ?",
                Integer.class, metadataUuid);
        return count != null ? count : 0;
    }

    /**
     * 按 ID 删除引用。
     */
    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM storage_reference WHERE id = ?", id);
    }
}