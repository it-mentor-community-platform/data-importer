package com.itmentorcommunityplatform.dataimporter.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReviewCreatedEvent(

        @JsonProperty("id")
        Long id,

        @JsonProperty("reviewer_telegram_user_id")
        Long reviewerTelegramUserId,

        @JsonProperty("reviewer_telegram_profile_url")
        String reviewerTelegramProfileUrl,

        @JsonProperty("url")
        String url,

        @JsonProperty("added_timestamp")
        Long addedTimestamp,

        @JsonProperty("project")
        ProjectResponse project,

        @JsonProperty("review_source_type")
        String reviewSourceType
) {
}
