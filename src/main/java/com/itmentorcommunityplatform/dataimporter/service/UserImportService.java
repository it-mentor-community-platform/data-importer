package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.UserUpsertRequestDto;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserImportService implements ImportCommand {
    private static final int TELEGRAM_ID_COLUMN_INDEX = 1;

    private final ImportSheetReader importSheetReader;
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
        return ImportType.USERS;
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

        List<List<Object>> rows = importSheetReader.readRowsOrFinishImport(execution,
                properties.getProjectSpreadsheetId(), properties.getSheetRangeTelegramAccounts(), usersImportErrorCounter);

        if (rows.isEmpty()) {
            return;
        }

        try {
            List<UserUpsertRequestDto> requests = prepareRequests(rows, execution);
            sendRequests(requests, execution);
        } catch (Exception e) {
            log.error("Failed to import users", e);
            usersImportErrorCounter.increment();
            executionService.finishFailed(execution, execution.getSuccessCount(), execution.getErrorCount(), SourceReadStatus.SUCCESS);
            return;
        }

        executionService.finishSuccessfully(execution, execution.getSuccessCount(), execution.getErrorCount());
        log.info("Users import finished. Success: {}, failed: {}", execution.getSuccessCount(), execution.getErrorCount());
    }

    private List<UserUpsertRequestDto> prepareRequests(List<List<Object>> rows, ImportExecution execution) {
        List<UserUpsertRequestDto> requests = new ArrayList<>();
        Set<Long> seenTelegramIds = new HashSet<>();
        List<String> roles = List.of(UserRole.STUDENT.name());
        int skippedDuplicates = 0;

        for (int i = 0; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            if (row == null || row.isEmpty()) {
                log.warn("Skipping empty row at index {}", i);
                continue;
            }

            String rawTelegramId = row.size() > TELEGRAM_ID_COLUMN_INDEX
                    ? String.valueOf(row.get(TELEGRAM_ID_COLUMN_INDEX)).trim()
                    : "";

            if (rawTelegramId.isEmpty()) {
                log.warn("Skipping row with empty telegram id at index {}", i);
                continue;
            }

            Long telegramId;
            try {
                telegramId = Long.parseLong(rawTelegramId);
            } catch (NumberFormatException e) {
                log.warn("Invalid telegram id '{}' at row index {}, skipping.", rawTelegramId, i);
                usersImportErrorCounter.increment();
                execution.recordFailure();
                continue;
            }

            if (!seenTelegramIds.add(telegramId)) {
                log.debug("Skipping duplicate telegramId={} found at row index {}", telegramId, i);
                skippedDuplicates++;
                continue;
            }

            requests.add(new UserUpsertRequestDto(telegramId, roles));
        }

        log.info("Users import preparation finished. Skipped duplicates: {}", skippedDuplicates);
        return requests;
    }

    private void sendRequests(List<UserUpsertRequestDto> requests, ImportExecution execution) {
        for (UserUpsertRequestDto request : requests) {
            try {
                httpClient.upsertUser(request);
                usersImportSuccessCounter.increment();
                execution.recordSuccess();
                log.info("Imported user: telegramId={}, roles={}",
                        request.telegramUserId(), request.roles());
            } catch (Exception e) {
                usersImportErrorCounter.increment();
                execution.recordFailure();
                log.error("Failed to import user telegramId={}", request.telegramUserId(), e);
            }
        }
    }
}