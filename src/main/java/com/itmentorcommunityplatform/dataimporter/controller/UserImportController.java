package com.itmentorcommunityplatform.dataimporter.controller;

import com.itmentorcommunityplatform.dataimporter.controller.api.UserImportApi;
import com.itmentorcommunityplatform.dataimporter.dto.response.ImportStartResponseDto;
import com.itmentorcommunityplatform.dataimporter.service.UserImportService;
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
public class UserImportController implements UserImportApi {

    private final UserImportService importService;

    @Override
    @PostMapping("/start-users-import")
    public ResponseEntity<ImportStartResponseDto> startUsersImport(
            @RequestHeader(value = "X-User-Roles", required = false) List<String> roles) {

        RoleValidator.validateAdminRole(roles);

        importService.startImportAsync();

        return ResponseEntity.ok(new ImportStartResponseDto("import_started"));
    }
}