package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.StorageDriverInfo;
import io.coreplatform.storage.application.domain.enums.DriverHealth;
import io.coreplatform.storage.application.domain.enums.DriverStatus;
import io.coreplatform.storage.application.domain.enums.DriverType;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * 存储驱动持久化 — 对应 storage_driver 表。
 */
@Repository
public class StorageDriverRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageDriverInfo> ROW_MAPPER = (rs, rowNum) -> {
        StorageDriverInfo info = new StorageDriverInfo();
        info.setId(rs.getLong("id"));
        info.setDriverName(rs.getString("driver_name"));
        info.setDriverType(safeEnum(rs.getString("driver_type")));
        info.setVersion(rs.getString("version"));
        info.setEnabled(rs.getInt("enabled") != 0);
        info.setStatus(safeStatusEnum(rs.getString("status")));
        info.setHealthStatus(safeHealthEnum(rs.getString("health_status")));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) info.setCreateTime(ct.toLocalDateTime());
        return info;
    };

    public StorageDriverRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 保存驱动信息，如果已存在（按 driver_name）则更新。
     */
    public StorageDriverInfo save(StorageDriverInfo info) {
        Optional<StorageDriverInfo> existing = findByDriverName(info.getDriverName());
        if (existing.isPresent()) {
            // update
            jdbc.update(
                    "UPDATE storage_driver SET driver_type=?, version=?, enabled=?, status=?, health_status=? WHERE driver_name=?",
                    info.getDriverType() != null ? info.getDriverType().name() : "CUSTOM",
                    info.getVersion(),
                    info.isEnabled() ? 1 : 0,
                    info.getStatus() != null ? info.getStatus().name() : "RUNNING",
                    info.getHealthStatus() != null ? info.getHealthStatus().name() : "UNKNOWN",
                    info.getDriverName()
            );
            info.setId(existing.get().getId());
            return info;
        }

        String sql = "INSERT INTO storage_driver (driver_name, driver_type, version, enabled, status, health_status, create_time) VALUES (?,?,?,?,?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, info.getDriverName());
            ps.setString(2, info.getDriverType() != null ? info.getDriverType().name() : "CUSTOM");
            ps.setString(3, info.getVersion());
            ps.setInt(4, info.isEnabled() ? 1 : 0);
            ps.setString(5, info.getStatus() != null ? info.getStatus().name() : "RUNNING");
            ps.setString(6, info.getHealthStatus() != null ? info.getHealthStatus().name() : "UNKNOWN");
            ps.setTimestamp(7, Timestamp.valueOf(info.getCreateTime() != null ? info.getCreateTime() : java.time.LocalDateTime.now()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) info.setId(key.longValue());
        return info;
    }

    public Optional<StorageDriverInfo> findByDriverName(String driverName) {
        try {
            StorageDriverInfo info = jdbc.queryForObject(
                    "SELECT * FROM storage_driver WHERE driver_name = ?", ROW_MAPPER, driverName);
            return Optional.ofNullable(info);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<StorageDriverInfo> findAll() {
        return jdbc.query("SELECT * FROM storage_driver ORDER BY driver_name", ROW_MAPPER);
    }

    public int updateHealthStatus(String driverName, DriverHealth healthStatus) {
        return jdbc.update(
                "UPDATE storage_driver SET health_status = ? WHERE driver_name = ?",
                healthStatus.name(), driverName);
    }

    public int updateStatus(String driverName, DriverStatus status) {
        return jdbc.update(
                "UPDATE storage_driver SET status = ? WHERE driver_name = ?",
                status.name(), driverName);
    }

    // --- helpers ---

    private static DriverType safeEnum(String value) {
        if (value == null || value.isBlank()) return DriverType.CUSTOM;
        try {
            return DriverType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return DriverType.CUSTOM;
        }
    }

    private static DriverStatus safeStatusEnum(String value) {
        if (value == null || value.isBlank()) return DriverStatus.RUNNING;
        try {
            return DriverStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return DriverStatus.RUNNING;
        }
    }

    private static DriverHealth safeHealthEnum(String value) {
        if (value == null || value.isBlank()) return DriverHealth.UNKNOWN;
        try {
            return DriverHealth.valueOf(value);
        } catch (IllegalArgumentException e) {
            return DriverHealth.UNKNOWN;
        }
    }
}
