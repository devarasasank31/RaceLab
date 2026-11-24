import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { executionApi, resultApi } from '../api/client';
import type { TestExecution, TestResult, TestExecutionRequest, PageResponse } from '../types';

export function useExecutions(page = 0, size = 20) {
  return useQuery({
    queryKey: ['executions', page, size],
    queryFn: () => executionApi.list({ page, size }),
    select: (data) => ({
      content: data.data.content,
      totalElements: data.data.totalElements,
      totalPages: data.data.totalPages,
      page: data.data.page,
      size: data.data.size,
    } as PageResponse<TestExecution>),
  });
}

export function useExecution(id: number) {
  return useQuery({
    queryKey: ['execution', id],
    queryFn: () => executionApi.get(id),
    select: (data) => data.data,
    enabled: !!id,
    refetchInterval: (query) => {
      const execution = query.state.data;
      return execution && (execution.status === 'RUNNING' || execution.status === 'PENDING') ? 2000 : false;
    },
  });
}

export function useExecutionsByScenario(scenarioId: number) {
  return useQuery({
    queryKey: ['executions', 'scenario', scenarioId],
    queryFn: () => executionApi.getByScenario(scenarioId),
    select: (data) => data.data,
    enabled: !!scenarioId,
  });
}

export function useRecentExecutions(limit = 20) {
  return useQuery({
    queryKey: ['executions', 'recent', limit],
    queryFn: () => executionApi.getRecent(limit),
    select: (data) => data.data,
  });
}

export function useRunningExecutions() {
  return useQuery({
    queryKey: ['executions', 'running'],
    queryFn: () => executionApi.getRunning(),
    select: (data) => data.data,
    refetchInterval: 3000,
  });
}

export function useStartExecution() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ scenarioId, data }: { scenarioId: number; data?: TestExecutionRequest }) =>
      executionApi.start(scenarioId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['executions'] });
      queryClient.invalidateQueries({ queryKey: ['executions', 'running'] });
    },
  });
}

export function useStartExecutionSync() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ scenarioId, data }: { scenarioId: number; data?: TestExecutionRequest }) =>
      executionApi.startSync(scenarioId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['executions'] });
      queryClient.invalidateQueries({ queryKey: ['executions', 'running'] });
    },
  });
}

export function useCancelExecution() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => executionApi.cancel(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['executions'] });
      queryClient.invalidateQueries({ queryKey: ['executions', 'running'] });
    },
  });
}

export function useTestResult(executionId: number) {
  return useQuery({
    queryKey: ['result', executionId],
    queryFn: () => resultApi.get(executionId),
    select: (data) => data.data,
    enabled: !!executionId,
  });
}