package com.itmentorcommunityplatform.dataimporter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "dataimporter")
@Data
public class DataImporterProperties {
    private String spreadsheetId;
    private String sheetRangeTelegramAccounts;
    private String sheetRangeProjects;
    private List<Long> adminIds;
    private String authServiceBaseUrl;
    private String profileServiceBaseUrl;
    private String projectServiceBaseUrl;
}
