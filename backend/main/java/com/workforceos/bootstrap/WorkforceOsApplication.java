package com.workforceos.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.workforceos")
@EntityScan("com.workforceos")
@EnableJpaRepositories("com.workforceos")
@EnableCaching
@EnableScheduling
public class WorkforceOsApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkforceOsApplication.class, args);
    }
}
