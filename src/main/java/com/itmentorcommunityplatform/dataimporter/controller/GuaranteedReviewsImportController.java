package com.itmentorcommunityplatform.dataimporter.controller;

import com.itmentorcommunityplatform.dataimporter.controller.api.GuaranteedReviewsImportApi;
import com.itmentorcommunityplatform.dataimporter.dto.response.ImportStartResponseDto;
import com.itmentorcommunityplatform.dataimporter.service.GuaranteedReviewsImportService;
import com.itmentorcommunityplatform.dataimporter.validator.RoleValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/data-importer")
public class GuaranteedReviewsImportController implements GuaranteedReviewsImportApi {

    private final GuaranteedReviewsImportService guaranteedReviewsImportService;

    @PostMapping("/start-guaranteed-reviews-import")
    public ResponseEntity<?> startGuaranteedReviewsImport(
            @RequestHeader(value = "X-User-Roles", required = false) List<String> roles
    ) {
        RoleValidator.validateAdminRole(roles);

        guaranteedReviewsImportService.startImportAsync();

        return ResponseEntity.ok(new ImportStartResponseDto("import_started"));
    }
}
