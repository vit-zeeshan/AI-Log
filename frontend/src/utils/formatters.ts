import { Severity, ErrorCategory } from '../types';

export function formatBytes(bytes: number, decimals = 2): string {
  if (!bytes || bytes === 0) return '0 Bytes';
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

export function formatDuration(ms: number): string {
  if (!ms || ms === 0) return '0 ms';
  if (ms < 1000) return `${ms} ms`;
  return `${(ms / 1000).toFixed(2)} s`;
}

export function formatDate(dateString: string): string {
  if (!dateString) return 'N/A';
  try {
    const d = new Date(dateString);
    if (isNaN(d.getTime())) return dateString;
    return d.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  } catch {
    return dateString;
  }
}

export function getSeverityColor(severity: Severity) {
  switch (severity) {
    case 'CRITICAL':
      return {
        bg: 'bg-rose-500/10 text-rose-500 border-rose-500/20',
        badge: 'bg-rose-500 text-white',
        border: 'border-rose-500',
        fill: '#f43f5e',
      };
    case 'HIGH':
      return {
        bg: 'bg-amber-500/10 text-amber-500 border-amber-500/20',
        badge: 'bg-amber-500 text-white',
        border: 'border-amber-500',
        fill: '#f59e0b',
      };
    case 'MEDIUM':
      return {
        bg: 'bg-yellow-500/10 text-yellow-500 border-yellow-500/20',
        badge: 'bg-yellow-500 text-white',
        border: 'border-yellow-500',
        fill: '#eab308',
      };
    case 'LOW':
    default:
      return {
        bg: 'bg-blue-500/10 text-blue-500 border-blue-500/20',
        badge: 'bg-blue-500 text-white',
        border: 'border-blue-500',
        fill: '#3b82f6',
      };
  }
}

export function getCategoryLabel(category: ErrorCategory | string): string {
  const map: Record<string, string> = {
    APPLICATION_ERRORS: 'Application Error',
    JAVA_EXCEPTIONS: 'Java Exception',
    DATABASE_ERRORS: 'Database / HikariCP',
    API_ERRORS: 'API / HTTP 5xx',
    NETWORK_ERRORS: 'Network / Socket',
    AUTHENTICATION_ERRORS: 'Auth / Security',
    MEMORY_ISSUES: 'Memory / OOM',
    CPU_PERFORMANCE_ISSUES: 'CPU Throttling',
    TIMEOUT_ERRORS: 'Timeout Error',
    CONFIGURATION_ERRORS: 'Config / Missing Bean',
    INFRASTRUCTURE_ERRORS: 'Infrastructure / K8s',
    DEPLOYMENT_ERRORS: 'Deployment / ClassDef',
    DEPENDENCY_FAILURES: 'Dependency / Circuit Breaker',
    SECURITY_RELATED_EVENTS: 'Security Anomaly',
  };
  return map[category] || category;
}
