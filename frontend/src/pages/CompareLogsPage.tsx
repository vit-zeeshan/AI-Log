import React, { useEffect, useState } from 'react';
import { analysisService } from '../services/analysisService';
import { LogAnalysis, CompareResponse, ErrorDiffItem } from '../types';
import { SeverityBadge } from '../components/SeverityBadge';
import {
  GitCompare,
  ArrowRight,
  TrendingUp,
  TrendingDown,
  PlusCircle,
  CheckCircle2,
  AlertTriangle,
  Play,
  Loader2,
  FileText,
} from 'lucide-react';

export const CompareLogsPage: React.FC = () => {
  const [analyses, setAnalyses] = useState<LogAnalysis[]>([]);
  const [baseId, setBaseId] = useState<number | ''>('');
  const [targetId, setTargetId] = useState<number | ''>('');
  const [comparison, setComparison] = useState<CompareResponse | null>(null);
  const [isComparing, setIsComparing] = useState(false);
  const [activeTab, setActiveTab] = useState<'all' | 'new' | 'resolved' | 'frequency'>('all');

  useEffect(() => {
    loadAnalyses();
  }, []);

  const loadAnalyses = async () => {
    try {
      const list = await analysisService.getAnalysisHistory();
      setAnalyses(list);
      if (list.length >= 2) {
        setBaseId(list[1].id);
        setTargetId(list[0].id);
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleCompare = async () => {
    if (!baseId || !targetId) return;
    setIsComparing(true);
    try {
      const res = await analysisService.compareAnalyses(Number(baseId), Number(targetId));
      setComparison(res);
    } catch (e) {
      console.error('Comparison failed', e);
    } finally {
      setIsComparing(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
          <GitCompare className="w-6 h-6 text-blue-400" />
          Log Comparison & Deployment Regression Diff
        </h1>
        <p className="mt-1 text-xs text-slate-400">
          Compare two incident runs, baseline vs canary deployments, or release versions to detect introduced regressions.
        </p>
      </div>

      {/* Selectors Bar */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/80 p-5 shadow-xl backdrop-blur-sm">
        <div className="grid grid-cols-1 md:grid-cols-5 gap-4 items-center">
          {/* Base File */}
          <div className="md:col-span-2 space-y-1.5">
            <label className="text-xs font-semibold text-slate-400 flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-slate-400" />
              Base Analysis (e.g. Previous Release / Baseline)
            </label>
            <select
              value={baseId}
              onChange={(e) => setBaseId(Number(e.target.value))}
              className="w-full rounded-xl bg-slate-950 border border-slate-800 p-2.5 text-xs text-white focus:outline-none focus:border-blue-500 font-mono"
            >
              <option value="">Select baseline log run...</option>
              {analyses.map((a) => (
                <option key={a.id} value={a.id}>
                  #{a.id} - {a.filename} ({a.totalErrors} errors)
                </option>
              ))}
            </select>
          </div>

          {/* Diff Arrow & Trigger Button */}
          <div className="flex flex-col items-center justify-center pt-5">
            <button
              onClick={handleCompare}
              disabled={!baseId || !targetId || baseId === targetId || isComparing}
              className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-5 py-2.5 text-xs font-semibold text-white shadow-lg hover:bg-blue-500 transition-colors disabled:opacity-40"
            >
              {isComparing ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Diffing...
                </>
              ) : (
                <>
                  <Play className="w-4 h-4" />
                  Run Diff
                </>
              )}
            </button>
          </div>

          {/* Target File */}
          <div className="md:col-span-2 space-y-1.5">
            <label className="text-xs font-semibold text-slate-400 flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-blue-500" />
              Target Analysis (e.g. Incident Run / Candidate)
            </label>
            <select
              value={targetId}
              onChange={(e) => setTargetId(Number(e.target.value))}
              className="w-full rounded-xl bg-slate-950 border border-slate-800 p-2.5 text-xs text-white focus:outline-none focus:border-blue-500 font-mono"
            >
              <option value="">Select target incident log...</option>
              {analyses.map((a) => (
                <option key={a.id} value={a.id}>
                  #{a.id} - {a.filename} ({a.totalErrors} errors)
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Comparison Results */}
      {comparison && (
        <div className="space-y-6">
          {/* Summary Stat Cards */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="rounded-xl border border-slate-800 bg-slate-900/90 p-4">
              <span className="text-xs font-semibold text-slate-400 uppercase">Error Volume Delta</span>
              <div className="mt-2 flex items-baseline gap-2">
                <span className="text-2xl font-bold font-mono text-white">
                  {comparison.errorCountDifference > 0 ? `+${comparison.errorCountDifference}` : comparison.errorCountDifference}
                </span>
                <span
                  className={`text-xs font-semibold ${
                    comparison.percentageChange > 0 ? 'text-rose-400' : 'text-emerald-400'
                  }`}
                >
                  {comparison.percentageChange > 0 ? `+${comparison.percentageChange}%` : `${comparison.percentageChange}%`}
                </span>
              </div>
              <p className="text-[11px] text-slate-400 mt-1">
                From {comparison.baseTotalErrors} to {comparison.targetTotalErrors}
              </p>
            </div>

            <div className="rounded-xl border border-rose-500/30 bg-rose-950/20 p-4">
              <span className="text-xs font-semibold text-rose-300 uppercase flex items-center gap-1">
                <PlusCircle className="w-3.5 h-3.5 text-rose-400" />
                New Errors Introduced
              </span>
              <div className="mt-2 text-2xl font-bold font-mono text-rose-400">
                {comparison.newErrors.length}
              </div>
              <p className="text-[11px] text-slate-400 mt-1">Absent in baseline</p>
            </div>

            <div className="rounded-xl border border-emerald-500/30 bg-emerald-950/20 p-4">
              <span className="text-xs font-semibold text-emerald-300 uppercase flex items-center gap-1">
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                Resolved Errors
              </span>
              <div className="mt-2 text-2xl font-bold font-mono text-emerald-400">
                {comparison.resolvedErrors.length}
              </div>
              <p className="text-[11px] text-slate-400 mt-1">Eliminated in target</p>
            </div>

            <div className="rounded-xl border border-amber-500/30 bg-amber-950/20 p-4">
              <span className="text-xs font-semibold text-amber-300 uppercase flex items-center gap-1">
                <TrendingUp className="w-3.5 h-3.5 text-amber-400" />
                Frequency Shifts
              </span>
              <div className="mt-2 text-2xl font-bold font-mono text-amber-400">
                {comparison.recurringPatternChanges.length}
              </div>
              <p className="text-[11px] text-slate-400 mt-1">Recurring rate delta</p>
            </div>
          </div>

          {/* SRE Executive Diff Summary */}
          <div className="rounded-xl border border-blue-500/20 bg-blue-950/10 p-4 text-xs text-blue-200 font-mono leading-relaxed">
            {comparison.comparisonSummary}
          </div>

          {/* Diff Tables Tabs */}
          <div className="rounded-2xl border border-slate-800 bg-slate-900/80 shadow-xl overflow-hidden backdrop-blur-sm">
            <div className="flex border-b border-slate-800 bg-slate-950/60 p-2 gap-2 text-xs">
              <button
                onClick={() => setActiveTab('all')}
                className={`rounded-lg px-3 py-1.5 font-semibold transition-colors ${
                  activeTab === 'all' ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-white'
                }`}
              >
                All Regressions & Shifts
              </button>
              <button
                onClick={() => setActiveTab('new')}
                className={`rounded-lg px-3 py-1.5 font-semibold transition-colors flex items-center gap-1 ${
                  activeTab === 'new' ? 'bg-rose-600 text-white' : 'text-slate-400 hover:text-white'
                }`}
              >
                New Introduced ({comparison.newErrors.length})
              </button>
              <button
                onClick={() => setActiveTab('resolved')}
                className={`rounded-lg px-3 py-1.5 font-semibold transition-colors flex items-center gap-1 ${
                  activeTab === 'resolved' ? 'bg-emerald-600 text-white' : 'text-slate-400 hover:text-white'
                }`}
              >
                Resolved ({comparison.resolvedErrors.length})
              </button>
              <button
                onClick={() => setActiveTab('frequency')}
                className={`rounded-lg px-3 py-1.5 font-semibold transition-colors flex items-center gap-1 ${
                  activeTab === 'frequency' ? 'bg-amber-600 text-white' : 'text-slate-400 hover:text-white'
                }`}
              >
                Pattern Rate Shifts ({comparison.recurringPatternChanges.length})
              </button>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-950/40 text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
                  <tr>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4">Severity</th>
                    <th className="py-3 px-4">Category</th>
                    <th className="py-3 px-4">Error Title & Details</th>
                    <th className="py-3 px-4">Service</th>
                    <th className="py-3 px-4 text-center">Base Count</th>
                    <th className="py-3 px-4 text-center">Target Count</th>
                    <th className="py-3 px-4 text-center">Delta</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/80">
                  {renderDiffRows(comparison, activeTab)}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

function renderDiffRows(comparison: CompareResponse, tab: 'all' | 'new' | 'resolved' | 'frequency') {
  let items: ErrorDiffItem[] = [];

  if (tab === 'new') items = comparison.newErrors;
  else if (tab === 'resolved') items = comparison.resolvedErrors;
  else if (tab === 'frequency') items = comparison.recurringPatternChanges;
  else items = [...comparison.newErrors, ...comparison.recurringPatternChanges, ...comparison.resolvedErrors];

  if (items.length === 0) {
    return (
      <tr>
        <td colSpan={8} className="py-8 text-center text-slate-500">
          No differences found in this tab category.
        </td>
      </tr>
    );
  }

  return items.map((item, idx) => (
    <tr key={idx} className="hover:bg-slate-800/40 transition-colors">
      <td className="py-3.5 px-4">
        <span
          className={`inline-block rounded-md px-2 py-0.5 text-[10px] font-bold ${
            item.status === 'NEW'
              ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40'
              : item.status === 'RESOLVED'
              ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
              : item.status === 'INCREASED'
              ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
              : 'bg-slate-800 text-slate-300'
          }`}
        >
          {item.status}
        </span>
      </td>
      <td className="py-3.5 px-4 font-mono">
        <span className="font-semibold text-white">{item.severity}</span>
      </td>
      <td className="py-3.5 px-4 text-slate-300">{item.category}</td>
      <td className="py-3.5 px-4 max-w-sm">
        <div className="font-semibold text-white font-mono truncate">{item.title}</div>
        <div className="text-[11px] text-slate-400 font-mono truncate mt-0.5">{item.sampleMessage}</div>
      </td>
      <td className="py-3.5 px-4 font-mono text-slate-300">{item.serviceName}</td>
      <td className="py-3.5 px-4 text-center font-mono text-slate-400">{item.baseCount}</td>
      <td className="py-3.5 px-4 text-center font-mono text-white font-bold">{item.targetCount}</td>
      <td className="py-3.5 px-4 text-center font-mono font-bold">
        <span
          className={
            item.countDelta > 0
              ? 'text-rose-400'
              : item.countDelta < 0
              ? 'text-emerald-400'
              : 'text-slate-400'
          }
        >
          {item.countDelta > 0 ? `+${item.countDelta}` : item.countDelta}
        </span>
      </td>
    </tr>
  ));
}
