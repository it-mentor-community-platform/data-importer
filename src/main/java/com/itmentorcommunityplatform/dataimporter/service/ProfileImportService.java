package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.ProfileUpsertRequestDto;
import com.itmentorcommunityplatform.dataimporter.google.GoogleSheetsClient;
import com.itmentorcommunityplatform.dataimporter.httpclient.ProfileServiceHttpClient;
import com.itmentorcommunityplatform.dataimporter.model.ImportExecution;
import com.itmentorcommunityplatform.dataimporter.model.ImportType;
import com.itmentorcommunityplatform.dataimporter.model.SourceReadStatus;
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
public class ProfileImportService implements ImportCommand {
    private final GoogleSheetsClient googleSheetsClient;
    private final ProfileServiceHttpClient httpClient;
    private final DataImporterProperties properties;

    private final Counter profilesImportSuccessCounter;
    private final Counter profilesImportErrorCounter;
    private final Timer profilesImportDurationTimer;
    private final ImportExecutionService executionService;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "profile-import-thread"));

    @Override
    public ImportType getImportType() {
        return new ImportType("PROFILES");
    }

    @Override
    public void startImportAsync(ImportExecution execution) {
        executor.submit(() -> {
            try {
                doImportMeasured(execution);
            } catch (Exception e) {
                log.error("Unexpected error in profile import, executionId={}", execution.getId(), e);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private void doImportMeasured(ImportExecution execution) {
        profilesImportDurationTimer.record(() -> doImport(execution));
    }

    private void doImport(ImportExecution execution) {
        log.info("Starting profile import (async)...");

        List<List<Object>> rows;
        try {
            rows = googleSheetsClient.readSheet(properties.getSheetRangeTelegramAccounts());
        } catch (Exception e) {
            log.error("Failed to read profile sheet", e);
            profilesImportErrorCounter.increment();
            executionService.finishFailed(execution, 0, 0, SourceReadStatus.FAILED);
            return;
        }

        if (rows.isEmpty()) {
            log.info("Sheet returned empty result.");
        }

        Set<String> processedGitHubLinks = new HashSet<>();
        Set<String> processedTelegramUsernames = new HashSet<>();
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

                String tgIdRaw = row.size() > 1 ? String.valueOf(row.get(1)).trim() : "";
                Long tgId = null;
                if (!tgIdRaw.isEmpty()) {
                    try {
                        tgId = Long.parseLong(tgIdRaw);
                    } catch (NumberFormatException ex) {
                        log.warn("Invalid telegram ID '{}' at row index {}, skipping.", tgIdRaw, i);
                        profilesImportErrorCounter.increment();
                        failedCount++;
                        continue;
                    }
                }

                String githubLink = String.valueOf(row.get(0)).trim();
                String telegramUsername = row.size() > 2 ? String.valueOf(row.get(2)).trim() : "";

                if (githubLink.isEmpty() && telegramUsername.isEmpty()) {
                    log.warn("Skipping row with no GitHub link or Telegram Username at index {}", i);
                    continue;
                }

                String formattedTelegramUsernameLink = telegramUsername.isEmpty() ? null
                        : String.format("https://t.me/%s", telegramUsername.replaceFirst("^@", ""));

                boolean duplicate = isDuplicateEntry(
                        githubLink,
                        telegramUsername,
                        processedGitHubLinks,
                        processedTelegramUsernames
                );
                if (duplicate) {
                    log.debug("Skipping duplicate entry at row index {}", i);
                    skippedDuplicates++;
                    continue;
                }

                var requestDto = new ProfileUpsertRequestDto(
                        tgId,
                        new ProfileUpsertRequestDto.DetailsDto(
                                formattedTelegramUsernameLink,
                                githubLink)
                );

                try {
                    httpClient.upsertProfile(requestDto);
                    profilesImportSuccessCounter.increment();
                    successCount++;
                    log.info("Imported profile: githubLink={}, telegramUsernameLink={}",
                            githubLink, formattedTelegramUsernameLink);
                } catch (Exception ex) {
                    profilesImportErrorCounter.increment();
                    failedCount++;
                    log.error("Failed to import profile at row index {}: githubLink={}, telegramUsername={}",
                            i, githubLink, formattedTelegramUsernameLink, ex);
                }
            }

        } catch (Exception e) {
            log.error("Failed to import profiles", e);
            profilesImportErrorCounter.increment();
            executionService.finishFailed(execution, successCount, failedCount, SourceReadStatus.SUCCESS);
            return;
        }

        executionService.finishSuccessfully(execution, successCount, failedCount);
        log.info("Profile import finished. Success: {}, failed: {}, skipped duplicates: {}",
                successCount, failedCount, skippedDuplicates);
    }

    private boolean isDuplicateEntry(
            String githubLink,
            String telegramUsername,
            Set<String> processedGithubs,
            Set<String> processedUsernames
    ) {
        boolean githubDuplicate = githubLink != null && !githubLink.isEmpty() && !processedGithubs.add(githubLink);
        boolean usernameDuplicate = telegramUsername != null && !telegramUsername.isEmpty() && !processedUsernames.add(telegramUsername);

        return githubDuplicate || usernameDuplicate;
    }
}