import React, { useState } from 'react';
import { useAuthStore } from '../store/authStore';
import {
  Settings as SettingsIcon,
  Cpu,
  Shield,
  Clock,
  Key,
  Save,
  CheckCircle2,
  HardDrive,
} from 'lucide-react';

export const SettingsPage: React.FC = () => {
  const { user } = useAuthStore();
  const [aiProvider, setAiProvider] = useState('heuristic');
  const [apiKey, setApiKey] = useState('');
  const [modelName, setModelName] = useState('gpt-4o-mini');
  const [maskPii, setMaskPii] = useState(true);
  const [maskJwt, setMaskJwt] = useState(true);
  const [retentionDays, setRetentionDays] = useState(30);
  const [isSaved, setIsSaved] = useState(false);

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaved(true);
    setTimeout(() => setIsSaved(false), 3000);
  };

  return (
    <div className="max-w-4xl space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
          <SettingsIcon className="w-6 h-6 text-blue-400" />
          System & AI Engine Configuration
        </h1>
        <p className="mt-1 text-xs text-slate-400">
          Manage AI inference providers, heuristic rule thresholds, data masking policies, and storage retention.
        </p>
      </div>

      <form onSubmit={handleSave} className="space-y-6">
        {/* AI Engine Settings */}
        <div className="rounded-2xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl backdrop-blur-sm space-y-4">
          <div className="flex items-center gap-2 text-sm font-semibold text-white">
            <Cpu className="w-4 h-4 text-blue-400" />
            <span>AI Reasoning & LLM Inference Provider</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <label
              onClick={() => setAiProvider('heuristic')}
              className={`flex flex-col justify-between rounded-xl border p-4 cursor-pointer transition-all ${
                aiProvider === 'heuristic'
                  ? 'border-blue-500 bg-blue-500/10'
                  : 'border-slate-800 bg-slate-950/60 hover:border-slate-700'
              }`}
            >
              <div>
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-white text-xs">
                    Heuristic Senior SRE Engine
                  </span>
                  <span className="rounded bg-emerald-500/20 px-2 py-0.5 text-[10px] text-emerald-300 font-bold">
                    RECOMMENDED
                  </span>
                </div>
                <p className="mt-2 text-xs text-slate-400 leading-relaxed">
                  High-speed rule reasoning designed for Java 21, Spring Boot, HikariCP, and Kubernetes incidents. 100% offline, zero latency, zero external API keys required.
                </p>
              </div>
            </label>

            <label
              onClick={() => setAiProvider('openai')}
              className={`flex flex-col justify-between rounded-xl border p-4 cursor-pointer transition-all ${
                aiProvider === 'openai'
                  ? 'border-blue-500 bg-blue-500/10'
                  : 'border-slate-800 bg-slate-950/60 hover:border-slate-700'
              }`}
            >
              <div>
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-white text-xs">
                    External LLM API (OpenAI / Gemini)
                  </span>
                  <span className="rounded bg-blue-500/20 px-2 py-0.5 text-[10px] text-blue-300 font-bold">
                    API KEY
                  </span>
                </div>
                <p className="mt-2 text-xs text-slate-400 leading-relaxed">
                  Delegates error diagnosis to external LLM with structured JSON parsing. Falls back to Heuristic Engine if offline.
                </p>
              </div>
            </label>
          </div>

          {aiProvider === 'openai' && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-300">API Key</label>
                <div className="relative">
                  <Key className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-500" />
                  <input
                    type="password"
                    placeholder="sk-..."
                    value={apiKey}
                    onChange={(e) => setApiKey(e.target.value)}
                    className="w-full rounded-lg bg-slate-950 border border-slate-800 pl-9 pr-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 font-mono"
                  />
                </div>
              </div>

              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-300">Model Name</label>
                <input
                  type="text"
                  value={modelName}
                  onChange={(e) => setModelName(e.target.value)}
                  className="w-full rounded-lg bg-slate-950 border border-slate-800 px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 font-mono"
                />
              </div>
            </div>
          )}
        </div>

        {/* Security & Masking */}
        <div className="rounded-2xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl backdrop-blur-sm space-y-4">
          <div className="flex items-center gap-2 text-sm font-semibold text-white">
            <Shield className="w-4 h-4 text-emerald-400" />
            <span>Security & Sensitive Telemetry Masking</span>
          </div>

          <div className="space-y-3">
            <label className="flex items-center justify-between rounded-xl bg-slate-950/60 p-3.5 border border-slate-800 cursor-pointer">
              <div>
                <p className="text-xs font-semibold text-white">Mask Bearer & JWT Auth Tokens</p>
                <p className="text-[11px] text-slate-400">Replaces Authorization headers and signatures with [MASKED_JWT_TOKEN]</p>
              </div>
              <input
                type="checkbox"
                checked={maskJwt}
                onChange={(e) => setMaskJwt(e.target.checked)}
                className="rounded bg-slate-900 border-slate-700 text-blue-600 focus:ring-0 h-4 w-4"
              />
            </label>

            <label className="flex items-center justify-between rounded-xl bg-slate-950/60 p-3.5 border border-slate-800 cursor-pointer">
              <div>
                <p className="text-xs font-semibold text-white">Mask Credentials, PII & Credit Cards</p>
                <p className="text-[11px] text-slate-400">Masks password=..., credit card numbers, and email identities</p>
              </div>
              <input
                type="checkbox"
                checked={maskPii}
                onChange={(e) => setMaskPii(e.target.checked)}
                className="rounded bg-slate-900 border-slate-700 text-blue-600 focus:ring-0 h-4 w-4"
              />
            </label>
          </div>
        </div>

        {/* Storage Retention */}
        <div className="rounded-2xl border border-slate-800 bg-slate-900/80 p-6 shadow-xl backdrop-blur-sm space-y-4">
          <div className="flex items-center gap-2 text-sm font-semibold text-white">
            <HardDrive className="w-4 h-4 text-purple-400" />
            <span>Storage & Log Retention</span>
          </div>

          <div className="flex items-center justify-between rounded-xl bg-slate-950/60 p-3.5 border border-slate-800">
            <div>
              <p className="text-xs font-semibold text-white">Audit Retention Period</p>
              <p className="text-[11px] text-slate-400">Days to retain raw log dumps before automatic purge</p>
            </div>
            <select
              value={retentionDays}
              onChange={(e) => setRetentionDays(Number(e.target.value))}
              className="rounded-lg bg-slate-900 border border-slate-800 px-3 py-1.5 text-xs text-white focus:outline-none"
            >
              <option value={7}>7 Days</option>
              <option value={30}>30 Days</option>
              <option value={90}>90 Days</option>
              <option value={365}>1 Year</option>
            </select>
          </div>
        </div>

        {/* Submit */}
        <div className="flex items-center justify-between">
          {isSaved && (
            <div className="flex items-center gap-2 text-xs font-semibold text-emerald-400">
              <CheckCircle2 className="w-4 h-4" />
              Settings saved successfully!
            </div>
          )}
          <button
            type="submit"
            className="ml-auto inline-flex items-center gap-2 rounded-xl bg-blue-600 px-6 py-2.5 text-xs font-semibold text-white shadow-lg shadow-blue-500/20 hover:bg-blue-500 transition-colors"
          >
            <Save className="w-4 h-4" />
            Save Configurations
          </button>
        </div>
      </form>
    </div>
  );
};
