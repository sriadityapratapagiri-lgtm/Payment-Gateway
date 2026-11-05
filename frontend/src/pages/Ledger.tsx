import React, { useEffect, useState } from 'react';
import api from '../api/client';
import { CheckCircle, RefreshCw, AlertTriangle } from 'lucide-react';
import type { JournalEntry, ApiResponse } from '../types';

export const Ledger: React.FC = () => {
  const [entries, setEntries] = useState<JournalEntry[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchEntries = async () => {
    try {
      setLoading(true);
      const res = await api.get<ApiResponse<JournalEntry[]>>('/ledger/entries');
      if (res.data.success) {
        setEntries(res.data.data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEntries();
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">General Ledger</h1>
          <p className="text-xs text-slate-400 mt-1">
            Immutable double-entry journal entries verifying mathematical balance: ∑ Debits = ∑ Credits
          </p>
        </div>
        <button
          onClick={fetchEntries}
          className="flex items-center gap-2 px-3 py-1.5 text-xs font-medium text-slate-300 bg-slate-900 border border-slate-800 rounded-lg hover:bg-slate-800 transition-colors"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          Refresh Ledger
        </button>
      </div>

      {loading ? (
        <div className="p-12 text-center text-slate-500 font-mono text-xs">
          LOADING JOURNAL AUDIT TRAILS...
        </div>
      ) : entries.length === 0 ? (
        <div className="p-12 text-center text-slate-500 font-mono text-xs border border-dashed border-slate-800 rounded-xl bg-slate-900/20">
          NO POSTED JOURNAL ENTRIES IN LEDGER YET
        </div>
      ) : (
        <div className="space-y-4">
          {entries.map((entry) => (
            <div
              key={entry.id}
              className="bg-slate-900/60 border border-slate-800 rounded-xl overflow-hidden backdrop-blur"
            >
              {/* Header */}
              <div className="px-6 py-4 bg-slate-900/90 border-b border-slate-800 flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-3">
                  <span className="font-mono text-xs text-blue-400 font-bold">
                    [{entry.referenceType}]
                  </span>
                  <span className="text-xs text-slate-200 font-medium">{entry.description}</span>
                  <span className="font-mono text-[11px] text-slate-500">
                    Ref ID: {entry.referenceId}
                  </span>
                </div>
                <div className="flex items-center gap-4">
                  <div className="flex items-center gap-1.5 text-xs">
                    {entry.balanced ? (
                      <span className="flex items-center gap-1 text-emerald-400 font-mono text-[11px]">
                        <CheckCircle className="w-3.5 h-3.5" />
                        BALANCED (₹{(entry.totalDebits / 100).toFixed(2)})
                      </span>
                    ) : (
                      <span className="flex items-center gap-1 text-rose-400 font-mono text-[11px]">
                        <AlertTriangle className="w-3.5 h-3.5" />
                        IMBALANCED
                      </span>
                    )}
                  </div>
                  <span className="text-[11px] text-slate-500 font-mono">
                    {new Date(entry.createdAt).toLocaleString()}
                  </span>
                </div>
              </div>

              {/* Lines Table */}
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-950/40 text-slate-500 uppercase font-semibold text-[10px] tracking-wider border-b border-slate-800/60">
                  <tr>
                    <th className="px-6 py-2.5">Account Code</th>
                    <th className="px-6 py-2.5">Account Type</th>
                    <th className="px-6 py-2.5">Direction</th>
                    <th className="px-6 py-2.5 text-right">Debit (₹)</th>
                    <th className="px-6 py-2.5 text-right">Credit (₹)</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/40 text-slate-300 font-mono">
                  {entry.lines.map((line) => {
                    const isDebit = line.direction === 'DEBIT';
                    const amountFormatted = (line.amountMinor / 100).toFixed(2);
                    return (
                      <tr key={line.id} className="hover:bg-slate-800/20">
                        <td className="px-6 py-2.5 text-slate-200">{line.accountCode}</td>
                        <td className="px-6 py-2.5 text-slate-500 text-[11px]">{line.accountType}</td>
                        <td className="px-6 py-2.5">
                          <span
                            className={`px-1.5 py-0.5 rounded text-[10px] ${
                              isDebit
                                ? 'bg-blue-500/10 text-blue-400'
                                : 'bg-indigo-500/10 text-indigo-400'
                            }`}
                          >
                            {line.direction}
                          </span>
                        </td>
                        <td className="px-6 py-2.5 text-right text-slate-200">
                          {isDebit ? amountFormatted : '-'}
                        </td>
                        <td className="px-6 py-2.5 text-right text-slate-200">
                          {!isDebit ? amountFormatted : '-'}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
