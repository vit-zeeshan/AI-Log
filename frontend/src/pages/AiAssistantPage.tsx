import React, { useEffect, useState, useRef } from 'react';
import { useLogStore } from '../store/logStore';
import { aiService } from '../services/aiService';
import { ChatMessage, ReferenceItem } from '../types';
import {
  BotMessageSquare,
  Send,
  User,
  Sparkles,
  Loader2,
  FileText,
  Clock,
  Terminal,
  ShieldAlert,
  ArrowRight,
  Database,
  Layers,
} from 'lucide-react';

const SUGGESTED_QUESTIONS = [
  'Why did my application crash?',
  'What is the root cause of this exception?',
  'Why is my API returning HTTP 500?',
  'Is this a database connection issue?',
  'Which error should I fix first?',
  'What happened just before the application failed?',
  'How can I prevent this issue?',
];

export const AiAssistantPage: React.FC = () => {
  const { files, fetchFiles } = useLogStore();
  const [selectedFileId, setSelectedFileId] = useState<number | ''>('');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    fetchFiles();
  }, []);

  useEffect(() => {
    if (files.length > 0 && !selectedFileId) {
      setSelectedFileId(files[0].id);
    }
  }, [files]);

  useEffect(() => {
    scrollToBottom();
  }, [messages, isLoading]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const handleSend = async (textToSend?: string) => {
    const text = textToSend || inputMessage;
    if (!text.trim() || !selectedFileId || isLoading) return;

    const userMessage: ChatMessage = {
      id: Date.now(),
      role: 'USER',
      content: text,
      timestamp: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInputMessage('');
    setIsLoading(true);

    try {
      const response = await aiService.sendMessage(Number(selectedFileId), text);
      setMessages((prev) => [...prev, response]);
    } catch (e: any) {
      const errorMsg: ChatMessage = {
        id: Date.now() + 1,
        role: 'ASSISTANT',
        content: `Error: Unable to analyze this question. ${
          e.response?.data?.message || 'Please ensure the log has been parsed and analyzed first.'
        }`,
        timestamp: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex h-[calc(100vh-7rem)] flex-col space-y-4">
      {/* Header bar */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between rounded-2xl border border-slate-800 bg-slate-900/80 p-4 shadow-xl backdrop-blur-sm gap-3">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white shadow-lg">
            <BotMessageSquare className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-base font-bold text-white flex items-center gap-2">
              Autonomous SRE AI Chat Assistant
              <span className="rounded-full bg-emerald-500/10 px-2 py-0.5 text-[10px] font-semibold text-emerald-400 border border-emerald-500/30">
                ACTIVE
              </span>
            </h1>
            <p className="text-xs text-slate-400">
              Interactive root cause reasoning based on real application stack traces and timestamps.
            </p>
          </div>
        </div>

        {/* Target log selector */}
        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-400 whitespace-nowrap">Context Log:</span>
          <select
            value={selectedFileId}
            onChange={(e) => {
              setSelectedFileId(Number(e.target.value));
              setMessages([]);
            }}
            className="rounded-lg bg-slate-950 border border-slate-800 px-3 py-1.5 text-xs text-white focus:outline-none focus:border-blue-500 font-mono"
          >
            {files.map((f) => (
              <option key={f.id} value={f.id}>
                {f.originalFilename}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Chat Messages Enclave */}
      <div className="flex-1 overflow-y-auto rounded-2xl border border-slate-800 bg-slate-900/50 p-6 shadow-xl backdrop-blur-sm space-y-4">
        {messages.length === 0 ? (
          <div className="flex h-full flex-col items-center justify-center text-center max-w-xl mx-auto space-y-6">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-500/10 text-blue-400 border border-blue-500/20 shadow-inner">
              <Sparkles className="h-7 w-7" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-white">Ask anything about this incident</h2>
              <p className="mt-1 text-xs text-slate-400 leading-relaxed">
                LogSense AI indexes all parsed exception chains, thread names, and database queries.
                Select a suggested question below or type your inquiry.
              </p>
            </div>

            {/* Suggested Pills */}
            <div className="flex flex-wrap justify-center gap-2 pt-2">
              {SUGGESTED_QUESTIONS.map((q, idx) => (
                <button
                  key={idx}
                  onClick={() => handleSend(q)}
                  className="rounded-xl border border-slate-800 bg-slate-900/80 px-3 py-2 text-xs font-medium text-slate-300 hover:border-blue-500/50 hover:bg-slate-800 hover:text-white transition-all text-left flex items-center gap-2 group"
                >
                  <span>{q}</span>
                  <ArrowRight className="w-3 h-3 text-slate-500 group-hover:text-blue-400 transition-colors" />
                </button>
              ))}
            </div>
          </div>
        ) : (
          messages.map((msg) => (
            <div
              key={msg.id}
              className={`flex gap-3 ${
                msg.role === 'USER' ? 'justify-end' : 'justify-start'
              }`}
            >
              {msg.role !== 'USER' && (
                <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-blue-600/20 text-blue-400 border border-blue-500/30">
                  <BotMessageSquare className="w-4 h-4" />
                </div>
              )}

              <div
                className={`max-w-2xl rounded-2xl p-4 text-xs leading-relaxed ${
                  msg.role === 'USER'
                    ? 'bg-blue-600 text-white rounded-tr-none'
                    : 'bg-slate-950 text-slate-200 border border-slate-800 rounded-tl-none space-y-3'
                }`}
              >
                <div className="whitespace-pre-wrap">{msg.content}</div>

                {/* References Box */}
                {msg.references && msg.references.length > 0 && (
                  <div className="mt-3 pt-3 border-t border-slate-800/80 space-y-2">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1">
                      <Terminal className="w-3 h-3 text-blue-400" />
                      Supporting Log Telemetry References
                    </span>
                    <div className="space-y-1.5">
                      {msg.references.map((ref, idx) => (
                        <div
                          key={idx}
                          className="rounded-lg bg-slate-900/90 p-2.5 border border-slate-800/80 space-y-1 font-mono"
                        >
                          <div className="flex items-center justify-between text-[11px] text-blue-300 font-semibold">
                            <span>{ref.title}</span>
                            <span className="text-slate-500">{ref.timestamp}</span>
                          </div>
                          <div className="text-[10px] text-slate-400 flex items-center gap-2">
                            <span>Service: <strong className="text-slate-300">{ref.service}</strong></span>
                            <span>•</span>
                            <span>Location: <strong className="text-slate-300">{ref.lineReference}</strong></span>
                          </div>
                          <p className="text-[11px] text-slate-300 truncate pt-0.5">
                            {ref.snippet}
                          </p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {msg.role === 'USER' && (
                <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-slate-800 text-slate-300 border border-slate-700">
                  <User className="w-4 h-4" />
                </div>
              )}
            </div>
          ))
        )}

        {isLoading && (
          <div className="flex items-center gap-3">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-600/20 text-blue-400 border border-blue-500/30">
              <BotMessageSquare className="w-4 h-4" />
            </div>
            <div className="flex items-center gap-2 rounded-2xl rounded-tl-none bg-slate-950 p-4 text-xs text-slate-400 border border-slate-800">
              <Loader2 className="w-4 h-4 animate-spin text-blue-400" />
              <span>Analyzing stack trace chains and evidence...</span>
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input Form */}
      <form
        onSubmit={(e) => {
          e.preventDefault();
          handleSend();
        }}
        className="flex items-center gap-3 rounded-2xl border border-slate-800 bg-slate-900/90 p-3 shadow-xl backdrop-blur-sm"
      >
        <input
          type="text"
          placeholder="Ask a technical question regarding this log incident..."
          value={inputMessage}
          onChange={(e) => setInputMessage(e.target.value)}
          disabled={!selectedFileId || isLoading}
          className="flex-1 bg-transparent px-3 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none font-mono"
        />
        <button
          type="submit"
          disabled={!inputMessage.trim() || !selectedFileId || isLoading}
          className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-600 text-white shadow-lg hover:bg-blue-500 transition-colors disabled:opacity-40"
        >
          <Send className="w-4 h-4" />
        </button>
      </form>
    </div>
  );
};
