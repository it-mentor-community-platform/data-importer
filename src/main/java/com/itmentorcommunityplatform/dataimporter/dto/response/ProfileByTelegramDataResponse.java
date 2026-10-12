package com.itmentorcommunityplatform.dataimporter.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProfileByTelegramDataResponse(
        @JsonProperty("telegram_user_id")
        Long telegramUserId,

        @JsonProperty("details")
        ProfileIdentityDetails details
) {
}
