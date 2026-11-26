package com.suwontrip.app.catalog;

public final class CatalogLoadException extends RuntimeException {
    private final String fieldPath;

    public CatalogLoadException(String fieldPath, String message) {
        super(fieldPath + ": " + message);
        this.fieldPath = fieldPath;
    }

    public CatalogLoadException(String fieldPath, String message, Throwable cause) {
        super(fieldPath + ": " + message, cause);
        this.fieldPath = fieldPath;
    }

    public String getFieldPath() {
        return fieldPath;
    }
}
