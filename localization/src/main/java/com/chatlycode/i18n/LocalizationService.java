package com.chatlycode.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class LocalizationService {

    private static final String BUNDLE_NAME = "i18n.messages";

    public String message(Locale locale, String key, Object... args) {
        Locale effectiveLocale = locale == null ? Locale.ENGLISH : locale;
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, effectiveLocale);
            return MessageFormat.format(bundle.getString(key), args);
        } catch (MissingResourceException exception) {
            return key;
        }
    }
}
