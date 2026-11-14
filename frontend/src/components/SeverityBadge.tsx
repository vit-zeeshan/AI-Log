import React from 'react';
import { Severity } from '../types';
import { getSeverityColor } from '../utils/formatters';

interface Props {
  severity: Severity;
  className?: string;
  size?: 'sm' | 'md';
}

export const SeverityBadge: React.FC<Props> = ({ severity, className = '', size = 'md' }) => {
  const colors = getSeverityColor(severity);
  const sizeClasses = size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs font-semibold';

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border ${colors.bg} ${colors.border} ${sizeClasses} ${className}`}
    >
      <span className={`w-1.5 h-1.5 rounded-full ${colors.badge} animate-pulse`} />
      {severity}
    </span>
  );
};
