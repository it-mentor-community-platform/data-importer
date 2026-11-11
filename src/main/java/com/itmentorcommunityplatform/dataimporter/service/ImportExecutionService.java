package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.model.ImportExecution;
import com.itmentorcommunityplatform.dataimporter.model.ImportExecutionStatus;
import com.itmentorcommunityplatform.dataimporter.model.SourceReadStatus;
import com.itmentorcommunityplatform.dataimporter.repository.ImportExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ImportExecutionService {

    private final ImportExecutionRepository executionRepository;

    public ImportExecution start(Long typeId) {
        return executionRepository.save(ImportExecution.builder()
                .typeId(typeId)
                .status(ImportExecutionStatus.RUNNING)
                .startedAt(Instant.now().getEpochSecond())
                .build());
    }

    public void finishSuccessfully(ImportExecution execution, int success, int failed) {
        execution.completeSuccessfully(success, failed);
        executionRepository.save(execution);
    }

    public void finishFailed(ImportExecution execution, int success, int failed, SourceReadStatus readStatus) {
        execution.completeFailed(success, failed, readStatus);
        executionRepository.save(execution);
    }

    public boolean isRunning(Long typeId) {
        return executionRepository.existsByTypeIdAndStatus(typeId, ImportExecutionStatus.RUNNING);
    }
}