import React, { useEffect, useState } from 'react';
import { useLogStore } from '../store/logStore';
import { FileUploadDropzone } from '../components/FileUploadDropzone';
import { formatBytes, formatDate } from '../utils/formatters';
import { LogFile } from '../types';
import { useNavigate } from 'react-router-dom';
import {
  FileText,
  Play,
  Trash2,
  CheckCircle2,
  Clock,
  Loader2,
  AlertCircle,
  Archive,
  Search,
} from 'lucide-react';

export const UploadLogsPage: React.FC = () => {
  const { files, fetchFiles, triggerAnalysis, deleteFile, isAnalyzing } = useLogStore();
  const [analyzingFileId, setAnalyzingFileId] = useState<number | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    fetchFiles();
  }, []);

  const handleAnalyze = async (fileId: number) => {
    setAnalyzingFileId(fileId);
    try {
      const result = await triggerAnalysis(fileId);
      navigate(`/explorer?analysisId=${result.id}`);
    } catch (e) {
      console.error('Analysis failed', e);
    } finally {
      setAnalyzingFileId(null);
    }
  };

  const filteredFiles = files.filter((f) =>
    f.originalFilename.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-white">
          Upload & Ingest Application Logs
        </h1>
        <p className="mt-1 text-xs text-slate-400">
          Upload raw incident dumps, microservice logs, or compressed server archives for automated AI diagnosis.
        </p>
      </div>

      {/* Upload Zone */}
      <FileUploadDropzone
        onFileUploaded={() => fetchFiles()}
        onAnalyzeTriggered={(id) => handleAnalyze(id)}
      />

      {/* Uploaded Files Table */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/80 shadow-xl overflow-hidden backdrop-blur-sm">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between border-b border-slate-800 p-5 gap-3">
          <div>
            <h3 className="text-sm font-semibold text-white">
              Stored Log Archives ({files.length})
            </h3>
            <p className="text-xs text-slate-400">
              Manage uploaded logs and trigger full-scale stream analysis
            </p>
          </div>

          <div className="relative w-full sm:w-64">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-500" />
            <input
              type="text"
              placeholder="Search filename..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full rounded-lg bg-slate-950 border border-slate-800 pl-9 pr-3 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
            />
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-950/60 text-slate-400 uppercase tracking-wider font-semibold border-b border-slate-800">
              <tr>
                <th className="py-3 px-4">Filename</th>
                <th className="py-3 px-4">Format</th>
                <th className="py-3 px-4">Size</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Uploaded At</th>
                <th className="py-3 px-4">SHA-256 Checksum</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/80">
              {filteredFiles.length > 0 ? (
                filteredFiles.map((f) => {
                  const isCurrentAnalyzing = analyzingFileId === f.id || (isAnalyzing && f.status === 'ANALYZING');
                  return (
                    <tr key={f.id} className="hover:bg-slate-800/40 transition-colors">
                      <td className="py-3.5 px-4 font-mono font-medium text-white flex items-center gap-2">
                        {f.fileFormat === 'gz' ? (
                          <Archive className="w-4 h-4 text-amber-400 shrink-0" />
                        ) : (
                          <FileText className="w-4 h-4 text-blue-400 shrink-0" />
                        )}
                        <span className="truncate max-w-xs">{f.originalFilename}</span>
                      </td>
                      <td className="py-3.5 px-4">
                        <span className="rounded bg-slate-800 px-2 py-0.5 font-mono text-[11px] text-slate-300 uppercase">
                          .{f.fileFormat}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 font-mono text-slate-300">
                        {formatBytes(f.fileSize)}
                      </td>
                      <td className="py-3.5 px-4">
                        <span
                          className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                            f.status === 'ANALYZED'
                              ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                              : f.status === 'FAILED'
                              ? 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                              : f.status === 'ANALYZING' || f.status === 'PARSING'
                              ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20 animate-pulse'
                              : 'bg-slate-800 text-slate-300'
                          }`}
                        >
                          {f.status === 'ANALYZED' && <CheckCircle2 className="w-3 h-3" />}
                          {f.status === 'FAILED' && <AlertCircle className="w-3 h-3" />}
                          {f.status}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-slate-400">
                        {formatDate(f.uploadTimestamp)}
                      </td>
                      <td className="py-3.5 px-4 font-mono text-[11px] text-slate-500">
                        {f.checksum ? `${f.checksum.substring(0, 14)}...` : 'N/A'}
                      </td>
                      <td className="py-3.5 px-4 text-right space-x-2">
                        <button
                          onClick={() => handleAnalyze(f.id)}
                          disabled={isCurrentAnalyzing}
                          className="inline-flex items-center gap-1.5 rounded-lg bg-blue-600/20 px-3 py-1.5 text-xs font-semibold text-blue-400 border border-blue-500/30 hover:bg-blue-600 hover:text-white transition-all disabled:opacity-50"
                        >
                          {isCurrentAnalyzing ? (
                            <>
                              <Loader2 className="w-3 h-3 animate-spin" />
                              Analyzing...
                            </>
                          ) : (
                            <>
                              <Play className="w-3 h-3" />
                              {f.status === 'ANALYZED' ? 'Re-Analyze' : 'Analyze'}
                            </>
                          )}
                        </button>
                        <button
                          onClick={() => deleteFile(f.id)}
                          className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-800 hover:text-rose-400 transition-colors"
                          title="Delete File"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-500">
                    No log files uploaded yet. Drag and drop a file above to begin.
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
