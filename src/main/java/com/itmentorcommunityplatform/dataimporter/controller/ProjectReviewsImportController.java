package com.itmentorcommunityplatform.dataimporter.controller;

import com.itmentorcommunityplatform.dataimporter.controller.api.ProjectReviewsImportApi;
import com.itmentorcommunityplatform.dataimporter.dto.response.ImportStartResponseDto;
import com.itmentorcommunityplatform.dataimporter.service.ProjectReviewImportService;
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
public class ProjectReviewsImportController implements ProjectReviewsImportApi {

    private final ProjectReviewImportService projectReviewImportService;

    @Override
    @PostMapping("/start-reviews-import")
    public ResponseEntity<ImportStartResponseDto> startProjectReviewsImport(
            @RequestHeader(value = "X-User-Roles", required = false) List<String> roles
    ) {
        RoleValidator.validateAdminRole(roles);

        projectReviewImportService.startImportAsync();

        return ResponseEntity.ok(new ImportStartResponseDto("import_started"));
    }
}