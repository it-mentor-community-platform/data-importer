package com.itmentorcommunityplatform.dataimporter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response to successful start of the import procedure")
public record ImportStartResponseDto(
        @Schema(description = "Operation status", example = "import_started")
        String status
) {
}