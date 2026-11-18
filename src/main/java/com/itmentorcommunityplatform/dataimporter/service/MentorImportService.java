package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.MentorDescriptionDto;
import com.itmentorcommunityplatform.dataimporter.dto.request.MentorUpsertRequestDto;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MentorImportService implements ImportCommand {
    private static final int NAME_COLUMN_INDEX = 2;
    private static final int TELEGRAM_USERNAME_COLUMN_INDEX = 3;
    private static final int LANGUAGES_COLUMN_INDEX = 4;
    private static final int SERVICES_COLUMN_INDEX = 5;
    private static final int COST_COLUMN_INDEX = 6;
    private static final int DESCRIPTION_COLUMN_INDEX = 7;
    private static final int TELEGRAM_ID_COLUMN_INDEX = 11;

    private final ImportSheetReader importSheetReader;
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
        return ImportType.MENTORS;
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

        List<List<Object>> rows = importSheetReader.readRowsOrFinishImport(execution,
                properties.getMentorSpreadsheetId(), properties.getSheetRangeMentors(), mentorImportErrorCounter);

        if (rows.isEmpty()) {
            return;
        }

        try {
            List<MentorUpsertRequestDto> requests = prepareRequests(rows, execution);
            sendRequests(requests, execution);
        } catch (Exception e) {
            log.error("Failed to import mentors", e);
            mentorImportErrorCounter.increment();
            executionService.finishFailed(execution, execution.getSuccessCount(), execution.getErrorCount(), SourceReadStatus.SUCCESS);
            return;
        }

        executionService.finishSuccessfully(execution, execution.getSuccessCount(), execution.getErrorCount());
        log.info("Mentor import finished. Success: {}, failed: {}", execution.getSuccessCount(), execution.getErrorCount());
    }

    private List<MentorUpsertRequestDto> prepareRequests(
            List<List<Object>> rows, ImportExecution execution
    ) {
        List<MentorUpsertRequestDto> requests = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            if (row == null || row.isEmpty()) {
                log.warn("Skipping empty row at index {}", i);
                continue;
            }

            String rawTelegramId = row.size() > TELEGRAM_ID_COLUMN_INDEX
                    ? String.valueOf(row.get(TELEGRAM_ID_COLUMN_INDEX)).trim()
                    : "";

            if (rawTelegramId.isEmpty()) {
                log.warn("Skipping row with empty telegram id at index {}", i);
                continue;
            }

            Long telegramId;
            try {
                telegramId = Long.valueOf(rawTelegramId);
            } catch (NumberFormatException e) {
                log.warn("Invalid telegram id '{}' at row index {}, skipping.", rawTelegramId, i);
                mentorImportErrorCounter.increment();
                execution.recordFailure();
                continue;
            }

            try {
                requests.add(buildMentorDto(row, telegramId));
            } catch (Exception e) {
                mentorImportErrorCounter.increment();
                execution.recordFailure();
                log.error("Failed to prepare mentor at row index {}, telegramId={}", i, telegramId, e);
            }
        }

        return requests;
    }

    private void sendRequests(List<MentorUpsertRequestDto> requests, ImportExecution execution) {
        for (MentorUpsertRequestDto request : requests) {
            try {
                httpClient.insertMentor(request);
                mentorImportSuccessCounter.increment();
                execution.recordSuccess();
                log.info("Mentor: telegramId={}, name={}, telegramUrl={}, languages={}, services={}, description={}",
                        request.mentorTelegramUserId(), request.description().name(),
                        request.telegramUrl(), request.programmingLanguages(),
                        request.services(), request.description());
            } catch (Exception e) {
                mentorImportErrorCounter.increment();
                execution.recordFailure();
                log.error("Failed to import mentor telegramId={}", request.mentorTelegramUserId(), e);
            }
        }
    }

    private MentorUpsertRequestDto buildMentorDto(List<Object> row, Long telegramId) {
        String name = row.get(NAME_COLUMN_INDEX).toString().trim();
        String telegramUsername = row.get(TELEGRAM_USERNAME_COLUMN_INDEX).toString().trim();
        String telegramUrl = TelegramLinkUtil.buildTelegramUrl(telegramUsername);

        String rawLanguages = String.valueOf(row.get(LANGUAGES_COLUMN_INDEX)).trim();
        List<String> languages = splitCommaSeparatedValues(rawLanguages);

        String rawServices = row.get(SERVICES_COLUMN_INDEX).toString().trim();
        List<String> services = splitCommaSeparatedValues(rawServices);

        String cost = row.get(COST_COLUMN_INDEX).toString().trim();
        String description = row.get(DESCRIPTION_COLUMN_INDEX).toString().trim();
        MentorDescriptionDto mentorDescription = new MentorDescriptionDto(name, cost, description);

        return new MentorUpsertRequestDto(
                telegramId, telegramUrl, mentorDescription, languages, services
        );
    }

    private List<String> splitCommaSeparatedValues(String string) {
        if (string == null) {
            return List.of();
        }

        return Arrays.stream(string.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }
}