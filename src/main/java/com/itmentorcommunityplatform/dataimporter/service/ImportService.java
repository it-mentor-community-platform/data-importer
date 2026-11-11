package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.dto.response.ImportExecutionResponseDto;
import com.itmentorcommunityplatform.dataimporter.exception.ImportAlreadyRunningException;
import com.itmentorcommunityplatform.dataimporter.exception.ImportNotImplementedException;
import com.itmentorcommunityplatform.dataimporter.exception.UnknownImportTypeException;
import com.itmentorcommunityplatform.dataimporter.model.ImportExecution;
import com.itmentorcommunityplatform.dataimporter.model.ImportType;
import com.itmentorcommunityplatform.dataimporter.repository.ImportTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final ImportTypeRepository importTypeRepository;
    private final ImportExecutionService importExecutionService;
    private final Map<ImportType, ImportCommand> importCommands;

    public ImportExecutionResponseDto start(String typeCode) {
        ImportType type = importTypeRepository.findByCode(typeCode)
                .orElseThrow(() -> new UnknownImportTypeException(typeCode));

        ImportCommand importCommand = importCommands.get(type);

        if (importCommand == null) {
            throw new ImportNotImplementedException(typeCode);
        }

        if (importExecutionService.isRunning(type.getId())) {
            throw new ImportAlreadyRunningException(typeCode);
        }

        ImportExecution execution = importExecutionService.start(type.getId());

        ImportExecutionResponseDto response = new ImportExecutionResponseDto(
                execution.getId(), type.getCode(), execution.getStatus());

        importCommand.startImportAsync(execution);

        return response;
    }
}