package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.QuestionUpsertRequestDto;
import com.itmentorcommunityplatform.dataimporter.google.GoogleSheetsClient;
import com.itmentorcommunityplatform.dataimporter.httpclient.IntervalRepetitionServiceHttpClient;
import com.itmentorcommunityplatform.dataimporter.model.ImportExecution;
import com.itmentorcommunityplatform.dataimporter.model.ImportType;
import com.itmentorcommunityplatform.dataimporter.model.SourceReadStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuestionImportService implements ImportCommand {

    private static final String ANSWERS_SHEET_PREFIX = "Ответы - ";

    private final GoogleSheetsClient googleSheetsClient;
    private final DataImporterProperties properties;
    private final IntervalRepetitionServiceHttpClient httpClient;

    private final Counter questionsImportSuccessCounter;
    private final Counter questionsImportErrorCounter;
    private final Timer questionsImportDurationTimer;
    private final ImportExecutionService executionService;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "question-import-thread"));

    @Override
    public ImportType getImportType() {
        return new ImportType("QUESTIONS");
    }

    @Override
    public void startImportAsync(ImportExecution execution) {
        executor.submit(() -> {
            try {
                doImportMeasured(execution);
            } catch (Exception e) {
                log.error("Unexpected error in question import, executionId={}", execution.getId(), e);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private void doImportMeasured(ImportExecution execution) {
        questionsImportDurationTimer.record(() -> doImport(execution));
    }

    private void doImport(ImportExecution execution) {
        log.info("Starting question import (async)...");

        boolean sourceReadFailed = false;

        try {
            for (DataImporterProperties.InterviewQuestionSource source : properties.getInterviewQuestionSources()) {
                try {
                    if (!importSource(execution, source)) {
                        sourceReadFailed = true;
                    }
                } catch (Exception e) {
                    sourceReadFailed = true;
                    log.error("Failed to import questions for specialization={}", source.getSpecialization(), e);
                }
            }
        } catch (Exception e) {
            log.error("Question import aborted, executionId={}", execution.getId(), e);
            executionService.finishFailed(execution, execution.getSuccessCount(), execution.getErrorCount(),
                    sourceReadFailed ? SourceReadStatus.FAILED : SourceReadStatus.SUCCESS);
            return;
        }

        if (sourceReadFailed) {
            executionService.finishFailed(execution, execution.getSuccessCount(), execution.getErrorCount(),
                    SourceReadStatus.FAILED);
        } else {
            executionService.finishSuccessfully(execution, execution.getSuccessCount(), execution.getErrorCount());
        }

        log.info("Question import finished. Success: {}, failed: {}",
                execution.getSuccessCount(), execution.getErrorCount());
    }

    private boolean importSource(ImportExecution execution, DataImporterProperties.InterviewQuestionSource source)
            throws IOException {
        log.info("Importing source={}...", source.getSpecialization());

        String specialization = source.getSpecialization();
        String spreadsheetId = source.getSpreadsheetId();

        List<String> sheetNames = googleSheetsClient.getSheetNames(spreadsheetId);

        List<String> questionSheets = sheetNames.stream()
                .filter(name -> name.startsWith(ANSWERS_SHEET_PREFIX))
                .toList();

        boolean allSheetsRead = true;
        for (String sheetName : questionSheets) {
            try {
                importSheet(execution, spreadsheetId, specialization, sheetName);
            } catch (Exception e) {
                allSheetsRead = false;
                log.error("Failed to import sheet={}, specialization={}", sheetName, specialization, e);
            }
        }

        log.info("Importing source={} completed.", source.getSpecialization());

        return allSheetsRead;
    }

    private void importSheet(ImportExecution execution, String spreadsheetId, String specialization, String sheetName)
            throws IOException {
        String category = sheetName.substring(ANSWERS_SHEET_PREFIX.length());

        log.info("Importing questions: specialization={}, category={}", specialization, category);

        List<List<Object>> rows = googleSheetsClient
                .readSheet(spreadsheetId, sheetName);

        if (rows.isEmpty()) {
            log.info("Sheet returned empty result.");
            return;
        }

        for (int i = 1; i < rows.size(); i++) {
            List<Object> row = rows.get(i);

            if (row == null || row.size() < 2) {
                log.warn("Skipping invalid row: sheet={}, row={}", sheetName, i + 1);
                continue;
            }

            String title = String.valueOf(row.get(0)).trim();
            String answer = String.valueOf(row.get(1)).trim();

            if (title.isBlank() || answer.isBlank()) {
                log.warn("Skipping empty question: sheet={}, row={}", sheetName, i + 1);
                continue;
            }

            QuestionUpsertRequestDto requestDto = new QuestionUpsertRequestDto(specialization, category, title, answer);

            try {
                httpClient.upsertQuestion(requestDto);
                questionsImportSuccessCounter.increment();
                execution.recordSuccess();
                log.info("Imported question: Specialization = {}, Category = {}, Title = {}",
                        requestDto.specialization(),
                        requestDto.category(),
                        requestDto.title());
            } catch (Exception ex) {
                questionsImportErrorCounter.increment();
                execution.recordFailure();
                log.error("Failed to import question at row {}.Specialization = {}, Category = {}, Title = {}",
                        i,
                        requestDto.specialization(),
                        requestDto.category(),
                        requestDto.title(),
                        ex);
            }

        }

        log.info("Questions with specialization = {} and category = {} import finished.",
                specialization,
                category);
    }
}