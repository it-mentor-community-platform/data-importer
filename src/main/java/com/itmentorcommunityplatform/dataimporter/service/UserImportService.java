package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.auth.AuthServiceClient;
import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.UserUpsertRequestDto;
import com.itmentorcommunityplatform.dataimporter.google.GoogleSheetsClient;
import com.itmentorcommunityplatform.dataimporter.metrics.ImportMetrics;
import com.itmentorcommunityplatform.dataimporter.model.UserRole;
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
    private final DataImporterProperties props;
    private final ImportMetrics importMetrics;
    private final AuthServiceClient authServiceClient;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "user-import-thread"));

    public void startImportAsync() {
        executor.submit(this::doImportMeasured);
    }

    private void doImportMeasured() {
        importMetrics.getImportDurationTimer().record(this::doImport);
    }

    private void doImport() {
        log.info("Starting users import (async)...");
        try {
            List<List<Object>> rows = googleSheetsClient.readSheet();
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
                    importMetrics.getImportErrorCounter().increment();
                    continue;
                }
                if (!processedTelegramIds.add(tgId)) {
                    log.debug("Skipping duplicate telegramId={} found at row index {}", tgId, i);
                    skippedDuplicates++;
                    continue;
                }
                boolean isAdmin = props.getAdminIds() != null && props.getAdminIds().contains(tgId);
                List<String> rolesToSend = isAdmin
                        ? List.of(UserRole.ADMIN.name(), UserRole.STUDENT.name())
                        : List.of(UserRole.STUDENT.name());
                UserUpsertRequestDto req = new UserUpsertRequestDto(tgId, rolesToSend);
                try {
                    authServiceClient.upsertUser(req);
                    importMetrics.getImportSuccessCounter().increment();
                    processed++;
                    log.info("Imported user: telegramId={}, roles={}", tgId, rolesToSend);
                } catch (Exception ex) {
                    importMetrics.getImportErrorCounter().increment();
                    log.error("Failed to import user telegramId={}", tgId, ex);
                }
            }
            log.info("Users import finished. Total processed users: {}. Skipped duplicates: {}", processed, skippedDuplicates);
        } catch (Exception e) {
            log.error("Failed to import users", e);
            importMetrics.getImportErrorCounter().increment();
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}