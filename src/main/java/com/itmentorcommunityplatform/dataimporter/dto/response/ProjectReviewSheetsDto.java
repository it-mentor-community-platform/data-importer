package com.itmentorcommunityplatform.dataimporter.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectReviewSheetsDto {

    private String period;

    private String project;

    @JsonProperty("programming_language")
    private String programmingLanguage;

    @JsonProperty("github_repository_url")
    private String githubRepositoryUrl;

    @JsonProperty("review_type")
    private String reviewType;

    @JsonProperty("review_url")
    private String reviewUrl;

    @JsonProperty("reviewer_name")
    private String reviewerName;

    @JsonProperty("reviewer_telegram_username")
    private String reviewerTelegramUsername;

    @JsonProperty("reviewer_telegram_profile_url")
    private String reviewerTelegramProfileUrl;
}