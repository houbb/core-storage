package io.coreplatform.storage.infrastructure.driver;

import io.coreplatform.storage.application.port.StorageDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 驱动运行时注册中心。
 * <p>
 * 所有驱动在启动时注册到此中心，后续通过 driverName 查找。
 * 支持运行时动态注册/注销，为热插拔提供基础。
 */
@Component
public class DriverRegistry {

    private static final Logger log = LoggerFactory.getLogger(DriverRegistry.class);

    private final Map<String, StorageDriver> drivers = new LinkedHashMap<>();

    /**
     * 注册一个驱动。
     *
     * @param name   驱动名称
     * @param driver 驱动实例
     */
    public void register(String name, StorageDriver driver) {
        drivers.put(name, driver);
        log.info("Driver registered: name={}, type={}, health={}", name, driver.type(), driver.health());
    }

    /**
     * 获取驱动，不存在返回 null。
     */
    public StorageDriver get(String name) {
        return drivers.get(name);
    }

    /**
     * 获取驱动，不存在抛出异常。
     */
    public StorageDriver getRequired(String name) {
        StorageDriver driver = drivers.get(name);
        if (driver == null) {
            throw new DriverNotFoundException("Driver not found: " + name);
        }
        return driver;
    }

    /**
     * 注销驱动。
     */
    public void unregister(String name) {
        StorageDriver removed = drivers.remove(name);
        if (removed != null) {
            log.info("Driver unregistered: name={}, type={}", name, removed.type());
        }
    }

    /**
     * 是否包含指定驱动。
     */
    public boolean contains(String name) {
        return drivers.containsKey(name);
    }

    /**
     * 所有已注册驱动名称。
     */
    public Set<String> listNames() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(drivers.keySet()));
    }

    /**
     * 所有已注册驱动。
     */
    public Map<String, StorageDriver> listAll() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(drivers));
    }

    // ---- exception ----

    public static class DriverNotFoundException extends RuntimeException {
        public DriverNotFoundException(String message) {
            super(message);
        }
    }
}
