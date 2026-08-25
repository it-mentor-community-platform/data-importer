package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.config.DataImporterProperties;
import com.itmentorcommunityplatform.dataimporter.dto.request.QuestionUpsertRequestDto;
import com.itmentorcommunityplatform.dataimporter.google.GoogleSheetsClient;
import com.itmentorcommunityplatform.dataimporter.httpclient.IntervalRepetitionServiceHttpClient;
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
public class QuestionImportService {

    private static final String ANSWERS_SHEET_PREFIX = "Ответы - ";

    private final GoogleSheetsClient googleSheetsClient;
    private final DataImporterProperties properties;
    private final IntervalRepetitionServiceHttpClient httpClient;

    private final Counter questionsImportSuccessCounter;
    private final Counter questionsImportErrorCounter;
    private final Timer questionsImportDurationTimer;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "question-import-thread"));

    public void startImportAsync() {
        executor.submit(this::doImportMeasured);
    }

    private void doImportMeasured() {
        questionsImportDurationTimer.record(this::doImport);
    }

    private void doImport() {
        log.info("Starting question import (async)...");

        int processed = 0;

        List<DataImporterProperties.InterviewQuestionSource> sources =
                properties.getInterviewQuestionSources();

        for (DataImporterProperties.InterviewQuestionSource source : sources) {
            try {
                processed += importSource(source);
            } catch (Exception e) {
                log.error("Failed to import questions for specialization={}", source.getSpecialization(), e);
            }
        }

        log.info("Question import finished. Total processed {} questions", processed);

    }

    private int importSource(DataImporterProperties.InterviewQuestionSource source) throws IOException {
        log.info("Importing source={}...", source.getSpecialization());

        int processed = 0;
        String specialization = source.getSpecialization();
        String spreadsheetId = source.getSpreadsheetId();

        List<String> sheetNames = googleSheetsClient.getSheetNames(spreadsheetId);

        List<String> questionSheets = sheetNames.stream()
                .filter(name -> name.startsWith(ANSWERS_SHEET_PREFIX))
                .toList();

        for (String sheetName : questionSheets) {
            try {
                processed += importSheet(spreadsheetId, specialization, sheetName);
            } catch (Exception e) {
                log.error("Failed to import sheet={}, specialization={}", sheetName, specialization, e);
            }
        }

        log.info("Importing source={} completed. Question processed - {}", source.getSpecialization(), processed);
        return processed;
    }

    private int importSheet(String spreadsheetId, String specialization, String sheetName) throws IOException {
        String category = sheetName.substring(ANSWERS_SHEET_PREFIX.length());
        int processed = 0;

        log.info("Importing questions: specialization={}, category={}", specialization, category);

        List<List<Object>> rows = googleSheetsClient
                .readSheet(spreadsheetId, sheetName);

        if (rows.isEmpty()) {
            log.info("Sheet returned empty result.");
            return processed;
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
                processed++;
                log.info("Imported question: Specialization = {}, Category = {}, Title = {}",
                        requestDto.specialization(),
                        requestDto.category(),
                        requestDto.title());
            } catch (Exception ex) {
                questionsImportErrorCounter.increment();
                log.error("Failed to import question at row {}.Specialization = {}, Category = {}, Title = {}",
                        i,
                        requestDto.specialization(),
                        requestDto.category(),
                        requestDto.title());
            }

        }

        log.info("Questions with specialization = {} and category = {} import finished. Total processed: {}.",
                specialization,
                category,
                processed);

        return processed;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

}
