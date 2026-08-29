package com.workforceos.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.workforceos")
@EntityScan("com.workforceos")
@EnableJpaRepositories("com.workforceos")
@EnableCaching
public class WorkforceOsApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkforceOsApplication.class, args);
    }
}
