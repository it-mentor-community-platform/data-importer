package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.response.ProfileUpsertResponseDto;
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
public class ProfileImportService {
    private final GoogleSheetsClient googleSheetsClient;
    private final DataImporterProperties props;
    private final ImportMetrics importMetrics;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "profile-import-thread"));

    public void startImportAsync() {
        executor.submit(this::doImportMeasured);
    }

    private void doImportMeasured() {
        importMetrics.getImportDurationTimer().record(this::doImport);
    }

    private void doImport() {
        log.info("Starting profile import (async)...");
        try {
            List<List<Object>> rows = googleSheetsClient.readSheet();
            if (rows.isEmpty()) {
                log.info("Sheet returned empty result.");
                return;
            }

            Set<String> processedGitHubLinks = new HashSet<>();
            Set<String> processedTelegramUsernames = new HashSet<>();
            Set<Long> processedTelegramIds = new HashSet<>();
            int processed = 0;
            int skippedDuplicates = 0;

            for (int i = 0; i < rows.size(); i++) {
                List<Object> row = rows.get(i);
                if (row == null || row.isEmpty()) {
                    log.warn("Skipping empty row at index {}", i);
                    continue;
                }

                String tgIdRaw = row.size() > 1 ? String.valueOf(row.get(1)).trim() : "";
                Long tgId = null;
                if (!tgIdRaw.isEmpty()) {
                    try {
                        tgId = Long.parseLong(tgIdRaw);
                    } catch (NumberFormatException ex) {
                        log.warn("Invalid telegram ID '{}' at row index {}, skipping.", tgIdRaw, i);
                        importMetrics.getImportErrorCounter().increment();
                        continue;
                    }
                }

                String githubLink = row.size() > 0 ? String.valueOf(row.get(0)).trim() : "";
                if (githubLink.isEmpty() && tgIdRaw.isEmpty()) {
                    log.warn("Skipping row with no GitHub link or Telegram ID at index {}", i);
                    continue;
                }

                String telegramUsername = row.size() > 2 ? String.valueOf(row.get(2)).trim() : "";
                String formattedTelegramUsernameLink = telegramUsername.isEmpty() ? null
                        : String.format("https://t.me/%s", telegramUsername.replaceFirst("^@", ""));


                boolean duplicate = (tgId != null && !processedTelegramIds.add(tgId)) ||
                                    (!githubLink.isEmpty() && !processedGitHubLinks.add(githubLink)) ||
                                    (!telegramUsername.isEmpty() && !processedTelegramUsernames.add(telegramUsername));
                if (duplicate) {
                    log.debug("Skipping duplicate entry at row index {}", i);
                    skippedDuplicates++;
                    continue;
                }


                boolean isAdmin = tgId != null && props.getAdminIds() != null && props.getAdminIds().contains(tgId);
                List<String> rolesToSend = isAdmin ? List.of(UserRole.ADMIN.name(), UserRole.STUDENT.name())
                        : List.of(UserRole.STUDENT.name());

                // DTO для отправки в сервис
                ProfileUpsertResponseDto req = new ProfileUpsertResponseDto(formattedTelegramUsernameLink, githubLink);

                try {
                    // Тут вызываем метод интеграции с Auth Service
                    // authServiceClient.upsertProfile(req);

                    importMetrics.getImportSuccessCounter().increment();
                    processed++;
                    log.info("Imported profile: tgId={}, githubLink={}, telegramUsername={}, roles={}",
                            tgId, githubLink, formattedTelegramUsernameLink, rolesToSend);
                } catch (Exception ex) {
                    importMetrics.getImportErrorCounter().increment();
                    log.error("Failed to import profile at row index {}: tgId={}, githubLink={}, telegramUsername={}",
                            i, tgId, githubLink, formattedTelegramUsernameLink, ex);
                }
            }
            log.info("Profile import finished. Total processed: {}. Skipped duplicates: {}", processed, skippedDuplicates);
        } catch (Exception e) {
            log.error("Failed to import profiles", e);
            importMetrics.getImportErrorCounter().increment();
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
