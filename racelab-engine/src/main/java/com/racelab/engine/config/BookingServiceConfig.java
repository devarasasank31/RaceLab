package com.racelab.engine.config;

import com.racelab.core.booking.*;
import com.racelab.core.distributed.RedisDistributedLockBookingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

/**
 * Configuration for booking services with different concurrency strategies.
 */
@Configuration
public class BookingServiceConfig {

    @Bean
    public Map<com.racelab.core.domain.ConcurrencyStrategyType, BookingService> bookingServices(
            VulnerableBookingService vulnerable,
            OptimisticLockingBookingService optimistic,
            PessimisticLockingBookingService pessimistic,
            AtomicUpdateBookingService atomic,
            IdempotencyKeyBookingService idempotency,
            RedisDistributedLockBookingService redisDistributed) {

        return new EnumMap<>(Map.of(
            com.racelab.core.domain.ConcurrencyStrategyType.NONE, vulnerable,
            com.racelab.core.domain.ConcurrencyStrategyType.OPTIMISTIC_LOCKING, optimistic,
            com.racelab.core.domain.ConcurrencyStrategyType.PESSIMISTIC_LOCKING, pessimistic,
            com.racelab.core.domain.ConcurrencyStrategyType.ATOMIC_UPDATE, atomic,
            com.racelab.core.domain.ConcurrencyStrategyType.IDEMPOTENCY_KEY, idempotency,
            com.racelab.core.domain.ConcurrencyStrategyType.REDIS_DISTRIBUTED_LOCK, redisDistributed
        ));
    }
}