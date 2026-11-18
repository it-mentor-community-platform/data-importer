package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.google.GoogleSheetsClient;
import com.itmentorcommunityplatform.dataimporter.model.ImportExecution;
import com.itmentorcommunityplatform.dataimporter.model.SourceReadStatus;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportSheetReader {

    private final GoogleSheetsClient googleSheetsClient;
    private final ImportExecutionService executionService;

    public List<List<Object>> readRowsOrFinishImport(
            ImportExecution execution, String spreadsheetId, String range, Counter errorCounter
    ) {
        List<List<Object>> rows;
        try {
            rows = googleSheetsClient.readSheet(spreadsheetId, range);
        } catch (Exception e) {
            log.error("Failed to read sheet for import {}, executionId={}",
                    execution.getType(), execution.getId(), e);
            errorCounter.increment();
            executionService.finishFailed(execution, 0, 0, SourceReadStatus.FAILED);
            return List.of();
        }

        if (rows == null || rows.isEmpty()) {
            log.info("Sheet is empty for import {}, executionId={}",
                    execution.getType(), execution.getId());
            executionService.finishSuccessfully(execution, 0, 0);
            return List.of();
        }

        return rows;
    }
}