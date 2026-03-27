package com.eqochat;

import com.eqochat.config.WorldModuleProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WorldModuleProperties.class)
public class EqoChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(EqoChatApplication.class, args);
    }
}