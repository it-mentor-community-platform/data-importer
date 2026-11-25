package com.itmentorcommunityplatform.dataimporter.exception;

public class UnknownImportTypeException extends RuntimeException {
    public UnknownImportTypeException(String typeCode) {
        super("Unknown import type: ".concat(typeCode));
    }
}