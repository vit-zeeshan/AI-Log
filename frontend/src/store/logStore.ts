import { create } from 'zustand';
import { LogFile, LogAnalysis } from '../types';
import { logService } from '../services/logService';
import { analysisService } from '../services/analysisService';

interface LogState {
  files: LogFile[];
  selectedFile: LogFile | null;
  activeAnalysis: LogAnalysis | null;
  isLoadingFiles: boolean;
  isAnalyzing: boolean;
  fetchFiles: () => Promise<void>;
  setSelectedFile: (file: LogFile | null) => void;
  setActiveAnalysis: (analysis: LogAnalysis | null) => void;
  triggerAnalysis: (fileId: number) => Promise<LogAnalysis>;
  deleteFile: (fileId: number) => Promise<void>;
}

export const useLogStore = create<LogState>((set, get) => ({
  files: [],
  selectedFile: null,
  activeAnalysis: null,
  isLoadingFiles: false,
  isAnalyzing: false,

  fetchFiles: async () => {
    set({ isLoadingFiles: true });
    try {
      const files = await logService.getFiles();
      set({ files, isLoadingFiles: false });
      if (!get().selectedFile && files.length > 0) {
        set({ selectedFile: files[0] });
      }
    } catch {
      set({ isLoadingFiles: false });
    }
  },

  setSelectedFile: (file) => set({ selectedFile: file }),
  setActiveAnalysis: (analysis) => set({ activeAnalysis: analysis }),

  triggerAnalysis: async (fileId: number) => {
    set({ isAnalyzing: true });
    try {
      const analysis = await analysisService.triggerAnalysis(fileId);
      set({ activeAnalysis: analysis, isAnalyzing: false });
      get().fetchFiles();
      return analysis;
    } catch (e) {
      set({ isAnalyzing: false });
      throw e;
    }
  },

  deleteFile: async (fileId: number) => {
    await logService.deleteFile(fileId);
    set((state) => ({
      files: state.files.filter((f) => f.id !== fileId),
      selectedFile: state.selectedFile?.id === fileId ? null : state.selectedFile,
      activeAnalysis: state.activeAnalysis?.logFileId === fileId ? null : state.activeAnalysis,
    }));
  },
}));
