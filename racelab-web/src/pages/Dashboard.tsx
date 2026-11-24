import { useQuery } from '@tanstack/react-query';
import { Card, CardHeader, CardTitle, CardContent } from '../components/Card';
import { Badge } from '../components/Badge';
import { Button } from '../components/Button';
import { useRecentExecutions, useRunningExecutions } from '../hooks/useExecutions';
import { useScenarios } from '../hooks/useScenarios';
import { TEST_STATUS_COLORS, CONCURRENCY_STRATEGY_LABELS } from '../types';
import { formatDistanceToNow } from 'date-fns';
import { clsx } from 'clsx';

export function Dashboard() {
  const { data: recentExecutions, isLoading: loadingExecutions } = useRecentExecutions(10);
  const { data: runningExecutions } = useRunningExecutions();
  const { data: scenarios } = useScenarios(0, 5);

  const stats = {
    totalScenarios: scenarios?.totalElements || 0,
    runningTests: runningExecutions?.length || 0,
    completedToday: recentExecutions?.filter(
      (e) => e.status === 'COMPLETED' && new Date(e.completedAt || '') > new Date(Date.now() - 86400000)
    ).length || 0,
    failedToday: recentExecutions?.filter(
      (e) => e.status === 'FAILED' && new Date(e.completedAt || '') > new Date(Date.now() - 86400000)
    ).length || 0,
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-[var(--color-text)]">Dashboard</h1>
          <p className="text-[var(--color-text-secondary)] mt-1">Overview of your concurrency testing platform</p>
        </div>
        <Button variant="primary" asChild>
          <a href="/scenarios/new">Create Test Scenario</a>
        </Button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Scenarios"
          value={stats.totalScenarios}
          icon={<ScenariosIcon />}
          color="var(--color-primary)"
        />
        <StatCard
          title="Running Tests"
          value={stats.runningTests}
          icon={<RunningIcon />}
          color="var(--color-primary)"
        />
        <StatCard
          title="Completed (24h)"
          value={stats.completedToday}
          icon={<CheckIcon />}
          color="var(--color-success)"
        />
        <StatCard
          title="Failed (24h)"
          value={stats.failedToday}
          icon={<AlertIcon />}
          color="var(--color-danger)"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Running Executions</CardTitle>
          </CardHeader>
          <CardContent>
            {runningExecutions && runningExecutions.length > 0 ? (
              <div className="space-y-3">
                {runningExecutions.map((execution) => (
                  <ExecutionRow key={execution.id} execution={execution} />
                ))}
              </div>
            ) : (
              <p className="text-[var(--color-text-secondary)] text-center py-8">No running executions</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Recent Executions</CardTitle>
          </CardHeader>
          <CardContent>
            {loadingExecutions ? (
              <p className="text-[var(--color-text-secondary)] text-center py-8">Loading...</p>
            ) : recentExecutions && recentExecutions.length > 0 ? (
              <div className="space-y-3">
                {recentExecutions.map((execution) => (
                  <ExecutionRow key={execution.id} execution={execution} />
                ))}
              </div>
            ) : (
              <p className="text-[var(--color-text-secondary)] text-center py-8">No recent executions</p>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Quick Actions</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              <QuickAction
                title="Create Scenario"
                description="Define a new concurrency test"
                href="/scenarios/new"
                icon={<PlusIcon />}
              />
              <QuickAction
                title="View All Scenarios"
                description="Browse and manage test scenarios"
                href="/scenarios"
                icon={<ListIcon />}
              />
              <QuickAction
                title="View All Executions"
                description="Browse test execution history"
                href="/executions"
                icon={<HistoryIcon />}
              />
              <QuickAction
                title="Compare Strategies"
                description="Run strategy comparison tests"
                href="/scenarios?compare=true"
                icon={<CompareIcon />}
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Recent Scenarios</CardTitle>
          </CardHeader>
          <CardContent>
            {scenarios && scenarios.content.length > 0 ? (
              <div className="space-y-3">
                {scenarios.content.map((scenario) => (
                  <ScenarioRow key={scenario.id} scenario={scenario} />
                ))}
              </div>
            ) : (
              <p className="text-[var(--color-text-secondary)] text-center py-8">No scenarios created yet</p>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function StatCard({ title, value, icon, color }: { title: string; value: number; icon: React.ReactNode; color: string }) {
  return (
    <Card>
      <CardContent className="flex items-center justify-between">
        <div>
          <p className="text-sm text-[var(--color-text-secondary)]">{title}</p>
          <p className="text-3xl font-bold text-[var(--color-text)] mt-1">{value}</p>
        </div>
        <div className={clsx('p-3 rounded-lg', `bg-[${color}]/20`)} style={{ color }}>
          {icon}
        </div>
      </CardContent>
    </Card>
  );
}

function ExecutionRow({ execution }: { execution: any }) {
  const statusColor = TEST_STATUS_COLORS[execution.status] || 'var(--color-text-secondary)';
  const timeAgo = execution.startedAt ? formatDistanceToNow(new Date(execution.startedAt), { addSuffix: true }) : 'Unknown';

  return (
    <div className="flex items-center justify-between p-3 bg-[var(--color-bg)] rounded-md">
      <div className="flex items-center gap-3">
        <div className="w-2 h-2 rounded-full" style={{ backgroundColor: statusColor }} />
        <div>
          <p className="font-medium text-sm">{execution.scenarioName || `Execution #${execution.id}`}</p>
          <p className="text-xs text-[var(--color-text-secondary)]">{timeAgo}</p>
        </div>
      </div>
      <Badge variant={execution.status === 'RUNNING' ? 'info' : execution.status === 'COMPLETED' ? 'success' : 'danger'}>
        {execution.status}
      </Badge>
    </div>
  );
}

function ScenarioRow({ scenario }: { scenario: any }) {
  return (
    <div className="flex items-center justify-between p-3 bg-[var(--color-bg)] rounded-md">
      <div className="flex items-center gap-3">
        <Badge variant="info">{CONCURRENCY_STRATEGY_LABELS[scenario.concurrencyStrategy]}</Badge>
        <div>
          <p className="font-medium text-sm">{scenario.name}</p>
          <p className="text-xs text-[var(--color-text-secondary)]">
            {scenario.concurrentUsers} users • {scenario.httpMethod} {scenario.targetEndpoint}
          </p>
        </div>
      </div>
    </div>
  );
}

function QuickAction({ title, description, href, icon }: { title: string; description: string; href: string; icon: React.ReactNode }) {
  return (
    <a href={href} className="flex items-center gap-3 p-3 bg-[var(--color-bg)] rounded-md hover:bg-[var(--color-bg-tertiary)] transition-colors">
      <div className="p-2 bg-[var(--color-primary)]/20 text-[var(--color-primary)] rounded-md">{icon}</div>
      <div>
        <p className="font-medium text-sm">{title}</p>
        <p className="text-xs text-[var(--color-text-secondary)]">{description}</p>
      </div>
    </a>
  );
}

function ScenariosIcon() {
  return <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>;
}
function RunningIcon() {
  return <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polygon points="5 3 19 12 5 21 5 3"/></svg>;
}
function CheckIcon() {
  return <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="20 6 9 17 4 12"/></svg>;
}
function AlertIcon() {
  return <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>;
}
function PlusIcon() {
  return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>;
}
function ListIcon() {
  return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>;
}
function HistoryIcon() {
  return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>;
}
function CompareIcon() {
  return <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="5" y1="9" x2="19" y2="9"/><line x1="5" y1="15" x2="19" y2="15"/><line x1="5" y1="9" x2="5" y2="15"/><line x1="19" y1="9" x2="19" y2="15"/></svg>;
}