# RaceLab API Reference

Base URL: `http://localhost:8080/api`

OpenAPI/Swagger UI: `http://localhost:8080/swagger-ui.html`

## Authentication

Currently no authentication required (Phase A). Future phases will add JWT/OAuth2.

## Error Responses

All errors follow consistent format:

```json
{
  "timestamp": "2026-01-15T10:30:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "concurrentUsers must be between 1 and 10000",
  "path": "/api/scenarios"
}
```

HTTP Status Codes:
- `200` - Success
- `201` - Created
- `202` - Accepted (async operation)
- `204` - No Content (successful deletion)
- `400` - Bad Request (validation)
- `404` - Not Found
- `409` - Conflict (concurrency violation)
- `500` - Internal Server Error

---

## Test Scenarios

### Create Scenario
```http
POST /api/scenarios
Content-Type: application/json

{
  "name": "ticket-booking-test",
  "description": "Test concurrent ticket booking",
  "concurrentUsers": 100,
  "totalRequests": 1000,
  "targetEndpoint": "http://localhost:8080/api/tickets/book",
  "httpMethod": "POST",
  "requestPayload": "{\"eventId\": \"concert-2024\", \"quantity\": 1}",
  "concurrencyStrategy": "NONE",
  "requestTimeoutSeconds": 30,
  "maxRetries": 0
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "name": "ticket-booking-test",
  "description": "Test concurrent ticket booking",
  "concurrentUsers": 100,
  "totalRequests": 1000,
  "targetEndpoint": "http://localhost:8080/api/tickets/book",
  "httpMethod": "POST",
  "concurrencyStrategy": "NONE",
  "requestTimeoutSeconds": 30,
  "maxRetries": 0,
  "createdAt": "2026-01-15T10:30:00Z",
  "updatedAt": "2026-01-15T10:30:00Z"
}
```

### Get Scenario
```http
GET /api/scenarios/{id}
```

**Response:** `200 OK` - Scenario object

### List Scenarios (Paginated)
```http
GET /api/scenarios?page=0&size=20
```

**Response:** `200 OK`
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3,
  "first": true,
  "last": false,
  "empty": false
}
```

### Search Scenarios
```http
GET /api/scenarios/search?q=ticket
```

**Response:** `200 OK` - Array of scenarios

### Update Scenario
```http
PUT /api/scenarios/{id}
Content-Type: application/json

{
  "name": "updated-name",
  "concurrentUsers": 200
}
```

**Response:** `200 OK` - Updated scenario

### Delete Scenario
```http
DELETE /api/scenarios/{id}
```

**Response:** `204 No Content`

---

## Test Executions

### Start Execution (Async)
```http
POST /api/tests/{scenarioId}/start
Content-Type: application/json

{
  "concurrentUsers": 50,
  "durationSeconds": 60
}
```

**Response:** `202 Accepted` - Execution object with PENDING status

### Start Execution (Sync - for testing)
```http
POST /api/tests/{scenarioId}/start-sync
Content-Type: application/json

{
  "concurrentUsers": 50,
  "totalRequests": 100
}
```

**Response:** `200 OK` - Completed execution with results

### Cancel Execution
```http
POST /api/tests/{executionId}/cancel
```

**Response:** `200 OK`

### Get Execution
```http
GET /api/tests/{executionId}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "scenarioId": 1,
  "scenarioName": "ticket-booking-test",
  "status": "COMPLETED",
  "startedAt": "2026-01-15T10:30:05Z",
  "completedAt": "2026-01-15T10:31:05Z",
  "executedRequests": 1000,
  "successfulRequests": 999,
  "failedRequests": 1,
  "configurationSnapshot": {...},
  "createdAt": "2026-01-15T10:30:00Z",
  "updatedAt": "2026-01-15T10:31:05Z"
}
```

### List Executions for Scenario
```http
GET /api/tests/scenario/{scenarioId}
```

**Response:** `200 OK` - Array of executions

### Get Recent Executions
```http
GET /api/tests/recent?limit=20
```

**Response:** `200 OK` - Array of executions

### Get Running Executions
```http
GET /api/tests/running
```

**Response:** `200 OK` - Array of RUNNING executions

---

## Test Results

### Get Results for Execution
```http
GET /api/results/execution/{executionId}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "executionId": 1,
  "totalRequests": 1000,
  "successfulRequests": 999,
  "failedRequests": 1,
  "requestsPerSecond": 16.67,
  "avgLatencyMs": 12.45,
  "medianLatencyMs": 10.12,
  "p95LatencyMs": 28.34,
  "p99LatencyMs": 45.67,
  "minLatencyMs": 1.23,
  "maxLatencyMs": 245.67,
  "stdDeviationMs": 8.91,
  "errorRatePercent": 0.1,
  "throughputMbps": 0.45,
  "errorBreakdown": {
    "OptimisticLockException": 1
  },
  "statusCodeDistribution": {
    "200": 999,
    "409": 1
  },
  "createdAt": "2026-01-15T10:31:05Z"
}
```

---

## Health & Monitoring

### Health Check
```http
GET /actuator/health
```

**Response:** `200 OK`
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### Prometheus Metrics
```http
GET /actuator/prometheus
```

**Response:** `200 OK` - Prometheus format metrics

### Info
```http
GET /actuator/info
```

**Response:** `200 OK` - Build info, version, etc.

---

## Concurrency Strategy Values

| Value | Description |
|-------|-------------|
| `NONE` | No protection (baseline) |
| `OPTIMISTIC_LOCKING` | JPA @Version with retry |
| `PESSIMISTIC_LOCKING` | SELECT FOR UPDATE |
| `ATOMIC_UPDATE` | Conditional UPDATE |
| `IDEMPOTENCY_KEY` | Client-provided deduplication keys |
| `REDIS_DISTRIBUTED_LOCK` | Redis Redlock algorithm |

## HTTP Method Values

`GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `HEAD`, `OPTIONS`

## Test Status Values

| Status | Description |
|--------|-------------|
| `PENDING` | Created, not started |
| `RUNNING` | Currently executing |
| `COMPLETED` | Finished successfully |
| `CANCELLED` | Stopped by user |
| `FAILED` | Error during execution |
| `COMPLETED_WITH_VIOLATIONS` | Finished but invariants violated |

## Rate Limits

No explicit rate limiting in Phase A. Recommended client-side limits:
- Scenario creation: 10/minute
- Execution start: 5/minute
- Result polling: 1/second

## WebSocket (Future)

Real-time execution updates will be available at:
`ws://localhost:8080/api/tests/{executionId}/stream`

Message format:
```json
{
  "type": "PROGRESS",
  "executionId": 1,
  "executedRequests": 500,
  "successfulRequests": 495,
  "failedRequests": 5,
  "currentRps": 45.2
}
```