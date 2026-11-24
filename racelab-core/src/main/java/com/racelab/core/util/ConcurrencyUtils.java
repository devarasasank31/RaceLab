package com.racelab.core.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Utilities for concurrency control and thread pool management.
 */
@Slf4j
public final class ConcurrencyUtils {

    private ConcurrencyUtils() {
        // Utility class
    }

    /**
     * Create a bounded thread pool for CPU-bound work.
     * Uses platform threads with a fixed size.
     */
    public static ThreadPoolExecutor createCpuThreadPool(int coreSize, int maxSize, int queueCapacity, String namePrefix) {
        return new ThreadPoolExecutor(
            coreSize,
            maxSize,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            new NamedThreadFactory(namePrefix),
            new ThreadPoolExecutor.AbortPolicy() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    log.warn("Task rejected in {}: queue full, active={}, pool={}",
                        namePrefix, executor.getActiveCount(), executor.getPoolSize());
                    super.rejectedExecution(r, executor);
                }
            }
        );
    }

    /**
     * Create a virtual thread executor for I/O-bound work (Java 21+).
     * Virtual threads are lightweight and can scale to millions.
     */
    public static ExecutorService createVirtualThreadExecutor(String namePrefix) {
        return Executors.newThreadPerTaskExecutor(new NamedThreadFactory(namePrefix));
    }

    /**
     * Create a scheduled executor for periodic tasks.
     */
    public static ScheduledExecutorService createScheduledExecutor(int coreSize, String namePrefix) {
        return new ScheduledThreadPoolExecutor(coreSize, new NamedThreadFactory(namePrefix));
    }

    /**
     * Thread factory that names threads for easier debugging.
     */
    public static final class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final boolean daemon;

        public NamedThreadFactory(String namePrefix) {
            this(namePrefix, false);
        }

        public NamedThreadFactory(String namePrefix, boolean daemon) {
            this.namePrefix = namePrefix + "-";
            this.daemon = daemon;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(daemon);
            return t;
        }
    }

    /**
     * Execute a task with retries and exponential backoff.
     */
    public static <T> T executeWithRetry(Supplier<T> task, int maxRetries, long baseDelayMs, double backoffMultiplier) {
        int attempt = 0;
        long delay = baseDelayMs;

        while (true) {
            try {
                return task.get();
            } catch (Exception e) {
                attempt++;
                if (attempt > maxRetries) {
                    throw new RuntimeException("Max retries exceeded", e);
                }
                log.debug("Attempt {} failed, retrying in {}ms: {}", attempt, delay, e.getMessage());
                sleepUninterruptibly(delay);
                delay = (long) (delay * backoffMultiplier);
            }
        }
    }

    /**
     * Execute a runnable with retries and exponential backoff.
     */
    public static void executeWithRetry(Runnable task, int maxRetries, long baseDelayMs, double backoffMultiplier) {
        executeWithRetry(() -> {
            task.run();
            return null;
        }, maxRetries, baseDelayMs, backoffMultiplier);
    }

    /**
     * Sleep without throwing InterruptedException.
     */
    public static void sleepUninterruptibly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Shutdown an executor service gracefully.
     */
    public static void shutdownGracefully(ExecutorService executor, long timeoutSeconds) {
        if (executor == null || executor.isShutdown()) {
            return;
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate in {}s, forcing shutdown", timeoutSeconds);
                executor.shutdownNow();
                if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                    log.error("Executor still did not terminate after forced shutdown");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Wait for all tasks to complete with a timeout.
     */
    public static boolean awaitAll(CompletionService<?> completionService, int taskCount, long timeoutSeconds) {
        try {
            for (int i = 0; i < taskCount; i++) {
                Future<?> future = completionService.poll(timeoutSeconds, TimeUnit.SECONDS);
                if (future == null) {
                    return false; // Timeout
                }
                future.get(); // Propagate exceptions
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException e) {
            throw new RuntimeException("Task failed", e.getCause());
        }
    }
}