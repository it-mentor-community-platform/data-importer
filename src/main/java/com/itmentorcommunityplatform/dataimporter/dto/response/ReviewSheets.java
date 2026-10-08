package com.itmentorcommunityplatform.dataimporter.dto.response;


public record ReviewSheets(
        String period,
        String roadmapProject,
        String programmingLanguage,
        String githubRepositoryUrl,
        String reviewType,
        String reviewUrl,
        String reviewAuthorName,
        String reviewAuthorTelegramName,
        String reviewAuthorTelegramUrl
) {
}
