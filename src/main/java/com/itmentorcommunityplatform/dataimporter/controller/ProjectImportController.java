package com.itmentorcommunityplatform.dataimporter.controller;

import com.itmentorcommunityplatform.dataimporter.controller.api.ProjectImportApi;
import com.itmentorcommunityplatform.dataimporter.dto.response.ImportStartResponseDto;
import com.itmentorcommunityplatform.dataimporter.service.ProjectImportService;
import com.itmentorcommunityplatform.dataimporter.validator.RoleValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/data-importer")
@RequiredArgsConstructor
public class ProjectImportController implements ProjectImportApi {
    private final ProjectImportService projectImportService;

    @Override
    @PostMapping("/start-projects-import")
    public ResponseEntity<ImportStartResponseDto> startProjectImport(
            @RequestHeader(value = "X-User-Roles", required = false) List<String> roles) {

        RoleValidator.validateAdminRole(roles);

        projectImportService.startImportAsync();

        return ResponseEntity.ok(new ImportStartResponseDto("import_started"));
    }
}
