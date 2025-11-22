package com.itmentorcommunityplatform.dataimporter.controller.api;

import com.itmentorcommunityplatform.dataimporter.dto.ErrorResponseDto;
import com.itmentorcommunityplatform.dataimporter.dto.ImportStartResponseDto;
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

@Tag(name = "User Import", description = "API for managing user imports")
public interface UserImportApi {

    @Operation(
            summary = "Starting user import",
            description = "Asynchronously starts the process of reading the Google Spreadsheet and creating/updating users in the Auth Service."
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
                    description = "Access is denied (there is no ADMIN role)",
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
    ResponseEntity<?> startUsersImport(
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
