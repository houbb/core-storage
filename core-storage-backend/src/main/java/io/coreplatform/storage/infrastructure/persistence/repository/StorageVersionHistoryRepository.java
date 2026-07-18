package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.StorageVersionHistory;
import io.coreplatform.storage.infrastructure.persistence.converter.StorageVersionHistoryConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageVersionHistoryEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class StorageVersionHistoryRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageVersionHistoryEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageVersionHistoryEntity e = new StorageVersionHistoryEntity();
        e.setId(rs.getLong("id"));
        e.setVersionUuid(rs.getString("version_uuid"));
        e.setResourceUuid(rs.getString("resource_uuid"));
        e.setAction(rs.getString("action"));
        e.setPreviousStatus(rs.getString("previous_status"));
        e.setNewStatus(rs.getString("new_status"));
        e.setOperatorId(rs.getString("operator_id"));
        e.setRemark(rs.getString("remark"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        return e;
    };

    public StorageVersionHistoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 保存操作历史 */
    public StorageVersionHistory save(StorageVersionHistory domain) {
        StorageVersionHistoryEntity entity = StorageVersionHistoryConverter.toEntity(domain);

        String sql = "INSERT INTO storage_version_history (" +
                "version_uuid, resource_uuid, action, previous_status, new_status, " +
                "operator_id, remark, create_time" +
                ") VALUES (?,?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, entity.getVersionUuid());
            ps.setString(i++, entity.getResourceUuid());
            ps.setString(i++, entity.getAction());
            ps.setString(i++, entity.getPreviousStatus());
            ps.setString(i++, entity.getNewStatus());
            ps.setString(i++, entity.getOperatorId());
            ps.setString(i++, entity.getRemark());
            ps.setTimestamp(i++, entity.getCreateTime() != null ? Timestamp.valueOf(entity.getCreateTime()) : null);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            entity.setId(key.longValue());
        }
        return StorageVersionHistoryConverter.toDomain(entity);
    }

    /** 查询某版本的操作历史 */
    public List<StorageVersionHistory> findByVersionUuid(String versionUuid) {
        List<StorageVersionHistoryEntity> entities = jdbc.query(
                "SELECT * FROM storage_version_history WHERE version_uuid = ? ORDER BY create_time DESC",
                ROW_MAPPER, versionUuid);
        return entities.stream().map(StorageVersionHistoryConverter::toDomain).toList();
    }

    /** 查询某 Resource 的操作历史（分页） */
    public List<StorageVersionHistory> findByResourceUuid(String resourceUuid, int offset, int limit) {
        List<StorageVersionHistoryEntity> entities = jdbc.query(
                "SELECT * FROM storage_version_history WHERE resource_uuid = ? ORDER BY create_time DESC LIMIT ? OFFSET ?",
                ROW_MAPPER, resourceUuid, limit, offset);
        return entities.stream().map(StorageVersionHistoryConverter::toDomain).toList();
    }

    /** 统计某 Resource 的操作历史记录数 */
    public int countByResourceUuid(String resourceUuid) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM storage_version_history WHERE resource_uuid = ?",
                Integer.class, resourceUuid);
        return count != null ? count : 0;
    }
}
