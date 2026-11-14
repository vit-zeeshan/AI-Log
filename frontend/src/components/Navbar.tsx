import React from 'react';
import { useAuthStore } from '../store/authStore';
import { useThemeStore } from '../store/themeStore';
import {
  Sun,
  Moon,
  LogOut,
  User as UserIcon,
  Activity,
  FileCode2,
} from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, logout } = useAuthStore();
  const { isDark, toggleTheme } = useThemeStore();

  return (
    <header className="sticky top-0 z-40 flex h-16 w-full items-center justify-between border-b border-slate-800 bg-slate-950/80 px-6 backdrop-blur-md">
      {/* Brand */}
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white shadow-lg shadow-blue-500/20">
          <Activity className="h-5 w-5" />
        </div>
        <div>
          <span className="text-base font-bold tracking-tight text-white flex items-center gap-2">
            LogSense AI
            <span className="rounded-full bg-blue-500/10 px-2 py-0.5 text-[10px] font-semibold text-blue-400 border border-blue-500/30">
              SRE PLATFORM
            </span>
          </span>
          <span className="block text-[11px] text-slate-400 font-mono">
            Autonomous Incident Diagnostics
          </span>
        </div>
      </div>

      {/* Right Controls */}
      <div className="flex items-center gap-3">
        {/* Swagger link */}
        <a
          href="http://localhost:8080/swagger-ui.html"
          target="_blank"
          rel="noopener noreferrer"
          className="hidden md:flex items-center gap-1.5 rounded-lg border border-slate-800 bg-slate-900 px-3 py-1.5 text-xs font-medium text-slate-300 hover:border-slate-700 hover:text-white transition-colors"
        >
          <FileCode2 className="w-3.5 h-3.5 text-blue-400" />
          <span>Swagger API</span>
        </a>

        {/* Theme Toggle */}
        <button
          onClick={toggleTheme}
          className="rounded-lg border border-slate-800 bg-slate-900 p-2 text-slate-400 hover:border-slate-700 hover:text-white transition-colors"
          title={isDark ? 'Switch to Light Theme' : 'Switch to Dark Theme'}
        >
          {isDark ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4 text-blue-400" />}
        </button>

        {/* User Info & Logout */}
        {user && (
          <div className="flex items-center gap-3 pl-2 border-l border-slate-800">
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-800 text-slate-300 border border-slate-700">
                <UserIcon className="w-4 h-4" />
              </div>
              <div className="hidden sm:block text-left text-xs">
                <p className="font-semibold text-white">{user.username}</p>
                <p className="text-[10px] text-slate-400 font-mono">{user.role}</p>
              </div>
            </div>

            <button
              onClick={logout}
              title="Sign Out"
              className="rounded-lg p-2 text-slate-400 hover:bg-slate-800 hover:text-rose-400 transition-colors"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>
    </header>
  );
};
