package io.coreplatform.storage.application.port;

import java.io.InputStream;

/**
 * 内容扫描 SPI — P9 定义接口，具体扫描逻辑由外部插件实现。
 * <p>
 * 实现方（如病毒扫描、敏感词检测、图片审核等）实现此接口并注册为 Spring Bean，
 * 上传流程中会通过此 SPI 触发扫描。
 */
public interface ScanProvider {

    /**
     * 扫描指定内容。
     *
     * @param resourceUuid 资源 UUID
     * @param content      文件内容流
     * @param mimeType     文件 MIME 类型
     * @return 扫描结果
     */
    ScanResult scan(String resourceUuid, InputStream content, String mimeType);

    /**
     * 此提供者支持的扫描类型。
     */
    String scanType();

    /**
     * 扫描结果。
     */
    record ScanResult(boolean passed, String message) {

        public boolean isClean() {
            return passed;
        }

        public static ScanResult clean() {
            return new ScanResult(true, "ok");
        }

        public static ScanResult infected(String message) {
            return new ScanResult(false, message);
        }
    }
}