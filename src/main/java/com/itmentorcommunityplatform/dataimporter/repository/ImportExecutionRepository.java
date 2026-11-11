package com.itmentorcommunityplatform.dataimporter.repository;

import com.itmentorcommunityplatform.dataimporter.model.ImportExecution;
import com.itmentorcommunityplatform.dataimporter.model.ImportExecutionStatus;
import org.springframework.data.repository.CrudRepository;

public interface ImportExecutionRepository extends CrudRepository<ImportExecution, Long> {

    boolean existsByTypeIdAndStatus(Long typeId, ImportExecutionStatus status);
}