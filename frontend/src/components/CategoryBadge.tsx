import React from 'react';
import { ErrorCategory } from '../types';
import { getCategoryLabel } from '../utils/formatters';
import {
  Database,
  Cpu,
  ShieldAlert,
  Server,
  AlertOctagon,
  Clock,
  Layers,
  Settings,
  AlertTriangle,
  Network,
  Bug,
  FileCode,
} from 'lucide-react';

interface Props {
  category: ErrorCategory | string;
  className?: string;
}

export const CategoryBadge: React.FC<Props> = ({ category, className = '' }) => {
  const getIcon = () => {
    switch (category) {
      case 'DATABASE_ERRORS':
        return <Database className="w-3.5 h-3.5 text-emerald-400" />;
      case 'MEMORY_ISSUES':
      case 'CPU_PERFORMANCE_ISSUES':
        return <Cpu className="w-3.5 h-3.5 text-purple-400" />;
      case 'AUTHENTICATION_ERRORS':
      case 'SECURITY_RELATED_EVENTS':
        return <ShieldAlert className="w-3.5 h-3.5 text-rose-400" />;
      case 'API_ERRORS':
        return <Server className="w-3.5 h-3.5 text-blue-400" />;
      case 'NETWORK_ERRORS':
        return <Network className="w-3.5 h-3.5 text-cyan-400" />;
      case 'TIMEOUT_ERRORS':
        return <Clock className="w-3.5 h-3.5 text-amber-400" />;
      case 'CONFIGURATION_ERRORS':
        return <Settings className="w-3.5 h-3.5 text-slate-400" />;
      case 'INFRASTRUCTURE_ERRORS':
      case 'DEPLOYMENT_ERRORS':
        return <AlertOctagon className="w-3.5 h-3.5 text-orange-400" />;
      case 'DEPENDENCY_FAILURES':
        return <Layers className="w-3.5 h-3.5 text-indigo-400" />;
      case 'JAVA_EXCEPTIONS':
        return <FileCode className="w-3.5 h-3.5 text-red-400" />;
      default:
        return <Bug className="w-3.5 h-3.5 text-blue-400" />;
    }
  };

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md text-xs font-medium bg-slate-100 dark:bg-slate-800/80 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700/60 ${className}`}
    >
      {getIcon()}
      {getCategoryLabel(category)}
    </span>
  );
};
