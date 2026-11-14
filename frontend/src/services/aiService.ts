import api from './api';
import { ChatMessage } from '../types';

export const aiService = {
  sendMessage: async (logFileId: number, message: string, analysisId?: number): Promise<ChatMessage> => {
    const res = await api.post('/ai/chat', {
      logFileId,
      analysisId,
      message,
    });
    return res.data.data;
  },
};
