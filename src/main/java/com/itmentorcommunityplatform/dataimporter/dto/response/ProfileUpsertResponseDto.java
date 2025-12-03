package com.itmentorcommunityplatform.dataimporter.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProfileUpsertResponseDto(
        @JsonProperty("github_profile_url")
        String githubProfileUrl,
        @JsonProperty("telegram_url")
        String telegramUrl
) {
}
