package com.chatlycode.shared.id;

import java.util.UUID;

public final class Ids {

    private Ids() {
    }

    public static String newId(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("Prefix must not be blank");
        }
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
