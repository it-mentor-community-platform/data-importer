package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.ProjectUpsertRequestDto;
import com.itmentorcommunityplatform.dataimporter.dto.response.ProfileByGithubResponseDto;
import com.itmentorcommunityplatform.dataimporter.google.GoogleSheetsClient;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectImportService implements ImportCommand {
    private final GoogleSheetsClient googleSheetsClient;
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
        return new ImportType("PROJECTS");
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

        List<List<Object>> rows;
        try {
            rows = googleSheetsClient.readSheet(properties.getSheetRangeProjects());
        } catch (Exception e) {
            log.error("Failed to read projects sheet", e);
            projectImportErrorCounter.increment();
            executionService.finishFailed(execution, 0, 0, SourceReadStatus.FAILED);
            return;
        }

        if (rows.isEmpty()) {
            log.info("Sheet returned empty result.");
        }

        Set<String> processedProjects = new HashSet<>();
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

                String addedTimestamp = String.valueOf(row.get(0)).trim();
                String roadmapProject = row.size() > 1 ? String.valueOf(row.get(1)).trim() : "";
                String programmingLanguage = row.size() > 2 ? String.valueOf(row.get(2)).trim() : "";
                String githubRepositoryLink = row.size() > 4 ? String.valueOf(row.get(4)).trim() : "";
                String githubProfileLink = row.size() > 6 ? String.valueOf(row.get(6)).trim() : "";

                if (githubRepositoryLink.isEmpty() && githubProfileLink.isEmpty()) {
                    log.warn("Skipping row with no GitHub repository or profile link at index {}", i);
                    continue;
                }

                if (processedProjects.contains(githubRepositoryLink)) {
                    skippedDuplicates++;
                    log.debug("Skipping duplicate project: {} at row {}", githubRepositoryLink, i);
                    continue;
                }

                ProfileByGithubResponseDto profile = profileServiceHttpClient
                        .getProfileByGithubUrl(githubProfileLink);

                if (profile == null || profile.telegramUserId() == null) {
                    log.warn("Profile not found or invalid Telegram User ID for GitHub URL: {} at row {}",
                            githubProfileLink, i);
                    projectImportErrorCounter.increment();
                    failedCount++;
                    continue;
                }

                String telegramUsername = profile.details() != null
                        ? getTelegramUsernameFromUrl(profile.details().getTelegramUrl())
                        : null;

                ProjectUpsertRequestDto requestDto = new ProjectUpsertRequestDto(
                        profile.telegramUserId(),
                        githubRepositoryLink,
                        programmingLanguage,
                        roadmapProject,
                        telegramUsername,
                        parseTimestamp(addedTimestamp),
                        PROJECT_SOURCE_TYPE
                );

                try {
                    projectServiceHttpClient.upsertProject(requestDto);
                    processedProjects.add(githubRepositoryLink);
                    projectImportSuccessCounter.increment();
                    successCount++;
                    log.info("Imported project: GitHubRepo={}, GitHubProfile={}, Roadmap project={}",
                            githubRepositoryLink, githubProfileLink, roadmapProject);
                } catch (Exception ex) {
                    projectImportErrorCounter.increment();
                    failedCount++;
                    log.error("Failed to import project at row {}: repo={}", i, githubRepositoryLink, ex);
                }
            }
        } catch (Exception e) {
            log.error("Failed to import projects", e);
            projectImportErrorCounter.increment();
            executionService.finishFailed(execution, successCount, failedCount, SourceReadStatus.SUCCESS);
            return;
        }

        executionService.finishSuccessfully(execution, successCount, failedCount);
        log.info("Project import finished. Success: {}, failed: {}, skipped duplicates: {}",
                successCount, failedCount, skippedDuplicates);
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