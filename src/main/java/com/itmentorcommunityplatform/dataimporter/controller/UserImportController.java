package com.itmentorcommunityplatform.dataimporter.controller;

import com.itmentorcommunityplatform.dataimporter.service.UserImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Arrays;

@RestController
@RequestMapping("/api/data-importer")
@RequiredArgsConstructor
public class UserImportController {

    private final UserImportService importService;

    @PostMapping("/start-users-import")
    public ResponseEntity<?> startUsersImport(@RequestHeader(value = "X-User-Roles", required = false)
                                                  String rolesHeader) {
        if (rolesHeader == null || !hasAdminRole(rolesHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access denied: missing ADMIN role in X-User-Roles header"));
        }
        importService.startImportAsync();
        return ResponseEntity.ok(Map.of("status", "import_started"));
    }

    private boolean hasAdminRole(String header) {
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .toList()
                .contains("admin");
    }
}
