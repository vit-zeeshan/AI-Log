import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { authService } from '../services/authService';
import {
  Activity,
  Lock,
  User,
  ArrowRight,
  AlertCircle,
  Loader2,
  ShieldCheck,
} from 'lucide-react';

export const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('password123');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const { login } = useAuthStore();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrorMessage(null);

    try {
      const res = await authService.login(username, password);
      login(res.token, res.user);
      navigate('/');
    } catch (err: any) {
      setErrorMessage(
        err.response?.data?.message || 'Invalid username or password. Please try again.'
      );
    } finally {
      setIsLoading(false);
    }
  };

  const quickFill = (u: string, p: string) => {
    setUsername(u);
    setPassword(p);
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-950 p-4">
      <div className="w-full max-w-md space-y-6">
        {/* Brand Banner */}
        <div className="text-center space-y-2">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white shadow-xl shadow-blue-500/20">
            <Activity className="h-7 w-7" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-white">LogSense AI</h1>
          <p className="text-xs text-slate-400">
            Senior SRE & AI-Powered Log Intelligence Platform
          </p>
        </div>

        {/* Login Box */}
        <div className="rounded-2xl border border-slate-800 bg-slate-900/90 p-8 shadow-2xl backdrop-blur-sm space-y-6">
          <div className="space-y-1">
            <h2 className="text-lg font-bold text-white">Sign In to Workspace</h2>
            <p className="text-xs text-slate-400">
              Access your diagnostic reports and live telemetry analyzers.
            </p>
          </div>

          {errorMessage && (
            <div className="flex items-center gap-2 rounded-xl bg-rose-500/10 p-3 text-xs text-rose-400 border border-rose-500/20">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{errorMessage}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-300">Username or Email</label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                <input
                  type="text"
                  required
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="w-full rounded-xl bg-slate-950 border border-slate-800 pl-10 pr-4 py-2.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 font-mono"
                  placeholder="admin or developer"
                />
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-300">Password</label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full rounded-xl bg-slate-950 border border-slate-800 pl-10 pr-4 py-2.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 font-mono"
                  placeholder="••••••••"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full flex items-center justify-center gap-2 rounded-xl bg-blue-600 py-2.5 text-xs font-semibold text-white shadow-lg shadow-blue-500/20 hover:bg-blue-500 transition-colors disabled:opacity-50"
            >
              {isLoading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Authenticating...
                </>
              ) : (
                <>
                  <span>Sign In</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Quick Credential Chips */}
          <div className="pt-2 border-t border-slate-800/80 space-y-2">
            <p className="text-[11px] font-semibold text-slate-400">Quick Test Credentials:</p>
            <div className="grid grid-cols-2 gap-2 text-[11px]">
              <button
                type="button"
                onClick={() => quickFill('admin', 'password123')}
                className="rounded-lg bg-slate-950 p-2 text-left border border-slate-800 hover:border-slate-700 hover:text-white text-slate-400 transition-colors"
              >
                <div className="font-semibold text-blue-400">Admin Account</div>
                <div className="font-mono text-[10px] text-slate-500">admin / password123</div>
              </button>
              <button
                type="button"
                onClick={() => quickFill('developer', 'devpass123')}
                className="rounded-lg bg-slate-950 p-2 text-left border border-slate-800 hover:border-slate-700 hover:text-white text-slate-400 transition-colors"
              >
                <div className="font-semibold text-emerald-400">Developer Account</div>
                <div className="font-mono text-[10px] text-slate-500">developer / devpass123</div>
              </button>
            </div>
          </div>

          <div className="text-center text-xs text-slate-400 pt-2">
            Don't have an account?{' '}
            <Link to="/register" className="font-semibold text-blue-400 hover:underline">
              Create an account
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
