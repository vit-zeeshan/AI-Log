import api from './api';
import { LogFile } from '../types';

export const logService = {
  uploadFile: async (file: File, onProgress?: (percent: number) => void): Promise<LogFile> => {
    const formData = new FormData();
    formData.append('file', file);

    const res = await api.post('/logs/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total && onProgress) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress(percent);
        }
      },
    });
    return res.data.data;
  },

  getFiles: async (): Promise<LogFile[]> => {
    const res = await api.get('/logs');
    return res.data.data;
  },

  getFile: async (id: number): Promise<LogFile> => {
    const res = await api.get(`/logs/${id}`);
    return res.data.data;
  },

  deleteFile: async (id: number): Promise<void> => {
    await api.delete(`/logs/${id}`);
  },
};
