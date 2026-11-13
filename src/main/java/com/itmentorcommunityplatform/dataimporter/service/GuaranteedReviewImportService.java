package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.GuaranteedReviewRequestDto;
import com.itmentorcommunityplatform.dataimporter.google.GoogleSheetsClient;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GuaranteedReviewImportService implements ImportCommand {

    private final GoogleSheetsClient googleSheetsClient;
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
        return new ImportType("GUARANTEED_REVIEWS");
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

        List<List<Object>> rows;
        try {
            rows = googleSheetsClient.readSheet(
                    properties.getGuaranteedReviewsSpreadsheetId(),
                    properties.getSheetRangeGuaranteedReviews());
        } catch (Exception e) {
            log.error("Failed to read guaranteed review sheet", e);
            guaranteedReviewImportErrorCounter.increment();
            executionService.finishFailed(execution, 0, 0, SourceReadStatus.FAILED);
            return;
        }

        if (rows == null || rows.isEmpty()) {
            log.warn("No data found in the specified range.");
        }

        Map<String, Long> mentorIdCache = new HashMap<>();
        String currentProjectName = "";
        int successCount = 0;
        int failedCount = 0;
        try {
            for (List<Object> row : rows) {

                if (row == null || row.isEmpty()) {
                    continue;
                }

                String projectInRow = row.size() > 0 ? String.valueOf(row.get(0)).trim() : "";
                String telegramRow = row.size() > 1 ? String.valueOf(row.get(1)).trim() : "";
                String languagesRaw = row.size() > 2 ? String.valueOf(row.get(2)).trim() : "";
                String priceRaw = row.size() > 3 ? String.valueOf(row.get(3)).trim() : "";

                if (!projectInRow.isEmpty()) {
                    currentProjectName = projectInRow;
                }

                String telegramUrl = TelegramLinkUtil.buildTelegramUrl(telegramRow);

                if (telegramUrl.isEmpty() || languagesRaw.isEmpty()) {
                    guaranteedReviewImportErrorCounter.increment();
                    continue;
                }

                Long mentorTelegramId = mentorIdCache.computeIfAbsent(telegramUrl, url -> {
                    log.debug("Cache miss for {}, fetching ID from Profile Service", url);
                    return profileServiceHttpClient.getTelegramUserIdByUrl(url);
                });

                if (mentorTelegramId == null) {
                    log.warn("Skip: Mentor with url {} not found in Profile Service", telegramUrl);
                    mentorIdCache.remove(telegramUrl);
                    guaranteedReviewImportErrorCounter.increment();
                    failedCount++;
                    continue;
                }

                int price = parsePrice(priceRaw);
                String[] languages = languagesRaw.split(",");

                for (String lang : languages) {
                    String cleanLang = lang.trim();
                    if (cleanLang.isEmpty()) {
                        guaranteedReviewImportErrorCounter.increment();
                        continue;
                    }

                    try {
                        String projectType = RoadmapProjectType.fromRussianName(currentProjectName).name();

                        log.info("Importing: Project={}, Mentor={}, Language={}, Price={}",
                                projectType, telegramUrl, cleanLang, priceRaw);

                        mentorServiceHttpClient.upsertGuaranteedReview(new GuaranteedReviewRequestDto(
                                telegramUrl, cleanLang, projectType, price
                        ), mentorTelegramId);

                        successCount++;
                        guaranteedReviewImportSuccessCounter.increment();
                    } catch (Exception e) {
                        log.error("Failed to import review for mentor {} (lang: {}): {}",
                                telegramUrl, cleanLang, e.getMessage());
                        guaranteedReviewImportErrorCounter.increment();
                        failedCount++;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Critical error during guaranteed reviews import", e);
            guaranteedReviewImportErrorCounter.increment();
            executionService.finishFailed(execution, successCount, failedCount, SourceReadStatus.SUCCESS);
            return;
        }

        executionService.finishSuccessfully(execution, successCount, failedCount);
        log.info("Guaranteed reviews import finished.. Success: {}, failed: {}",
                successCount, failedCount);
    }

    private Integer parsePrice(String priceRaw) {
        if (priceRaw == null || priceRaw.isBlank()) {
            return 0;
        }
        String cleanPrice = priceRaw.replaceAll("[^0-9]", "");
        return cleanPrice.isEmpty() ? 0 : Integer.parseInt(cleanPrice);
    }
}