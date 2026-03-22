package com.example.sftpwatcher.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SftpValidationInitializer {

    @Bean
    ApplicationRunner validateSftpConfiguration(SftpConfigurationValidator validator) {
        return args -> validator.validate();
    }
}
