import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import { Card, CardHeader, CardTitle, CardContent, CardFooter } from '../components/Card';
import { Button } from '../components/Button';
import { Input } from '../components/Input';
import { Select } from '../components/Select';
import { Table } from '../components/Table';
import { Badge } from '../components/Badge';
import { useScenarios, useCreateScenario, useDeleteScenario } from '../hooks/useScenarios';
import { scenarioApi } from '../api/client';
import { format } from 'date-fns';
import { CONCURRENCY_STRATEGY_LABELS, type TestScenario, type TestScenarioRequest, type HttpMethod, type ConcurrencyStrategyType } from '../types';
import { clsx } from 'clsx';

const HTTP_METHODS: { value: HttpMethod; label: string }[] = [
  { value: 'GET', label: 'GET' },
  { value: 'POST', label: 'POST' },
  { value: 'PUT', label: 'PUT' },
  { value: 'PATCH', label: 'PATCH' },
  { value: 'DELETE', label: 'DELETE' },
];

const STRATEGY_OPTIONS: { value: ConcurrencyStrategyType; label: string }[] = [
  { value: 'NONE', label: 'No Protection' },
  { value: 'OPTIMISTIC_LOCKING', label: 'Optimistic Locking' },
  { value: 'PESSIMISTIC_LOCKING', label: 'Pessimistic Locking' },
  { value: 'ATOMIC_UPDATE', label: 'Atomic Update' },
  { value: 'IDEMPOTENCY_KEY', label: 'Idempotency Key' },
  { value: 'REDIS_DISTRIBUTED_LOCK', label: 'Redis Distributed Lock' },
];

