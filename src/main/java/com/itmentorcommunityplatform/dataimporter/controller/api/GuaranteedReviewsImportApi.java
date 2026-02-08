package com.itmentorcommunityplatform.dataimporter.controller.api;

import com.itmentorcommunityplatform.dataimporter.dto.response.ErrorResponseDto;
import com.itmentorcommunityplatform.dataimporter.dto.response.ImportStartResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Guaranteed Reviews Import", description = "API for managing guaranteed reviews imports")
public interface GuaranteedReviewsImportApi {

    @Operation(
            summary = "Starting guaranteed reviews import",
            description = "Asynchronously starts the process of reading the Google Spreadsheet and updating prices for guaranteed reviews in the Mentor Service."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Import started successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ImportStartResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access is denied (missing ADMIN role)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    ResponseEntity<?> startGuaranteedReviewsImport(
            @Parameter(
                    name = "X-User-Roles",
                    in = ParameterIn.HEADER,
                    description = "The list of user roles. The ADMIN role is required.",
                    required = true,
                    schema = @Schema(type = "array", implementation = String.class, example = "[\"ADMIN\"]")
            )
            List<String> roles
    );
}