package com.racelab.engine.http;

import com.racelab.core.domain.HttpMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Reactive HTTP client service for executing test requests.
 * Uses Spring WebFlux WebClient for non-blocking I/O.
 */
public interface HttpClientService {

    /**
     * Execute a single HTTP request and return the response.
     */
    HttpResponse execute(HttpRequest request);

    /**
     * Execute a single HTTP request asynchronously.
     */
    Mono<HttpResponse> executeAsync(HttpRequest request);

    /**
     * Request DTO for HTTP execution.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class HttpRequest {
        private HttpMethod method;
        private String url;
        private Map<String, String> headers;
        private String body;
        private Duration timeout;
        private String idempotencyKey;
    }

    /**
     * Response DTO from HTTP execution.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class HttpResponse {
        private int statusCode;
        private String body;
        private HttpHeaders headers;
        private long latencyMs;
        private boolean success;
        private String errorMessage;
        private long responseSizeBytes;
    }
}