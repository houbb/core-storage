package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.LifecyclePolicy;
import io.coreplatform.storage.infrastructure.persistence.converter.LifecyclePolicyConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.LifecyclePolicyEntity;
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
public class LifecyclePolicyRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<LifecyclePolicyEntity> ROW_MAPPER = (rs, rowNum) -> {
        LifecyclePolicyEntity e = new LifecyclePolicyEntity();
        e.setId(rs.getLong("id"));
        e.setPolicyName(rs.getString("policy_name"));
        e.setResourceType(rs.getString("resource_type"));
        e.setCategory(rs.getString("category"));
        e.setActiveDays(rs.getInt("active_days"));
        e.setWarmDays(rs.getInt("warm_days"));
        e.setColdDays(rs.getInt("cold_days"));
        e.setArchiveDays(rs.getInt("archive_days"));
        e.setDeleteDays(rs.getInt("delete_days"));
        e.setEnabled(rs.getInt("enabled"));
        e.setDescription(rs.getString("description"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("update_time");
        if (ut != null) e.setUpdateTime(ut.toLocalDateTime());
        return e;
    };

    public LifecyclePolicyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public LifecyclePolicy save(LifecyclePolicy domain) {
        LifecyclePolicyEntity entity = LifecyclePolicyConverter.toEntity(domain);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        String sql = "INSERT INTO storage_lifecycle_policy (" +
                "policy_name, resource_type, category, active_days, warm_days, cold_days, " +
                "archive_days, delete_days, enabled, description, create_time, update_time" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, entity.getPolicyName());
            ps.setString(i++, entity.getResourceType());
            ps.setString(i++, entity.getCategory());
            ps.setInt(i++, entity.getActiveDays() != null ? entity.getActiveDays() : 0);
            ps.setInt(i++, entity.getWarmDays() != null ? entity.getWarmDays() : 0);
            ps.setInt(i++, entity.getColdDays() != null ? entity.getColdDays() : 0);
            ps.setInt(i++, entity.getArchiveDays() != null ? entity.getArchiveDays() : 0);
            ps.setInt(i++, entity.getDeleteDays() != null ? entity.getDeleteDays() : 0);
            ps.setInt(i++, entity.getEnabled() != null ? entity.getEnabled() : 1);
            ps.setString(i++, entity.getDescription());
            ps.setTimestamp(i++, Timestamp.valueOf(entity.getCreateTime()));
            ps.setTimestamp(i++, Timestamp.valueOf(entity.getUpdateTime()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            entity.setId(key.longValue());
        }
        return LifecyclePolicyConverter.toDomain(entity);
    }

    public List<LifecyclePolicy> findAll() {
        return jdbc.query("SELECT * FROM storage_lifecycle_policy ORDER BY resource_type, category", ROW_MAPPER)
                .stream().map(LifecyclePolicyConverter::toDomain).toList();
    }

    public Optional<LifecyclePolicy> findById(Long id) {
        try {
            LifecyclePolicyEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_lifecycle_policy WHERE id = ?", ROW_MAPPER, id);
            return Optional.ofNullable(LifecyclePolicyConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Optional<LifecyclePolicy> findByTypeAndCategory(String resourceType, String category) {
        try {
            LifecyclePolicyEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_lifecycle_policy WHERE resource_type = ? AND category = ? AND enabled = 1",
                    ROW_MAPPER, resourceType, category);
            return Optional.ofNullable(LifecyclePolicyConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public boolean existsByTypeAndCategory(String resourceType, String category) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM storage_lifecycle_policy WHERE resource_type = ? AND category = ?",
                Integer.class, resourceType, category);
        return count != null && count > 0;
    }

    public int update(LifecyclePolicy domain) {
        return jdbc.update(
                "UPDATE storage_lifecycle_policy SET policy_name = ?, active_days = ?, warm_days = ?, " +
                        "cold_days = ?, archive_days = ?, delete_days = ?, enabled = ?, " +
                        "description = ?, update_time = ? WHERE id = ?",
                domain.getPolicyName(), domain.getActiveDays(), domain.getWarmDays(),
                domain.getColdDays(), domain.getArchiveDays(), domain.getDeleteDays(),
                domain.isEnabled() ? 1 : 0, domain.getDescription(),
                Timestamp.valueOf(LocalDateTime.now()), domain.getId());
    }

    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM storage_lifecycle_policy WHERE id = ?", id);
    }
}