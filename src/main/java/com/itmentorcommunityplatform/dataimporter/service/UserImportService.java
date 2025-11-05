package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.UserImportDto;
import com.itmentorcommunityplatform.dataimporter.google.GoogleSheetsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserImportService {

    private final GoogleSheetsClient googleSheetsClient;
    private final DataImporterProperties props;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setName("users-import-thread");
        return t;
    });

    public void startImportAsync() {
        executor.submit(this::doImport);
    }

    private void doImport() {
        log.info("Starting users import (async)...");
        try {
            List<List<Object>> rows = googleSheetsClient.readSheet();

            if (rows.isEmpty()) {
                log.info("Sheet returned empty result.");
                return;
            }
            int processed = 0;
            for (int i = 0; i < rows.size(); i++) {
                List<Object> row = rows.get(i);
                if (row == null || row.isEmpty()) {
                    log.warn("Skipping empty row at index {}", i);
                    continue;
                }
                String tgRaw = String.valueOf(row.getFirst()).trim();
                if (tgRaw.isEmpty()) {
                    log.warn("Skipping row with empty telegram id at index {}", i);
                    continue;
                }
                Long tgId;
                try {
                    tgId = Long.parseLong(tgRaw);
                } catch (NumberFormatException ex) {
                    log.warn("Invalid telegram id '{}' at row index {}, skipping.", tgRaw, i);
                    continue;
                }
                String role = (props.getAdminIds() != null && props.getAdminIds().contains(tgId)) ? "ADMIN" : "STUDENT";
                UserImportDto dto = new UserImportDto(tgId, role);
                log.info("Imported user: telegramId={}, role={}", dto.getTelegramId(), dto.getRole());
                processed++;
            }
            log.info("Users import finished. Total processed users: {}", processed);

        } catch (Exception e) {
            log.error("Failed to import users", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
