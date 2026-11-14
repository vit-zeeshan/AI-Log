import React, { useState } from 'react';
import { ParsedError } from '../types';
import { SeverityBadge } from './SeverityBadge';
import { CategoryBadge } from './CategoryBadge';
import {
  X,
  Copy,
  Check,
  AlertCircle,
  HelpCircle,
  Wrench,
  Terminal,
  ShieldCheck,
  Code2,
  GitCommit,
  ExternalLink,
} from 'lucide-react';

interface Props {
  error: ParsedError | null;
  onClose: () => void;
}

export const ErrorDetailModal: React.FC<Props> = ({ error, onClose }) => {
  const [copiedCode, setCopiedCode] = useState(false);
  const [copiedCommands, setCopiedCommands] = useState(false);

  if (!error) return null;

  const report = error.rootCauseReport;

  const copyToClipboard = (text: string, type: 'code' | 'commands') => {
    navigator.clipboard.writeText(text);
    if (type === 'code') {
      setCopiedCode(true);
      setTimeout(() => setCopiedCode(false), 2000);
    } else {
      setCopiedCommands(true);
      setTimeout(() => setCopiedCommands(false), 2000);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/75 p-4 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="relative my-8 w-full max-w-4xl overflow-hidden rounded-2xl border border-slate-800 bg-slate-900 shadow-2xl">
        {/* Header */}
        <div className="flex items-start justify-between border-b border-slate-800 bg-slate-950/60 p-6">
          <div className="space-y-2 pr-6">
            <div className="flex flex-wrap items-center gap-2">
              <SeverityBadge severity={error.severity} />
              <CategoryBadge category={error.category} />
              {error.isPrimary ? (
                <span className="inline-flex items-center gap-1 rounded-md bg-rose-500/10 px-2.5 py-0.5 text-xs font-semibold text-rose-400 border border-rose-500/30">
                  <span className="w-1.5 h-1.5 rounded-full bg-rose-500 animate-ping" />
                  PRIMARY ROOT CAUSE
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 rounded-md bg-slate-800 px-2 py-0.5 text-xs text-slate-400 border border-slate-700">
                  SECONDARY / CASCADE
                </span>
              )}
              <span className="rounded bg-slate-800 px-2 py-0.5 text-xs font-mono text-slate-300">
                {error.serviceName}
              </span>
            </div>
            <h2 className="text-xl font-bold tracking-tight text-white font-mono">
              {error.title}
            </h2>
            <p className="text-sm text-slate-400 font-mono break-all line-clamp-2">
              {error.message}
            </p>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-2 text-slate-400 hover:bg-slate-800 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="max-h-[75vh] overflow-y-auto p-6 space-y-6">
          {/* Metadata Grid */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 rounded-xl bg-slate-950/40 p-4 border border-slate-800/80 text-xs">
            <div>
              <span className="text-slate-500 uppercase tracking-wider font-semibold">Total Occurrences</span>
              <p className="mt-1 font-bold text-white text-base">{error.occurrenceCount}x</p>
            </div>
            <div>
              <span className="text-slate-500 uppercase tracking-wider font-semibold">Source Location</span>
              <p className="mt-1 font-mono text-slate-300 truncate">
                {error.fileName ? `${error.fileName}:${error.lineNumber ?? 0}` : 'Unknown Line'}
              </p>
            </div>
            <div>
              <span className="text-slate-500 uppercase tracking-wider font-semibold">First Occurrence</span>
              <p className="mt-1 font-mono text-slate-300 truncate">{error.firstOccurrence}</p>
            </div>
            <div>
              <span className="text-slate-500 uppercase tracking-wider font-semibold">Last Occurrence</span>
              <p className="mt-1 font-mono text-slate-300 truncate">{error.lastOccurrence}</p>
            </div>
          </div>

          {report && (
            <>
              {/* Root Cause Section */}
              <div className="rounded-xl border border-slate-800 bg-slate-950/30 p-5 space-y-4">
                <div className="flex items-center gap-2 text-blue-400 font-semibold text-sm uppercase tracking-wider">
                  <AlertCircle className="w-4 h-4" />
                  AI Root Cause Diagnosis
                </div>

                <div className="grid md:grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <span className="text-xs font-semibold text-slate-400">What Exactly Went Wrong?</span>
                    <p className="text-sm text-slate-200 leading-relaxed bg-slate-900/60 p-3 rounded-lg border border-slate-800/60">
                      {report.whatWentWrong}
                    </p>
                  </div>
                  <div className="space-y-1">
                    <span className="text-xs font-semibold text-slate-400">Why Did It Happen?</span>
                    <p className="text-sm text-slate-200 leading-relaxed bg-slate-900/60 p-3 rounded-lg border border-slate-800/60">
                      {report.whyItHappened}
                    </p>
                  </div>
                </div>

                <div className="grid md:grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <span className="text-xs font-semibold text-slate-400">Affected Component</span>
                    <p className="text-sm text-amber-400 font-mono bg-slate-900/60 p-3 rounded-lg border border-slate-800/60">
                      {report.affectedComponent}
                    </p>
                  </div>
                  <div className="space-y-1">
                    <span className="text-xs font-semibold text-slate-400">Application Impact</span>
                    <p className="text-sm text-rose-300 leading-relaxed bg-slate-900/60 p-3 rounded-lg border border-slate-800/60">
                      {report.impactAssessment}
                    </p>
                  </div>
                </div>

                <div className="space-y-1">
                  <span className="text-xs font-semibold text-slate-400">Supporting Evidence</span>
                  <p className="text-xs text-slate-300 font-mono bg-slate-900/80 p-3 rounded-lg border border-slate-800/60">
                    {report.supportingEvidence}
                  </p>
                </div>
              </div>

              {/* Recommended Solution Section */}
              <div className="rounded-xl border border-blue-500/20 bg-blue-950/10 p-5 space-y-4">
                <div className="flex items-center gap-2 text-emerald-400 font-semibold text-sm uppercase tracking-wider">
                  <Wrench className="w-4 h-4" />
                  Recommended Resolution & Engineering Fixes
                </div>

                <div className="space-y-1">
                  <span className="text-xs font-semibold text-slate-400">Immediate Remediation Action</span>
                  <div className="p-3.5 rounded-lg bg-emerald-950/20 border border-emerald-500/30 text-sm text-emerald-200 font-medium">
                    {report.immediateFix}
                  </div>
                </div>

                <div className="space-y-1">
                  <span className="text-xs font-semibold text-slate-400">Step-by-Step Debugging Protocol</span>
                  <pre className="p-3.5 rounded-lg bg-slate-950 text-xs font-mono text-slate-300 border border-slate-800 overflow-x-auto whitespace-pre-wrap leading-relaxed">
                    {report.stepByStepDebugging}
                  </pre>
                </div>

                {report.suggestedCodeChange && (
                  <div className="space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold text-slate-400 flex items-center gap-1.5">
                        <Code2 className="w-3.5 h-3.5 text-blue-400" />
                        Suggested Java Code or Configuration Change
                      </span>
                      <button
                        onClick={() => copyToClipboard(report.suggestedCodeChange!, 'code')}
                        className="flex items-center gap-1 text-xs text-blue-400 hover:text-blue-300 transition-colors"
                      >
                        {copiedCode ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                        {copiedCode ? 'Copied' : 'Copy Snippet'}
                      </button>
                    </div>
                    <pre className="p-4 rounded-lg bg-slate-950 text-xs font-mono text-emerald-400 border border-slate-800 overflow-x-auto whitespace-pre-wrap">
                      {report.suggestedCodeChange}
                    </pre>
                  </div>
                )}

                {report.troubleshootingCommands && (
                  <div className="space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold text-slate-400 flex items-center gap-1.5">
                        <Terminal className="w-3.5 h-3.5 text-amber-400" />
                        Troubleshooting Commands
                      </span>
                      <button
                        onClick={() => copyToClipboard(report.troubleshootingCommands!, 'commands')}
                        className="flex items-center gap-1 text-xs text-amber-400 hover:text-amber-300 transition-colors"
                      >
                        {copiedCommands ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                        {copiedCommands ? 'Copied' : 'Copy Commands'}
                      </button>
                    </div>
                    <pre className="p-3.5 rounded-lg bg-slate-950 text-xs font-mono text-amber-300 border border-slate-800 overflow-x-auto whitespace-pre-wrap">
                      {report.troubleshootingCommands}
                    </pre>
                  </div>
                )}

                <div className="space-y-1">
                  <span className="text-xs font-semibold text-slate-400 flex items-center gap-1.5">
                    <ShieldCheck className="w-3.5 h-3.5 text-indigo-400" />
                    Long-Term Preventive Measures
                  </span>
                  <p className="text-sm text-slate-300 bg-slate-950/60 p-3.5 rounded-lg border border-slate-800 leading-relaxed">
                    {report.longTermPrevention}
                  </p>
                </div>
              </div>
            </>
          )}

          {/* Stack Trace Accordion */}
          {error.stackTrace && (
            <div className="space-y-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                Captured Stack Trace
              </span>
              <pre className="max-h-60 overflow-y-auto rounded-lg bg-slate-950 p-4 font-mono text-xs text-rose-300/90 border border-slate-800/80 whitespace-pre leading-relaxed">
                {error.stackTrace}
              </pre>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between border-t border-slate-800 bg-slate-950/80 p-4 px-6">
          <span className="text-xs font-mono text-slate-500">
            Fingerprint: {error.fingerprint}
          </span>
          <button
            onClick={onClose}
            className="rounded-lg bg-slate-800 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 transition-colors"
          >
            Close Investigation
          </button>
        </div>
      </div>
    </div>
  );
};
