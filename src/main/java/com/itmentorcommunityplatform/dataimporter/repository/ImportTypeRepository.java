package com.itmentorcommunityplatform.dataimporter.repository;

import com.itmentorcommunityplatform.dataimporter.model.ImportType;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ImportTypeRepository extends CrudRepository<ImportType, Long> {

    Optional<ImportType> findByCode(String code);
}