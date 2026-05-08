package com.affordmed.vehicle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.affordmed")
@ConfigurationPropertiesScan(basePackages = "com.affordmed")
public class VehicleMaintenanceSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(VehicleMaintenanceSchedulerApplication.class, args);
    }
}
