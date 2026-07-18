package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.StorageVersionAlias;
import io.coreplatform.storage.infrastructure.persistence.converter.StorageVersionAliasConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageVersionAliasEntity;
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
public class StorageVersionAliasRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageVersionAliasEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageVersionAliasEntity e = new StorageVersionAliasEntity();
        e.setId(rs.getLong("id"));
        e.setVersionUuid(rs.getString("version_uuid"));
        e.setResourceUuid(rs.getString("resource_uuid"));
        e.setAliasName(rs.getString("alias_name"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        return e;
    };

    public StorageVersionAliasRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 创建别名记录 */
    public StorageVersionAlias save(StorageVersionAlias domain) {
        StorageVersionAliasEntity entity = StorageVersionAliasConverter.toEntity(domain);
        entity.setCreateTime(LocalDateTime.now());

        String sql = "INSERT INTO storage_version_alias (version_uuid, resource_uuid, alias_name, create_time) VALUES (?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, entity.getVersionUuid());
            ps.setString(i++, entity.getResourceUuid());
            ps.setString(i++, entity.getAliasName());
            ps.setTimestamp(i++, Timestamp.valueOf(entity.getCreateTime()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            entity.setId(key.longValue());
        }
        return StorageVersionAliasConverter.toDomain(entity);
    }

    /** 根据 version_uuid 查询别名 */
    public Optional<StorageVersionAlias> findByVersionUuid(String versionUuid) {
        try {
            StorageVersionAliasEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_version_alias WHERE version_uuid = ?", ROW_MAPPER, versionUuid);
            return Optional.ofNullable(StorageVersionAliasConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /** 根据 resourceUuid + aliasName 查询 */
    public Optional<StorageVersionAlias> findByResourceUuidAndAlias(String resourceUuid, String aliasName) {
        try {
            StorageVersionAliasEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_version_alias WHERE resource_uuid = ? AND alias_name = ?",
                    ROW_MAPPER, resourceUuid, aliasName);
            return Optional.ofNullable(StorageVersionAliasConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /** 查询 Resource 下所有别名 */
    public List<StorageVersionAlias> findAllByResourceUuid(String resourceUuid) {
        List<StorageVersionAliasEntity> entities = jdbc.query(
                "SELECT * FROM storage_version_alias WHERE resource_uuid = ? ORDER BY create_time",
                ROW_MAPPER, resourceUuid);
        return entities.stream().map(StorageVersionAliasConverter::toDomain).toList();
    }

    /** 删除指定版本的别名 */
    public int deleteByVersionUuid(String versionUuid) {
        return jdbc.update("DELETE FROM storage_version_alias WHERE version_uuid = ?", versionUuid);
    }

    /** 删除指定 Resource 下的别名 */
    public int deleteByResourceUuidAndAlias(String resourceUuid, String aliasName) {
        return jdbc.update("DELETE FROM storage_version_alias WHERE resource_uuid = ? AND alias_name = ?",
                resourceUuid, aliasName);
    }
}
