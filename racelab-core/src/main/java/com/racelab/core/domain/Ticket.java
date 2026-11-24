package com.racelab.core.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Ticket entity for the race condition demonstration.
 * Used to show duplicate booking, lost updates, and overselling scenarios.
 */
@Entity
@Table(name = "tickets", indexes = {
    @Index(name = "idx_ticket_event", columnList = "event_id"),
    @Index(name = "idx_ticket_status", columnList = "status"),
    @Index(name = "idx_ticket_booking_ref", columnList = "booking_reference", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "event_id", nullable = false)
    private String eventId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Min(1)
    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Min(0)
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Min(0)
    @Column(name = "booked_quantity")
    @Builder.Default
    private Integer bookedQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TicketStatus status = TicketStatus.AVAILABLE;

    @Column(name = "booking_reference", length = 100, unique = true)
    private String bookingReference;

    @Column(name = "booked_by", length = 255)
    private String bookedBy;

    @Column(name = "booked_at")
    private Instant bookedAt;

    @Column(name = "idempotency_key", length = 255, unique = true)
    private String idempotencyKey;

    @Version
    @Column(name = "version")
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean canBook(int quantity) {
        return status == TicketStatus.AVAILABLE && availableQuantity >= quantity;
    }

    public void book(int quantity, String bookedBy, String bookingReference, String idempotencyKey) {
        if (!canBook(quantity)) {
            throw new IllegalStateException("Cannot book: insufficient availability or invalid status");
        }
        this.availableQuantity -= quantity;
        this.bookedQuantity += quantity;
        this.status = availableQuantity == 0 ? TicketStatus.SOLD_OUT : TicketStatus.PARTIALLY_BOOKED;
        this.bookedBy = bookedBy;
        this.bookingReference = bookingReference;
        this.idempotencyKey = idempotencyKey;
        this.bookedAt = Instant.now();
    }

    public void release() {
        this.availableQuantity = this.totalQuantity;
        this.bookedQuantity = 0;
        this.status = TicketStatus.AVAILABLE;
        this.bookedBy = null;
        this.bookingReference = null;
        this.idempotencyKey = null;
        this.bookedAt = null;
    }
}