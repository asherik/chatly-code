package com.chatlycode.language.spi;

public interface LanguageExtractor {

    boolean supports(SourceFile sourceFile);

    ExtractionResult extract(SourceFile sourceFile);
}
