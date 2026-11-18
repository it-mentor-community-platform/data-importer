package com.itmentorcommunityplatform.dataimporter.dto.response;

import com.itmentorcommunityplatform.dataimporter.model.ImportExecutionStatus;
import com.itmentorcommunityplatform.dataimporter.model.SourceReadStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current state of an import execution")
public record ImportExecutionStatusResponseDto(
        @Schema(description = "Execution ID", example = "125")
        Long id,

        @Schema(description = "Import type code", example = "USERS")
        String type,

        @Schema(description = "Execution status", example = "SUCCESS")
        ImportExecutionStatus status,

        @Schema(description = "Start time in Unix seconds", example = "1761834600")
        Long startedAt,

        @Schema(description = "Finish time in Unix seconds. Null while running.",
                example = "1761834867", nullable = true)
        Long finishedAt,

        @Schema(description = "Number of successfully imported entities", example = "498")
        int successCount,

        @Schema(description = "Number of entities that failed to import", example = "2")
        int errorCount,

        @Schema(description = "Source read result. Null until the result is recorded.",
                example = "SUCCESS", nullable = true)
        SourceReadStatus sourceReadStatus
) {
}