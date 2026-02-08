package com.itmentorcommunityplatform.dataimporter.controller;

import com.itmentorcommunityplatform.dataimporter.controller.api.ProfileImportApi;
import com.itmentorcommunityplatform.dataimporter.dto.response.ImportStartResponseDto;
import com.itmentorcommunityplatform.dataimporter.service.ProfileImportService;
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
public class ProfileImportController implements ProfileImportApi {

    private final ProfileImportService profileImportService;

    @Override
    @PostMapping("/start-profiles-import")
    public ResponseEntity<ImportStartResponseDto> startProfileImport(
            @RequestHeader(value = "X-User-Roles", required = false) List<String> roles
    ){
        RoleValidator.validateAdminRole(roles);

        profileImportService.startImportAsync();

        return ResponseEntity.ok(new ImportStartResponseDto("import_started"));
    }
}
