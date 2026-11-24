import { useParams } from 'react-router-dom';
import { Card, CardHeader, CardTitle, CardContent } from '../components/Card';
import { Badge } from '../components/Badge';
import { Button } from '../components/Button';
import { useExecution, useTestResult } from '../hooks/useExecutions';
import { TEST_STATUS_COLORS, type TestExecution, type TestResult } from '../types';
import { format, formatDistanceToNow } from 'date-fns';
import { clsx } from 'clsx';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  BarChart, Bar, PieChart, Pie, Cell
} from 'recharts';

export function ExecutionDetail() {
  const { id } = useParams<{ id: string }>();
  const executionId = id ? parseInt(id, 10) : null;

  const { data: execution, isLoading: loadingExecution, error: executionError } = useExecution(executionId!);
  const { data: result, isLoading: loadingResult } = useTestResult(executionId!);

  if (loadingExecution) {
    return <div className="text-center py-12 text-[var(--color-text-secondary)]">Loading execution...</div>;
  }

  if (executionError || !execution) {
    return <div className="text-center py-12 text-[var(--color-danger)]">Execution not found</div>;
  }

  const statusColor = TEST_STATUS_COLORS[execution.status] || 'var(--color-text-secondary)';
  const isRunning = execution.status === 'RUNNING' || execution.status === 'PENDING';

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-[var(--color-text)]">{execution.scenarioName}</h1>
          <p className="text-[var(--color-text-secondary)] mt-1">Execution #{execution.id}</p>
        </div>
        <div className="flex items-center gap-3">
          <Badge variant={execution.status === 'COMPLETED' ? 'success' : execution.status === 'FAILED' ? 'danger' : 'warning'} className="text-lg px-4 py-2">
            {execution.status}
          </Badge>
          {isRunning && (
            <Button variant="secondary" onClick={() => window.location.reload()}>Refresh</Button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard label="Total Requests" value={execution.executedRequests} icon={<RequestIcon />} />
        <MetricCard label="Successful" value={execution.successfulRequests} color="var(--color-success)" icon={<CheckIcon />} />
        <MetricCard label="Failed" value={execution.failedRequests} color="var(--color-danger)" icon={<XIcon />} />
        <MetricCard label="Success Rate" value={`${execution.executedRequests > 0 ? ((execution.successfulRequests / execution.executedRequests) * 100).toFixed(1) : 0}%`} icon={<PercentIcon />} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Execution Details</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <DetailRow label="Scenario" value={execution.scenarioName} />
            <DetailRow label="Status" value={
              <span className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full" style={{ backgroundColor: statusColor }} />
                {execution.status}
              </span>
            } />
            <DetailRow label="Started" value={execution.startedAt ? format(new Date(execution.startedAt), 'MMM d, yyyy HH:mm:ss') : 'Not started'} />
            <DetailRow label="Completed" value={execution.completedAt ? format(new Date(execution.completedAt), 'MMM d, yyyy HH:mm:ss') : execution.cancelledAt ? format(new Date(execution.cancelledAt), 'MMM d, yyyy HH:mm:ss') + ' (cancelled)' : isRunning ? 'Running...' : 'N/A'} />
            <DetailRow label="Duration" value={execution.startedAt ? `${formatDistanceToNow(new Date(execution.startedAt), { addSuffix: true })}` : 'N/A'} />
            <DetailRow label="Error" value={execution.errorMessage || 'None'} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Configuration</CardTitle>
          </CardHeader>
          <CardContent>
            <pre className="text-xs text-[var(--color-text-secondary)] overflow-auto max-h-64">
              {JSON.stringify(execution.configurationSnapshot, null, 2)}
            </pre>
          </CardContent>
        </Card>
      </div>

      {result && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card>
            <CardHeader>
              <CardTitle>Latency Metrics</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <MetricRow label="Average" value={`${result.avgLatencyMs?.toFixed(2) || 0} ms`} />
                <MetricRow label="Median (p50)" value={`${result.medianLatencyMs?.toFixed(2) || 0} ms`} />
                <MetricRow label="p95" value={`${result.p95LatencyMs?.toFixed(2) || 0} ms`} />
                <MetricRow label="p99" value={`${result.p99LatencyMs?.toFixed(2) || 0} ms`} />
                <MetricRow label="Min" value={`${result.minLatencyMs?.toFixed(2) || 0} ms`} />
                <MetricRow label="Max" value={`${result.maxLatencyMs?.toFixed(2) || 0} ms`} />
                <MetricRow label="Std Dev" value={`${result.stdDeviationMs?.toFixed(2) || 0} ms`} />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Throughput</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <MetricRow label="Requests/sec" value={result.requestsPerSecond?.toFixed(2) || '0'} />
                <MetricRow label="Error Rate" value={`${result.errorRatePercent?.toFixed(2) || 0}%`} />
                <MetricRow label="Throughput" value={`${result.throughputMbps?.toFixed(2) || 0} Mbps`} />
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {result?.statusCodeDistribution && Object.keys(result.statusCodeDistribution).length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Status Code Distribution</CardTitle>
          </CardHeader>
          <CardContent>
            <StatusCodeChart data={result.statusCodeDistribution} />
          </CardContent>
        </Card>
      )}

      {result?.errorBreakdown && Object.keys(result.errorBreakdown).length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Error Breakdown</CardTitle>
          </CardHeader>
          <CardContent>
            <ErrorBreakdownChart data={result.errorBreakdown} />
          </CardContent>
        </Card>
      )}
    </div>
  );
}

