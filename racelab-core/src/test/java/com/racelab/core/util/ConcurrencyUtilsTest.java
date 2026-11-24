package com.racelab.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for ConcurrencyUtils.
 */
class ConcurrencyUtilsTest {

    @Test
    void createCpuThreadPool_ShouldCreateBoundedPool() {
        ExecutorService executor = ConcurrencyUtils.createCpuThreadPool(2, 4, 10, "test-pool");

        assertThat(executor).isNotNull();
        assertThat(executor.isShutdown()).isFalse();

        ConcurrencyUtils.shutdownGracefully(executor, 5);
    }

    @Test
    void createVirtualThreadExecutor_ShouldCreateVirtualThreadExecutor() {
        ExecutorService executor = ConcurrencyUtils.createVirtualThreadExecutor("test-virtual");

        assertThat(executor).isNotNull();
        assertThat(executor.isShutdown()).isFalse();

        ConcurrencyUtils.shutdownGracefully(executor, 5);
    }

    @Test
    void createScheduledExecutor_ShouldCreateScheduledExecutor() {
        var executor = ConcurrencyUtils.createScheduledExecutor(2, "test-scheduled");

        assertThat(executor).isNotNull();

        executor.shutdown();
    }

    @Test
    void executeWithRetry_SuccessOnFirstTry_ShouldReturnResult() {
        String result = ConcurrencyUtils.executeWithRetry(() -> "success", 3, 10, 2.0);

        assertThat(result).isEqualTo("success");
    }

    @Test
    void executeWithRetry_FailureThenSuccess_ShouldRetryAndSucceed() {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = ConcurrencyUtils.executeWithRetry(() -> {
            if (attempts.incrementAndGet() < 2) {
                throw new RuntimeException("Temporary failure");
            }
            return "success";
        }, 3, 1, 2.0);

        assertThat(result).isEqualTo("success");
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void executeWithRetry_MaxRetriesExceeded_ShouldThrowException() {
        AtomicInteger attempts = new AtomicInteger(0);

        assertThatThrownBy(() -> ConcurrencyUtils.executeWithRetry(() -> {
            attempts.incrementAndGet();
            throw new RuntimeException("Always fails");
        }, 3, 1, 2.0))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Max retries exceeded");

        assertThat(attempts.get()).isEqualTo(4); // Initial + 3 retries
    }

    @Test
    void executeWithRetry_Runnable_ShouldWorkSameWay() {
        AtomicInteger attempts = new AtomicInteger(0);

        ConcurrencyUtils.executeWithRetry(() -> {
            if (attempts.incrementAndGet() < 2) {
                throw new RuntimeException("Temporary failure");
            }
        }, 3, 1, 2.0);

        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void shutdownGracefully_AlreadyShutdown_ShouldNotThrow() {
        ExecutorService executor = ConcurrencyUtils.createCpuThreadPool(1, 1, 1, "test");
        executor.shutdown();

        ConcurrencyUtils.shutdownGracefully(executor, 1); // Should not throw
    }

    @Test
    void shutdownGracefully_NullExecutor_ShouldNotThrow() {
        ConcurrencyUtils.shutdownGracefully(null, 1); // Should not throw
    }

    @Test
    @Timeout(10)
    void shutdownGracefully_ShouldWaitForTasks() throws InterruptedException {
        ExecutorService executor = ConcurrencyUtils.createCpuThreadPool(1, 1, 10, "test-shutdown");
        var latch = new java.util.concurrent.CountDownLatch(1);

        executor.submit(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        ConcurrencyUtils.shutdownGracefully(executor, 5);

        assertThat(latch.getCount()).isZero();
        assertThat(executor.isTerminated()).isTrue();
    }

    @Test
    void namedThreadFactory_ShouldCreateNamedThreads() {
        var factory = new ConcurrencyUtils.NamedThreadFactory("custom-prefix");
        Thread thread = factory.newThread(() -> {});

        assertThat(thread.getName()).startsWith("custom-prefix-");
        assertThat(thread.isDaemon()).isFalse();
    }

    @Test
    void namedThreadFactory_Daemon_ShouldCreateDaemonThreads() {
        var factory = new ConcurrencyUtils.NamedThreadFactory("daemon-prefix", true);
        Thread thread = factory.newThread(() -> {});

        assertThat(thread.getName()).startsWith("daemon-prefix-");
        assertThat(thread.isDaemon()).isTrue();
    }

    @Test
    void sleepUninterruptibly_ShouldNotThrowInterruptedException() {
        Thread thread = new Thread(() -> {
            ConcurrencyUtils.sleepUninterruptibly(50);
        });

        thread.start();
        thread.interrupt(); // Interrupt during sleep

        try {
            thread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(thread.isAlive()).isFalse();
    }
}