package com.itmentorcommunityplatform.dataimporter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The standard error response")
public record ErrorResponseDto(
        @Schema(description = "Error description", example = "Access denied: missing ADMIN role")
        String message
) {
}