package io.coreplatform.storage.application.port;

import io.coreplatform.storage.application.domain.enums.DriverType;
import io.coreplatform.storage.application.domain.enums.StorageCapability;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * 统一存储驱动 SPI（Storage Service Provider Interface）。
 * 业务层只依赖此接口，不直接操作磁盘/数据库/对象存储。
 * <p>
 * P0 阶段仅定义了 upload/download/delete/exists 四个方法。
 * P5 扩展为完整的驱动能力，包括 move/copy/metadata/url/health 及类型与能力声明。
 * <p>
 * 设计原则：
 * <ul>
 *     <li>Storage Runtime 永远只依赖 StorageDriver SPI，禁止依赖任何具体 Driver。</li>
 *     <li>新增存储能力只能通过新增 Driver，不允许修改 Runtime 或业务代码。</li>
 * </ul>
 */
public interface StorageDriver {

    /**
     * 驱动类型标识。
     */
    DriverType type();

    /**
     * 驱动支持的能力集合。
     * Runtime 根据此声明自动启用或隐藏相关功能。
     */
    Set<StorageCapability> capabilities();

    // ---- 基础 CRUD (P0) ----

    /**
     * 上传文件内容到指定相对路径。
     *
     * @param relativePath 相对目录（如 2026/07/15/）
     * @param storageName  存储文件名（如 uuid.bin）
     * @param in           文件输入流
     * @throws IOException 写入失败
     */
    void upload(String relativePath, String storageName, InputStream in) throws IOException;

    /**
     * 下载文件内容。
     *
     * @param relativePath 相对目录
     * @param storageName  存储文件名
     * @return 文件输入流
     * @throws IOException 读取失败
     */
    InputStream download(String relativePath, String storageName) throws IOException;

    /**
     * 删除物理文件。
     *
     * @param relativePath 相对目录
     * @param storageName  存储文件名
     * @return true 删除成功
     * @throws IOException 删除失败
     */
    boolean delete(String relativePath, String storageName) throws IOException;

    /**
     * 检查文件是否存在。
     *
     * @param relativePath 相对目录
     * @param storageName  存储文件名
     * @return true 文件存在
     */
    boolean exists(String relativePath, String storageName);

    // ---- 文件操作 (P5 扩展) ----

    /**
     * 移动文件（重命名/换目录）。
     *
     * @param sourceRelativePath 源文件相对目录
     * @param sourceStorageName  源文件名
     * @param targetRelativePath 目标文件相对目录
     * @param targetStorageName  目标文件名
     * @return true 移动成功
     * @throws IOException 操作失败
     */
    boolean move(String sourceRelativePath, String sourceStorageName,
                 String targetRelativePath, String targetStorageName) throws IOException;

    /**
     * 复制文件。
     *
     * @param sourceRelativePath 源文件相对目录
     * @param sourceStorageName  源文件名
     * @param targetRelativePath 目标文件相对目录
     * @param targetStorageName  目标文件名
     * @return 目标文件的存储标识
     * @throws IOException 操作失败
     */
    String copy(String sourceRelativePath, String sourceStorageName,
                String targetRelativePath, String targetStorageName) throws IOException;

    // ---- 元数据与访问 (P5 扩展) ----

    /**
     * 获取文件元数据。
     * 返回的 Map 至少包含：
     * <ul>
     *     <li>{@code size} — 文件大小（字节，Long）</li>
     *     <li>{@code lastModified} — 最后修改时间（Long，epoch millis）</li>
     *     <li>{@code contentType} — MIME 类型（String）</li>
     * </ul>
     *
     * @param relativePath 相对目录
     * @param storageName  存储文件名
     * @return 元数据键值对
     * @throws IOException 查询失败
     */
    Map<String, Object> metadata(String relativePath, String storageName) throws IOException;

    /**
     * 生成文件访问 URL。
     * 对于本地驱动返回 file:// 绝对路径。
     * 对于对象存储返回预签名 URL。
     * 对于数据库驱动返回内部 API 下载地址。
     *
     * @param relativePath 相对目录
     * @param storageName  存储文件名
     * @param ttlSeconds   URL 有效期（秒），0 或负数表示永久
     * @return 可访问的文件 URL
     */
    String url(String relativePath, String storageName, long ttlSeconds);

    // ---- 健康检查 (P5 扩展) ----

    /**
     * 健康检查 — 验证驱动后端是否可达且可用。
     * 例如：Local 检查磁盘可读写，S3 检查 Bucket 可达，Database 检查连接可用。
     *
     * @return true 驱动后端健康
     */
    boolean health();
}
