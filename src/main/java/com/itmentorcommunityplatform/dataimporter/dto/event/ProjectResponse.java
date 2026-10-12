package com.itmentorcommunityplatform.dataimporter.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProjectResponse(

        @JsonProperty("id")
        Long id,

        @JsonProperty("author_telegram_user_id")
        Long authorTelegramUserId,

        @JsonProperty("github_repository_url")
        String githubRepositoryUrl,

        @JsonProperty("programming_language")
        String programmingLanguage,

        @JsonProperty("roadmap_project")
        String roadmapProject,

        @JsonProperty("added_timestamp")
        Long addedTimestamp
) {
}