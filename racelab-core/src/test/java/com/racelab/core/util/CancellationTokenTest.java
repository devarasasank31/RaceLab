package com.racelab.core.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for CancellationToken.
 */
class CancellationTokenTest {

    @Test
    void create_ShouldReturnNonCancelledToken() {
        CancellationToken token = CancellationToken.create();

        assertThat(token.isCancelled()).isFalse();
        assertThat(token.getReason()).isEqualTo("Cancelled by user");
    }

    @Test
    void createWithReason_ShouldReturnNonCancelledTokenWithCustomReason() {
        CancellationToken token = CancellationToken.create("Custom reason");

        assertThat(token.isCancelled()).isFalse();
        assertThat(token.getReason()).isEqualTo("Custom reason");
    }

    @Test
    void cancel_ShouldMarkTokenAsCancelled() {
        CancellationToken token = CancellationToken.create();

        token.cancel();

        assertThat(token.isCancelled()).isTrue();
    }

    @Test
    void cancelWithReason_ShouldMarkTokenAsCancelledAndSetReason() {
        CancellationToken token = CancellationToken.create();

        token.cancel("New reason");

        assertThat(token.isCancelled()).isTrue();
        assertThat(token.getReason()).isEqualTo("New reason");
    }

    @Test
    void throwIfCancelled_WhenNotCancelled_ShouldNotThrow() {
        CancellationToken token = CancellationToken.create();

        token.throwIfCancelled(); // Should not throw
    }

    @Test
    void throwIfCancelled_WhenCancelled_ShouldThrowCancellationException() {
        CancellationToken token = CancellationToken.create();
        token.cancel("Test cancellation");

        assertThatThrownBy(token::throwIfCancelled)
            .isInstanceOf(CancellationToken.CancellationException.class)
            .hasMessage("Test cancellation");
    }

    @Test
    void cancellationException_ShouldBeRuntimeException() {
        CancellationToken token = CancellationToken.create();
        token.cancel();

        assertThatThrownBy(token::throwIfCancelled)
            .isInstanceOf(RuntimeException.class);
    }
}