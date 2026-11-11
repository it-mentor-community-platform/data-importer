package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.UserUpsertRequestDto;
import com.itmentorcommunityplatform.dataimporter.google.GoogleSheetsClient;
import com.itmentorcommunityplatform.dataimporter.httpclient.AuthServiceHttpClient;
import com.itmentorcommunityplatform.dataimporter.model.ImportExecution;
import com.itmentorcommunityplatform.dataimporter.model.ImportType;
import com.itmentorcommunityplatform.dataimporter.model.SourceReadStatus;
import com.itmentorcommunityplatform.dataimporter.model.UserRole;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserImportService implements ImportCommand {
    private final GoogleSheetsClient googleSheetsClient;
    private final DataImporterProperties properties;
    private final AuthServiceHttpClient httpClient;

    private final Counter usersImportSuccessCounter;
    private final Counter usersImportErrorCounter;
    private final Timer usersImportDurationTimer;
    private final ImportExecutionService executionService;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "user-import-thread"));

    @Override
    public ImportType getImportType() {
        return new ImportType("USERS");
    }

    @Override
    public void startImportAsync(ImportExecution execution) {
        executor.submit(() -> {
            try {
                doImportMeasured(execution);
            } catch (Exception e) {
                log.error("Unexpected error in users import, executionId={}", execution.getId(), e);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private void doImportMeasured(ImportExecution execution) {
        usersImportDurationTimer.record(() -> doImport(execution));
    }

    private void doImport(ImportExecution execution) {
        log.info("Starting users import (async)...");

        List<List<Object>> rows;
        try {
            rows = googleSheetsClient.readSheet(properties.getSheetRangeTelegramAccounts());
        } catch (Exception e) {
            log.error("Failed to read users sheet", e);
            usersImportErrorCounter.increment();
            executionService.finishFailed(execution, 0, 0, SourceReadStatus.FAILED);
            return;
        }

        if (rows.isEmpty()) {
            log.info("Sheet returned empty result.");
        }

        Set<Long> processedTelegramIds = new HashSet<>();
        int successCount = 0;
        int failedCount = 0;
        int skippedDuplicates = 0;
        try {
            for (int i = 0; i < rows.size(); i++) {
                List<Object> row = rows.get(i);

                if (row == null || row.isEmpty()) {
                    log.warn("Skipping empty row at index {}", i);
                    continue;
                }

                String tgRaw = row.size() > 1 ? String.valueOf(row.get(1)).trim() : "";
                if (tgRaw.isEmpty()) {
                    log.warn("Skipping row with empty telegram id at index {}", i);
                    continue;
                }

                Long tgId;
                try {
                    tgId = Long.parseLong(tgRaw);
                } catch (NumberFormatException ex) {
                    log.warn("Invalid telegram id '{}' at row index {}, skipping.", tgRaw, i);
                    usersImportErrorCounter.increment();
                    failedCount++;
                    continue;
                }

                if (!processedTelegramIds.add(tgId)) {
                    log.debug("Skipping duplicate telegramId={} found at row index {}", tgId, i);
                    skippedDuplicates++;
                    continue;
                }

                List<String> rolesToSend = List.of(UserRole.STUDENT.name());
                try {
                    httpClient.upsertUser(new UserUpsertRequestDto(tgId, rolesToSend));
                    usersImportSuccessCounter.increment();
                    successCount++;
                    log.info("Imported user: telegramId={}, roles={}", tgId, rolesToSend);
                } catch (Exception ex) {
                    usersImportErrorCounter.increment();
                    failedCount++;
                    log.error("Failed to import user telegramId={}", tgId, ex);
                }
            }
        } catch (Exception e) {
            log.error("Failed to import users", e);
            usersImportErrorCounter.increment();
            executionService.finishFailed(execution, successCount, failedCount, SourceReadStatus.SUCCESS);
            return;
        }

        executionService.finishSuccessfully(execution, successCount, failedCount);
        log.info("Users import finished. Total successCount users: {}. Skipped duplicates: {}", successCount, skippedDuplicates);
    }
}