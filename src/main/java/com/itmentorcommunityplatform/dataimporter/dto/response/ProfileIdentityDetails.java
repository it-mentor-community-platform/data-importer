package com.itmentorcommunityplatform.dataimporter.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProfileIdentityDetails(
        @JsonProperty("github_profile_url")
        String githubProfileUrl,

        @JsonProperty("telegram_url")
        String telegramUrl,

        @JsonProperty("first_name")
        String firstName,

        @JsonProperty("last_name")
        String lastName
) {
}
