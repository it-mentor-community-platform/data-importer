package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.response.ProjectReviewSheetsDto;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectReviewImportService implements ImportCommand {

    private static final int REVIEW_SHEET_COLUMNS_COUNT = 9;

    private final GoogleSheetsClient googleSheetsClient;
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
        return new ImportType("REVIEWS");
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

        List<List<Object>> rows;
        try {
            rows = googleSheetsClient.readSheet(properties.getSheetRangeProjectReviews());
        } catch (Exception e) {
            log.error("Failed to read reviews sheet", e);
            projectReviewImportErrorCounter.increment();
            executionService.finishFailed(execution, 0, 0, SourceReadStatus.FAILED);
            return;
        }

        if (rows.isEmpty()) {
            log.warn("No project reviews found in the specified range.");
        }

        Map<String, Long> mentorIdCache = new HashMap<>();
        int successCount = 0;
        int failedCount = 0;
        try {
            for (List<Object> row : rows) {

                if (row == null || row.size() < REVIEW_SHEET_COLUMNS_COUNT) {
                    log.debug("Skipping service row: {}", row);
                    projectReviewImportErrorCounter.increment();
                    continue;
                }

                String period = getCell(row, 0);
                String githubRepositoryUrl = getCell(row, 3);
                String reviewUrl = getCell(row, 5);
                String reviewerTelegramProfileUrl = getCell(row, 8);

                Long mentorTelegramId = mentorIdCache.computeIfAbsent(reviewerTelegramProfileUrl, url -> {
                    log.debug("Cache miss for {}, fetching ID from Profile Service", url);
                    return profileServiceHttpClient.getTelegramUserIdByUrl(url);
                });

                if (mentorTelegramId == null) {
                    log.warn("Skip: Mentor with url {} not found in Profile Service", reviewerTelegramProfileUrl);
                    mentorIdCache.remove(reviewerTelegramProfileUrl);
                    projectReviewImportErrorCounter.increment();
                    failedCount++;
                    continue;
                }

                ProjectReviewSheetsDto projectReview = new ProjectReviewSheetsDto(
                        githubRepositoryUrl,
                        reviewUrl,
                        mentorTelegramId,
                        reviewerTelegramProfileUrl,
                        parseTimestamp(period)
                );
                try {
                    projectServiceHttpClient.upsertProjectReview(projectReview);
                    successCount++;
                    log.info("Parsed project review: {}", projectReview);
                    projectReviewImportSuccessCounter.increment();
                } catch (Exception ex) {
                    log.error("Failed to import project review at row {}: repo={}",
                            row, projectReview.getProjectGithubRepositoryUrl(), ex);
                    projectReviewImportErrorCounter.increment();
                    failedCount++;
                }
            }
        } catch (Exception e) {
            log.error("Failed to import project reviews", e);
            executionService.finishFailed(execution, successCount, failedCount, SourceReadStatus.SUCCESS);
            return;
        }

        executionService.finishSuccessfully(execution, successCount, failedCount);
        log.info("Project reviews import finished. Success: {}, failed: {}", successCount, failedCount);
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