package com.chatlycode.language.java;

import com.chatlycode.language.java.extract.JavaSourceExtractor;
import com.chatlycode.language.spi.LanguageExtractor;
import com.chatlycode.language.spi.LanguagePlugin;

import java.util.List;

public final class JavaLanguagePlugin implements LanguagePlugin {

    private final JavaSourceExtractor extractor = new JavaSourceExtractor();

    @Override
    public String languageId() {
        return "java";
    }

    @Override
    public List<String> supportedExtensions() {
        return List.of(".java");
    }

    @Override
    public LanguageExtractor extractor() {
        return extractor;
    }
}
