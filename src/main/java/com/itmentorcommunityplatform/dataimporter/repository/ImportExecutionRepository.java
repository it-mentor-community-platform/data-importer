package com.itmentorcommunityplatform.dataimporter.repository;

import com.itmentorcommunityplatform.dataimporter.model.ImportExecution;
import com.itmentorcommunityplatform.dataimporter.model.ImportExecutionStatus;
import com.itmentorcommunityplatform.dataimporter.model.ImportType;
import org.springframework.data.repository.CrudRepository;

public interface ImportExecutionRepository extends CrudRepository<ImportExecution, Long> {

    boolean existsByTypeAndStatus(ImportType type, ImportExecutionStatus status);
}