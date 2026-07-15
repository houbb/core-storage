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
}
