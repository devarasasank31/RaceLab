export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' | 'HEAD' | 'OPTIONS';

export type ConcurrencyStrategyType =
  | 'NONE'
  | 'OPTIMISTIC_LOCKING'
  | 'PESSIMISTIC_LOCKING'
  | 'ATOMIC_UPDATE'
  | 'IDEMPOTENCY_KEY'
  | 'REDIS_DISTRIBUTED_LOCK';

export type TestStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'CANCELLED' | 'FAILED' | 'COMPLETED_WITH_VIOLATIONS';

export interface TestScenario {
  id: number;
  name: string;
  description?: string;
  concurrentUsers: number;
  totalRequests?: number;
  durationSeconds?: number;
  rampUpSeconds?: number;
  targetEndpoint: string;
  httpMethod: HttpMethod;
  requestHeaders?: Record<string, string>;
  requestPayload?: string;
  concurrencyStrategy: ConcurrencyStrategyType;
  strategyConfig?: Record<string, unknown>;
  requestTimeoutSeconds: number;
  maxRetries: number;
  createdAt: string;
  updatedAt: string;
}

export interface TestScenarioRequest {
  name: string;
  description?: string;
  concurrentUsers: number;
  totalRequests?: number;
  durationSeconds?: number;
  rampUpSeconds?: number;
  targetEndpoint: string;
  httpMethod: HttpMethod;
  requestHeaders?: Record<string, string>;
  requestPayload?: string;
  concurrencyStrategy?: ConcurrencyStrategyType;
  strategyConfig?: Record<string, unknown>;
  requestTimeoutSeconds?: number;
  maxRetries?: number;
}

export interface TestExecution {
  id: number;
  scenarioId: number;
  scenarioName: string;
  status: TestStatus;
  startedAt?: string;
  completedAt?: string;
  cancelledAt?: string;
  errorMessage?: string;
  executedRequests: number;
  successfulRequests: number;
  failedRequests: number;
  configurationSnapshot?: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

export interface TestResult {
  id: number;
  executionId: number;
  totalRequests: number;
  successfulRequests: number;
  failedRequests: number;
  requestsPerSecond: number;
  avgLatencyMs: number;
  medianLatencyMs: number;
  p95LatencyMs: number;
  p99LatencyMs: number;
  minLatencyMs: number;
  maxLatencyMs: number;
  stdDeviationMs: number;
  errorRatePercent: number;
  throughputMbps: number;
  errorBreakdown: Record<string, number>;
  statusCodeDistribution: Record<number, number>;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
}

export interface TestExecutionRequest {
  concurrentUsers?: number;
  totalRequests?: number;
  durationSeconds?: number;
  rampUpSeconds?: number;
}

export const CONCURRENCY_STRATEGY_LABELS: Record<ConcurrencyStrategyType, string> = {
  NONE: 'No Protection',
  OPTIMISTIC_LOCKING: 'Optimistic Locking',
  PESSIMISTIC_LOCKING: 'Pessimistic Locking',
  ATOMIC_UPDATE: 'Atomic Update',
  IDEMPOTENCY_KEY: 'Idempotency Key',
  REDIS_DISTRIBUTED_LOCK: 'Redis Distributed Lock',
};

export const TEST_STATUS_COLORS: Record<TestStatus, string> = {
  PENDING: 'var(--color-text-secondary)',
  RUNNING: 'var(--color-primary)',
  COMPLETED: 'var(--color-success)',
  CANCELLED: 'var(--color-warning)',
  FAILED: 'var(--color-danger)',
  COMPLETED_WITH_VIOLATIONS: 'var(--color-warning)',
};