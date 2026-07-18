package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.StorageProfile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 存储配置持久化 — 对应 storage_profile 表。
 */
@Repository
public class StorageProfileRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageProfile> ROW_MAPPER = (rs, rowNum) -> {
        StorageProfile profile = new StorageProfile();
        profile.setId(rs.getLong("id"));
        profile.setProfileName(rs.getString("profile_name"));
        profile.setDriverName(rs.getString("driver_name"));
        profile.setDefault(rs.getInt("is_default") != 0);
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) profile.setCreateTime(ct.toLocalDateTime());
        return profile;
    };

    public StorageProfileRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public StorageProfile save(StorageProfile profile) {
        if (profile.getCreateTime() == null) {
            profile.setCreateTime(LocalDateTime.now());
        }

        String sql = "INSERT INTO storage_profile (profile_name, driver_name, is_default, create_time) VALUES (?,?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, profile.getProfileName());
            ps.setString(2, profile.getDriverName());
            ps.setInt(3, profile.isDefault() ? 1 : 0);
            ps.setTimestamp(4, Timestamp.valueOf(profile.getCreateTime()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) profile.setId(key.longValue());
        return profile;
    }

    public Optional<StorageProfile> findByProfileName(String profileName) {
        try {
            StorageProfile profile = jdbc.queryForObject(
                    "SELECT * FROM storage_profile WHERE profile_name = ?", ROW_MAPPER, profileName);
            return Optional.ofNullable(profile);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<StorageProfile> findAll() {
        return jdbc.query("SELECT * FROM storage_profile ORDER BY is_default DESC, profile_name", ROW_MAPPER);
    }

    public Optional<StorageProfile> findDefault() {
        try {
            StorageProfile profile = jdbc.queryForObject(
                    "SELECT * FROM storage_profile WHERE is_default = 1 LIMIT 1", ROW_MAPPER);
            return Optional.ofNullable(profile);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * 更新配置的驱动绑定。
     */
    public int update(String profileName, String driverName) {
        return jdbc.update(
                "UPDATE storage_profile SET driver_name = ? WHERE profile_name = ?",
                driverName, profileName);
    }

    /**
     * 将指定配置设为默认，并清除其他配置的默认标记（事务操作）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(String profileName) {
        // 清除所有默认标记
        jdbc.update("UPDATE storage_profile SET is_default = 0");
        // 设置新默认
        jdbc.update("UPDATE storage_profile SET is_default = 1 WHERE profile_name = ?", profileName);
    }

    public int deleteByProfileName(String profileName) {
        return jdbc.update("DELETE FROM storage_profile WHERE profile_name = ?", profileName);
    }

    public boolean existsByProfileName(String profileName) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM storage_profile WHERE profile_name = ?", Integer.class, profileName);
        return count != null && count > 0;
    }

    public int count() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM storage_profile", Integer.class);
        return count != null ? count : 0;
    }
}
