package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.GuaranteedReviewRequestDto;
import com.itmentorcommunityplatform.dataimporter.httpclient.MentorServiceHttpClient;
import com.itmentorcommunityplatform.dataimporter.httpclient.ProfileServiceHttpClient;
import com.itmentorcommunityplatform.dataimporter.model.ImportExecution;
import com.itmentorcommunityplatform.dataimporter.model.ImportType;
import com.itmentorcommunityplatform.dataimporter.model.RoadmapProjectType;
import com.itmentorcommunityplatform.dataimporter.model.SourceReadStatus;
import com.itmentorcommunityplatform.dataimporter.util.TelegramLinkUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GuaranteedReviewImportService implements ImportCommand {
    private static final int PROJECT_NAME_COLUMN_INDEX = 0;
    private static final int TELEGRAM_COLUMN_INDEX = 1;
    private static final int LANGUAGES_COLUMN_INDEX = 2;
    private static final int PRICE_COLUMN_INDEX = 3;

    private final ImportSheetReader importSheetReader;
    private final DataImporterProperties properties;
    private final ProfileServiceHttpClient profileServiceHttpClient;
    private final MentorServiceHttpClient mentorServiceHttpClient;

    private final Counter guaranteedReviewImportSuccessCounter;
    private final Counter guaranteedReviewImportErrorCounter;
    private final Timer guaranteedReviewImportDurationTimer;
    private final ImportExecutionService executionService;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "guaranteed-reviews-import-thread"));

    @Override
    public ImportType getImportType() {
        return ImportType.GUARANTEED_REVIEWS;
    }

    @Override
    public void startImportAsync(ImportExecution execution) {
        executor.submit(() -> {
            try {
                doImportMeasured(execution);
            } catch (Exception e) {
                log.error("Unexpected error in guaranteed reviews import, executionId={}", execution.getId(), e);
            }
        });
    }

    private void doImportMeasured(ImportExecution execution) {
        guaranteedReviewImportDurationTimer.record(() -> doImport(execution));
    }

    private void doImport(ImportExecution execution) {
        log.info("Starting guaranteed reviews import...");

        List<List<Object>> rows = importSheetReader.readRowsOrFinishImport(execution,
                properties.getGuaranteedReviewsSpreadsheetId(), properties.getSheetRangeGuaranteedReviews(), guaranteedReviewImportErrorCounter);

        if (rows.isEmpty()) {
            return;
        }

        List<GuaranteedReviewImportRequest> requests = new ArrayList<>();
        boolean criticalFailure = false;

        try {
            prepareRequests(rows, requests, execution);
        } catch (Exception e) {
            log.error("Failed to prepare guaranteed review requests", e);
            criticalFailure = true;
        }

        try {
            sendRequests(requests, execution);
        } catch (Exception e) {
            log.error("Failed to send guaranteed review requests", e);
            criticalFailure = true;
        }

        if (criticalFailure) {
            guaranteedReviewImportErrorCounter.increment();
            executionService.finishFailed(execution, execution.getSuccessCount(), execution.getErrorCount(), SourceReadStatus.SUCCESS);
            return;
        }

        executionService.finishSuccessfully(execution, execution.getSuccessCount(), execution.getErrorCount());
        log.info("Guaranteed reviews import finished. Success: {}, failed: {}",
                execution.getSuccessCount(), execution.getErrorCount());
    }

    private void prepareRequests(
            List<List<Object>> rows,
            List<GuaranteedReviewImportRequest> requests,
            ImportExecution execution
    ) {
        Map<String, Long> mentorIdCache = new HashMap<>();
        String currentProjectName = "";

        for (List<Object> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }

            String projectInRow = readTrimmedCell(row, PROJECT_NAME_COLUMN_INDEX);
            String telegramRow = readTrimmedCell(row, TELEGRAM_COLUMN_INDEX);
            String languagesRaw = readTrimmedCell(row, LANGUAGES_COLUMN_INDEX);
            String priceRaw = readTrimmedCell(row, PRICE_COLUMN_INDEX);

            if (!projectInRow.isEmpty()) {
                currentProjectName = projectInRow;
            }

            String telegramUrl = TelegramLinkUtil.buildTelegramUrl(telegramRow);

            if (languagesRaw.isEmpty()) {
                guaranteedReviewImportErrorCounter.increment();
                continue;
            }

            Long mentorTelegramId = mentorIdCache.computeIfAbsent(telegramUrl, url -> {
                log.debug("Cache miss for {}, fetching ID from Profile Service", url);
                return profileServiceHttpClient.getTelegramUserIdByUrl(url);
            });

            if (mentorTelegramId == null) {
                log.warn("Skip: Mentor with url {} not found in Profile Service", telegramUrl);
                guaranteedReviewImportErrorCounter.increment();
                execution.recordFailure();
                continue;
            }

            int price = parsePrice(priceRaw);
            String projectType = RoadmapProjectType.fromRussianName(currentProjectName).name();

            for (String language : languagesRaw.split(",")) {
                String cleanLanguage = language.trim();
                if (cleanLanguage.isEmpty()) {
                    guaranteedReviewImportErrorCounter.increment();
                    continue;
                }

                GuaranteedReviewRequestDto request = new GuaranteedReviewRequestDto(telegramUrl, cleanLanguage, projectType, price);
                requests.add(new GuaranteedReviewImportRequest(request, mentorTelegramId));
            }
        }
    }

    private void sendRequests(
            List<GuaranteedReviewImportRequest> requests,
            ImportExecution execution
    ) {
        for (GuaranteedReviewImportRequest prepared : requests) {
            GuaranteedReviewRequestDto request = prepared.request();

            try {
                log.info("Importing: Project={}, Mentor={}, Language={}, Price={}",
                        request.getProjectType(), request.getTelegramUrl(),
                        request.getLanguage(), request.getPriceUsd());

                mentorServiceHttpClient.upsertGuaranteedReview(request, prepared.mentorTelegramId());
                execution.recordSuccess();
                guaranteedReviewImportSuccessCounter.increment();
            } catch (Exception e) {
                log.error("Failed to import review for mentor {} (lang: {}): {}",
                        request.getTelegramUrl(), request.getLanguage(), e.getMessage());
                guaranteedReviewImportErrorCounter.increment();
                execution.recordFailure();
            }
        }
    }

    private Integer parsePrice(String priceRaw) {
        if (priceRaw == null || priceRaw.isBlank()) {
            return 0;
        }
        String cleanPrice = priceRaw.replaceAll("[^0-9]", "");
        return cleanPrice.isEmpty() ? 0 : Integer.parseInt(cleanPrice);
    }

    private static String readTrimmedCell(List<Object> row, int columnIndex) {
        return row.size() > columnIndex
                ? String.valueOf(row.get(columnIndex)).trim()
                : "";
    }
}