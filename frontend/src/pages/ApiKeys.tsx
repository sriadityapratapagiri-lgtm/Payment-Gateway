import React, { useState } from 'react';
import { Copy, Check, ShieldAlert } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const ApiKeys: React.FC = () => {
  const { merchant } = useAuth();
  const [copied, setCopied] = useState(false);

  // Simulated API Key derived from merchant session
  const simulatedApiKey = `pk_test_${(merchant?.id || 'demo').replace(/-/g, '').slice(0, 24)}`;

  const copyToClipboard = () => {
    navigator.clipboard.writeText(simulatedApiKey);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-white">API Credentials</h1>
        <p className="text-xs text-slate-400 mt-1">
          Merchant publishable authentication keys for integrating PayFlow REST endpoints
        </p>
      </div>

      <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6 backdrop-blur space-y-6">
        <div>
          <h2 className="text-sm font-semibold text-slate-200 uppercase tracking-wider mb-2">
            Publishable API Key (Test Mode)
          </h2>
          <p className="text-xs text-slate-400 mb-4">
            Use this key in Authorization headers or SDK configurations to originate transactions.
          </p>

          <div className="flex items-center gap-2">
            <div className="flex-1 bg-slate-950 border border-slate-800 rounded-lg px-4 py-2.5 font-mono text-xs text-blue-400 select-all">
              {simulatedApiKey}
            </div>
            <button
              onClick={copyToClipboard}
              className="px-3.5 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg text-xs font-medium flex items-center gap-1.5 transition-colors border border-slate-700"
            >
              {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
              {copied ? 'Copied' : 'Copy'}
            </button>
          </div>
        </div>

        <div className="border-t border-slate-800 pt-6">
          <div className="p-4 rounded-xl border border-amber-900/30 bg-amber-950/20 text-xs text-amber-300/80 flex items-start gap-3">
            <ShieldAlert className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
            <div className="space-y-1">
              <p className="font-semibold text-amber-300">Sandbox Environment Notice</p>
              <p>
                This gateway operates purely in simulation mode. No live banking networks, card brands,
                or PCI vaults are touched. Do not submit production payment credentials.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
