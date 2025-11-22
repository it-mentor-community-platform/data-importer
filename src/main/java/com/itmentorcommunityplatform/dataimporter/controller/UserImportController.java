package com.itmentorcommunityplatform.dataimporter.controller;

import com.itmentorcommunityplatform.dataimporter.controller.api.UserImportApi;
import com.itmentorcommunityplatform.dataimporter.dto.ErrorResponseDto;
import com.itmentorcommunityplatform.dataimporter.dto.ImportStartResponseDto;
import com.itmentorcommunityplatform.dataimporter.service.UserImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/data-importer")
@RequiredArgsConstructor
public class UserImportController implements UserImportApi {

    private final UserImportService importService;

    @Override
    @PostMapping("/start-users-import")
    public ResponseEntity<?> startUsersImport(
            @RequestHeader(value = "X-User-Roles", required = false) List<String> roles) {

        if (roles == null || roles.stream().noneMatch(r -> r.equalsIgnoreCase("ADMIN"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponseDto("Access denied: missing ADMIN role in X-User-Roles header"));
        }
        importService.startImportAsync();
        return ResponseEntity.ok(new ImportStartResponseDto("import_started"));
    }
}