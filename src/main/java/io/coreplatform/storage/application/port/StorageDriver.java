package io.coreplatform.storage.application.port;

import java.io.IOException;
import java.io.InputStream;

/**
 * 存储驱动接口 — 业务层只依赖此接口，不直接操作磁盘。
 * P0 仅实现 LocalDiskDriver，后续可扩展 DatabaseDriver / S3Driver / OSSDriver。
 */
public interface StorageDriver {

    /**
     * 上传文件内容到指定相对路径
     *
     * @param relativePath 相对目录（如 2026/07/15/）
     * @param storageName  存储文件名（如 uuid.bin）
     * @param in           文件输入流
     * @throws IOException 写入失败
     */
    void upload(String relativePath, String storageName, InputStream in) throws IOException;

    /**
     * 下载文件内容
     *
     * @param relativePath 相对目录
     * @param storageName  存储文件名
     * @return 文件输入流
     * @throws IOException 读取失败
     */
    InputStream download(String relativePath, String storageName) throws IOException;

    /**
     * 删除物理文件
     *
     * @param relativePath 相对目录
     * @param storageName  存储文件名
     * @return true 删除成功
     * @throws IOException 删除失败
     */
    boolean delete(String relativePath, String storageName) throws IOException;

    /**
     * 检查文件是否存在
     *
     * @param relativePath 相对目录
     * @param storageName  存储文件名
     * @return true 文件存在
     */
    boolean exists(String relativePath, String storageName);
}