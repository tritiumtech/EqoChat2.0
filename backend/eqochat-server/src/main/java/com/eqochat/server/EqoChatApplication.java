package com.eqochat.server;

import com.eqochat.business.chat.config.ChatModuleProperties;
import com.eqochat.business.project.config.ProjectModuleProperties;
import com.eqochat.business.world.config.WorldModuleProperties;
import com.eqochat.framework.file.FileStorageProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({
    "com.eqochat.server",
    "com.eqochat.framework",
    "com.eqochat.business"
})
@MapperScan("com.eqochat.business.**.mapper")
@EnableConfigurationProperties({
    WorldModuleProperties.class,
    ChatModuleProperties.class,
    ProjectModuleProperties.class,
    FileStorageProperties.class
})
public class EqoChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(EqoChatApplication.class, args);
    }
}
