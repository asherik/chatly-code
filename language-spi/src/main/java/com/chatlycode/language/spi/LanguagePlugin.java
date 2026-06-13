package com.chatlycode.language.spi;

import java.util.List;

public interface LanguagePlugin {

    String languageId();

    List<String> supportedExtensions();

    LanguageExtractor extractor();
}
