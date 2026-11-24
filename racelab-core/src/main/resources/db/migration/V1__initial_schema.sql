-- RaceLab Initial Schema
-- Version 1.0.0

-- Enable UUID extension if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Test Scenarios
CREATE TABLE test_scenarios (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(1000),
    concurrent_users INTEGER NOT NULL CHECK (concurrent_users BETWEEN 1 AND 10000),
    total_requests INTEGER CHECK (total_requests BETWEEN 1 AND 1000000),
    duration_seconds INTEGER CHECK (duration_seconds BETWEEN 1 AND 3600),
    ramp_up_seconds INTEGER DEFAULT 0 CHECK (ramp_up_seconds BETWEEN 0 AND 300),
    target_endpoint VARCHAR(2048) NOT NULL,
    http_method VARCHAR(10) NOT NULL CHECK (http_method IN ('GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS')),
    request_headers JSONB,
    request_payload TEXT,
    concurrency_strategy VARCHAR(50) NOT NULL DEFAULT 'NONE' CHECK (concurrency_strategy IN (
        'NONE', 'OPTIMISTIC_LOCKING', 'PESSIMISTIC_LOCKING',
        'ATOMIC_UPDATE', 'IDEMPOTENCY_KEY', 'REDIS_DISTRIBUTED_LOCK'
    )),
    strategy_config JSONB,
    request_timeout_seconds INTEGER DEFAULT 30 CHECK (request_timeout_seconds BETWEEN 1 AND 300),
    max_retries INTEGER DEFAULT 0 CHECK (max_retries BETWEEN 0 AND 10),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_test_scenario_name ON test_scenarios(name);
CREATE INDEX idx_test_scenario_created_at ON test_scenarios(created_at);

-- Test Executions
CREATE TABLE test_executions (
    id BIGSERIAL PRIMARY KEY,
    scenario_id BIGINT NOT NULL REFERENCES test_scenarios(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'RUNNING', 'COMPLETED', 'CANCELLED', 'FAILED', 'COMPLETED_WITH_VIOLATIONS'
    )),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    error_message TEXT,
    executed_requests BIGINT DEFAULT 0,
    successful_requests BIGINT DEFAULT 0,
    failed_requests BIGINT DEFAULT 0,
    configuration_snapshot JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_test_execution_scenario ON test_executions(scenario_id);
CREATE INDEX idx_test_execution_status ON test_executions(status);
CREATE INDEX idx_test_execution_started_at ON test_executions(started_at);

-- Test Results
CREATE TABLE test_results (
    id BIGSERIAL PRIMARY KEY,
    execution_id BIGINT NOT NULL UNIQUE REFERENCES test_executions(id) ON DELETE CASCADE,
    total_requests BIGINT,
    successful_requests BIGINT,
    failed_requests BIGINT,
    requests_per_second DOUBLE PRECISION,
    avg_latency_ms DOUBLE PRECISION,
    median_latency_ms DOUBLE PRECISION,
    p95_latency_ms DOUBLE PRECISION,
    p99_latency_ms DOUBLE PRECISION,
    min_latency_ms DOUBLE PRECISION,
    max_latency_ms DOUBLE PRECISION,
    std_deviation_ms DOUBLE PRECISION,
    error_rate_percent DOUBLE PRECISION,
    throughput_mbps DOUBLE PRECISION,
    latency_histogram BYTEA,
    error_breakdown JSONB,
    status_code_distribution JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_test_result_execution ON test_results(execution_id);

-- Request Results
CREATE TABLE request_results (
    id BIGSERIAL PRIMARY KEY,
    execution_id BIGINT NOT NULL REFERENCES test_executions(id) ON DELETE CASCADE,
    request_number BIGINT,
    thread_name VARCHAR(100),
    virtual_thread BOOLEAN,
    timestamp TIMESTAMPTZ NOT NULL,
    latency_ms BIGINT,
    status_code INTEGER,
    success BOOLEAN,
    error_message TEXT,
    response_size_bytes BIGINT,
    idempotency_key VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_request_result_execution ON request_results(execution_id);
CREATE INDEX idx_request_result_timestamp ON request_results(timestamp);
CREATE INDEX idx_request_result_status ON request_results(success);
CREATE INDEX idx_request_result_latency ON request_results(latency_ms);

-- Tickets (for race condition demos)
CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    event_name VARCHAR(255) NOT NULL,
    total_quantity INTEGER NOT NULL CHECK (total_quantity >= 1),
    available_quantity INTEGER NOT NULL CHECK (available_quantity >= 0),
    booked_quantity INTEGER DEFAULT 0 CHECK (booked_quantity >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'PARTIALLY_BOOKED', 'SOLD_OUT', 'CANCELLED')),
    booking_reference VARCHAR(100) UNIQUE,
    booked_by VARCHAR(255),
    booked_at TIMESTAMPTZ,
    idempotency_key VARCHAR(255) UNIQUE,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ticket_event ON tickets(event_id);
CREATE INDEX idx_ticket_status ON tickets(status);
CREATE INDEX idx_ticket_booking_ref ON tickets(booking_reference);
CREATE INDEX idx_ticket_idempotency_key ON tickets(idempotency_key);

-- Accounts (for lost update demos)
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    owner_id VARCHAR(100) NOT NULL,
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0.0 CHECK (balance >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    version BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_account_owner ON accounts(owner_id);
CREATE INDEX idx_account_number ON accounts(account_number);

-- Updated at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at triggers
CREATE TRIGGER update_test_scenarios_updated_at BEFORE UPDATE ON test_scenarios
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_test_executions_updated_at BEFORE UPDATE ON test_executions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tickets_updated_at BEFORE UPDATE ON tickets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_accounts_updated_at BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();