package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.ProfileUpsertRequestDto;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileImportService implements ImportCommand {
    private static final int GITHUB_LINK_COLUMN_INDEX = 0;
    private static final int TELEGRAM_ID_COLUMN_INDEX = 1;
    private static final int TELEGRAM_USERNAME_COLUMN_INDEX = 2;

    private final ImportSheetReader importSheetReader;
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
        return ImportType.PROFILES;
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

        List<List<Object>> rows = importSheetReader.readRowsOrFinishImport(execution,
                properties.getProjectSpreadsheetId(), properties.getSheetRangeTelegramAccounts(), profilesImportErrorCounter);

        if (rows.isEmpty()) {
            return;
        }

        try {
            List<ProfileUpsertRequestDto> requests = prepareRequests(rows, execution);
            sendRequests(requests, execution);
        } catch (Exception e) {
            log.error("Failed to import profiles", e);
            profilesImportErrorCounter.increment();
            executionService.finishFailed(execution, execution.getSuccessCount(), execution.getErrorCount(), SourceReadStatus.SUCCESS);
            return;
        }

        executionService.finishSuccessfully(execution, execution.getSuccessCount(), execution.getErrorCount());
        log.info("Profile import finished. Success: {}, failed: {}", execution.getSuccessCount(), execution.getErrorCount());
    }

    private List<ProfileUpsertRequestDto> prepareRequests(
            List<List<Object>> rows, ImportExecution execution
    ) {
        List<ProfileUpsertRequestDto> requests = new ArrayList<>();
        Set<String> seenGitHubLinks = new HashSet<>();
        Set<String> seenTelegramUsernames = new HashSet<>();
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

            Long telegramId;
            try {
                telegramId = rawTelegramId.isEmpty() ? null : Long.valueOf(rawTelegramId);
            } catch (NumberFormatException e) {
                log.warn("Invalid telegram ID '{}' at row index {}, skipping.", rawTelegramId, i);
                profilesImportErrorCounter.increment();
                execution.recordFailure();
                continue;
            }

            String githubLink = String.valueOf(row.get(GITHUB_LINK_COLUMN_INDEX)).trim();

            String telegramUsername = row.size() > TELEGRAM_USERNAME_COLUMN_INDEX
                    ? String.valueOf(row.get(TELEGRAM_USERNAME_COLUMN_INDEX)).trim()
                    : "";

            if (githubLink.isEmpty() && telegramUsername.isEmpty()) {
                log.warn("Skipping row with no GitHub link or Telegram Username at index {}", i);
                continue;
            }

            if (registerKeysAndCheckDuplicate(githubLink, telegramUsername, seenGitHubLinks, seenTelegramUsernames)) {
                log.debug("Skipping duplicate entry at row index {}", i);
                skippedDuplicates++;
                continue;
            }

            String telegramUrl = telegramUsername.isEmpty()
                    ? null
                    : String.format("https://t.me/%s", telegramUsername.replaceFirst("^@", ""));

            requests.add(new ProfileUpsertRequestDto(
                    telegramId,
                    new ProfileUpsertRequestDto.DetailsDto(telegramUrl, githubLink)
            ));
        }

        log.info("Profile import preparation finished. Skipped duplicates: {}", skippedDuplicates);
        return requests;
    }

    private void sendRequests(List<ProfileUpsertRequestDto> requests, ImportExecution execution) {
        for (ProfileUpsertRequestDto request : requests) {
            ProfileUpsertRequestDto.DetailsDto details = request.details();
            try {
                httpClient.upsertProfile(request);
                profilesImportSuccessCounter.increment();
                execution.recordSuccess();
                log.info("Imported profile: githubLink={}, telegramUsernameLink={}", details.githubProfileUrl(), details.telegramUrl());
            } catch (Exception e) {
                profilesImportErrorCounter.increment();
                execution.recordFailure();
                log.error("Failed to import profile: telegramId={}, githubLink={}, telegramUrl={}",
                        request.telegramUserId(), details.githubProfileUrl(), details.telegramUrl(), e);
            }
        }
    }

    private boolean registerKeysAndCheckDuplicate(
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