package com.itmentorcommunityplatform.dataimporter.controller;

import com.itmentorcommunityplatform.dataimporter.controller.api.ImportApi;
import com.itmentorcommunityplatform.dataimporter.dto.request.ImportExecutionStartRequestDto;
import com.itmentorcommunityplatform.dataimporter.dto.response.ImportExecutionResponseDto;
import com.itmentorcommunityplatform.dataimporter.service.ImportService;
import com.itmentorcommunityplatform.dataimporter.validator.RoleValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/data-importer")
@RequiredArgsConstructor
public class ImportController implements ImportApi {
    private final ImportService importService;

    @PostMapping("/import")
    public ResponseEntity<ImportExecutionResponseDto> startImport(
            @RequestHeader(value = "X-User-Roles", required = false) List<String> roles,
            @RequestBody @Valid ImportExecutionStartRequestDto request
    ) {
        RoleValidator.validateAdminRole(roles);
        ImportExecutionResponseDto response = importService.start(request.type());

        return ResponseEntity.accepted()
                .location(URI.create("/api/data-importer/imports/".concat(String.valueOf(response.id()))))
                .body(response);
    }
}