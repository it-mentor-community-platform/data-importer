package com.itmentorcommunityplatform.dataimporter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to start an import")
public record ImportExecutionStartRequestDto(

        @Schema(description = "Import type code",
                example = "USERS",
                allowableValues = {"USERS", "PROFILES", "PROJECTS", "REVIEWS", "MENTORS",
                        "GUARANTEED_REVIEWS", "QUESTIONS"})
        @NotBlank(message = "must not be blank")
        String type
) {
}