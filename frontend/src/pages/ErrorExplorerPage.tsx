import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { analysisService } from '../services/analysisService';
import { ParsedError, Severity, ErrorCategory, LogAnalysis } from '../types';
import { SeverityBadge } from '../components/SeverityBadge';
import { CategoryBadge } from '../components/CategoryBadge';
import { ErrorDetailModal } from '../components/ErrorDetailModal';
import {
  Search,
  Filter,
  ChevronLeft,
  ChevronRight,
  ChevronDown,
  Layers,
  ArrowUpDown,
  RefreshCw,
} from 'lucide-react';

const CATEGORIES: { value: ErrorCategory; label: string }[] = [
  { value: 'APPLICATION_ERRORS', label: 'Application Errors' },
  { value: 'JAVA_EXCEPTIONS', label: 'Java Exceptions' },
  { value: 'DATABASE_ERRORS', label: 'Database Errors' },
  { value: 'API_ERRORS', label: 'API Errors' },
  { value: 'NETWORK_ERRORS', label: 'Network Errors' },
  { value: 'AUTHENTICATION_ERRORS', label: 'Authentication Errors' },
  { value: 'MEMORY_ISSUES', label: 'Memory Issues' },
  { value: 'CPU_PERFORMANCE_ISSUES', label: 'CPU Performance Issues' },
  { value: 'TIMEOUT_ERRORS', label: 'Timeout Errors' },
  { value: 'CONFIGURATION_ERRORS', label: 'Configuration Errors' },
  { value: 'INFRASTRUCTURE_ERRORS', label: 'Infrastructure Errors' },
  { value: 'DEPLOYMENT_ERRORS', label: 'Deployment Errors' },
  { value: 'DEPENDENCY_FAILURES', label: 'Dependency Failures' },
  { value: 'SECURITY_RELATED_EVENTS', label: 'Security-related Events' },
];

