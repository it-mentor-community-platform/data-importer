package com.itmentorcommunityplatform.dataimporter.dto.request;

import java.util.List;

public record MentorUpsertRequestDto(
        Long mentorTelegramUserId,
        String telegramUrl,
        MentorDescriptionDto description,
        List<String> programmingLanguages,
        List<String> services

) {
}
