package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.response.ProjectReviewSheetsDto;
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
public class ProjectReviewImportService implements ImportCommand {
    private static final int PERIOD_COLUMN_INDEX = 0;
    private static final int REPOSITORY_URL_COLUMN_INDEX = 3;
    private static final int REVIEW_URL_COLUMN_INDEX = 5;
    private static final int REVIEWER_PROFILE_URL_COLUMN_INDEX = 8;
    private static final int REVIEW_SHEET_COLUMNS_COUNT = 9;

    private final ImportSheetReader importSheetReader;
    private final DataImporterProperties properties;
    private final ProfileServiceHttpClient profileServiceHttpClient;
    private final ProjectServiceHttpClient projectServiceHttpClient;

    private final Counter projectReviewImportSuccessCounter;
    private final Counter projectReviewImportErrorCounter;
    private final Timer projectReviewImportDurationTimer;
    private final ImportExecutionService executionService;

    private static final String[] RU_MONTHS = {
            "январь", "февраль", "март", "апрель", "май", "июнь", "июль",
            "август", "сентябрь", "октябрь", "ноябрь", "декабрь"
    };

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "project-reviews-import-thread"));

    @Override
    public ImportType getImportType() {
        return ImportType.REVIEWS;
    }

    @Override
    public void startImportAsync(ImportExecution execution) {
        executor.submit(() -> {
            try {
                doImportMeasured(execution);
            } catch (Exception e) {
                log.error("Unexpected error in review import, executionId={}", execution.getId(), e);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private void doImportMeasured(ImportExecution execution) {
        projectReviewImportDurationTimer.record(() -> doImport(execution));
    }

    private void doImport(ImportExecution execution) {
        log.info("Starting project reviews import.");

        List<List<Object>> rows = importSheetReader.readRowsOrFinishImport(
                execution,
                properties.getProjectSpreadsheetId(),
                properties.getSheetRangeProjectReviews(),
                projectReviewImportErrorCounter
        );
        if (rows.isEmpty()) {
            return;
        }

        List<ProjectReviewSheetsDto> requests = new ArrayList<>();
        boolean criticalFailure = false;

        try {
            prepareRequests(rows, requests, execution);
        } catch (Exception e) {
            log.error("Failed to prepare project review requests", e);
            criticalFailure = true;
        }

        try {
            sendRequests(requests, execution);
        } catch (Exception e) {
            log.error("Failed to send project review requests", e);
            criticalFailure = true;
        }

        if (criticalFailure) {
            executionService.finishFailed(execution, execution.getSuccessCount(), execution.getErrorCount(), SourceReadStatus.SUCCESS);
            return;
        }

        executionService.finishSuccessfully(execution, execution.getSuccessCount(), execution.getErrorCount());
        log.info("Project reviews import finished. Success: {}, failed: {}", execution.getSuccessCount(), execution.getErrorCount());
    }

    private void prepareRequests(
            List<List<Object>> rows, List<ProjectReviewSheetsDto> requests, ImportExecution execution
    ) {
        Map<String, Long> mentorIdCache = new HashMap<>();

        for (List<Object> row : rows) {
            if (row == null || row.size() < REVIEW_SHEET_COLUMNS_COUNT) {
                log.debug("Skipping service row: {}", row);
                projectReviewImportErrorCounter.increment();
                continue;
            }

            String period = getCell(row, PERIOD_COLUMN_INDEX);
            String githubRepositoryUrl = getCell(row, REPOSITORY_URL_COLUMN_INDEX);
            String reviewUrl = getCell(row, REVIEW_URL_COLUMN_INDEX);
            String reviewerProfileUrl = getCell(row, REVIEWER_PROFILE_URL_COLUMN_INDEX);

            Long mentorTelegramId = mentorIdCache.computeIfAbsent(reviewerProfileUrl, url -> {
                log.debug("Cache miss for {}, fetching ID from Profile Service", url);
                return profileServiceHttpClient.getTelegramUserIdByUrl(url);
            });

            if (mentorTelegramId == null) {
                log.warn("Skip: Mentor with url {} not found in Profile Service", reviewerProfileUrl);
                projectReviewImportErrorCounter.increment();
                execution.recordFailure();
                continue;
            }

            requests.add(new ProjectReviewSheetsDto(
                    githubRepositoryUrl,
                    reviewUrl,
                    mentorTelegramId,
                    reviewerProfileUrl,
                    parseTimestamp(period)
            ));
        }
    }

    private void sendRequests(List<ProjectReviewSheetsDto> requests, ImportExecution execution) {
        for (ProjectReviewSheetsDto request : requests) {
            try {
                projectServiceHttpClient.upsertProjectReview(request);
                execution.recordSuccess();
                log.info("Imported project review: {}", request);
                projectReviewImportSuccessCounter.increment();
            } catch (Exception e) {
                log.error("Failed to import project review: repo={}, reviewUrl={}",
                        request.getProjectGithubRepositoryUrl(), request.getReviewUrl(), e);
                projectReviewImportErrorCounter.increment();
                execution.recordFailure();
            }
        }
    }

    private String getCell(List<Object> row, int index) {
        if (row.size() <= index || row.get(index) == null) {
            return "";
        }

        return String.valueOf(row.get(index)).trim();
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