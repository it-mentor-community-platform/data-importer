package com.itmentorcommunityplatform.dataimporter.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProjectUpsertRequestDto(
        @JsonProperty("author_telegram_user_id")
        Long authorTelegramUserId,

        @JsonProperty("github_repository_url")
        String githubRepositoryUrl,

        @JsonProperty("programming_language")
        String programmingLanguage,

        @JsonProperty("roadmap_project")
        String roadmapProject,

        @JsonProperty("telegram_user_id")
        Long telegramUserId,

        @JsonProperty("telegram_username")
        String telegramUsername,

        @JsonProperty("added_timestamp")
        Long addedTimestamp,

        @JsonProperty("project_source_type")
        String projectSourceType) {
}
