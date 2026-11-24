package com.racelab.core.distributed;

import com.racelab.core.booking.BookingService;
import com.racelab.core.booking.BookingResult;
import com.racelab.core.domain.Ticket;
import com.racelab.core.domain.TicketStatus;
import com.racelab.core.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency test for Redis distributed lock booking service.
 * Note: Requires Redis container - uses Testcontainers.
 */
@Testcontainers
@DataJpaTest
@Import({
    RedisDistributedLock.class,
    RedisDistributedLockBookingService.class,
    TicketRepository.class
})
class RedisDistributedLockBookingServiceConcurrencyTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("racelab")
        .withUsername("racelab")
        .withPassword("racelab")
        .withReuse(true);

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379)
        .withReuse(true);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.redis.host", REDIS::getHost);
        registry.add("spring.redis.port", REDIS::getFirstMappedPort);
    }

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RedisDistributedLockBookingService redisLockService;

    @Test
    void redisDistributedLock_OneTicket100ConcurrentRequests_ExactlyOneSuccess() throws InterruptedException {
        // Given: 1 ticket
        String eventId = "redis-test-event";
        Ticket ticket = Ticket.builder()
            .eventId(eventId)
            .eventName("Redis Test Event")
            .totalQuantity(1)
            .availableQuantity(1)
            .status(TicketStatus.AVAILABLE)
            .build();
        ticketRepository.save(ticket);
        entityManager.flush();

        // When: 100 concurrent requests with Redis distributed lock
        int concurrentRequests = 100;
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(concurrentRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < concurrentRequests; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    BookingResult result = redisLockService.book(eventId, "user-" + userId, 1);
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        // Then: Exactly one success
        assertThat(completed).isTrue();
        assertThat(successCount.get() + failureCount.get()).isEqualTo(concurrentRequests);
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(99);

        // And: Database state consistent
        entityManager.flush();
        entityManager.clear();
        var finalTicket = ticketRepository.findByEventId(eventId).orElseThrow();
        assertThat(finalTicket.getAvailableQuantity()).isZero();
        assertThat(finalTicket.getBookedQuantity()).isEqualTo(1);
        assertThat(finalTicket.getStatus()).isEqualTo(TicketStatus.SOLD_OUT);

        System.out.printf("Redis Distributed Lock: successes=%d, failures=%d%n",
            successCount.get(), failureCount.get());
    }
}