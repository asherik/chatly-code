package com.chatlycode.graph.inventory;

import java.nio.file.Path;
import java.time.Instant;

public record InventoryFile(Path relativePath, long size, Instant modifiedAt) {
}
