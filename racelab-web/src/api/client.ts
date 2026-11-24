import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import type { ErrorResponse } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ErrorResponse>) => {
    const message = error.response?.data?.message || error.message || 'An error occurred';
    return Promise.reject(new Error(message));
  }
);

export const scenarioApi = {
  list: (params?: { page?: number; size?: number; search?: string }) =>
    api.get<{ content: any[]; totalElements: number; totalPages: number; page: number; size: number }>('/scenarios', { params }),

  get: (id: number) => api.get<any>(`/scenarios/${id}`),

  create: (data: any) => api.post<any>('/scenarios', data),

  update: (id: number, data: any) => api.put<any>(`/scenarios/${id}`, data),

  delete: (id: number) => api.delete(`/scenarios/${id}`),

  search: (query: string) => api.get<any[]>(`/scenarios/search`, { params: { q: query } }),
};

export const executionApi = {
  list: (params?: { page?: number; size?: number }) =>
    api.get<{ content: any[]; totalElements: number; totalPages: number; page: number; size: number }>('/tests', { params }),

  get: (id: number) => api.get<any>(`/tests/${id}`),

  start: (scenarioId: number, data?: any) => api.post<any>(`/tests/${scenarioId}/start`, data),

  startSync: (scenarioId: number, data?: any) => api.post<any>(`/tests/${scenarioId}/start-sync`, data),

  cancel: (id: number) => api.post(`/tests/${id}/cancel`),

  getByScenario: (scenarioId: number) => api.get<any[]>(`/tests/scenario/${scenarioId}`),

  getRecent: (limit = 20) => api.get<any[]>(`/tests/recent`, { params: { limit } }),

  getRunning: () => api.get<any[]>(`/tests/running`),
};

export const resultApi = {
  get: (executionId: number) => api.get<any>(`/results/execution/${executionId}`),
};

export default api;