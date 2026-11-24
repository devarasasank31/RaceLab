import { Card, CardHeader, CardTitle, CardContent } from '../components/Card';
import { Badge } from '../components/Badge';
import { useScenarios } from '../hooks/useScenarios';
import { useExecutions } from '../hooks/useExecutions';
import { useTestResult } from '../hooks/useExecutions';
import { CONCURRENCY_STRATEGY_LABELS } from '../types';
import { format } from 'date-fns';

export function Results() {
  const { data: scenarios } = useScenarios(0, 100);
  const { data: executions } = useExecutions(0, 50);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-[var(--color-text)]">Test Results</h1>
        <p className="text-[var(--color-text-secondary)] mt-1">Compare results across different concurrency strategies</p>
      </div>

      {scenarios && scenarios.content.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Strategy Comparison</CardTitle>
          </CardHeader>
          <CardContent>
            <StrategyComparison scenarios={scenarios.content} executions={executions?.content || []} />
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>All Results</CardTitle>
        </CardHeader>
        <CardContent>
          <ResultsTable executions={executions?.content || []} />
        </CardContent>
      </Card>
    </div>
  );
}

function StrategyComparison({ scenarios, executions }: { scenarios: any[]; executions: any[] }) {
  // Group scenarios by target endpoint to compare strategies
  const byEndpoint = scenarios.reduce((acc, s) => {
    const key = s.targetEndpoint;
    if (!acc[key]) acc[key] = [];
    acc[key].push(s);
    return acc;
  }, {} as Record<string, any[]>);

  const comparisons = Object.entries(byEndpoint)
    .filter(([, scs]) => scs.length > 1)
    .map(([endpoint, scs]) => {
      const strategyResults = scs.map(scenario => {
        const exec = executions.find(e => e.scenarioId === scenario.id && e.status === 'COMPLETED');
        const result = exec ? { ...exec, scenario } : null;
        return result;
      }).filter(Boolean);

      return { endpoint, strategies: strategyResults };
    });

  if (comparisons.length === 0) {
    return <p className="text-[var(--color-text-secondary)] text-center py-8">No strategy comparisons available. Create multiple scenarios with the same endpoint but different strategies.</p>;
  }

  return (
    <div className="space-y-6">
      {comparisons.map(({ endpoint, strategies }) => (
        <Card key={endpoint} className="bg-[var(--color-bg)]">
          <CardHeader className="pb-2">
            <CardTitle className="text-base">Endpoint: <code className="text-xs">{endpoint}</code></CardTitle>
          </CardHeader>
          <CardContent>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-[var(--color-border)]">
                    <th className="px-4 py-2 text-left font-medium text-[var(--color-text-secondary)]">Strategy</th>
                    <th className="px-4 py-2 text-right font-medium text-[var(--color-text-secondary)]">Success Rate</th>
                    <th className="px-4 py-2 text-right font-medium text-[var(--color-text-secondary)]">Avg Latency</th>
                    <th className="px-4 py-2 text-right font-medium text-[var(--color-text-secondary)]">p95 Latency</th>
                    <th className="px-4 py-2 text-right font-medium text-[var(--color-text-secondary)]">Throughput (req/s)</th>
                    <th className="px-4 py-2 text-right font-medium text-[var(--color-text-secondary)]">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {strategies.map((s: any) => (
                    <tr key={s.scenario.id} className="border-b border-[var(--color-border)] last:border-0">
                      <td className="px-4 py-3">
                        <Badge variant="info">{CONCURRENCY_STRATEGY_LABELS[s.scenario.concurrencyStrategy]}</Badge>
                      </td>
                      <td className="px-4 py-3 text-right font-mono">
                        {s.executedRequests > 0 ? ((s.successfulRequests / s.executedRequests) * 100).toFixed(1) + '%' : 'N/A'}
                      </td>
                      <td className="px-4 py-3 text-right font-mono">N/A</td>
                      <td className="px-4 py-3 text-right font-mono">N/A</td>
                      <td className="px-4 py-3 text-right font-mono">N/A</td>
                      <td className="px-4 py-3 text-right">
                        <Badge variant={s.status === 'COMPLETED' ? 'success' : s.status === 'FAILED' ? 'danger' : 'warning'}>
                          {s.status}
                        </Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

function ResultsTable({ executions }: { executions: any[] }) {
  const completedExecutions = executions.filter(e => e.status === 'COMPLETED' || e.status === 'FAILED' || e.status === 'COMPLETED_WITH_VIOLATIONS');

  if (completedExecutions.length === 0) {
    return <p className="text-[var(--color-text-secondary)] text-center py-8">No completed executions yet</p>;
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-[var(--color-border)]">
            <th className="px-4 py-2 text-left font-medium text-[var(--color-text-secondary)]">Execution</th>
            <th className="px-4 py-2 text-left font-medium text-[var(--color-text-secondary)]">Scenario</th>
            <th className="px-4 py-2 text-center font-medium text-[var(--color-text-secondary)]">Status</th>
            <th className="px-4 py-2 text-right font-medium text-[var(--color-text-secondary)]">Requests</th>
            <th className="px-4 py-2 text-right font-medium text-[var(--color-text-secondary)]">Success Rate</th>
            <th className="px-4 py-2 text-right font-medium text-[var(--color-text-secondary)]">Completed</th>
          </tr>
        </thead>
        <tbody>
          {completedExecutions.map((execution) => (
            <tr key={execution.id} className="border-b border-[var(--color-border)] last:border-0 hover:bg-[var(--color-bg-tertiary)]">
              <td className="px-4 py-3 font-mono text-[var(--color-text-secondary)]">#{execution.id}</td>
              <td className="px-4 py-3">{execution.scenarioName}</td>
              <td className="px-4 py-3 text-center">
                <Badge variant={execution.status === 'COMPLETED' ? 'success' : execution.status === 'FAILED' ? 'danger' : 'warning'}>
                  {execution.status}
                </Badge>
              </td>
              <td className="px-4 py-3 text-right font-mono">{execution.executedRequests}</td>
              <td className="px-4 py-3 text-right font-mono">
                {execution.executedRequests > 0 ? ((execution.successfulRequests / execution.executedRequests) * 100).toFixed(1) + '%' : 'N/A'}
              </td>
              <td className="px-4 py-3 text-right text-[var(--color-text-secondary)]">
                {execution.completedAt ? format(new Date(execution.completedAt), 'MMM d, HH:mm') : '-'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}