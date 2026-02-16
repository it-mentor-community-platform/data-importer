package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.UserUpsertRequestDto;
import com.itmentorcommunityplatform.dataimporter.google.GoogleSheetsClient;
import com.itmentorcommunityplatform.dataimporter.httpclient.ServiceHttpClient;
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
public class UserImportService {

    private final GoogleSheetsClient googleSheetsClient;
    private final DataImporterProperties properties;
    private final ServiceHttpClient httpClient;

    private final Counter usersImportSuccessCounter;
    private final Counter usersImportErrorCounter;
    private final Timer usersImportDurationTimer;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "user-import-thread"));

    public void startImportAsync() {
        executor.submit(this::doImportMeasured);
    }

    private void doImportMeasured() {
        usersImportDurationTimer.record(this::doImport);
    }

    private void doImport() {
        log.info("Starting users import (async)...");
        try {
            List<List<Object>> rows = googleSheetsClient.readSheet(properties.getSheetRangeTelegramAccounts());
            if (rows.isEmpty()) {
                log.info("Sheet returned empty result.");
                return;
            }
            Set<Long> processedTelegramIds = new HashSet<>();
            int processed = 0;
            int skippedDuplicates = 0;
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
                    continue;
                }
                if (!processedTelegramIds.add(tgId)) {
                    log.debug("Skipping duplicate telegramId={} found at row index {}", tgId, i);
                    skippedDuplicates++;
                    continue;
                }
                List<String> rolesToSend = List.of(UserRole.STUDENT.name());
                UserUpsertRequestDto req = new UserUpsertRequestDto(tgId, rolesToSend);
                try {
                    httpClient.upsertUser(req);
                    usersImportSuccessCounter.increment();
                    processed++;
                    log.info("Imported user: telegramId={}, roles={}", tgId, rolesToSend);
                } catch (Exception ex) {
                    usersImportErrorCounter.increment();
                    log.error("Failed to import user telegramId={}", tgId, ex);
                }
            }
            log.info("Users import finished. Total processed users: {}. Skipped duplicates: {}", processed, skippedDuplicates);
        } catch (Exception e) {
            log.error("Failed to import users", e);
            usersImportErrorCounter.increment();
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}