export const ErrorExplorerPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialAnalysisId = searchParams.get('analysisId')
    ? parseInt(searchParams.get('analysisId')!)
    : undefined;

  const [analyses, setAnalyses] = useState<LogAnalysis[]>([]);
  const [selectedAnalysisId, setSelectedAnalysisId] = useState<number | undefined>(initialAnalysisId);
  const [errors, setErrors] = useState<ParsedError[]>([]);
  const [selectedError, setSelectedError] = useState<ParsedError | null>(null);

  // Filters
  const [severityFilter, setSeverityFilter] = useState<Severity | undefined>(undefined);
  const [categoryFilter, setCategoryFilter] = useState<ErrorCategory | undefined>(undefined);
  const [serviceFilter, setServiceFilter] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    loadAnalyses();
  }, []);

  useEffect(() => {
    if (selectedAnalysisId) {
      loadErrors();
    }
  }, [selectedAnalysisId, severityFilter, categoryFilter, serviceFilter, page]);

  const loadAnalyses = async () => {
    try {
      const list = await analysisService.getAnalysisHistory();
      setAnalyses(list);
      if (!selectedAnalysisId && list.length > 0) {
        setSelectedAnalysisId(list[0].id);
      }
    } catch (e) {
      console.error(e);
    }
  };

  const loadErrors = async () => {
    if (!selectedAnalysisId) return;
    setIsLoading(true);
    try {
      const data = await analysisService.getErrors({
        analysisId: selectedAnalysisId,
        severity: severityFilter,
        category: categoryFilter,
        serviceName: serviceFilter.trim() || undefined,
        query: searchQuery.trim() || undefined,
        page,
        size: 15,
      });
      setErrors(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (e) {
      console.error(e);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    loadErrors();
  };

  const resetFilters = () => {
    setSeverityFilter(undefined);
    setCategoryFilter(undefined);
    setServiceFilter('');
    setSearchQuery('');
    setPage(0);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
            <Layers className="w-6 h-6 text-blue-400" />
            Error Explorer & Smart Triage
          </h1>
          <p className="mt-1 text-xs text-slate-400">
            Multi-dimensional search, categorization, and stack trace exploration for diagnosed incidents.
          </p>
        </div>

        {/* Analysis selector dropdown */}
        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-400">Target Incident:</span>
          <select
            value={selectedAnalysisId || ''}
            onChange={(e) => {
              const id = parseInt(e.target.value);
              setSelectedAnalysisId(id);
              setSearchParams({ analysisId: id.toString() });
              setPage(0);
            }}
            className="rounded-lg bg-slate-900 border border-slate-800 px-3 py-1.5 text-xs text-white focus:outline-none focus:border-blue-500"
          >
            {analyses.map((a) => (
              <option key={a.id} value={a.id}>
                #{a.id} - {a.filename} ({a.totalErrors} errors)
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/80 p-4 shadow-xl backdrop-blur-sm space-y-3">
        <form onSubmit={handleSearchSubmit} className="flex flex-wrap items-center gap-3">
          {/* Search bar */}
          <div className="relative flex-1 min-w-[220px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-500" />
            <input
              type="text"
              placeholder="Search error message, code, or trace ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full rounded-lg bg-slate-950 border border-slate-800 pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
            />
          </div>

          {/* Severity filter */}
          <select
            value={severityFilter || ''}
            onChange={(e) => {
              setSeverityFilter((e.target.value as Severity) || undefined);
              setPage(0);
            }}
            className="rounded-lg bg-slate-950 border border-slate-800 px-3 py-2 text-xs text-slate-300 focus:outline-none focus:border-blue-500"
          >
            <option value="">All Severities</option>
            <option value="CRITICAL">Critical</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>

          {/* Category filter */}
          <select
            value={categoryFilter || ''}
            onChange={(e) => {
              setCategoryFilter((e.target.value as ErrorCategory) || undefined);
              setPage(0);
            }}
            className="rounded-lg bg-slate-950 border border-slate-800 px-3 py-2 text-xs text-slate-300 focus:outline-none focus:border-blue-500"
          >
            <option value="">All 14 Categories</option>
            {CATEGORIES.map((c) => (
              <option key={c.value} value={c.value}>
                {c.label}
              </option>
            ))}
          </select>

          {/* Service filter */}
          <input
            type="text"
            placeholder="Filter by service..."
            value={serviceFilter}
            onChange={(e) => {
              setServiceFilter(e.target.value);
              setPage(0);
            }}
            className="w-36 rounded-lg bg-slate-950 border border-slate-800 px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
          />

          <button
            type="submit"
            className="rounded-lg bg-blue-600 px-4 py-2 text-xs font-semibold text-white hover:bg-blue-500 transition-colors"
          >
            Search
          </button>

          <button
            type="button"
            onClick={resetFilters}
            className="rounded-lg bg-slate-800 px-3 py-2 text-xs text-slate-400 hover:text-white hover:bg-slate-700 transition-colors"
          >
            Reset
          </button>
        </form>

        <div className="flex items-center justify-between text-xs text-slate-400 pt-1">
          <span>Found {totalElements} matching error signatures</span>
          {isLoading && <span className="text-blue-400 animate-pulse">Refreshing results...</span>}
        </div>
      </div>

      {/* Errors Table */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/80 shadow-xl overflow-hidden backdrop-blur-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-950/60 text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
              <tr>
                <th className="py-3 px-4">Severity</th>
                <th className="py-3 px-4">Category</th>
                <th className="py-3 px-4">Error Title & Details</th>
                <th className="py-3 px-4">Service</th>
                <th className="py-3 px-4">Location</th>
                <th className="py-3 px-4 text-center">Occurrences</th>
                <th className="py-3 px-4">First Occurrence</th>
                <th className="py-3 px-4 text-right">Root Cause</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {errors.length > 0 ? (
                errors.map((err) => (
                  <tr
                    key={err.id}
                    onClick={() => setSelectedError(err)}
                    className="hover:bg-slate-800/40 cursor-pointer transition-colors group"
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
                      <div className="text-[11px] text-slate-400 truncate mt-0.5 font-mono">
                        {err.message}
                      </div>
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-300">
                      {err.serviceName}
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-400">
                      {err.fileName ? `${err.fileName}:${err.lineNumber ?? 0}` : 'Unknown'}
                    </td>
                    <td className="py-3.5 px-4 text-center font-mono">
                      <span className="rounded bg-slate-800 px-2 py-0.5 text-white font-bold">
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
                        className="rounded-lg bg-blue-600/10 px-2.5 py-1 text-xs font-semibold text-blue-400 border border-blue-500/20 group-hover:bg-blue-600 group-hover:text-white transition-colors"
                      >
                        Diagnose
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-500">
                    No errors found matching the current filters.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Controls */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-slate-800 p-4 text-xs text-slate-400">
            <span>
              Page {page + 1} of {totalPages}
            </span>
            <div className="flex items-center gap-2">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="rounded-lg border border-slate-800 p-1.5 hover:bg-slate-800 hover:text-white disabled:opacity-30"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="rounded-lg border border-slate-800 p-1.5 hover:bg-slate-800 hover:text-white disabled:opacity-30"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Error Detail Modal */}
      <ErrorDetailModal
        error={selectedError}
        onClose={() => setSelectedError(null)}
      />
    </div>
  );
};
