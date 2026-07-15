package io.coreplatform.storage.infrastructure.config;

import io.coreplatform.storage.application.port.StorageDriver;
import io.coreplatform.storage.infrastructure.driver.LocalDiskDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Bean
    public StorageDriver storageDriver(StorageProperties properties) {
        return new LocalDiskDriver(properties.getLocal());
    }
}
