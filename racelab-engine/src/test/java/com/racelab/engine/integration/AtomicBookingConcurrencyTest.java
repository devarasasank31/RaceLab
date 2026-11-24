package com.racelab.engine.integration;

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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency test for atomic ticket booking.
 * Verifies that exactly one booking succeeds when 1000 concurrent requests
 * try to book a single ticket.
 */
@Testcontainers
@DataJpaTest
@Import(TicketRepository.class)
class AtomicBookingConcurrencyTest {

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
    void atomicBooking_OneTicket1000ConcurrentRequests_ExactlyOneSuccess() throws InterruptedException {
        // Given: 1 ticket with 1 available seat
        Ticket ticket = Ticket.builder()
            .eventId("concert-atomic-test")
            .eventName("Atomic Test Concert")
            .totalQuantity(1)
            .availableQuantity(1)
            .status(TicketStatus.AVAILABLE)
            .build();

        repository.save(ticket);
        entityManager.flush();

        int threadCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Exception> errors = new CopyOnWriteArrayList<>();

        // When: 1000 concurrent booking attempts
        for (int i = 0; i < threadCount; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    int updated = repository.bookTicketAtomically(
                        ticket.getId(),
                        1,
                        "BOOK-" + userId,
                        "user-" + userId,
                        "idem-" + userId
                    );
                    if (updated > 0) {
                        successCount.incrementAndGet();
                    } else {
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
        assertThat(failureCount.get()).isEqualTo(999);

        // And: Database state is consistent
        entityManager.flush();
        entityManager.clear();

        Ticket finalTicket = repository.findById(ticket.getId()).orElseThrow();
        assertThat(finalTicket.getAvailableQuantity()).isZero();
        assertThat(finalTicket.getBookedQuantity()).isEqualTo(1);
        assertThat(finalTicket.getStatus()).isEqualTo(TicketStatus.SOLD_OUT);
        assertThat(finalTicket.getBookingReference()).isNotNull();
    }

    @Test
    void atomicBooking_MultipleTickets_ShouldAllowParallelBookings() throws InterruptedException {
        // Given: 10 tickets, each with 1 seat
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Ticket ticket = Ticket.builder()
                .eventId("concert-multi-" + i)
                .eventName("Multi Test Concert " + i)
                .totalQuantity(1)
                .availableQuantity(1)
                .status(TicketStatus.AVAILABLE)
                .build();
            tickets.add(repository.save(ticket));
        }
        entityManager.flush();

        int threadCount = 1000;
        int ticketsCount = tickets.size();
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Exception> errors = new CopyOnWriteArrayList<>();

        // When: 1000 concurrent requests across 10 tickets
        for (int i = 0; i < threadCount; i++) {
            final int userId = i;
            final int ticketIndex = i % ticketsCount;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Ticket ticket = tickets.get(ticketIndex);
                    int updated = repository.bookTicketAtomically(
                        ticket.getId(),
                        1,
                        "BOOK-" + userId,
                        "user-" + userId,
                        "idem-" + userId
                    );
                    if (updated > 0) {
                        successCount.incrementAndGet();
                    } else {
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

        // Then: Exactly 10 successes (one per ticket)
        assertThat(completed).isTrue();
        assertThat(errors).isEmpty();
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failureCount.get()).isEqualTo(990);

        // And: All tickets sold out
        entityManager.flush();
        entityManager.clear();
        for (Ticket ticket : tickets) {
            Ticket reloaded = repository.findById(ticket.getId()).orElseThrow();
            assertThat(reloaded.getAvailableQuantity()).isZero();
            assertThat(reloaded.getStatus()).isEqualTo(TicketStatus.SOLD_OUT);
        }
    }
}