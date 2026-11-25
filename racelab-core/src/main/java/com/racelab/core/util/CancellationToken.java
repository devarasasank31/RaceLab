package com.racelab.core.util;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Token for cooperative cancellation of long-running operations.
 * Thread-safe and can be shared across multiple threads.
 */
public final class CancellationToken {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    @Getter
    private String reason;

    private CancellationToken(String reason) {
        this.reason = reason;
    }

    public static CancellationToken create() {
        return new CancellationToken("Cancelled by user");
    }

    public static CancellationToken create(String reason) {
        return new CancellationToken(reason);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void cancel() {
        cancelled.set(true);
    }

    public void cancel(String reason) {
        this.reason = reason;
        cancelled.set(true);
    }

    public void throwIfCancelled() {
        if (cancelled.get()) {
            throw new CancellationException(reason);
        }
    }

    public static class CancellationException extends RuntimeException {
        public CancellationException(String reason) {
            super(reason);
        }
    }
}