export function Scenarios() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const navigate = useNavigate();

  const { data, isLoading, error } = useScenarios(page, 20, search);
  const createMutation = useCreateScenario();
  const deleteMutation = useDeleteScenario();

  const handleCreate = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const formData = new FormData(e.currentTarget);
    const request: TestScenarioRequest = {
      name: formData.get('name') as string,
      description: formData.get('description') as string || undefined,
      concurrentUsers: parseInt(formData.get('concurrentUsers') as string, 10),
      totalRequests: formData.get('totalRequests') ? parseInt(formData.get('totalRequests') as string, 10) : undefined,
      durationSeconds: formData.get('durationSeconds') ? parseInt(formData.get('durationSeconds') as string, 10) : undefined,
      rampUpSeconds: formData.get('rampUpSeconds') ? parseInt(formData.get('rampUpSeconds') as string, 10) : undefined,
      targetEndpoint: formData.get('targetEndpoint') as string,
      httpMethod: formData.get('httpMethod') as HttpMethod,
      requestPayload: formData.get('requestPayload') as string || undefined,
      concurrencyStrategy: formData.get('concurrencyStrategy') as ConcurrencyStrategyType,
      requestTimeoutSeconds: parseInt(formData.get('requestTimeoutSeconds') as string, 10) || 30,
      maxRetries: parseInt(formData.get('maxRetries') as string, 10) || 0,
    };

    try {
      await createMutation.mutateAsync(request);
      setShowCreateModal(false);
      (e.target as HTMLFormElement).reset();
    } catch (err) {
      console.error('Failed to create scenario:', err);
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Are you sure you want to delete this scenario?')) {
      try {
        await deleteMutation.mutateAsync(id);
      } catch (err) {
        console.error('Failed to delete scenario:', err);
      }
    }
  };

  const columns = [
    { header: 'Name', accessor: 'name' as keyof TestScenario, cellClassName: 'font-medium' },
    { header: 'Strategy', accessor: (row: TestScenario) => <Badge variant="info">{CONCURRENCY_STRATEGY_LABELS[row.concurrencyStrategy]}</Badge> },
    { header: 'Users', accessor: 'concurrentUsers' as keyof TestScenario, className: 'text-right', cellClassName: 'font-mono' },
    { header: 'Requests/Duration', accessor: (row: TestScenario) => row.totalRequests ? `${row.totalRequests} req` : `${row.durationSeconds}s`, className: 'text-right' },
    { header: 'Endpoint', accessor: (row: TestScenario) => <code className="text-xs">{row.targetEndpoint}</code> },
    { header: 'Created', accessor: (row: TestScenario) => format(new Date(row.createdAt), 'MMM d, yyyy HH:mm') },
    {
      header: 'Actions',
      accessor: (row: TestScenario) => (
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="sm" asChild>
            <Link to={`/executions?scenario=${row.id}`}>Run</Link>
          </Button>
          <Button variant="ghost" size="sm" onClick={() => handleDelete(row.id!)} className="text-[var(--color-danger)]">
            Delete
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-[var(--color-text)]">Test Scenarios</h1>
          <p className="text-[var(--color-text-secondary)] mt-1">Manage your concurrency test configurations</p>
        </div>
        <Button onClick={() => setShowCreateModal(true)}>Create Scenario</Button>
      </div>

      <Card padding="sm">
        <div className="flex flex-col sm:flex-row gap-4">
          <Input
            placeholder="Search scenarios..."
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            className="max-w-xs"
          />
        </div>
      </Card>

      <Card>
        {isLoading ? (
          <CardContent className="py-12 text-center text-[var(--color-text-secondary)]">Loading scenarios...</CardContent>
        ) : error ? (
          <CardContent className="py-12 text-center text-[var(--color-danger)]">Failed to load scenarios</CardContent>
        ) : data && data.content.length > 0 ? (
          <>
            <Table
              columns={columns}
              data={data.content}
              keyAccessor={(row) => row.id!}
              emptyMessage="No scenarios found"
            />
            <CardFooter className="flex items-center justify-between">
              <p className="text-sm text-[var(--color-text-secondary)]">
                Showing {data.content.length} of {data.totalElements} scenarios
              </p>
              <div className="flex items-center gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage(page - 1)}
                >
                  Previous
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={page >= (data.totalPages - 1)}
                  onClick={() => setPage(page + 1)}
                >
                  Next
                </Button>
              </div>
            </CardFooter>
          </>
        ) : (
          <CardContent className="py-12 text-center text-[var(--color-text-secondary)]">
            <p>No scenarios created yet</p>
            <Button variant="primary" className="mt-4" onClick={() => setShowCreateModal(true)}>
              Create Your First Scenario
            </Button>
          </CardContent>
        )}
      </Card>

      {showCreateModal && (
        <CreateScenarioModal onClose={() => setShowCreateModal(false)} onSubmit={handleCreate} />
      )}
    </div>
  );
}

function CreateScenarioModal({ onClose, onSubmit }: { onClose: () => void; onSubmit: (e: React.FormEvent) => void }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50">
      <div className="bg-[var(--color-bg-secondary)] border border-[var(--color-border)] rounded-lg w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="p-6 border-b border-[var(--color-border)] flex items-center justify-between">
          <h2 className="text-xl font-bold">Create Test Scenario</h2>
          <button onClick={onClose} className="text-[var(--color-text-secondary)] hover:text-white p-1">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <form onSubmit={onSubmit} className="p-6 space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input label="Name*" name="name" required placeholder="e.g., ticket-booking-test" />
            <Select label="HTTP Method*" name="httpMethod" options={HTTP_METHODS} required defaultValue="POST" />
          </div>

          <Input label="Description" name="description" placeholder="Optional description" className="md:col-span-2" />

          <Input label="Target Endpoint*" name="targetEndpoint" required placeholder="http://localhost:8080/api/tickets/book" />

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Input label="Concurrent Users*" name="concurrentUsers" type="number" min="1" max="10000" required defaultValue="10" />
            <Input label="Total Requests" name="totalRequests" type="number" min="1" max="1000000" placeholder="Leave empty for duration-based" />
            <Input label="Duration (seconds)" name="durationSeconds" type="number" min="1" max="3600" placeholder="Leave empty for request-count-based" />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Input label="Ramp-up (seconds)" name="rampUpSeconds" type="number" min="0" max="300" defaultValue="0" />
            <Input label="Request Timeout (s)" name="requestTimeoutSeconds" type="number" min="1" max="300" defaultValue="30" />
            <Input label="Max Retries" name="maxRetries" type="number" min="0" max="10" defaultValue="0" />
          </div>

          <Select label="Concurrency Strategy" name="concurrencyStrategy" options={STRATEGY_OPTIONS} defaultValue="NONE" />

          <div className="flex justify-end gap-3 pt-4 border-t border-[var(--color-border)]">
            <Button variant="secondary" type="button" onClick={onClose}>Cancel</Button>
            <Button variant="primary" type="submit" disabled={createMutation.isPending}>
              {createMutation.isPending ? 'Creating...' : 'Create Scenario'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

// Need to import createMutation for the modal
import { useCreateScenario } from '../hooks/useScenarios';
const createMutation = useCreateScenario();