package com.chatlycode.shared.time;

import java.time.Clock;
import java.time.Instant;

public final class ClockProvider {

    private final Clock clock;

    public ClockProvider(Clock clock) {
        this.clock = clock;
    }

    public static ClockProvider systemUtc() {
        return new ClockProvider(Clock.systemUTC());
    }

    public Instant now() {
        return Instant.now(clock);
    }
}
