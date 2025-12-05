package com.itmentorcommunityplatform.dataimporter.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record ProfileUpsertRequestDto(

        @NotNull
        @JsonProperty("telegram_user_id")
        Long telegramUserId,

        DetailsDto details
) {
    public record DetailsDto(

            @NotNull
            @JsonProperty("telegram_url")
            String telegramUrl,

            @NotNull
            @JsonProperty("github_profile_url")
            String githubProfileUrl
    ) {}
}