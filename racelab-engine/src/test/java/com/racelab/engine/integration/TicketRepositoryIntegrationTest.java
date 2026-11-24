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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TicketRepository.
 */
@Testcontainers
@DataJpaTest
@Import(TicketRepository.class)
class TicketRepositoryIntegrationTest {

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
    void saveAndFindByEventId_ShouldWork() {
        Ticket ticket = Ticket.builder()
            .eventId("concert-2024")
            .eventName("Summer Concert")
            .totalQuantity(100)
            .availableQuantity(100)
            .status(TicketStatus.AVAILABLE)
            .build();

        Ticket saved = repository.save(ticket);
        entityManager.flush();

        Optional<Ticket> found = repository.findByEventId("concert-2024");

        assertThat(found).isPresent();
        assertThat(found.get().getEventName()).isEqualTo("Summer Concert");
        assertThat(found.get().getAvailableQuantity()).isEqualTo(100);
    }

    @Test
    void findByBookingReference_ShouldWork() {
        Ticket ticket = Ticket.builder()
            .eventId("concert-2024")
            .eventName("Summer Concert")
            .totalQuantity(100)
            .availableQuantity(99)
            .bookedQuantity(1)
            .status(TicketStatus.PARTIALLY_BOOKED)
            .bookingReference("BOOK-123")
            .bookedBy("user-1")
            .build();

        repository.save(ticket);
        entityManager.flush();

        Optional<Ticket> found = repository.findByBookingReference("BOOK-123");

        assertThat(found).isPresent();
        assertThat(found.get().getBookedBy()).isEqualTo("user-1");
    }

    @Test
    void findByIdempotencyKey_ShouldWork() {
        Ticket ticket = Ticket.builder()
            .eventId("concert-2024")
            .eventName("Summer Concert")
            .totalQuantity(100)
            .availableQuantity(99)
            .bookedQuantity(1)
            .status(TicketStatus.PARTIALLY_BOOKED)
            .idempotencyKey("idem-key-123")
            .build();

        repository.save(ticket);
        entityManager.flush();

        Optional<Ticket> found = repository.findByIdempotencyKey("idem-key-123");

        assertThat(found).isPresent();
    }

    @Test
    void findByEventIdAndStatusNot_ShouldReturnBookedTickets() {
        Ticket available = Ticket.builder()
            .eventId("concert-2024")
            .eventName("Summer Concert")
            .totalQuantity(100)
            .availableQuantity(100)
            .status(TicketStatus.AVAILABLE)
            .build();

        Ticket booked = Ticket.builder()
            .eventId("concert-2024")
            .eventName("Summer Concert")
            .totalQuantity(100)
            .availableQuantity(99)
            .bookedQuantity(1)
            .status(TicketStatus.PARTIALLY_BOOKED)
            .bookingReference("BOOK-123")
            .build();

        repository.save(available);
        repository.save(booked);
        entityManager.flush();

        List<Ticket> bookedTickets = repository.findByEventIdAndStatusNot("concert-2024", TicketStatus.AVAILABLE);

        assertThat(bookedTickets).hasSize(1);
        assertThat(bookedTickets.get(0).getBookingReference()).isEqualTo("BOOK-123");
    }

    @Test
    void bookTicketAtomically_ShouldUpdateAtomically() {
        Ticket ticket = Ticket.builder()
            .eventId("concert-2024")
            .eventName("Summer Concert")
            .totalQuantity(1)
            .availableQuantity(1)
            .status(TicketStatus.AVAILABLE)
            .build();

        repository.save(ticket);
        entityManager.flush();

        int updated = repository.bookTicketAtomically(
            ticket.getId(), 1, "BOOK-123", "user-1", "idem-key-123"
        );

        assertThat(updated).isEqualTo(1);

        entityManager.flush();
        entityManager.clear();

        Ticket reloaded = repository.findById(ticket.getId()).orElseThrow();
        assertThat(reloaded.getAvailableQuantity()).isZero();
        assertThat(reloaded.getBookedQuantity()).isEqualTo(1);
        assertThat(reloaded.getStatus()).isEqualTo(TicketStatus.SOLD_OUT);
        assertThat(reloaded.getBookingReference()).isEqualTo("BOOK-123");
    }

    @Test
    void bookTicketAtomically_InsufficientQuantity_ShouldNotUpdate() {
        Ticket ticket = Ticket.builder()
            .eventId("concert-2024")
            .eventName("Summer Concert")
            .totalQuantity(1)
            .availableQuantity(1)
            .status(TicketStatus.AVAILABLE)
            .build();

        repository.save(ticket);
        entityManager.flush();

        // Try to book 2 when only 1 available
        int updated = repository.bookTicketAtomically(
            ticket.getId(), 2, "BOOK-123", "user-1", "idem-key-123"
        );

        assertThat(updated).isZero();

        entityManager.flush();
        entityManager.clear();

        Ticket reloaded = repository.findById(ticket.getId()).orElseThrow();
        assertThat(reloaded.getAvailableQuantity()).isEqualTo(1);
        assertThat(reloaded.getBookedQuantity()).isZero();
    }
}