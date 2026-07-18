package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.StorageScan;
import io.coreplatform.storage.infrastructure.persistence.converter.StorageScanConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageScanEntity;
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
public class StorageScanRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageScanEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageScanEntity e = new StorageScanEntity();
        e.setId(rs.getLong("id"));
        e.setResourceUuid(rs.getString("resource_uuid"));
        e.setScanType(rs.getString("scan_type"));
        e.setStatus(rs.getString("status"));
        e.setResultMessage(rs.getString("result_message"));
        Timestamp st = rs.getTimestamp("scan_time");
        if (st != null) e.setScanTime(st.toLocalDateTime());
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        return e;
    };

    public StorageScanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public StorageScan save(StorageScan domain) {
        StorageScanEntity entity = StorageScanConverter.toEntity(domain);
        if (entity.getCreateTime() == null) entity.setCreateTime(LocalDateTime.now());

        String sql = "INSERT INTO storage_scan (resource_uuid, scan_type, status, result_message, scan_time, create_time) " +
                "VALUES (?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, entity.getResourceUuid());
            ps.setString(2, entity.getScanType());
            ps.setString(3, entity.getStatus());
            ps.setString(4, entity.getResultMessage());
            ps.setTimestamp(5, entity.getScanTime() != null ? Timestamp.valueOf(entity.getScanTime()) : null);
            ps.setTimestamp(6, Timestamp.valueOf(entity.getCreateTime()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            entity.setId(key.longValue());
        }
        return StorageScanConverter.toDomain(entity);
    }

    public Optional<StorageScan> findById(Long id) {
        try {
            StorageScanEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_scan WHERE id = ?", ROW_MAPPER, id);
            return Optional.ofNullable(StorageScanConverter.toDomain(e));
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<StorageScan> findByResourceUuid(String resourceUuid) {
        List<StorageScanEntity> entities = jdbc.query(
                "SELECT * FROM storage_scan WHERE resource_uuid = ? ORDER BY create_time DESC",
                ROW_MAPPER, resourceUuid);
        return entities.stream().map(StorageScanConverter::toDomain).toList();
    }

    public Optional<StorageScan> findByResourceUuidAndType(String resourceUuid, String scanType) {
        try {
            StorageScanEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_scan WHERE resource_uuid = ? AND scan_type = ? ORDER BY create_time DESC LIMIT 1",
                    ROW_MAPPER, resourceUuid, scanType);
            return Optional.ofNullable(StorageScanConverter.toDomain(e));
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public int updateStatus(Long id, String status, String resultMessage) {
        return jdbc.update(
                "UPDATE storage_scan SET status = ?, result_message = ?, scan_time = ? WHERE id = ?",
                status, resultMessage, Timestamp.valueOf(LocalDateTime.now()), id);
    }

    public List<StorageScan> search(String resourceUuid, String scanType, String status, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM storage_scan WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (resourceUuid != null && !resourceUuid.isBlank()) {
            sql.append(" AND resource_uuid = ?");
            params.add(resourceUuid);
        }
        if (scanType != null && !scanType.isBlank()) {
            sql.append(" AND scan_type = ?");
            params.add(scanType);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        sql.append(" ORDER BY create_time DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<StorageScanEntity> entities = jdbc.query(sql.toString(), ROW_MAPPER, params.toArray());
        return entities.stream().map(StorageScanConverter::toDomain).toList();
    }

    public int countSearch(String resourceUuid, String scanType, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM storage_scan WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (resourceUuid != null && !resourceUuid.isBlank()) {
            sql.append(" AND resource_uuid = ?");
            params.add(resourceUuid);
        }
        if (scanType != null && !scanType.isBlank()) {
            sql.append(" AND scan_type = ?");
            params.add(scanType);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        Integer count = jdbc.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }
}