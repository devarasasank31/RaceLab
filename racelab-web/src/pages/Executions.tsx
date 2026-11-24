import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { Card, CardHeader, CardTitle, CardContent, CardFooter } from '../components/Card';
import { Button } from '../components/Button';
import { Table } from '../components/Table';
import { Badge } from '../components/Badge';
import { useExecutions, useRunningExecutions, useStartExecutionSync, useCancelExecution } from '../hooks/useExecutions';
import { useScenarios } from '../hooks/useScenarios';
import { format, formatDistanceToNow } from 'date-fns';
import { TEST_STATUS_COLORS, type TestExecution } from '../types';
import { clsx } from 'clsx';

export function Executions() {
  const [page, setPage] = useState(0);
  const [searchParams] = useSearchParams();
  const scenarioId = searchParams.get('scenario') ? parseInt(searchParams.get('scenario')!, 10) : null;
  const navigate = () => {}; // Placeholder for navigation

  const { data: scenarios } = useScenarios(0, 100);
  const { data, isLoading, error } = scenarioId
    ? useExecutionsByScenario(scenarioId)
    : useExecutions(page, 20);
  const { data: runningExecutions } = useRunningExecutions();
  const startMutation = useStartExecutionSync();
  const cancelMutation = useCancelExecution();

  const columns = [
    { header: 'Scenario', accessor: (row: TestExecution) => (
      <Link to={`/executions/${row.id}`} className="hover:underline">{row.scenarioName}</Link>
    )},
    { header: 'Status', accessor: (row: TestExecution) => (
      <Badge variant={row.status === 'RUNNING' ? 'info' : row.status === 'COMPLETED' ? 'success' : row.status === 'FAILED' ? 'danger' : 'warning'}>
        {row.status}
      </Badge>
    )},
    { header: 'Requests', accessor: (row: TestExecution) => `${row.successfulRequests}/${row.executedRequests}`, className: 'text-right', cellClassName: 'font-mono' },
    { header: 'Success Rate', accessor: (row: TestExecution) => `${row.executedRequests > 0 ? ((row.successfulRequests / row.executedRequests) * 100).toFixed(1) : 0}%`, className: 'text-right' },
    { header: 'Duration', accessor: (row: TestExecution) => {
      if (!row.startedAt) return '-';
      const end = row.completedAt || row.cancelledAt || new Date().toISOString();
      const ms = new Date(end).getTime() - new Date(row.startedAt).getTime();
      return `${(ms / 1000).toFixed(1)}s`;
    }, className: 'text-right' },
    { header: 'Started', accessor: (row: TestExecution) => row.startedAt ? formatDistanceToNow(new Date(row.startedAt), { addSuffix: true }) : '-' },
    {
      header: 'Actions',
      accessor: (row: TestExecution) => (
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="sm" asChild>
            <Link to={`/executions/${row.id}`}>View</Link>
          </Button>
          {row.status === 'RUNNING' && (
            <Button variant="ghost" size="sm" onClick={() => cancelMutation.mutate(row.id!)} className="text-[var(--color-danger)]">
              Cancel
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-[var(--color-text)]">Test Executions</h1>
          <p className="text-[var(--color-text-secondary)] mt-1">Monitor and manage test runs</p>
        </div>
        <div className="flex items-center gap-3">
          {scenarios && scenarios.content.length > 0 && (
            <select
              className="px-3 py-2 bg-[var(--color-bg)] border border-[var(--color-border)] rounded-md text-[var(--color-text)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary)]"
              value={scenarioId || ''}
              onChange={(e) => {
                const val = e.target.value;
                if (val) {
                  window.location.href = `/executions?scenario=${val}`;
                } else {
                  window.location.href = '/executions';
                }
              }}
            >
              <option value="">All Scenarios</option>
              {scenarios.content.map((s) => (
                <option key={s.id} value={s.id}>{s.name}</option>
              ))}
            </select>
          )}
          {scenarios && scenarios.content.length > 0 && (
            <Button variant="primary" onClick={() => startNewExecution(scenarios.content[0].id!)} disabled={startMutation.isPending}>
              {startMutation.isPending ? 'Starting...' : 'New Execution'}
            </Button>
          )}
        </div>
      </div>

      {runningExecutions && runningExecutions.length > 0 && (
        <Card padding="sm">
          <CardHeader>
            <CardTitle>Running Executions ({runningExecutions.length})</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              {runningExecutions.map((execution) => (
                <RunningExecutionCard key={execution.id} execution={execution} onCancel={() => cancelMutation.mutate(execution.id!)} />
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      <Card>
        {isLoading ? (
          <CardContent className="py-12 text-center text-[var(--color-text-secondary)]">Loading executions...</CardContent>
        ) : error ? (
          <CardContent className="py-12 text-center text-[var(--color-danger)]">Failed to load executions</CardContent>
        ) : data && 'content' in data && data.content.length > 0 ? (
          <>
            <Table
              columns={columns}
              data={data.content}
              keyAccessor={(row) => row.id!}
              emptyMessage="No executions found"
            />
            <CardFooter className="flex items-center justify-between">
              <p className="text-sm text-[var(--color-text-secondary)]">
                Showing {data.content.length} of {data.totalElements} executions
              </p>
              <div className="flex items-center gap-2">
                <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>
                <Button variant="secondary" size="sm" disabled={page >= (data.totalPages - 1)} onClick={() => setPage(page + 1)}>Next</Button>
              </div>
            </CardFooter>
          </>
        ) : (
          <CardContent className="py-12 text-center text-[var(--color-text-secondary)]">
            <p>No executions found</p>
            <Button variant="primary" className="mt-4" onClick={() => navigate('/scenarios/new')}>Create a Scenario First</Button>
          </CardContent>
        )}
      </Card>
    </div>
  );
}

function useExecutionsByScenario(scenarioId: number) {
  const { useExecutionsByScenario: hook } = require('../hooks/useExecutions');
  return hook(scenarioId);
}

function startNewExecution(scenarioId: number) {
  // This would need to be implemented with the mutation
  console.log('Start execution for scenario', scenarioId);
}

function RunningExecutionCard({ execution, onCancel }: { execution: TestExecution; onCancel: () => void }) {
  const progress = execution.executedRequests > 0 ? (execution.successfulRequests / execution.executedRequests) * 100 : 0;

  return (
    <div className="p-4 bg-[var(--color-bg)] rounded-md border border-[var(--color-border)]">
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-3">
          <div className="w-3 h-3 rounded-full bg-[var(--color-primary)] animate-pulse" />
          <div>
            <p className="font-medium">{execution.scenarioName}</p>
            <p className="text-sm text-[var(--color-text-secondary)]">Started {formatDistanceToNow(new Date(execution.startedAt!), { addSuffix: true })}</p>
          </div>
        </div>
        <Button variant="ghost" size="sm" onClick={onCancel} className="text-[var(--color-danger)]">Cancel</Button>
      </div>
      <div className="h-2 bg-[var(--color-bg-tertiary)] rounded-full overflow-hidden">
        <div className="h-full bg-[var(--color-primary)] transition-all duration-300" style={{ width: `${progress}%` }} />
      </div>
      <div className="flex justify-between text-sm text-[var(--color-text-secondary)] mt-1">
        <span>{execution.executedRequests} requests</span>
        <span>{execution.successfulRequests} successful</span>
        <span>{execution.failedRequests} failed</span>
      </div>
    </div>
  );
}