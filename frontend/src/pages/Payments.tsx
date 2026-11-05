import React, { useEffect, useState } from 'react';
import api from '../api/client';
import { X, AlertCircle, Play } from 'lucide-react';
import type { Payment, Order, ApiResponse } from '../types';

export const Payments: React.FC = () => {
  const [payments, setPayments] = useState<Payment[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);

  // Simulation Form State
  const [selectedOrderId, setSelectedOrderId] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('SIMULATED_CARD');
  const [simulationResult, setSimulationResult] = useState<'SUCCESS' | 'FAILURE' | 'PENDING'>('SUCCESS');
  const [idempotencyKey, setIdempotencyKey] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const fetchData = async () => {
    try {
      setLoading(true);
      const [paymentsRes, ordersRes] = await Promise.all([
        api.get<ApiResponse<Payment[]>>('/payments'),
        api.get<ApiResponse<Order[]>>('/orders'),
      ]);
      if (paymentsRes.data.success) setPayments(paymentsRes.data.data);
      if (ordersRes.data.success) setOrders(ordersRes.data.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const openSimulateModal = () => {
    // Generate fresh idempotency key
    setIdempotencyKey(`idem-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`);
    // Pre-select first eligible order
    const eligible = orders.find((o) => o.status === 'CREATED');
    if (eligible) setSelectedOrderId(eligible.id);
    setError('');
    setShowModal(true);
  };

  const handleSimulatePayment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedOrderId) {
      setError('Please select an order to pay');
      return;
    }
    setError('');
    setSubmitting(true);

    try {
      const headers: Record<string, string> = {};
      if (idempotencyKey.trim()) {
        headers['Idempotency-Key'] = idempotencyKey.trim();
      }

      const res = await api.post<ApiResponse<Payment>>(
        '/payments',
        {
          orderId: selectedOrderId,
          paymentMethod,
          simulationResult,
        },
        { headers }
      );

      if (res.data.success) {
        setShowModal(false);
        fetchData();
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Payment simulation failed');
    } finally {
      setSubmitting(false);
    }
  };

  const getStatusBadge = (status: Payment['status']) => {
    switch (status) {
      case 'CAPTURED':
        return <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">CAPTURED</span>;
      case 'PARTIALLY_REFUNDED':
        return <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-amber-500/10 text-amber-400 border border-amber-500/20">PARTIALLY REFUNDED</span>;
      case 'REFUNDED':
        return <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-purple-500/10 text-purple-400 border border-purple-500/20">REFUNDED</span>;
      case 'FAILED':
        return <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-rose-500/10 text-rose-400 border border-rose-500/20">FAILED</span>;
      case 'CANCELLED':
        return <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-slate-500/10 text-slate-400 border border-slate-500/20">CANCELLED</span>;
      default:
        return <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-blue-500/10 text-blue-400 border border-blue-500/20">{status}</span>;
    }
  };

  const eligibleOrders = orders.filter((o) => o.status === 'CREATED');

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Payments</h1>
          <p className="text-xs text-slate-400 mt-1">Processed transactions, authorization states, and sandbox simulation</p>
        </div>
        <button
          onClick={openSimulateModal}
          className="flex items-center gap-2 px-3.5 py-2 text-xs font-medium text-white bg-blue-600 hover:bg-blue-500 rounded-lg shadow-lg shadow-blue-600/20 transition-all"
        >
          <Play className="w-3.5 h-3.5 fill-current" />
          Simulate Payment
        </button>
      </div>

      {/* Payments Table */}
      <div className="bg-slate-900/50 border border-slate-800 rounded-xl overflow-hidden backdrop-blur">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-900 border-b border-slate-800 text-slate-400 uppercase font-semibold">
              <tr>
                <th className="px-6 py-3.5">Payment ID</th>
                <th className="px-6 py-3.5">Order ID</th>
                <th className="px-6 py-3.5">Amount</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5">Method</th>
                <th className="px-6 py-3.5">Provider Ref</th>
                <th className="px-6 py-3.5">Timestamp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800 text-slate-300">
              {loading ? (
                <tr>
                  <td colSpan={7} className="px-6 py-8 text-center text-slate-500 font-mono">
                    FETCHING TRANSACTIONS...
                  </td>
                </tr>
              ) : payments.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-6 py-8 text-center text-slate-500 font-mono">
                    NO PAYMENTS PROCESSED YET
                  </td>
                </tr>
              ) : (
                payments.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="px-6 py-4 font-mono text-[11px] font-medium text-slate-200">{p.id}</td>
                    <td className="px-6 py-4 font-mono text-[11px] text-slate-500">{p.orderId}</td>
                    <td className="px-6 py-4 font-semibold text-slate-100">
                      ₹{(p.amountMinor / 100).toFixed(2)} <span className="text-[10px] text-slate-500">{p.currency}</span>
                    </td>
                    <td className="px-6 py-4">{getStatusBadge(p.status)}</td>
                    <td className="px-6 py-4 font-mono text-[11px] text-slate-400">{p.paymentMethod}</td>
                    <td className="px-6 py-4 font-mono text-[11px] text-slate-500">
                      {p.providerReference || '-'}
                    </td>
                    <td className="px-6 py-4 text-[11px] text-slate-500">
                      {new Date(p.createdAt).toLocaleString()}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Simulate Payment Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 w-full max-w-lg shadow-2xl">
            <div className="flex items-center justify-between mb-4 border-b border-slate-800 pb-3">
              <div className="flex items-center gap-2">
                <Play className="w-4 h-4 text-blue-400" />
                <h2 className="text-sm font-semibold text-white uppercase tracking-wider">
                  Payment Processing Simulator
                </h2>
              </div>
              <button
                onClick={() => setShowModal(false)}
                className="text-slate-400 hover:text-slate-200 transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {error && (
              <div className="mb-4 p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleSimulatePayment} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Target Order</label>
                {eligibleOrders.length === 0 ? (
                  <div className="p-3 bg-slate-950 border border-slate-800 rounded-lg text-xs text-amber-400 font-mono">
                    No orders in CREATED state. Please create a new order first!
                  </div>
                ) : (
                  <select
                    value={selectedOrderId}
                    onChange={(e) => setSelectedOrderId(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-100 focus:border-blue-500 outline-none"
                  >
                    {eligibleOrders.map((o) => (
                      <option key={o.id} value={o.id}>
                        {o.orderReference} - ₹{(o.amountMinor / 100).toFixed(2)} ({o.currency})
                      </option>
                    ))}
                  </select>
                )}
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-400 mb-1">Payment Method</label>
                  <select
                    value={paymentMethod}
                    onChange={(e) => setPaymentMethod(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-100 focus:border-blue-500 outline-none"
                  >
                    <option value="SIMULATED_CARD">Simulated Card</option>
                    <option value="SIMULATED_UPI">Simulated UPI</option>
                    <option value="SIMULATED_NETBANKING">Simulated NetBanking</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-400 mb-1">Simulation Outcome</label>
                  <select
                    value={simulationResult}
                    onChange={(e) => setSimulationResult(e.target.value as any)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-100 focus:border-blue-500 outline-none font-semibold text-emerald-400"
                  >
                    <option value="SUCCESS" className="text-emerald-400">SUCCESS (Capture & Ledger)</option>
                    <option value="FAILURE" className="text-rose-400">FAILURE (Decline)</option>
                    <option value="PENDING" className="text-amber-400">PENDING (Auth only)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">
                  Idempotency-Key Header
                </label>
                <input
                  type="text"
                  value={idempotencyKey}
                  onChange={(e) => setIdempotencyKey(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-100 font-mono focus:border-blue-500 outline-none"
                  placeholder="unique-request-uuid"
                />
                <p className="text-[10px] text-slate-500 mt-1">
                  Re-submitting with this identical key returns cached response without duplicate billing.
                </p>
              </div>

              <div className="pt-2 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-3.5 py-2 text-xs font-medium text-slate-400 hover:bg-slate-800 rounded-lg transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting || eligibleOrders.length === 0}
                  className="px-4 py-2 text-xs font-medium text-white bg-blue-600 hover:bg-blue-500 disabled:opacity-50 rounded-lg shadow-md transition-all flex items-center gap-1.5"
                >
                  {submitting ? 'Executing Flow...' : 'Execute Simulation'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
