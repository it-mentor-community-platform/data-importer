package com.itmentorcommunityplatform.dataimporter.exception;

public class ImportExecutionNotFoundException extends RuntimeException {
    public ImportExecutionNotFoundException(Long id) {
        super("Import execution with id %d was not found".formatted(id));
    }
}