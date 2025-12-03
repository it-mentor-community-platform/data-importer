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

@Tag(name = "Profile Import", description = "API for managing profile imports")
public interface ProfileImportApi {

    @Operation(
            summary = "Starting profiles import",
            description = "Asynchronously starts reading the Google Spreadsheet and creating/updating profiles. Requires ADMIN role.",
            parameters = {
                    @Parameter(
                            name = "X-User-Roles",
                            description = "Comma-separated list of user roles. Must include ADMIN.",
                            required = true,
                            in = ParameterIn.HEADER
                    )
            }
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
                    description = "Access denied: missing X-User-Roles header or ADMIN role",
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
    ResponseEntity<?> startProfileImport(
            @Parameter(
                    name = "X-User-Roles",
                    in = ParameterIn.HEADER,
                    description = "The list of user roles. The ADMIN role is required.",
                    required = true,
                    schema = @Schema(type = "string", example = "ADMIN")
            )
            List<String> roles
    );
}
