import React, { useEffect, useState } from 'react';
import { analysisService } from '../services/analysisService';
import { LogAnalysis } from '../types';
import { formatDate, formatDuration } from '../utils/formatters';
import { Link } from 'react-router-dom';
import {
  History,
  FileDown,
  ExternalLink,
  Search,
  CheckCircle2,
  AlertOctagon,
  Clock,
  Layers,
  ArrowRight,
} from 'lucide-react';

export const AnalysisHistoryPage: React.FC = () => {
  const [history, setHistory] = useState<LogAnalysis[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadHistory();
  }, []);

  const loadHistory = async () => {
    setIsLoading(true);
    try {
      const data = await analysisService.getAnalysisHistory();
      setHistory(data);
    } catch (e) {
      console.error('Failed to load analysis history', e);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDownloadReport = async (analysisId: number, filename: string) => {
    try {
      const markdown = await analysisService.exportReport(analysisId);
      const blob = new Blob([markdown], { type: 'text/markdown;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `incident-root-cause-report-${filename}.md`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch (e) {
      console.error('Failed to export report', e);
    }
  };

  const filteredHistory = history.filter(
    (h) =>
      h.filename.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (h.summary && h.summary.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
            <History className="w-6 h-6 text-blue-400" />
            Analysis Audit & Incident History
          </h1>
          <p className="mt-1 text-xs text-slate-400">
            Persistent historical records of all analyzed production incidents, diagnostics, and root cause reports.
          </p>
        </div>

        <div className="relative w-full sm:w-72">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-500" />
          <input
            type="text"
            placeholder="Search incident reports..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full rounded-lg bg-slate-900 border border-slate-800 pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
          />
        </div>
      </div>

      {/* History Table */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/80 shadow-xl overflow-hidden backdrop-blur-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-950/60 text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
              <tr>
                <th className="py-3 px-4">Analysis ID</th>
                <th className="py-3 px-4">Analyzed File</th>
                <th className="py-3 px-4">Lines Parsed</th>
                <th className="py-3 px-4">Total Errors</th>
                <th className="py-3 px-4">Critical</th>
                <th className="py-3 px-4">Patterns</th>
                <th className="py-3 px-4">Duration</th>
                <th className="py-3 px-4">Timestamp</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {filteredHistory.length > 0 ? (
                filteredHistory.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-800/40 transition-colors">
                    <td className="py-3.5 px-4 font-mono text-slate-400">
                      #{item.id}
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="font-semibold text-white font-mono">{item.filename}</div>
                      <div className="text-[11px] text-slate-400 truncate max-w-sm mt-0.5">
                        {item.summary}
                      </div>
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-300">
                      {item.totalLines?.toLocaleString()}
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-300">
                      <span className="rounded bg-rose-500/10 px-2 py-0.5 text-rose-400 font-semibold border border-rose-500/20">
                        {item.totalErrors}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 font-mono">
                      {item.criticalErrors > 0 ? (
                        <span className="rounded bg-rose-500/20 px-2 py-0.5 text-rose-300 font-bold border border-rose-500/40">
                          {item.criticalErrors}
                        </span>
                      ) : (
                        <span className="text-slate-500">0</span>
                      )}
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-300">
                      {item.uniquePatterns}
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-400">
                      {formatDuration(item.durationMs)}
                    </td>
                    <td className="py-3.5 px-4 text-slate-400">
                      {formatDate(item.createdAt)}
                    </td>
                    <td className="py-3.5 px-4 text-right space-x-2">
                      <button
                        onClick={() => handleDownloadReport(item.id, item.filename)}
                        className="inline-flex items-center gap-1 rounded-lg border border-slate-700 bg-slate-800 px-2.5 py-1.5 text-xs font-semibold text-slate-300 hover:border-slate-600 hover:text-white transition-colors"
                        title="Download Markdown Root Cause Report"
                      >
                        <FileDown className="w-3.5 h-3.5 text-blue-400" />
                        <span>Export</span>
                      </button>

                      <Link
                        to={`/explorer?analysisId=${item.id}`}
                        className="inline-flex items-center gap-1 rounded-lg bg-blue-600/20 px-2.5 py-1.5 text-xs font-semibold text-blue-400 border border-blue-500/30 hover:bg-blue-600 hover:text-white transition-colors"
                      >
                        <span>Open Explorer</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </Link>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={9} className="py-8 text-center text-slate-500">
                    No historical analysis runs found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
