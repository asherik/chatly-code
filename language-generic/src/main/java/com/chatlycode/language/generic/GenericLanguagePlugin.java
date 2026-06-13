package com.chatlycode.language.generic;

import com.chatlycode.language.spi.LanguageExtractor;
import com.chatlycode.language.spi.LanguagePlugin;

import java.util.List;

public final class GenericLanguagePlugin implements LanguagePlugin {

    private final String languageId;
    private final List<String> supportedExtensions;
    private final LanguageExtractor extractor;

    private GenericLanguagePlugin(String languageId, List<String> supportedExtensions, GenericPatternSet patterns) {
        this.languageId = languageId;
        this.supportedExtensions = List.copyOf(supportedExtensions);
        this.extractor = new GenericRegexExtractor(supportedExtensions, patterns);
    }

    public static GenericLanguagePlugin rust() {
        return new GenericLanguagePlugin("rust", List.of(".rs"), GenericPatternSet.rust());
    }

    public static GenericLanguagePlugin typescript() {
        return new GenericLanguagePlugin("typescript", List.of(".ts", ".tsx"), GenericPatternSet.typescript());
    }

    public static GenericLanguagePlugin javascript() {
        return new GenericLanguagePlugin("javascript", List.of(".js", ".jsx", ".mjs", ".cjs"), GenericPatternSet.typescript());
    }

    @Override
    public String languageId() {
        return languageId;
    }

    @Override
    public List<String> supportedExtensions() {
        return supportedExtensions;
    }

    @Override
    public LanguageExtractor extractor() {
        return extractor;
    }
}
