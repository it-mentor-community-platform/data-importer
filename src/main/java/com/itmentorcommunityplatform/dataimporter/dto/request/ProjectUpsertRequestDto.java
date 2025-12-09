package com.itmentorcommunityplatform.dataimporter.dto.request;

public record ProjectUpsertRequestDto(
        Long authorTelegramUserId,
        String githubRepositoryUrl,
        String programmingLanguage,
        String roadmapProject,
        Long addedTimestamp,
        String projectSourceType) {
}
