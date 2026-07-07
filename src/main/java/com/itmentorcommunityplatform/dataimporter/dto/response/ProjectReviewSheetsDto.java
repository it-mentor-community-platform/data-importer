package com.itmentorcommunityplatform.dataimporter.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectReviewSheetsDto {

    @JsonProperty("project_github_repository_url")
    private String projectGithubRepositoryUrl;

    @JsonProperty("review_url")
    private String reviewUrl;

    @JsonProperty("reviewer_telegram_user_id")
    private Long reviewerTelegramUserId;

    @JsonProperty("added_timestamp")
    private Long addedTimestamp;
}