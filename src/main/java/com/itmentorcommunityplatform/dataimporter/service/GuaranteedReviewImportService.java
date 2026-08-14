package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.GuaranteedReviewRequestDto;
import com.itmentorcommunityplatform.dataimporter.google.GoogleSheetsClient;
import com.itmentorcommunityplatform.dataimporter.httpclient.MentorServiceHttpClient;
import com.itmentorcommunityplatform.dataimporter.httpclient.ProfileServiceHttpClient;
import com.itmentorcommunityplatform.dataimporter.model.RoadmapProjectType;
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
public class GuaranteedReviewImportService {

    private final GoogleSheetsClient googleSheetsClient;
    private final DataImporterProperties properties;
    private final ProfileServiceHttpClient profileServiceHttpClient;
    private final MentorServiceHttpClient mentorServiceHttpClient;

    private final Counter guaranteedReviewImportSuccessCounter;
    private final Counter guaranteedReviewImportErrorCounter;
    private final Timer guaranteedReviewImportDurationTimer;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "guaranteed-reviews-import-thread"));

    public void startImportAsync() {
        executor.submit(this::doImportMeasured);
    }

    private void doImportMeasured() {
        guaranteedReviewImportDurationTimer.record(this::doImport);
    }

    private void doImport() {
        log.info("Starting guaranteed reviews import...");
        try {
            List<List<Object>> rows = googleSheetsClient.readSheet(
                    properties.getGuaranteedReviewsSpreadsheetId(),
                    properties.getSheetRangeGuaranteedReviews()
            );
            if (rows == null || rows.isEmpty()) {
                log.warn("No data found in the specified range.");
                return;
            }

            Map<String, Long> mentorIdCache = new HashMap<>();

            String currentProjectName = "";
            int totalImportedCount = 0;

            for (List<Object> row : rows) {
                if (row == null || row.isEmpty()) {
                    guaranteedReviewImportErrorCounter.increment();
                    continue;
                }

                String projectInRow = row.size() > 0 ? String.valueOf(row.get(0)).trim() : "";
                String telegramRow = row.size() > 1 ? String.valueOf(row.get(1)).trim() : "";
                String languagesRaw = row.size() > 2 ? String.valueOf(row.get(2)).trim() : "";
                String priceRaw = row.size() > 3 ? String.valueOf(row.get(3)).trim() : "";

                if (!projectInRow.isEmpty()) {
                    currentProjectName = projectInRow;
                }

                String telegramUrl = tgUrlFromTgName(telegramRow);

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

                        totalImportedCount++;
                        guaranteedReviewImportSuccessCounter.increment();
                    } catch (Exception e) {
                        log.error("Failed to import review for mentor {} (lang: {}): {}",
                                telegramUrl, cleanLang, e.getMessage());
                        guaranteedReviewImportErrorCounter.increment();
                    }
                }
            }
            log.info("Guaranteed reviews import finished. Total successfully imported: {}", totalImportedCount);
        } catch (Exception e) {
            log.error("Critical error during guaranteed reviews import", e);
        }
    }

    private String tgUrlFromTgName(String telegram) {
        if (telegram == null || !telegram.contains("@")) {
            return "";
        }

        return "https://t.me/" + telegram.substring(telegram.indexOf("@") + 1).trim();
    }

    private Integer parsePrice(String priceRaw) {
        if (priceRaw == null || priceRaw.isBlank()) {
            return 0;
        }
        String cleanPrice = priceRaw.replaceAll("[^0-9]", "");
        return cleanPrice.isEmpty() ? 0 : Integer.parseInt(cleanPrice);
    }
}