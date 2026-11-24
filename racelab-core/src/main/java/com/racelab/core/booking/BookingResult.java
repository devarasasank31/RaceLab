package com.racelab.core.booking;

import lombok.*;

import java.time.Instant;

/**
 * Result of a booking attempt.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class BookingResult {

    private boolean success;
    private String bookingReference;
    private String message;
    private int availableAfterBooking;
    private Instant timestamp;
    private String strategyType;

    public static BookingResult success(String bookingReference, int availableAfterBooking, String strategyType) {
        return BookingResult.builder()
            .success(true)
            .bookingReference(bookingReference)
            .message("Booking successful")
            .availableAfterBooking(availableAfterBooking)
            .timestamp(Instant.now())
            .strategyType(strategyType)
            .build();
    }

    public static BookingResult failure(String message, int availableAfterBooking, String strategyType) {
        return BookingResult.builder()
            .success(false)
            .message(message)
            .availableAfterBooking(availableAfterBooking)
            .timestamp(Instant.now())
            .strategyType(strategyType)
            .build();
    }
}