package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.MentorDescriptionDto;
import com.itmentorcommunityplatform.dataimporter.dto.request.MentorUpsertRequestDto;
import com.itmentorcommunityplatform.dataimporter.google.GoogleSheetsClient;
import com.itmentorcommunityplatform.dataimporter.httpclient.MentorServiceHttpClient;
import com.itmentorcommunityplatform.dataimporter.model.ImportExecution;
import com.itmentorcommunityplatform.dataimporter.model.ImportType;
import com.itmentorcommunityplatform.dataimporter.model.SourceReadStatus;
import com.itmentorcommunityplatform.dataimporter.util.TelegramLinkUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MentorImportService implements ImportCommand {

    private final GoogleSheetsClient googleSheetsClient;
    private final DataImporterProperties properties;
    private final MentorServiceHttpClient httpClient;

    private final Counter mentorImportSuccessCounter;
    private final Counter mentorImportErrorCounter;
    private final Timer mentorImportDurationTimer;
    private final ImportExecutionService executionService;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "mentor-import-thread"));

    @Override
    public ImportType getImportType() {
        return new ImportType("MENTORS");
    }

    @Override
    public void startImportAsync(ImportExecution execution) {
        executor.submit(() -> {
            try {
                doImportMeasured(execution);
            } catch (Exception e) {
                log.error("Unexpected error in mentor import, executionId={}", execution.getId(), e);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private void doImportMeasured(ImportExecution execution) {
        mentorImportDurationTimer.record(() -> doImport(execution));
    }

    private void doImport(ImportExecution execution) {
        log.info("Starting mentor import (async)...");


        List<List<Object>> rows;
        try {
            rows = googleSheetsClient.readSheet(properties.getMentorSpreadsheetId(),
                    properties.getSheetRangeMentors());
        } catch (Exception e) {
            log.error("Failed to read mentors sheet", e);
            mentorImportErrorCounter.increment();
            executionService.finishFailed(execution, 0, 0, SourceReadStatus.FAILED);
            return;
        }

        if (rows.isEmpty()) {
            log.info("Sheet returned empty result.");
        }

        int successCount = 0;
        int failedCount = 0;
        try {
            for (int i = 0; i < rows.size(); i++) {
                List<Object> row = rows.get(i);

                if (row == null || row.isEmpty()) {
                    log.warn("Skipping empty row at index {}", i);
                    continue;
                }

                String tgRaw = row.size() > 11 ? String.valueOf(row.get(11)).trim() : "";

                if (tgRaw.isEmpty()) {
                    log.warn("Skipping row with empty telegram id at index {}", i);
                    continue;
                }

                Long tgId;
                try {
                    tgId = Long.parseLong(tgRaw);
                } catch (NumberFormatException ex) {
                    log.warn("Invalid telegram id '{}' at row index {}, skipping.", tgRaw, i);
                    mentorImportErrorCounter.increment();
                    failedCount++;
                    continue;
                }

                try {
                    MentorUpsertRequestDto mentor = buildMentorDto(row, tgId);

                    httpClient.insertMentor(mentor);
                    mentorImportSuccessCounter.increment();
                    successCount++;

                    log.info("Mentor: telegramId={}, name={}, telegramUrl={}, languages={}, services={}," +
                                    "description={}",
                            tgId, mentor.description().name(), mentor.telegramUrl(),
                            mentor.programmingLanguages(), mentor.services(), mentor.description());

                } catch (Exception ex) {
                    mentorImportErrorCounter.increment();
                    failedCount++;
                    log.error("Failed to import mentor telegramId={}", tgId, ex);
                }
            }
        } catch (Exception e) {
            log.error("Failed to import mentors", e);
            mentorImportErrorCounter.increment();
            executionService.finishFailed(execution, successCount, failedCount, SourceReadStatus.SUCCESS);
            return;
        }

        executionService.finishSuccessfully(execution, successCount, failedCount);
        log.info("Mentor import finished. Success: {}, failed: {}", successCount, failedCount);
    }

    private MentorUpsertRequestDto buildMentorDto(List<Object> row, Long tgId) {
        String name = row.get(2).toString().trim();

        String tgUsername = row.get(3).toString().trim();
        String telegramUrl = TelegramLinkUtil.buildTelegramUrl(tgUsername);

        String languagesRaw = String.valueOf(row.get(4)).trim();
        List<String> languages = getListFromCommaSeparatedString(languagesRaw);

        String servicesRaw = row.get(5).toString().trim();
        List<String> services = getListFromCommaSeparatedString(servicesRaw);

        String cost = row.get(6).toString().trim();
        String description = row.get(7).toString().trim();

        MentorDescriptionDto mentorDescriptionDto = new MentorDescriptionDto(
                name,
                cost,
                description
        );

        return new MentorUpsertRequestDto(
                tgId,
                telegramUrl,
                mentorDescriptionDto,
                languages,
                services
        );
    }

    private List<String> getListFromCommaSeparatedString(String string) {
        if (string == null) {
            return List.of();
        }

        return Arrays.stream(
                        string.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }
}