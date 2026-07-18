package io.coreplatform.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoreStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreStorageApplication.class, args);
    }
}
