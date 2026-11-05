import React, { useEffect, useState } from 'react';
import api from '../api/client';
import {
  TrendingUp,
  CheckCircle2,
  RotateCcw,
  Percent,
  RefreshCw,
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from 'recharts';
import type { DashboardAnalytics, ApiResponse } from '../types';

export const Dashboard: React.FC = () => {
  const [data, setData] = useState<DashboardAnalytics | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchAnalytics = async () => {
    try {
      setLoading(true);
      const res = await api.get<ApiResponse<DashboardAnalytics>>('/analytics/dashboard');
      if (res.data.success) {
        setData(res.data.data);
      }
    } catch (err) {
      console.error('Failed to load dashboard data', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAnalytics();
  }, []);

  const formatCurrency = (minor: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(minor / 100);
  };

  if (loading && !data) {
    return (
      <div className="flex h-64 items-center justify-center text-slate-500 font-mono text-xs">
        LOADING ANALYTICS ENGINE...
      </div>
    );
  }

  const chartData = (data?.volumeByDay || []).map((d) => ({
    date: d.date,
    amount: d.volumeMinor / 100,
    count: d.count,
  }));

  return (
    <div className="space-y-8">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Merchant Overview</h1>
          <p className="text-xs text-slate-400 mt-1">Real-time payment settlements, volume, and operational metrics</p>
        </div>
        <button
          onClick={fetchAnalytics}
          className="flex items-center gap-2 px-3 py-1.5 text-xs font-medium text-slate-300 bg-slate-900 border border-slate-800 rounded-lg hover:bg-slate-800 transition-colors"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          Refresh
        </button>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total Captured Volume */}
        <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-5 backdrop-blur">
          <div className="flex items-center justify-between text-slate-400 mb-3">
            <span className="text-xs font-semibold uppercase tracking-wider">Settled Volume</span>
            <TrendingUp className="w-4 h-4 text-blue-400" />
          </div>
          <div className="text-2xl font-bold text-white tracking-tight">
            {formatCurrency(data?.totalVolumeMinor || 0)}
          </div>
          <p className="text-[11px] text-slate-500 mt-2">Gross processed payment volume</p>
        </div>

        {/* Successful Transactions */}
        <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-5 backdrop-blur">
          <div className="flex items-center justify-between text-slate-400 mb-3">
            <span className="text-xs font-semibold uppercase tracking-wider">Captured Charges</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-bold text-white tracking-tight">
            {data?.capturedCount || 0}
          </div>
          <p className="text-[11px] text-slate-500 mt-2">Completed and settled orders</p>
        </div>

        {/* Success Rate */}
        <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-5 backdrop-blur">
          <div className="flex items-center justify-between text-slate-400 mb-3">
            <span className="text-xs font-semibold uppercase tracking-wider">Success Rate</span>
            <Percent className="w-4 h-4 text-indigo-400" />
          </div>
          <div className="text-2xl font-bold text-white tracking-tight">
            {data?.successRate || 0}%
          </div>
          <p className="text-[11px] text-slate-500 mt-2">Conversion across attempts</p>
        </div>

        {/* Total Refunds */}
        <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-5 backdrop-blur">
          <div className="flex items-center justify-between text-slate-400 mb-3">
            <span className="text-xs font-semibold uppercase tracking-wider">Total Refunded</span>
            <RotateCcw className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-2xl font-bold text-white tracking-tight">
            {formatCurrency(data?.refundedVolumeMinor || 0)}
          </div>
          <p className="text-[11px] text-slate-500 mt-2">{data?.totalRefundCount || 0} refunds executed</p>
        </div>
      </div>

      {/* Volume Chart */}
      <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6 backdrop-blur">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-300 mb-6">
          Volume Trend (Simulated)
        </h2>
        <div className="h-72 w-full">
          {chartData.length > 0 ? (
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="volumeGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#2563eb" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#2563eb" stopOpacity={0.0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" vertical={false} />
                <XAxis dataKey="date" stroke="#64748b" fontSize={11} tickLine={false} />
                <YAxis stroke="#64748b" fontSize={11} tickLine={false} tickFormatter={(v) => `₹${v}`} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#0f172a',
                    borderColor: '#1e293b',
                    borderRadius: '8px',
                    fontSize: '12px',
                  }}
                  formatter={(value: any) => [`₹${value}`, 'Settled Volume']}
                />
                <Area
                  type="monotone"
                  dataKey="amount"
                  stroke="#3b82f6"
                  strokeWidth={2}
                  fillOpacity={1}
                  fill="url(#volumeGrad)"
                />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-full flex items-center justify-center text-slate-600 text-xs font-mono">
              NO TRANSACTION VOLUME RECORDED YET
            </div>
          )}
        </div>
      </div>

      {/* System Invariant Notes */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="p-4 rounded-xl border border-blue-900/30 bg-blue-950/20 text-xs text-slate-400 space-y-1">
          <p className="font-semibold text-blue-400">Double-Entry Ledger Invariant</p>
          <p>Every transaction debit balance rigorously matches credit entries. Check General Ledger for audit trails.</p>
        </div>
        <div className="p-4 rounded-xl border border-indigo-900/30 bg-indigo-950/20 text-xs text-slate-400 space-y-1">
          <p className="font-semibold text-indigo-400">Idempotency Guarantee</p>
          <p>All network payment retries pass strict Idempotency-Key validation to ensure single execution.</p>
        </div>
      </div>
    </div>
  );
};
