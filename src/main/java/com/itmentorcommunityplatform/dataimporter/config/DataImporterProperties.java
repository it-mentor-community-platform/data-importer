package com.itmentorcommunityplatform.dataimporter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "dataimporter")
@Data
public class DataImporterProperties {

    private String projectSpreadsheetId;
    private String mentorSpreadsheetId;
    private String guaranteedReviewsSpreadsheetId;

    private String sheetRangeTelegramAccounts;
    private String sheetRangeProjects;
    private String sheetRangeGuaranteedReviews;
    private String sheetRangeProjectReviews;
    private String sheetRangeMentors;

    private String authServiceBaseUrl;
    private String profileServiceBaseUrl;
    private String projectServiceBaseUrl;
    private String mentorServiceBaseUrl;
    private String intervalRepetitionServiceBaseUrl;


    private List<InterviewQuestionSource> interviewQuestionSources;

    @Data
    public static class InterviewQuestionSource {

        private String specialization;
        private String spreadsheetId;
    }

}
