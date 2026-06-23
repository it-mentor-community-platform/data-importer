package com.itmentorcommunityplatform.dataimporter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "dataimporter")
@Data
public class DataImporterProperties {

    private String projectSpreadsheetId;
    private String mentorSpreadsheetId;

    private String sheetRangeTelegramAccounts;
    private String sheetRangeProjects;
    private String sheetRangeReviews;
    private String sheetRangeProjectReviews;

    private String authServiceBaseUrl;
    private String profileServiceBaseUrl;
    private String projectServiceBaseUrl;
    private String mentorServiceBaseUrl;
}
