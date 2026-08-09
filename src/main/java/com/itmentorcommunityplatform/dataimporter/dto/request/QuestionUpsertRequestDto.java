package com.itmentorcommunityplatform.dataimporter.dto.request;

public record QuestionUpsertRequestDto(

        String specialization,

        String category,

        String title,

        String answer

) {
}
