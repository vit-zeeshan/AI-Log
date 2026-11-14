import React, { useEffect, useState } from 'react';
import { analysisService } from '../services/analysisService';
import { DashboardSummary, ParsedError } from '../types';
import { MetricCard } from '../components/MetricCard';
import { SeverityBadge } from '../components/SeverityBadge';
import { CategoryBadge } from '../components/CategoryBadge';
import { ErrorDetailModal } from '../components/ErrorDetailModal';
import { formatBytes, formatDuration, formatDate } from '../utils/formatters';
import { Link } from 'react-router-dom';
import {
  FileText,
  AlertOctagon,
  AlertTriangle,
  Flame,
  Fingerprint,
  Timer,
  UploadCloud,
  ChevronRight,
  TrendingUp,
  Activity,
  ArrowUpRight,
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
} from 'recharts';

const SEVERITY_COLORS: Record<string, string> = {
  CRITICAL: '#f43f5e',
  HIGH: '#f59e0b',
  MEDIUM: '#eab308',
  LOW: '#3b82f6',
};

export const DashboardPage: React.FC = () => {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedError, setSelectedError] = useState<ParsedError | null>(null);

  useEffect(() => {
    loadSummary();
  }, []);

  const loadSummary = async () => {
    setIsLoading(true);
    try {
      const data = await analysisService.getDashboardSummary();
      setSummary(data);
    } catch (e) {
      console.error('Failed to load dashboard summary', e);
    } finally {
      setIsLoading(false);
    }
  };

  // Prepare chart data
  const severityPieData = summary?.errorDistributionBySeverity
    ? Object.entries(summary.errorDistributionBySeverity).map(([name, value]) => ({
        name,
        value,
      }))
    : [];

  const categoryBarData = summary?.errorDistributionByCategory
    ? Object.entries(summary.errorDistributionByCategory).slice(0, 6).map(([name, value]) => ({
        name: name.replace('_ERRORS', '').replace('_ISSUES', ''),
        count: value,
      }))
    : [];

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">
            Incident & SRE Reliability Dashboard
          </h1>
          <p className="mt-1 text-xs text-slate-400">
            Real-time automated root cause diagnosis, log anomaly clustering, and error analytics.
          </p>
        </div>
        <Link
          to="/upload"
          className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2.5 text-xs font-semibold text-white shadow-lg shadow-blue-500/20 hover:bg-blue-500 transition-colors"
        >
          <UploadCloud className="w-4 h-4" />
          <span>Upload & Analyze Log</span>
        </Link>
      </div>

      {/* Metric Cards Grid */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
        <MetricCard
          title="Total Log Lines"
          value={summary?.totalLogEntries?.toLocaleString() || '0'}
          subtitle="Processed entries"
          icon={<FileText className="w-4 h-4 text-blue-400" />}
          variant="info"
        />
        <MetricCard
          title="Total Errors"
          value={summary?.totalErrors?.toLocaleString() || '0'}
          subtitle="Captured failures"
          icon={<AlertOctagon className="w-4 h-4 text-rose-400" />}
          variant="danger"
        />
        <MetricCard
          title="Critical Issues"
          value={summary?.criticalErrors || 0}
          subtitle="Priority 1 alerts"
          icon={<Flame className="w-4 h-4 text-rose-500" />}
          variant="danger"
        />
        <MetricCard
          title="Warnings"
          value={summary?.totalWarnings?.toLocaleString() || '0'}
          subtitle="Non-fatal anomalies"
          icon={<AlertTriangle className="w-4 h-4 text-amber-400" />}
          variant="warning"
        />
        <MetricCard
          title="Unique Signatures"
          value={summary?.uniqueErrorPatterns || 0}
          subtitle="Deduplicated patterns"
          icon={<Fingerprint className="w-4 h-4 text-purple-400" />}
        />
        <MetricCard
          title="Avg Analysis Time"
          value={formatDuration(summary?.avgAnalysisDurationMs || 0)}
          subtitle="Stream processing"
          icon={<Timer className="w-4 h-4 text-emerald-400" />}
          variant="success"
        />
      </div>

      {/* Interactive Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Error Frequency Over Time */}
        <div className="lg:col-span-2 rounded-2xl border border-slate-800 bg-slate-900/80 p-5 shadow-xl backdrop-blur-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-sm font-semibold text-white flex items-center gap-2">
                <Activity className="w-4 h-4 text-blue-400" />
                Error Frequency & Timeline Trends
              </h3>
              <p className="text-xs text-slate-400">Captured failures over previous log runs</p>
            </div>
            <span className="text-xs font-mono text-slate-500">Live Telemetry</span>
          </div>
          <div className="h-64 w-full">
            {summary?.errorFrequencyOverTime && summary.errorFrequencyOverTime.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={summary.errorFrequencyOverTime}>
                  <defs>
                    <linearGradient id="errorColor" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#f43f5e" stopOpacity={0.4} />
                      <stop offset="95%" stopColor="#f43f5e" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                  <XAxis dataKey="timestamp" stroke="#64748b" fontSize={11} />
                  <YAxis stroke="#64748b" fontSize={11} />
                  <Tooltip
                    contentStyle={{ backgroundColor: '#090d16', borderColor: '#334155', borderRadius: '8px' }}
                    labelStyle={{ color: '#94a3b8', fontSize: '12px' }}
                  />
                  <Area type="monotone" dataKey="errorCount" stroke="#f43f5e" fillOpacity={1} fill="url(#errorColor)" name="Errors" />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex h-full items-center justify-center text-xs text-slate-500">
                No timeline telemetry points yet. Upload a log file to visualize frequency trends.
              </div>
            )}
          </div>
        </div>

        {/* Severity Distribution */}
        <div className="rounded-2xl border border-slate-800 bg-slate-900/80 p-5 shadow-xl backdrop-blur-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-sm font-semibold text-white">Severity Breakdown</h3>
              <p className="text-xs text-slate-400">Proportion of impact tiers</p>
            </div>
          </div>
          <div className="h-48 w-full flex items-center justify-center">
            {severityPieData.some((d) => d.value > 0) ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={severityPieData}
                    cx="50%"
                    cy="50%"
                    innerRadius={50}
                    outerRadius={75}
                    paddingAngle={4}
                    dataKey="value"
                  >
                    {severityPieData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={SEVERITY_COLORS[entry.name] || '#3b82f6'} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{ backgroundColor: '#090d16', borderColor: '#334155', borderRadius: '8px' }}
                  />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="text-xs text-slate-500">No severity distributions recorded</div>
            )}
          </div>
          {/* Legend */}
          <div className="grid grid-cols-2 gap-2 pt-2 border-t border-slate-800/80 text-xs">
            {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map((sev) => (
              <div key={sev} className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: SEVERITY_COLORS[sev] }} />
                <span className="text-slate-400">{sev}:</span>
                <span className="font-mono font-semibold text-white">
                  {summary?.errorDistributionBySeverity?.[sev] || 0}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Top Recurring Errors Table */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/80 shadow-xl overflow-hidden backdrop-blur-sm">
        <div className="flex items-center justify-between border-b border-slate-800 p-5">
          <div>
            <h3 className="text-sm font-semibold text-white flex items-center gap-2">
              <TrendingUp className="w-4 h-4 text-rose-400" />
              Top Recurring Error Signatures
            </h3>
            <p className="text-xs text-slate-400">
              Aggregated error clusters requiring immediate Senior SRE triage
            </p>
          </div>
          <Link
            to="/explorer"
            className="flex items-center gap-1 text-xs font-semibold text-blue-400 hover:text-blue-300 transition-colors"
          >
            <span>View All in Explorer</span>
            <ArrowUpRight className="w-4 h-4" />
          </Link>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-950/60 text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
              <tr>
                <th className="py-3 px-4">Severity</th>
                <th className="py-3 px-4">Category</th>
                <th className="py-3 px-4">Error Signature & Message</th>
                <th className="py-3 px-4">Service</th>
                <th className="py-3 px-4 text-center">Occurrences</th>
                <th className="py-3 px-4">First Occurrence</th>
                <th className="py-3 px-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {summary?.topRecurringErrors && summary.topRecurringErrors.length > 0 ? (
                summary.topRecurringErrors.map((err) => (
                  <tr
                    key={err.id}
                    onClick={() => setSelectedError(err)}
                    className="hover:bg-slate-800/50 cursor-pointer transition-colors group"
                  >
                    <td className="py-3.5 px-4">
                      <SeverityBadge severity={err.severity} size="sm" />
                    </td>
                    <td className="py-3.5 px-4">
                      <CategoryBadge category={err.category} />
                    </td>
                    <td className="py-3.5 px-4 max-w-md">
                      <div className="font-semibold text-white font-mono group-hover:text-blue-400 transition-colors truncate">
                        {err.title}
                      </div>
                      <div className="text-slate-400 text-[11px] truncate mt-0.5">
                        {err.message}
                      </div>
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-300">
                      {err.serviceName}
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <span className="inline-block rounded bg-slate-800 px-2 py-0.5 font-mono font-semibold text-white">
                        {err.occurrenceCount}x
                      </span>
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-400">
                      {err.firstOccurrence}
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedError(err);
                        }}
                        className="inline-flex items-center gap-1 text-xs text-blue-400 group-hover:underline"
                      >
                        Investigate
                        <ChevronRight className="w-3.5 h-3.5" />
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-500">
                    No recurring errors recorded. Upload and analyze a log file to view findings.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Investigation Modal */}
      <ErrorDetailModal
        error={selectedError}
        onClose={() => setSelectedError(null)}
      />
    </div>
  );
};
