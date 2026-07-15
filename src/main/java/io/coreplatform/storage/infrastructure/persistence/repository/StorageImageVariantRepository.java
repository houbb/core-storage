package io.coreplatform.storage.infrastructure.persistence.repository;

import io.coreplatform.storage.application.domain.ImageVariant;
import io.coreplatform.storage.infrastructure.persistence.converter.StorageImageVariantConverter;
import io.coreplatform.storage.infrastructure.persistence.entity.StorageImageVariantEntity;
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
public class StorageImageVariantRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<StorageImageVariantEntity> ROW_MAPPER = (rs, rowNum) -> {
        StorageImageVariantEntity e = new StorageImageVariantEntity();
        e.setId(rs.getLong("id"));
        e.setImageUuid(rs.getString("image_uuid"));
        e.setVariantName(rs.getString("variant_name"));
        e.setMetadataUuid(rs.getString("metadata_uuid"));
        e.setWidth(rs.getInt("width"));
        e.setHeight(rs.getInt("height"));
        e.setFormat(rs.getString("format"));
        e.setFileSize(rs.getLong("file_size"));
        Timestamp ct = rs.getTimestamp("create_time");
        if (ct != null) e.setCreateTime(ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("update_time");
        if (ut != null) e.setUpdateTime(ut.toLocalDateTime());
        e.setCreateUser(rs.getString("create_user"));
        e.setUpdateUser(rs.getString("update_user"));
        return e;
    };

    public StorageImageVariantRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 创建变体记录。
     */
    public ImageVariant save(ImageVariant domain) {
        StorageImageVariantEntity entity = StorageImageVariantConverter.toEntity(domain);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        String sql = "INSERT INTO storage_image_variant (" +
                "image_uuid, variant_name, metadata_uuid, width, height, format, file_size, " +
                "create_time, update_time, create_user, update_user" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, entity.getImageUuid());
            ps.setString(i++, entity.getVariantName());
            ps.setString(i++, entity.getMetadataUuid());
            ps.setInt(i++, entity.getWidth() != null ? entity.getWidth() : 0);
            ps.setInt(i++, entity.getHeight() != null ? entity.getHeight() : 0);
            ps.setString(i++, entity.getFormat());
            ps.setLong(i++, entity.getFileSize() != null ? entity.getFileSize() : 0);
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
        return StorageImageVariantConverter.toDomain(entity);
    }

    /**
     * 查询某个图片的所有变体。
     */
    public List<ImageVariant> findByImageUuid(String imageUuid) {
        List<StorageImageVariantEntity> entities = jdbc.query(
                "SELECT * FROM storage_image_variant WHERE image_uuid = ? ORDER BY create_time ASC",
                ROW_MAPPER, imageUuid);
        return entities.stream().map(StorageImageVariantConverter::toDomain).toList();
    }

    /**
     * 查询某个图片的指定变体。
     */
    public Optional<ImageVariant> findByImageUuidAndVariantName(String imageUuid, String variantName) {
        try {
            StorageImageVariantEntity e = jdbc.queryForObject(
                    "SELECT * FROM storage_image_variant WHERE image_uuid = ? AND variant_name = ?",
                    ROW_MAPPER, imageUuid, variantName);
            return Optional.ofNullable(StorageImageVariantConverter.toDomain(e));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * 检查变体是否已存在。
     */
    public boolean existsByImageUuidAndVariantName(String imageUuid, String variantName) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM storage_image_variant WHERE image_uuid = ? AND variant_name = ?",
                Integer.class, imageUuid, variantName);
        return count != null && count > 0;
    }

    /**
     * 删除图片的所有变体记录。
     */
    public int deleteByImageUuid(String imageUuid) {
        return jdbc.update("DELETE FROM storage_image_variant WHERE image_uuid = ?", imageUuid);
    }
}
