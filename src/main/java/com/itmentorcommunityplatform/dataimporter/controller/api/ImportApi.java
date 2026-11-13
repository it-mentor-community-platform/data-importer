package com.itmentorcommunityplatform.dataimporter.controller.api;

import com.itmentorcommunityplatform.dataimporter.dto.request.ImportExecutionStartRequestDto;
import com.itmentorcommunityplatform.dataimporter.dto.response.ErrorResponseDto;
import com.itmentorcommunityplatform.dataimporter.dto.response.ImportExecutionResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Data import", description = "Admin endpoints for running data imports")
public interface ImportApi {

    @Operation(
            summary = "Start import of the given type",
            description = "Creates an ImportExecution with status RUNNING and starts the import asynchronously"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Import started",
                    headers = @Header(
                            name = "Location",
                            description = "URL for getting the import state",
                            schema = @Schema(type = "string", example = "/api/data-importer/imports/125")
                    ),
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ImportExecutionResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body or unknown import type",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = {
                                    @ExampleObject(name = "Unknown type",
                                            value = "{\"message\": \"Unknown import type: WRONG\"}"),
                                    @ExampleObject(name = "Blank type",
                                            value = "{\"message\": \"type: must not be blank\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User has no ADMIN role",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(
                                    value = "{\"message\": \"Access denied: missing ADMIN role\"}"
                            )

                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Import of this type is already running",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(
                                    value = "{\"message\": \"Import of type USERS is already running\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(
                                    value = "{\"message\": \"An unexpected error occurred. Please try again later.\"}"
                            )
                    )
            )
    })
    ResponseEntity<ImportExecutionResponseDto> startImport(
            @Parameter(
                    name = "X-User-Roles",
                    in = ParameterIn.HEADER,
                    description = "The list of user roles. The ADMIN role is required.",
                    required = true,
                    schema = @Schema(type = "string", example = "ADMIN")
            )
            List<String> roles,
            ImportExecutionStartRequestDto request
    );
}