package com.example.sftpwatcher;

import com.example.sftpwatcher.config.AppSftpProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = AppSftpProperties.class)
public class SftpWatcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(SftpWatcherApplication.class, args);
    }
}
