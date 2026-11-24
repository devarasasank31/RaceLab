package com.racelab.core.repository;

import com.racelab.core.domain.Ticket;
import com.racelab.core.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

/**
 * Repository for Ticket entities.
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByEventId(String eventId);

    Optional<Ticket> findByBookingReference(String bookingReference);

    Optional<Ticket> findByIdempotencyKey(String idempotencyKey);

    List<Ticket> findByEventIdAndStatusNot(String eventId, TicketStatus status);

    List<Ticket> findByBookingReferenceNotNull();

    long countByEventIdAndStatusNot(String eventId, TicketStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.eventId = :eventId")
    Optional<Ticket> findByEventIdForUpdate(@Param("eventId") String eventId);

    @Query("SELECT t FROM Ticket t WHERE t.id = :id")
    Optional<Ticket> findByIdForUpdate(@Param("id") Long id);

    @Query("UPDATE Ticket t SET t.availableQuantity = t.availableQuantity - :quantity, " +
           "t.bookedQuantity = t.bookedQuantity + :quantity, " +
           "t.status = CASE WHEN t.availableQuantity - :quantity = 0 THEN 'SOLD_OUT' ELSE 'PARTIALLY_BOOKED' END, " +
           "t.bookingReference = :bookingRef, " +
           "t.bookedBy = :bookedBy, " +
           "t.idempotencyKey = :idempotencyKey, " +
           "t.bookedAt = CURRENT_TIMESTAMP " +
           "WHERE t.id = :id AND t.availableQuantity >= :quantity")
    int bookTicketAtomically(@Param("id") Long id,
                             @Param("quantity") int quantity,
                             @Param("bookingRef") String bookingRef,
                             @Param("bookedBy") String bookedBy,
                             @Param("idempotencyKey") String idempotencyKey);
}