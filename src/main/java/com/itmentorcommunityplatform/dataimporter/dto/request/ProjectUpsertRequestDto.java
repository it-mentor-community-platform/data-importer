package com.itmentorcommunityplatform.dataimporter.dto.request;

public record ProjectUpsertRequestDto(
        Long authorTelegramUserId,
        String githubRepositoryUrl,
        String programmingLanguage,
        String roadmapProject,
        Long telegramUserId,
        String telegramUsername,
        Long addedTimestamp,
        String projectSourceType) {
}
