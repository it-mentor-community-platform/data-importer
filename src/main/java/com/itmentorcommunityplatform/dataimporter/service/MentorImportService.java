package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.google.GoogleSheetsClient;
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
public class MentorImportService {

    private final GoogleSheetsClient googleSheetsClient;
    private final DataImporterProperties properties;

    private final Counter mentorImportSuccessCounter;
    private final Counter mentorImportErrorCounter;
    private final Timer mentorImportDurationTimer;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "mentor-import-thread"));

    public void startImportAsync() {
        executor.submit(this::doImportMeasured);
    }

    private void doImportMeasured() {
        mentorImportDurationTimer.record(this::doImport);
    }

    private void doImport() {
        log.info("Starting mentor import (async)...");
        try {
            List<List<Object>> rows = googleSheetsClient.readSheet(properties.getMentorSpreadsheetId(),
                    properties.getSheetRangeMentors());
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

                String tgRaw = row.size() > 11 ? String.valueOf(row.get(11)).trim() : "";

                if (tgRaw.isEmpty()) {
                    log.warn("Skipping row with empty telegram id at index {}", i);
                    continue;
                }

                Long tgId;
                try {
                    tgId = Long.parseLong(tgRaw);
                } catch (NumberFormatException ex) {
                    log.warn("Invalid telegram id '{}' at row index {}, skipping.", tgRaw, i);
                    mentorImportErrorCounter.increment();
                    continue;
                }

                if (!processedTelegramIds.add(tgId)) {
                    log.debug("Skipping duplicate telegramId={} found at row index {}", tgId, i);
                    skippedDuplicates++;
                    continue;
                }

                try {
                    String name = row.get(2).toString().trim();
                    String username = row.get(3).toString().trim();
                    String languagesRaw = String.valueOf(row.get(4)).trim();
                    String[] languages = languagesRaw.split(",");
                    String services = row.get(5).toString().trim();
                    String description = row.get(7).toString().trim();
                    
                    for (String language : languages) {
                        log.info("Mentor: telegramId={}, name={}, telegramUsername={}, language={}, services={}," +
                                        "description={}",
                                tgId, name, username, language.trim(), services, description);
                    }
                    mentorImportSuccessCounter.increment();
                    processed++;
                } catch (Exception ex) {
                    mentorImportErrorCounter.increment();
                    log.error("Failed to import mentor telegramId={}", tgId, ex);
                }
            }
            log.info("Mentor import finished. Total processed mentors: {}. Skipped duplicates: {}", processed, skippedDuplicates);
        } catch (Exception e) {
            log.error("Failed to import mentors", e);
            mentorImportErrorCounter.increment();
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
