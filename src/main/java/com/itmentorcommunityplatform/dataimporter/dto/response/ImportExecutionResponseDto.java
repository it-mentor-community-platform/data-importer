package com.itmentorcommunityplatform.dataimporter.dto.response;

import com.itmentorcommunityplatform.dataimporter.model.ImportExecutionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "State of an import execution")
public record ImportExecutionResponseDto(

        @Schema(description = "Execution id", example = "125")
        Long id,

        @Schema(description = "Import type code", example = "USERS")
        String type,

        @Schema(description = "Execution status", example = "RUNNING")
        ImportExecutionStatus status
) {
}