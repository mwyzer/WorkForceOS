package com.workforceos.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.workforceos")
public class WorkforceOsApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkforceOsApplication.class, args);
    }
}
