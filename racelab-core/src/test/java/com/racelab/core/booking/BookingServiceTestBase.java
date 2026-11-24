package com.racelab.core.booking;

import com.racelab.core.domain.Ticket;
import com.racelab.core.domain.TicketStatus;
import com.racelab.core.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

/**
 * Base class for booking service concurrency tests.
 */
@Testcontainers
@DataJpaTest
@Import({
    VulnerableBookingService.class,
    OptimisticLockingBookingService.class,
    PessimisticLockingBookingService.class,
    AtomicUpdateBookingService.class,
    IdempotencyKeyBookingService.class,
    TicketRepository.class
})
public abstract class BookingServiceTestBase {

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
    protected TicketRepository ticketRepository;

    @Autowired
    protected TestEntityManager entityManager;

    @Autowired
    protected VulnerableBookingService vulnerableService;

    @Autowired
    protected OptimisticLockingBookingService optimisticService;

    @Autowired
    protected PessimisticLockingBookingService pessimisticService;

    @Autowired
    protected AtomicUpdateBookingService atomicService;

    @Autowired
    protected IdempotencyKeyBookingService idempotencyService;

    protected static final String EVENT_ID = "test-event";

    @BeforeEach
    void setup() {
        // Clean up any existing test data
        ticketRepository.findByEventId(EVENT_ID).ifPresent(ticketRepository::delete);
        entityManager.flush();
    }

    protected void createTicket(int quantity) {
        Ticket ticket = Ticket.builder()
            .eventId(EVENT_ID)
            .eventName("Test Event")
            .totalQuantity(quantity)
            .availableQuantity(quantity)
            .status(TicketStatus.AVAILABLE)
            .build();
        ticketRepository.save(ticket);
        entityManager.flush();
    }

    protected void resetTicket(int quantity) {
        ticketRepository.findByEventId(EVENT_ID).ifPresent(ticket -> {
            ticket.setAvailableQuantity(quantity);
            ticket.setBookedQuantity(0);
            ticket.setStatus(TicketStatus.AVAILABLE);
            ticket.setBookingReference(null);
            ticket.setBookedBy(null);
            ticket.setBookedAt(null);
            ticket.setIdempotencyKey(null);
            ticket.setVersion(0L);
            ticketRepository.save(ticket);
        });
        entityManager.flush();
    }

    protected ConcurrencyTestResult runConcurrentTest(BookingService service, int concurrentRequests) {
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
                    BookingResult result = service.book(EVENT_ID, "user-" + userId, 1);
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
        boolean completed = false;
        try {
            completed = endLatch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor.shutdownNow();

        // Check final state
        entityManager.flush();
        entityManager.clear();
        var finalTicket = ticketRepository.findByEventId(EVENT_ID).orElse(null);
        int finalAvailable = finalTicket != null ? finalTicket.getAvailableQuantity() : -1;
        int finalBooked = finalTicket != null ? finalTicket.getBookedQuantity() : -1;

        return new ConcurrencyTestResult(
            completed,
            successCount.get(),
            failureCount.get(),
            finalAvailable,
            finalBooked
        );
    }

    public record ConcurrencyTestResult(
        boolean completed,
        int successes,
        int failures,
        int finalAvailable,
        int finalBooked
    ) {}
}