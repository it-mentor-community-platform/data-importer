package com.itmentorcommunityplatform.dataimporter.service;

import com.itmentorcommunityplatform.dataimporter.model.ImportExecution;
import com.itmentorcommunityplatform.dataimporter.model.ImportType;

public interface ImportCommand {

    ImportType getImportType();

    void startImportAsync(ImportExecution execution);
}