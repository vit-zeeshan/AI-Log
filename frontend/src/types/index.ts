export type Severity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';

export type ErrorCategory =
  | 'APPLICATION_ERRORS'
  | 'JAVA_EXCEPTIONS'
  | 'DATABASE_ERRORS'
  | 'API_ERRORS'
  | 'NETWORK_ERRORS'
  | 'AUTHENTICATION_ERRORS'
  | 'MEMORY_ISSUES'
  | 'CPU_PERFORMANCE_ISSUES'
  | 'TIMEOUT_ERRORS'
  | 'CONFIGURATION_ERRORS'
  | 'INFRASTRUCTURE_ERRORS'
  | 'DEPLOYMENT_ERRORS'
  | 'DEPENDENCY_FAILURES'
  | 'SECURITY_RELATED_EVENTS';

export type FileStatus = 'UPLOADED' | 'PARSING' | 'PARSED' | 'ANALYZING' | 'ANALYZED' | 'FAILED';

export type AnalysisStatus = 'PENDING' | 'PARSING' | 'ANALYZING' | 'COMPLETED' | 'FAILED';

export interface User {
  id: number;
  username: string;
  email: string;
  role: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface LogFile {
  id: number;
  filename: string;
  originalFilename: string;
  fileSize: number;
  fileFormat: string;
  status: FileStatus;
  checksum?: string;
  uploadTimestamp: string;
  latestAnalysisId?: number;
}

export interface RootCauseReport {
  id: number;
  parsedErrorId: number;
  whatWentWrong: string;
  whyItHappened: string;
  affectedComponent: string;
  impactAssessment: string;
  primaryOrSecondary: 'PRIMARY' | 'SECONDARY';
  supportingEvidence: string;
  immediateFix: string;
  stepByStepDebugging: string;
  suggestedCodeChange?: string;
  troubleshootingCommands?: string;
  longTermPrevention: string;
  createdAt: string;
}

export interface ParsedError {
  id: number;
  analysisId: number;
  title: string;
  message: string;
  severity: Severity;
  category: ErrorCategory;
  categoryDisplayName: string;
  errorTimestamp: string;
  fileName?: string;
  lineNumber?: number;
  stackTrace?: string;
  serviceName: string;
  occurrenceCount: number;
  firstOccurrence: string;
  lastOccurrence: string;
  fingerprint: string;
  isPrimary: boolean;
  supportingEvidence?: string;
  rawSnippet?: string;
  rootCauseReport?: RootCauseReport;
}

export interface LogAnalysis {
  id: number;
  logFileId: number;
  filename: string;
  totalLines: number;
  totalErrors: number;
  totalWarnings: number;
  criticalErrors: number;
  uniquePatterns: number;
  durationMs: number;
  status: AnalysisStatus;
  summary: string;
  createdAt: string;
  topErrors?: ParsedError[];
}

export interface TimeSeriesPoint {
  timestamp: string;
  errorCount: number;
  warningCount: number;
}

export interface DashboardSummary {
  totalLogFiles: number;
  totalAnalyses: number;
  totalLogEntries: number;
  totalErrors: number;
  totalWarnings: number;
  criticalErrors: number;
  uniqueErrorPatterns: number;
  avgAnalysisDurationMs: number;
  errorDistributionBySeverity: Record<string, number>;
  errorDistributionByCategory: Record<string, number>;
  serviceWiseDistribution: Record<string, number>;
  topRecurringErrors: ParsedError[];
  errorFrequencyOverTime: TimeSeriesPoint[];
  recentAnalyses: LogAnalysis[];
}

export interface ErrorDiffItem {
  fingerprint: string;
  title: string;
  severity: Severity;
  category: string;
  baseCount: number;
  targetCount: number;
  countDelta: number;
  sampleMessage: string;
  serviceName: string;
  status: 'NEW' | 'RESOLVED' | 'INCREASED' | 'DECREASED' | 'UNCHANGED';
}

export interface CompareResponse {
  baseAnalysisId: number;
  baseFilename: string;
  targetAnalysisId: number;
  targetFilename: string;
  baseTotalErrors: number;
  targetTotalErrors: number;
  errorCountDifference: number;
  percentageChange: number;
  newErrors: ErrorDiffItem[];
  resolvedErrors: ErrorDiffItem[];
  recurringPatternChanges: ErrorDiffItem[];
  comparisonSummary: string;
}

export interface ReferenceItem {
  title: string;
  timestamp: string;
  lineReference: string;
  service: string;
  snippet: string;
}

export interface ChatMessage {
  id: number;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  references?: ReferenceItem[];
  timestamp: string;
}
