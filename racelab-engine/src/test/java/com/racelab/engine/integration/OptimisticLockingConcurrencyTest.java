package com.racelab.engine.integration;

import com.racelab.core.domain.Ticket;
import com.racelab.core.domain.TicketStatus;
import com.racelab.core.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency test for optimistic locking.
 * Verifies that JPA @Version with retry handles concurrent updates correctly.
 */
@Testcontainers
@DataJpaTest
@Import(TicketRepository.class)
class OptimisticLockingConcurrencyTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("racelab")
        .withUsername("racelab")
        .withPassword("racelab")
        .withReuse(true);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private TicketRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void optimisticLocking_OneTicket100ConcurrentRequests_ExactlyOneSuccessWithRetries() throws InterruptedException {
        // Given: 1 ticket with 1 available seat
        Ticket ticket = Ticket.builder()
            .eventId("concert-optimistic-test")
            .eventName("Optimistic Test Concert")
            .totalQuantity(1)
            .availableQuantity(1)
            .status(TicketStatus.AVAILABLE)
            .build();

        repository.save(ticket);
        entityManager.flush();

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger retryCount = new AtomicInteger(0);
        List<Exception> errors = new CopyOnWriteArrayList<>();

        // When: 100 concurrent booking attempts with optimistic locking
        for (int i = 0; i < threadCount; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    boolean booked = false;
                    int retries = 0;
                    while (!booked && retries < 10) {
                        try {
                            // Use pessimistic lock for read, then update with version check
                            Ticket lockedTicket = entityManager.find(Ticket.class, ticket.getId(), LockModeType.OPTIMISTIC);
                            if (lockedTicket != null && lockedTicket.canBook(1)) {
                                lockedTicket.book(1, "user-" + userId, "BOOK-" + userId, "idem-" + userId);
                                entityManager.merge(lockedTicket);
                                entityManager.flush();
                                booked = true;
                                successCount.incrementAndGet();
                            } else {
                                failureCount.incrementAndGet();
                                booked = true; // No more tickets, stop retrying
                            }
                        } catch (OptimisticLockingFailureException e) {
                            retries++;
                            retryCount.incrementAndGet();
                            entityManager.clear(); // Clear persistence context
                        }
                    }
                    if (!booked) {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors.add(e);
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
        assertThat(errors).isEmpty();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(99);
        assertThat(retryCount.get()).isGreaterThan(0); // Retries occurred

        // And: Database state is consistent
        entityManager.flush();
        entityManager.clear();

        Ticket finalTicket = repository.findById(ticket.getId()).orElseThrow();
        assertThat(finalTicket.getAvailableQuantity()).isZero();
        assertThat(finalTicket.getBookedQuantity()).isEqualTo(1);
        assertThat(finalTicket.getStatus()).isEqualTo(TicketStatus.SOLD_OUT);
    }
}