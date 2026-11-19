package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.ProjectUpsertRequestDto;
import com.itmentorcommunityplatform.dataimporter.dto.response.ProfileByGithubResponseDto;
import com.itmentorcommunityplatform.dataimporter.httpclient.ProfileServiceHttpClient;
import com.itmentorcommunityplatform.dataimporter.httpclient.ProjectServiceHttpClient;
import com.itmentorcommunityplatform.dataimporter.model.ImportExecution;
import com.itmentorcommunityplatform.dataimporter.model.ImportType;
import com.itmentorcommunityplatform.dataimporter.model.SourceReadStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectImportService implements ImportCommand {
    private static final int ADDED_DATE_COLUMN_INDEX = 0;
    private static final int ROADMAP_PROJECT_COLUMN_INDEX = 1;
    private static final int PROGRAMMING_LANGUAGE_COLUMN_INDEX = 2;
    private static final int GITHUB_REPOSITORY_COLUMN_INDEX = 4;
    private static final int GITHUB_PROFILE_COLUMN_INDEX = 6;

    private final ImportSheetReader importSheetReader;
    private final DataImporterProperties properties;
    private final ProjectServiceHttpClient projectServiceHttpClient;
    private final ProfileServiceHttpClient profileServiceHttpClient;

    private final Counter projectImportSuccessCounter;
    private final Counter projectImportErrorCounter;
    private final Timer projectImportDurationTimer;
    private final ImportExecutionService executionService;

    private static final String PROJECT_SOURCE_TYPE = "DATA_IMPORTER";

    private static final String[] RU_MONTHS = {
            "январь", "февраль", "март", "апрель", "май", "июнь", "июль",
            "август", "сентябрь", "октябрь", "ноябрь", "декабрь"
    };

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "project-import-thread"));

    @Override
    public ImportType getImportType() {
        return ImportType.PROJECTS;
    }

    @Override
    public void startImportAsync(ImportExecution execution) {
        executor.submit(() -> {
            try {
                doImportMeasured(execution);
            } catch (Exception e) {
                log.error("Unexpected error in project import, executionId={}", execution.getId(), e);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private void doImportMeasured(ImportExecution execution) {
        projectImportDurationTimer.record(() -> doImport(execution));
    }

    private void doImport(ImportExecution execution) {
        log.info("Starting project import (async)...");

        List<List<Object>> rows = importSheetReader.readRowsOrFinishImport(execution,
                properties.getProjectSpreadsheetId(), properties.getSheetRangeProjects(), projectImportErrorCounter);

        if (rows.isEmpty()) {
            return;
        }

        List<ProjectUpsertRequestDto> requests = new ArrayList<>();
        boolean criticalFailure = false;
        int skippedDuplicates = 0;

        try {
            skippedDuplicates = prepareRequests(rows, requests, execution);
        } catch (Exception e) {
            log.error("Failed to prepare project requests", e);
            criticalFailure = true;
        }

        try {
            sendRequests(requests, execution);
        } catch (Exception e) {
            log.error("Failed to send project requests", e);
            criticalFailure = true;
        }

        if (criticalFailure) {
            projectImportErrorCounter.increment();
            executionService.finishFailed(execution, execution.getSuccessCount(), execution.getErrorCount(), SourceReadStatus.SUCCESS);
            return;
        }

        executionService.finishSuccessfully(execution, execution.getSuccessCount(), execution.getErrorCount());
        log.info("Project import finished. Success: {}, failed: {}, skipped duplicates: {}",
                execution.getSuccessCount(), execution.getErrorCount(), skippedDuplicates);
    }

    private int prepareRequests(
            List<List<Object>> rows,
            List<ProjectUpsertRequestDto> requests,
            ImportExecution execution
    ) {
        Set<String> seenRepositoryUrls = new HashSet<>();
        int skippedDuplicates = 0;

        for (int i = 0; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            if (row == null || row.isEmpty()) {
                log.warn("Skipping empty row at index {}", i);
                continue;
            }

            String addedTimestamp = readTrimmedCell(row, ADDED_DATE_COLUMN_INDEX);
            String roadmapProject = readTrimmedCell(row, ROADMAP_PROJECT_COLUMN_INDEX);
            String programmingLanguage = readTrimmedCell(row, PROGRAMMING_LANGUAGE_COLUMN_INDEX);
            String githubRepositoryLink = readTrimmedCell(row, GITHUB_REPOSITORY_COLUMN_INDEX);
            String githubProfileLink = readTrimmedCell(row, GITHUB_PROFILE_COLUMN_INDEX);

            if (githubRepositoryLink.isEmpty() && githubProfileLink.isEmpty()) {
                log.warn("Skipping row with no GitHub repository or profile link at index {}", i);
                continue;
            }

            if (!githubRepositoryLink.isEmpty() && !seenRepositoryUrls.add(githubRepositoryLink)) {
                skippedDuplicates++;
                log.debug("Skipping duplicate project: {} at row {}", githubRepositoryLink, i);
                continue;
            }

            ProfileByGithubResponseDto profile = profileServiceHttpClient.getProfileByGithubUrl(githubProfileLink);
            if (profile == null || profile.telegramUserId() == null) {
                log.warn("Profile not found or invalid Telegram User ID for GitHub URL: {} at row {}", githubProfileLink, i);
                projectImportErrorCounter.increment();
                execution.recordFailure();
                continue;
            }

            String telegramUsername = profile.details() != null
                    ? getTelegramUsernameFromUrl(profile.details().getTelegramUrl())
                    : null;

            requests.add(new ProjectUpsertRequestDto(
                    profile.telegramUserId(),
                    githubRepositoryLink,
                    programmingLanguage,
                    roadmapProject,
                    telegramUsername,
                    parseTimestamp(addedTimestamp),
                    PROJECT_SOURCE_TYPE
            ));
        }

        return skippedDuplicates;
    }

    private void sendRequests(List<ProjectUpsertRequestDto> requests, ImportExecution execution) {
        for (ProjectUpsertRequestDto request : requests) {
            try {
                projectServiceHttpClient.upsertProject(request);
                projectImportSuccessCounter.increment();
                execution.recordSuccess();
                log.info("Imported project: repo={}, authorTelegramId={}, roadmapProject={}",
                        request.githubRepositoryUrl(), request.authorTelegramUserId(), request.roadmapProject());
            } catch (Exception e) {
                projectImportErrorCounter.increment();
                execution.recordFailure();
                log.error("Failed to import project: repo={}, authorTelegramId={}",
                        request.githubRepositoryUrl(), request.authorTelegramUserId(), e);
            }
        }
    }

    private static String readTrimmedCell(List<Object> row, int columnIndex) {
        return row.size() > columnIndex
                ? String.valueOf(row.get(columnIndex)).trim()
                : "";
    }

    private String getTelegramUsernameFromUrl(String url) {
        if (url == null || url.isBlank()) return null;
        return url.substring(url.lastIndexOf('/'));
    }

    private Long parseTimestamp(String dateStr) {
        if (dateStr.isEmpty()) return null;

        try {
            dateStr = dateStr.replace('\u00A0', ' ').trim();

            SimpleDateFormat sdf = new SimpleDateFormat("MMMM, yyyy", Locale.forLanguageTag("ru"));
            sdf.setDateFormatSymbols(new DateFormatSymbols() {{
                setMonths(RU_MONTHS);
            }});
            return sdf.parse(dateStr)
                    .toInstant()
                    .getEpochSecond();
        } catch (Exception e) {
            log.warn("Failed to parse date '{}': {}", dateStr, e.getMessage());
            return Instant.now().getEpochSecond();
        }
    }
}