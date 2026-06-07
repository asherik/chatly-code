package com.chatlycode.language.spi;

import java.nio.file.Path;

public record ExtractionError(String message, Path filePath, String severity, String code) {

    public ExtractionError {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message must not be blank");
        }
        severity = severity == null ? "error" : severity;
        code = code == null ? "" : code;
    }
}
