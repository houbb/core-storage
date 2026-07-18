package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.StorageRegion;
import io.coreplatform.storage.infrastructure.persistence.converter.StorageRegionConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageRegionEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class StorageRegionRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageRegionEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageRegionEntity e = new StorageRegionEntity();
        e.setRegionCode(rs.getString("region_code"));
        e.setRegionName(rs.getString("region_name"));
        e.setEndpoint(rs.getString("endpoint"));
        e.setDriverName(rs.getString("driver_name"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        return e;
    };

    public StorageRegionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public StorageRegion save(StorageRegion domain) {
        StorageRegionEntity entity = StorageRegionConverter.toEntity(domain);

        String sql = "INSERT OR REPLACE INTO storage_region (region_code, region_name, endpoint, driver_name, create_time) " +
                "VALUES (?,?,?,?,?)";
        jdbc.update(sql,
                entity.getRegionCode(),
                entity.getRegionName(),
                entity.getEndpoint(),
                entity.getDriverName(),
                Timestamp.valueOf(entity.getCreateTime() != null ? entity.getCreateTime() : java.time.LocalDateTime.now()));
        return StorageRegionConverter.toDomain(entity);
    }

    public Optional<StorageRegion> findByRegionCode(String regionCode) {
        try {
            StorageRegionEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_region WHERE region_code = ?", ROW_MAPPER, regionCode);
            return Optional.ofNullable(StorageRegionConverter.toDomain(e));
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<StorageRegion> listAll() {
        List<StorageRegionEntity> entities = jdbc.query(
                "SELECT * FROM storage_region ORDER BY create_time DESC", ROW_MAPPER);
        return entities.stream().map(StorageRegionConverter::toDomain).toList();
    }

    public int deleteByRegionCode(String regionCode) {
        return jdbc.update("DELETE FROM storage_region WHERE region_code = ?", regionCode);
    }

    public boolean existsByRegionCode(String regionCode) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM storage_region WHERE region_code = ?", Integer.class, regionCode);
        return count != null && count > 0;
    }
}