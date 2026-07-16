package io.coreplatform.storage.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "core.storage")
public class StorageProperties {

    private String driver = "local";

    private Local local = new Local();

    private Image image = new Image();

    private Database database = new Database();

    private Replication replication = new Replication();

    private Lifecycle lifecycle = new Lifecycle();

    // ---- getters & setters ----

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public Replication getReplication() {
        return replication;
    }

    public void setReplication(Replication replication) {
        this.replication = replication;
    }

    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public static class Local {
        private String root = "./data/storage";
        private boolean datePath = true;

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }

        public boolean isDatePath() {
            return datePath;
        }

        public void setDatePath(boolean datePath) {
            this.datePath = datePath;
        }
    }

    public static class Image {
        /** 图片最大尺寸限制（宽或高的像素值），超过此值拒绝处理 */
        private int maxDimension = 10000;

        public int getMaxDimension() {
            return maxDimension;
        }

        public void setMaxDimension(int maxDimension) {
            this.maxDimension = maxDimension;
        }
    }

    public static class Database {
        /** 数据库驱动是否启用 */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Replication {
        /** 调度器扫描间隔（毫秒），默认 5000 */
        private long schedulerIntervalMs = 5000;

        /** 每批最大处理任务数 */
        private int maxBatchSize = 10;

        /** 同步后是否校验 SHA-256 */
        private boolean checksumVerify = true;

        public long getSchedulerIntervalMs() {
            return schedulerIntervalMs;
        }

        public void setSchedulerIntervalMs(long schedulerIntervalMs) {
            this.schedulerIntervalMs = schedulerIntervalMs;
        }

        public int getMaxBatchSize() {
            return maxBatchSize;
        }

        public void setMaxBatchSize(int maxBatchSize) {
            this.maxBatchSize = maxBatchSize;
        }

        public boolean isChecksumVerify() {
            return checksumVerify;
        }

        public void setChecksumVerify(boolean checksumVerify) {
            this.checksumVerify = checksumVerify;
        }
    }

    public static class Lifecycle {
        /** 调度器扫描间隔（毫秒），默认 60000（1 分钟） */
        private long schedulerIntervalMs = 60000;

        /** 每批最大处理资源数 */
        private int maxBatchSize = 50;

        /** 删除前的宽限期（天），默认 7 天 */
        private int deleteGracePeriodDays = 7;

        public long getSchedulerIntervalMs() {
            return schedulerIntervalMs;
        }

        public void setSchedulerIntervalMs(long schedulerIntervalMs) {
            this.schedulerIntervalMs = schedulerIntervalMs;
        }

        public int getMaxBatchSize() {
            return maxBatchSize;
        }

        public void setMaxBatchSize(int maxBatchSize) {
            this.maxBatchSize = maxBatchSize;
        }

        public int getDeleteGracePeriodDays() {
            return deleteGracePeriodDays;
        }

        public void setDeleteGracePeriodDays(int deleteGracePeriodDays) {
            this.deleteGracePeriodDays = deleteGracePeriodDays;
        }
    }
}
