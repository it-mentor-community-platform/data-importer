package com.itmentorcommunityplatform.dataimporter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Data Importer Api")
                        .version("1.0")
                        .description("Сервис для импорта данных студентов из Google Sheets")
                        .contact(new Contact()
                                .name("IT Mentor Community Platform")
                                .email("dev@example.com")));
    }
}