package com.itmentorcommunityplatform.dataimporter.controller;

import com.itmentorcommunityplatform.dataimporter.controller.api.ProjectReviewsImportApi;
import com.itmentorcommunityplatform.dataimporter.dto.response.ImportStartResponseDto;
import com.itmentorcommunityplatform.dataimporter.service.ProjectReviewsImportService;
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

    private final ProjectReviewsImportService projectReviewsImportService;

    @Override
    @PostMapping("/start-reviews-import")
    public ResponseEntity<ImportStartResponseDto> startProjectReviewsImport(
            @RequestHeader(value = "X-User-Roles", required = false) List<String> roles
    ) {
        RoleValidator.validateAdminRole(roles);

        projectReviewsImportService.startImportAsync();

        return ResponseEntity.ok(new ImportStartResponseDto("import_started"));
    }
}