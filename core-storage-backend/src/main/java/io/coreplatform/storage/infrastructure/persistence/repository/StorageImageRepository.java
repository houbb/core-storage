package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.StorageImage;
import io.coreplatform.storage.infrastructure.persistence.converter.StorageImageConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageImageEntity;
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
import java.util.Optional;

@Repository
public class StorageImageRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageImageEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageImageEntity e = new StorageImageEntity();
        e.setId(rs.getLong("id"));
        e.setImageUuid(rs.getString("image_uuid"));
        e.setMetadataUuid(rs.getString("metadata_uuid"));
        e.setWidth(rs.getInt("width"));
        e.setHeight(rs.getInt("height"));
        e.setFormat(rs.getString("format"));
        e.setColorSpace(rs.getString("color_space"));
        e.setHasAlpha(rs.getInt("has_alpha"));
        e.setOrientation(rs.getInt("orientation"));
        e.setDpi(rs.getInt("dpi"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("update_time");
        if (ut != null) e.setUpdateTime(ut.toLocalDateTime());
        e.setCreateUser(rs.getString("create_user"));
        e.setUpdateUser(rs.getString("update_user"));
        return e;
    };

    public StorageImageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 创建图片记录。
     */
    public StorageImage save(StorageImage domain) {
        StorageImageEntity entity = StorageImageConverter.toEntity(domain);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        String sql = "INSERT INTO storage_image (" +
                "image_uuid, metadata_uuid, width, height, format, color_space, has_alpha, " +
                "orientation, dpi, create_time, update_time, create_user, update_user" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, entity.getImageUuid());
            ps.setString(i++, entity.getMetadataUuid());
            ps.setInt(i++, entity.getWidth() != null ? entity.getWidth() : 0);
            ps.setInt(i++, entity.getHeight() != null ? entity.getHeight() : 0);
            ps.setString(i++, entity.getFormat());
            ps.setString(i++, entity.getColorSpace());
            ps.setInt(i++, entity.getHasAlpha() != null ? entity.getHasAlpha() : 0);
            ps.setInt(i++, entity.getOrientation() != null ? entity.getOrientation() : 1);
            ps.setInt(i++, entity.getDpi() != null ? entity.getDpi() : 72);
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
        return StorageImageConverter.toDomain(entity);
    }

    /**
     * 根据 image_uuid 查询。
     */
    public Optional<StorageImage> findByImageUuid(String imageUuid) {
        try {
            StorageImageEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_image WHERE image_uuid = ?", ROW_MAPPER, imageUuid);
            return Optional.ofNullable(StorageImageConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * 根据 metadata_uuid 查询。
     */
    public Optional<StorageImage> findByMetadataUuid(String metadataUuid) {
        try {
            StorageImageEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_image WHERE metadata_uuid = ?", ROW_MAPPER, metadataUuid);
            return Optional.ofNullable(StorageImageConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}
