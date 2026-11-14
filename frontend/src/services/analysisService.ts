import api from './api';
import {
  LogAnalysis,
  ParsedError,
  Severity,
  ErrorCategory,
  CompareResponse,
  DashboardSummary,
} from '../types';

export const analysisService = {
  triggerAnalysis: async (logFileId: number): Promise<LogAnalysis> => {
    const res = await api.post(`/logs/${logFileId}/analyze`);
    return res.data.data;
  },

  getAnalysis: async (id: number): Promise<LogAnalysis> => {
    const res = await api.get(`/analysis/${id}`);
    return res.data.data;
  },

  getAnalysisHistory: async (): Promise<LogAnalysis[]> => {
    const res = await api.get('/analysis/history');
    return res.data.data;
  },

  getErrors: async (params: {
    analysisId: number;
    severity?: Severity;
    category?: ErrorCategory;
    serviceName?: string;
    query?: string;
    page?: number;
    size?: number;
  }): Promise<{ content: ParsedError[]; totalElements: number; totalPages: number }> => {
    const res = await api.get(`/analysis/${params.analysisId}/errors`, {
      params: {
        severity: params.severity,
        category: params.category,
        serviceName: params.serviceName,
        query: params.query,
        page: params.page ?? 0,
        size: params.size ?? 15,
      },
    });
    return res.data.data;
  },

  compareAnalyses: async (baseId: number, targetId: number): Promise<CompareResponse> => {
    const res = await api.post('/analysis/compare', {
      baseAnalysisId: baseId,
      targetAnalysisId: targetId,
    });
    return res.data.data;
  },

  exportReport: async (id: number): Promise<string> => {
    const res = await api.get(`/analysis/${id}/export`, {
      responseType: 'text',
    });
    return res.data;
  },

  getDashboardSummary: async (): Promise<DashboardSummary> => {
    const res = await api.get('/dashboard/summary');
    return res.data.data;
  },
};
