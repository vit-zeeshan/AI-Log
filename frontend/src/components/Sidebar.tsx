import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  UploadCloud,
  History,
  SearchCode,
  GitCompare,
  BotMessageSquare,
  Settings,
  ShieldCheck,
} from 'lucide-react';

const NAV_ITEMS = [
  { path: '/', label: 'Dashboard', icon: LayoutDashboard },
  { path: '/upload', label: 'Upload Logs', icon: UploadCloud },
  { path: '/history', label: 'Analysis History', icon: History },
  { path: '/explorer', label: 'Error Explorer', icon: SearchCode },
  { path: '/compare', label: 'Compare Logs', icon: GitCompare },
  { path: '/chat', label: 'AI Assistant', icon: BotMessageSquare },
  { path: '/settings', label: 'Settings', icon: Settings },
];

export const Sidebar: React.FC = () => {
  return (
    <aside className="fixed inset-y-0 left-0 top-16 z-30 hidden w-64 border-r border-slate-200 dark:border-slate-800 bg-white/70 dark:bg-slate-950/70 p-4 backdrop-blur-md md:block transition-colors duration-200">
      <div className="flex h-full flex-col justify-between">
        {/* Navigation list */}
        <div className="space-y-1">
          <p className="px-3 pb-2 text-[10px] font-bold uppercase tracking-wider text-slate-400 dark:text-slate-500">
            Platform Navigation
          </p>
          {NAV_ITEMS.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  `flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-xs font-semibold transition-all ${
                    isActive
                      ? 'bg-blue-50 dark:bg-blue-600/10 text-blue-600 dark:text-blue-400 border border-blue-200 dark:border-blue-500/30 shadow-sm'
                      : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-900 hover:text-slate-900 dark:hover:text-slate-200'
                  }`
                }
              >
                <Icon className="h-4 w-4 shrink-0" />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </div>

        {/* Bottom SRE Security Badge */}
        <div className="rounded-xl border border-slate-200 dark:border-slate-800/80 bg-slate-100/60 dark:bg-slate-900/60 p-3.5 space-y-2">
          <div className="flex items-center gap-2 text-xs font-semibold text-slate-800 dark:text-slate-300">
            <ShieldCheck className="h-4 w-4 text-emerald-500 dark:text-emerald-400" />
            <span>Sensitive Data Masking</span>
          </div>
          <p className="text-[11px] text-slate-500 dark:text-slate-500 leading-relaxed">
            Bearer tokens, JWTs, credentials, and PII are masked automatically at log ingestion.
          </p>
        </div>
      </div>
    </aside>
  );
};
