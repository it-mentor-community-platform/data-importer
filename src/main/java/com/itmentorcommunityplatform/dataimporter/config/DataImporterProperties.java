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
    private String sheetRange;
    private List<Long> adminIds;
}
