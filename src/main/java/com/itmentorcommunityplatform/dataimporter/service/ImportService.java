package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.dto.response.ImportExecutionResponseDto;
import com.itmentorcommunityplatform.dataimporter.exception.ImportAlreadyRunningException;
import com.itmentorcommunityplatform.dataimporter.exception.ImportNotImplementedException;
import com.itmentorcommunityplatform.dataimporter.exception.UnknownImportTypeException;
import com.itmentorcommunityplatform.dataimporter.model.ImportExecution;
import com.itmentorcommunityplatform.dataimporter.model.ImportType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final ImportExecutionService importExecutionService;
    private final Map<ImportType, ImportCommand> importCommands;

    public ImportExecutionResponseDto start(String typeCode) {
        ImportType type;

        try {
            type = ImportType.valueOf(typeCode);
        } catch (IllegalArgumentException e) {
            throw new UnknownImportTypeException(typeCode);
        }

        ImportCommand importCommand = importCommands.get(type);

        if (importCommand == null) {
            throw new ImportNotImplementedException(typeCode);
        }

        if (importExecutionService.isRunning(type)) {
            throw new ImportAlreadyRunningException(typeCode);
        }

        ImportExecution execution = importExecutionService.start(type);

        ImportExecutionResponseDto response = new ImportExecutionResponseDto(
                execution.getId(), type.name(), execution.getStatus());

        importCommand.startImportAsync(execution);

        return response;
    }
}