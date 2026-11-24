package com.racelab.engine.http;

import com.racelab.core.domain.HttpMethod;
import com.racelab.engine.config.EngineProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * WebClient implementation of HttpClientService.
 * Uses Spring WebFlux for non-blocking HTTP requests.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebClientHttpClientService implements HttpClientService {

    private final WebClient webClient;
    private final EngineProperties properties;

    @Override
    public HttpResponse execute(HttpRequest request) {
        return executeAsync(request).block();
    }

    @Override
    public Mono<HttpResponse> executeAsync(HttpRequest request) {
        Instant start = Instant.now();

        WebClient.RequestHeadersSpec<?> requestSpec = buildRequest(request);

        return requestSpec
            .exchangeToMono(response -> {
                long latencyMs = Duration.between(start, Instant.now()).toMillis();
                long contentLength = response.headers().contentLength().orElse(0L);

                return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> HttpResponse.builder()
                        .statusCode(response.statusCode().value())
                        .body(body)
                        .headers(response.headers().asHttpHeaders())
                        .latencyMs(latencyMs)
                        .success(response.statusCode().is2xxSuccessful())
                        .responseSizeBytes(contentLength + (body != null ? body.getBytes().length : 0))
                        .build())
                    .onErrorResume(e -> Mono.just(HttpResponse.builder()
                        .statusCode(0)
                        .latencyMs(Duration.between(start, Instant.now()).toMillis())
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build()));
            })
            .onErrorResume(e -> {
                long latencyMs = Duration.between(start, Instant.now()).toMillis();
                log.debug("Request failed: {} {} - {}", request.getMethod(), request.getUrl(), e.getMessage());
                return Mono.just(HttpResponse.builder()
                    .statusCode(0)
                    .latencyMs(latencyMs)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build());
            })
            .timeout(request.getTimeout() != null ? request.getTimeout() : Duration.ofSeconds(properties.getDefaultRequestTimeoutSeconds()));
    }

    private WebClient.RequestHeadersSpec<?> buildRequest(HttpRequest request) {
        WebClient.RequestBodySpec spec = webClient.method(mapMethod(request.getMethod()))
            .uri(request.getUrl());

        // Add headers
        if (request.getHeaders() != null) {
            request.getHeaders().forEach(spec::header);
        }

        // Add idempotency key if present
        if (request.getIdempotencyKey() != null) {
            spec.header("Idempotency-Key", request.getIdempotencyKey());
        }

        // Add body for methods that support it
        if (request.getBody() != null && hasBody(request.getMethod())) {
            spec.contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request.getBody());
        }

        return spec;
    }

    private org.springframework.http.HttpMethod mapMethod(HttpMethod method) {
        return switch (method) {
            case GET -> org.springframework.http.HttpMethod.GET;
            case POST -> org.springframework.http.HttpMethod.POST;
            case PUT -> org.springframework.http.HttpMethod.PUT;
            case PATCH -> org.springframework.http.HttpMethod.PATCH;
            case DELETE -> org.springframework.http.HttpMethod.DELETE;
            case HEAD -> org.springframework.http.HttpMethod.HEAD;
            case OPTIONS -> org.springframework.http.HttpMethod.OPTIONS;
        };
    }

    private boolean hasBody(HttpMethod method) {
        return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
    }
}