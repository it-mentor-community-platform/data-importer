package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.response.ProjectReviewSheetsDto;
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
public class ProjectReviewsImportService {

    private static final int REVIEW_SHEET_COLUMNS_COUNT = 9;

    private final GoogleSheetsClient googleSheetsClient;
    private final DataImporterProperties properties;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "project-reviews-import-thread"));

    public void startImportAsync() {
        executor.submit(this::doImport);
    }

    private void doImport() {
        log.info("Starting project reviews import.");

        try {
            List<List<Object>> rows = googleSheetsClient.readSheet(
                    properties.getSheetRangeProjectReviews()
            );

            if (rows.isEmpty()) {
                log.warn("No project reviews found in the specified range.");
                return;
            }

            int parsed = 0;

            for (List<Object> row : rows) {
                if (row == null || row.size() < REVIEW_SHEET_COLUMNS_COUNT) {
                    log.debug("Skipping service row: {}", row);
                    continue;
                }

                String period = getCell(row, 0);
                String project = getCell(row, 1);
                String programmingLanguage = getCell(row, 2);
                String githubRepositoryUrl = getCell(row, 3);
                String reviewType = getCell(row, 4);
                String reviewUrl = getCell(row, 5);
                String reviewerName = getCell(row, 6);
                String reviewerTelegramUsername = getCell(row, 7);
                String reviewerTelegramProfileUrl = getCell(row, 8);

                ProjectReviewSheetsDto projectReview = new ProjectReviewSheetsDto(
                        period,
                        project,
                        programmingLanguage,
                        githubRepositoryUrl,
                        reviewType,
                        reviewUrl,
                        reviewerName,
                        reviewerTelegramUsername,
                        reviewerTelegramProfileUrl
                );

                log.info("Parsed project review: {}", projectReview);

                parsed++;
            }

            log.info("Project reviews import finished. Total parsed: {}", parsed);
        } catch (Exception e) {
            log.error("Failed to import project reviews", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private String getCell(List<Object> row, int index) {
        if (row.size() <= index || row.get(index) == null) {
            return "";
        }

        return String.valueOf(row.get(index)).trim();
    }
}