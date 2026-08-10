package com.itmentorcommunityplatform.dataimporter.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record MentorUpsertRequestDto(
        Long mentorTelegramUserId,
        String telegramUrl,
        MentorDescriptionDto description,
        List<String> programmingLanguages,
        List<String> services

) {
}
