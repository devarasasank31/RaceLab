import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { scenarioApi } from '../api/client';
import type { TestScenario, TestScenarioRequest, PageResponse } from '../types';

export function useScenarios(page = 0, size = 20, search?: string) {
  return useQuery({
    queryKey: ['scenarios', page, size, search],
    queryFn: () => scenarioApi.list({ page, size, search }),
    select: (data) => ({
      content: data.data.content,
      totalElements: data.data.totalElements,
      totalPages: data.data.totalPages,
      page: data.data.page,
      size: data.data.size,
    } as PageResponse<TestScenario>),
  });
}

export function useScenario(id: number) {
  return useQuery({
    queryKey: ['scenario', id],
    queryFn: () => scenarioApi.get(id),
    select: (data) => data.data,
    enabled: !!id,
  });
}

export function useCreateScenario() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: TestScenarioRequest) => scenarioApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['scenarios'] });
    },
  });
}

export function useUpdateScenario() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: TestScenarioRequest }) => scenarioApi.update(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['scenarios'] });
      queryClient.invalidateQueries({ queryKey: ['scenario', id] });
    },
  });
}

export function useDeleteScenario() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => scenarioApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['scenarios'] });
    },
  });
}