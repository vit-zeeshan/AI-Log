import React from 'react';

interface MetricCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  trend?: {
    value: string;
    isPositive?: boolean;
  };
  variant?: 'default' | 'danger' | 'warning' | 'info' | 'success';
}

export const MetricCard: React.FC<MetricCardProps> = ({
  title,
  value,
  subtitle,
  icon,
  trend,
  variant = 'default',
}) => {
  const getVariantStyles = () => {
    switch (variant) {
      case 'danger':
        return 'border-rose-200 dark:border-rose-500/30 bg-rose-50/50 dark:bg-gradient-to-br dark:from-rose-950/20 dark:to-slate-900';
      case 'warning':
        return 'border-amber-200 dark:border-amber-500/30 bg-amber-50/50 dark:bg-gradient-to-br dark:from-amber-950/20 dark:to-slate-900';
      case 'info':
        return 'border-blue-200 dark:border-blue-500/30 bg-blue-50/50 dark:bg-gradient-to-br dark:from-blue-950/20 dark:to-slate-900';
      case 'success':
        return 'border-emerald-200 dark:border-emerald-500/30 bg-emerald-50/50 dark:bg-gradient-to-br dark:from-emerald-950/20 dark:to-slate-900';
      default:
        return 'border-slate-200 dark:border-slate-800 bg-white/90 dark:bg-slate-900/90';
    }
  };

  return (
    <div
      className={`relative overflow-hidden rounded-xl border p-5 shadow-sm dark:shadow-lg backdrop-blur-sm transition-all duration-200 hover:border-slate-300 dark:hover:border-slate-700 hover:shadow-md dark:hover:shadow-xl ${getVariantStyles()}`}
    >
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
          {title}
        </span>
        <div className="rounded-lg bg-slate-100 dark:bg-slate-800/80 p-2 text-slate-700 dark:text-slate-300">
          {icon}
        </div>
      </div>

      <div className="mt-3 flex items-baseline gap-2">
        <span className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
          {value}
        </span>
        {trend && (
          <span
            className={`text-xs font-medium ${
              trend.isPositive ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'
            }`}
          >
            {trend.value}
          </span>
        )}
      </div>

      {subtitle && (
        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{subtitle}</p>
      )}
    </div>
  );
};
