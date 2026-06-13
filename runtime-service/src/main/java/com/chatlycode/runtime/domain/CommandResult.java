package com.chatlycode.runtime.domain;

import java.time.Duration;

public record CommandResult(int exitCode, String stdout, String stderr, Duration duration) {
}
