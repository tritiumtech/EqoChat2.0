package com.eqochat;

import com.eqochat.config.ChatModuleProperties;
import com.eqochat.config.FileStorageProperties;
import com.eqochat.config.ProjectModuleProperties;
import com.eqochat.config.WorldModuleProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({WorldModuleProperties.class, ChatModuleProperties.class, ProjectModuleProperties.class, FileStorageProperties.class})
public class EqoChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(EqoChatApplication.class, args);
    }
}