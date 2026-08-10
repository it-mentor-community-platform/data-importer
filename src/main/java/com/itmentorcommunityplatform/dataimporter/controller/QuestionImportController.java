package com.itmentorcommunityplatform.dataimporter.controller;

import com.itmentorcommunityplatform.dataimporter.controller.api.QuestionImportApi;
import com.itmentorcommunityplatform.dataimporter.dto.response.ImportStartResponseDto;
import com.itmentorcommunityplatform.dataimporter.service.QuestionImportService;
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
public class QuestionImportController implements QuestionImportApi {

    private final QuestionImportService questionImportService;

    @Override
    @PostMapping("/start-question-import")
    public ResponseEntity<ImportStartResponseDto> startQuestionImport(
            @RequestHeader(value = "X-User-Roles", required = false) List<String> roles) {

        RoleValidator.validateAdminRole(roles);
        questionImportService.startImportAsync();

        return ResponseEntity.ok(new ImportStartResponseDto("import_started"));
    }

}
