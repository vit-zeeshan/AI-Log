import React, { useState, useRef } from 'react';
import { logService } from '../services/logService';
import { formatBytes } from '../utils/formatters';
import { LogFile } from '../types';
import {
  UploadCloud,
  FileText,
  AlertTriangle,
  CheckCircle2,
  X,
  Play,
  Loader2,
  Archive,
} from 'lucide-react';

interface Props {
  onFileUploaded: (file: LogFile) => void;
  onAnalyzeTriggered?: (fileId: number) => void;
}

const SUPPORTED_EXTENSIONS = ['.log', '.txt', '.json', '.csv', '.out', '.gz'];
const MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

export const FileUploadDropzone: React.FC<Props> = ({ onFileUploaded, onAnalyzeTriggered }) => {
  const [isDragging, setIsDragging] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [uploadedLogFile, setUploadedLogFile] = useState<LogFile | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const validateFile = (file: File): boolean => {
    const name = file.name.toLowerCase();
    const isSupported = SUPPORTED_EXTENSIONS.some((ext) => name.endsWith(ext));

    if (!isSupported) {
      setErrorMessage(`Unsupported file format. Please upload one of: ${SUPPORTED_EXTENSIONS.join(', ')}`);
      return false;
    }

    if (file.size > MAX_FILE_SIZE) {
      setErrorMessage(`File exceeds 100MB limit (${formatBytes(file.size)}). Please compress with .gz or upload a smaller slice.`);
      return false;
    }

    if (file.size === 0) {
      setErrorMessage('Selected file is empty or corrupted (0 bytes).');
      return false;
    }

    setErrorMessage(null);
    return true;
  };

  const handleFile = (file: File) => {
    if (validateFile(file)) {
      setSelectedFile(file);
      setUploadedLogFile(null);
      setUploadProgress(null);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFile(e.dataTransfer.files[0]);
    }
  };

  const startUpload = async () => {
    if (!selectedFile) return;

    setIsUploading(true);
    setUploadProgress(0);
    setErrorMessage(null);

    try {
      const uploaded = await logService.uploadFile(selectedFile, (percent) => {
        setUploadProgress(percent);
      });
      setUploadedLogFile(uploaded);
      onFileUploaded(uploaded);
      setIsUploading(false);
    } catch (err: any) {
      setIsUploading(false);
      setUploadProgress(null);
      setErrorMessage(err.response?.data?.message || 'Failed to upload log file. Please check backend connection.');
    }
  };

  const removeFile = () => {
    setSelectedFile(null);
    setUploadedLogFile(null);
    setUploadProgress(null);
    setErrorMessage(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div className="w-full space-y-4">
      {/* Dropzone Box */}
      <div
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() => !selectedFile && fileInputRef.current?.click()}
        className={`relative flex flex-col items-center justify-center rounded-2xl border-2 border-dashed p-10 text-center transition-all duration-200 cursor-pointer ${
          isDragging
            ? 'border-blue-500 bg-blue-500/10 scale-[1.01]'
            : 'border-slate-700/80 bg-slate-900/60 hover:border-slate-600 hover:bg-slate-900'
        } ${selectedFile ? 'cursor-default' : ''}`}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept=".log,.txt,.json,.csv,.out,.gz"
          className="hidden"
          onChange={(e) => e.target.files?.[0] && handleFile(e.target.files[0])}
        />

        {!selectedFile ? (
          <div className="space-y-4">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-blue-500/10 text-blue-400 border border-blue-500/20 shadow-inner">
              <UploadCloud className="h-8 w-8" />
            </div>
            <div>
              <p className="text-base font-semibold text-white">
                Drag and drop your application log file here
              </p>
              <p className="mt-1 text-xs text-slate-400">
                or click to browse your computer
              </p>
            </div>
            {/* Format tags */}
            <div className="flex flex-wrap items-center justify-center gap-1.5 pt-2">
              {SUPPORTED_EXTENSIONS.map((ext) => (
                <span
                  key={ext}
                  className="rounded-md bg-slate-800 px-2 py-0.5 font-mono text-xs text-slate-300 border border-slate-700/60"
                >
                  {ext}
                </span>
              ))}
              <span className="text-xs text-slate-500 ml-2">Max 100MB</span>
            </div>
          </div>
        ) : (
          <div className="w-full max-w-lg space-y-4 text-left">
            <div className="flex items-center justify-between rounded-xl bg-slate-800/90 p-4 border border-slate-700">
              <div className="flex items-center gap-3 overflow-hidden">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-blue-500/20 text-blue-400">
                  {selectedFile.name.endsWith('.gz') ? <Archive className="w-5 h-5" /> : <FileText className="w-5 h-5" />}
                </div>
                <div className="overflow-hidden">
                  <p className="truncate text-sm font-medium text-white">{selectedFile.name}</p>
                  <p className="text-xs text-slate-400">{formatBytes(selectedFile.size)}</p>
                </div>
              </div>
              {!isUploading && !uploadedLogFile && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    removeFile();
                  }}
                  className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-700 hover:text-white transition-colors"
                >
                  <X className="w-4 h-4" />
                </button>
              )}
            </div>

            {/* Upload Progress Bar */}
            {uploadProgress !== null && (
              <div className="space-y-1.5">
                <div className="flex justify-between text-xs text-slate-400">
                  <span>{uploadProgress < 100 ? 'Uploading to secure enclave...' : 'File stored & verified'}</span>
                  <span className="font-mono">{uploadProgress}%</span>
                </div>
                <div className="h-2 w-full overflow-hidden rounded-full bg-slate-800">
                  <div
                    className="h-full bg-gradient-to-r from-blue-500 to-indigo-500 transition-all duration-300"
                    style={{ width: `${uploadProgress}%` }}
                  />
                </div>
              </div>
            )}

            {/* Action Buttons */}
            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={removeFile}
                disabled={isUploading}
                className="rounded-lg px-4 py-2 text-xs font-semibold text-slate-300 hover:bg-slate-800 transition-colors"
              >
                Choose Another File
              </button>

              {!uploadedLogFile ? (
                <button
                  type="button"
                  onClick={startUpload}
                  disabled={isUploading}
                  className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-5 py-2 text-xs font-semibold text-white shadow-lg hover:bg-blue-500 transition-colors disabled:opacity-50"
                >
                  {isUploading ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      Uploading...
                    </>
                  ) : (
                    <>
                      <UploadCloud className="w-4 h-4" />
                      Upload File
                    </>
                  )}
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => onAnalyzeTriggered && onAnalyzeTriggered(uploadedLogFile.id)}
                  className="inline-flex items-center gap-2 rounded-lg bg-emerald-600 px-5 py-2 text-xs font-semibold text-white shadow-lg hover:bg-emerald-500 transition-colors animate-pulse"
                >
                  <Play className="w-4 h-4" />
                  Trigger AI Analysis Now
                </button>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Error Alert */}
      {errorMessage && (
        <div className="flex items-center gap-2 rounded-xl bg-rose-500/10 p-3.5 text-xs text-rose-400 border border-rose-500/20">
          <AlertTriangle className="h-4 w-4 shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Success notification */}
      {uploadedLogFile && (
        <div className="flex items-center justify-between rounded-xl bg-emerald-500/10 p-3.5 text-xs text-emerald-400 border border-emerald-500/20">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4 shrink-0" />
            <span>File uploaded successfully! SHA-256: <code className="font-mono">{uploadedLogFile.checksum?.substring(0, 16)}...</code></span>
          </div>
          <span className="font-semibold">Ready for diagnosis</span>
        </div>
      )}
    </div>
  );
};
