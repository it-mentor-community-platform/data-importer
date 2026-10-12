package com.itmentorcommunityplatform.dataimporter.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

public record ProjectCreatedEvent(

        @JsonProperty("author_telegram_user_id")
        Long authorTelegramUserId,

        @JsonProperty("author_telegram_profile_url")
        String authorTelegramProfileUrl,

        @JsonProperty("github_repository_url")
        String githubRepositoryUrl,

        @JsonProperty("programming_language")
        String programmingLanguage,

        @JsonProperty("roadmap_project")
        String roadmapProject,

        @JsonProperty("added_timestamp")
        Long addedTimestamp,

        @JsonProperty("project_source_type")
        String projectSourceType
) {
}