package com.itmentorcommunityplatform.dataimporter.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProjectCreatedEvent {

    @JsonProperty("author_telegram_user_id")
    private Long authorTelegramUserId;

    @JsonProperty("author_telegram_profile_url")
    private String authorTelegramProfileUrl;

    @JsonProperty("github_repository_url")
    private String githubRepositoryUrl;

    @JsonProperty("programming_language")
    private String programmingLanguage;

    @JsonProperty("roadmap_project")
    private String roadmapProject;

    @JsonProperty("added_timestamp")
    private Long addedTimestamp;

    @JsonProperty("project_source_type")
    private String projectSourceType;

}