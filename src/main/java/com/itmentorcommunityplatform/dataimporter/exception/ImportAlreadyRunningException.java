package com.itmentorcommunityplatform.dataimporter.exception;

public class ImportAlreadyRunningException extends RuntimeException {
    public ImportAlreadyRunningException(String typeCode) {
        super("Import of type %s is already running".formatted(typeCode));
    }
}