function MetricCard({ label, value, color = 'var(--color-primary)', icon }: { label: string; value: string | number; color?: string; icon: React.ReactNode }) {
  return (
    <Card>
      <CardContent className="flex items-center justify-between">
        <div>
          <p className="text-sm text-[var(--color-text-secondary)]">{label}</p>
          <p className="text-2xl font-bold text-[var(--color-text)] mt-1">{value}</p>
        </div>
        <div className="p-3 rounded-lg" style={{ backgroundColor: `${color}20`, color }}>
          {icon}
        </div>
      </CardContent>
    </Card>
  );
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-center gap-2 py-2 border-b border-[var(--color-border)] last:border-0">
      <span className="text-sm text-[var(--color-text-secondary)] w-full sm:w-32">{label}</span>
      <span className="text-sm text-[var(--color-text)] font-mono">{value}</span>
    </div>
  );
}

function MetricRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between py-2 border-b border-[var(--color-border)] last:border-0">
      <span className="text-[var(--color-text-secondary)]">{label}</span>
      <span className="font-mono text-[var(--color-text)]">{value}</span>
    </div>
  );
}

function StatusCodeChart({ data }: { data: Record<number, number> }) {
  const chartData = Object.entries(data).map(([code, count]) => ({ code: `HTTP ${code}`, count }));

  return (
    <div className="h-64">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
          <XAxis dataKey="code" stroke="#94a3b8" tick={{ fill: '#94a3b8' }} />
          <YAxis stroke="#94a3b8" tick={{ fill: '#94a3b8' }} />
          <Tooltip />
          <Bar dataKey="count" fill="#3b82f6" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

function ErrorBreakdownChart({ data }: { data: Record<string, number> }) {
  const chartData = Object.entries(data).map(([error, count]) => ({ error: error.length > 30 ? error.slice(0, 30) + '...' : error, count }));
  const COLORS = ['#ef4444', '#f59e0b', '#3b82f6', '#22c55e', '#a855f7', '#ec4899'];

  return (
    <div className="h-64">
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={chartData}
            cx="50%"
            cy="50%"
            innerRadius={60}
            outerRadius={100}
            paddingAngle={2}
            dataKey="count"
            nameKey="error"
            label={({ error, percent }) => `${error} ${(percent * 100).toFixed(1)}%`}
            labelLine={false}
          >
            {chartData.map((_, i) => (
              <Cell key={`cell-${i}`} fill={COLORS[i % COLORS.length]} />
            ))}
          </Pie>
          <Tooltip formatter={(value: number) => [value.toString(), 'occurrences']} />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
}

function RequestIcon() { return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="2" y="2" width="20" height="20" rx="2"/><path d="M8 12h8M12 8v8"/></svg>; }
function CheckIcon() { return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="20 6 9 17 4 12"/></svg>; }
function XIcon() { return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>; }
function PercentIcon() { return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="19" y1="5" x2="5" y2="19"/><circle cx="6.5" cy="6.5" r="2.5"/><circle cx="17.5" cy="17.5" r="2.5"/></svg>; }