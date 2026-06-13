package com.chatlycode.graph.mapper;

import com.chatlycode.graph.domain.NodeKind;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class GraphNodeIdGenerator {

    private GraphNodeIdGenerator() {
    }

    public static String generate(Path filePath, NodeKind kind, String name, int line) {
        String normalizedPath = filePath.toString().replace('\\', '/');
        String payload = normalizedPath + ":" + kind.name().toLowerCase() + ":" + name + ":" + line;
        return kind.name().toLowerCase() + ":" + sha256(payload).substring(0, 32);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
