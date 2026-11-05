import React, { useEffect, useState } from 'react';
import api from '../api/client';
import { Plus, X, AlertCircle } from 'lucide-react';
import type { Refund, Payment, ApiResponse } from '../types';

export const Refunds: React.FC = () => {
  const [refunds, setRefunds] = useState<Refund[]>([]);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);

  // Form State
  const [selectedPaymentId, setSelectedPaymentId] = useState('');
  const [amountRupees, setAmountRupees] = useState('');
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [refundsRes, paymentsRes] = await Promise.all([
        api.get<ApiResponse<Refund[]>>('/refunds'),
        api.get<ApiResponse<Payment[]>>('/payments'),
      ]);
      if (refundsRes.data.success) setRefunds(refundsRes.data.data);
      if (paymentsRes.data.success) setPayments(paymentsRes.data.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const eligiblePayments = payments.filter((p) =>
    ['CAPTURED', 'PARTIALLY_REFUNDED'].includes(p.status)
  );

  const openRefundModal = () => {
    if (eligiblePayments.length > 0) {
      setSelectedPaymentId(eligiblePayments[0].id);
      setAmountRupees((eligiblePayments[0].amountMinor / 100).toString());
    }
    setError('');
    setShowModal(true);
  };

  const handleCreateRefund = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedPaymentId) {
      setError('Please select a payment');
      return;
    }
    setError('');
    setSubmitting(true);

    try {
      const amountMinor = Math.round(parseFloat(amountRupees) * 100);
      if (isNaN(amountMinor) || amountMinor <= 0) {
        throw new Error('Please enter a valid refund amount');
      }

      const res = await api.post<ApiResponse<Refund>>('/refunds', {
        paymentId: selectedPaymentId,
        amountMinor,
        reason,
      });

      if (res.data.success) {
        setShowModal(false);
        setReason('');
        fetchData();
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Refund processing failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Refunds</h1>
          <p className="text-xs text-slate-400 mt-1">
            Partial & full reversals with double-entry refund ledger posting
          </p>
        </div>
        <button
          onClick={openRefundModal}
          className="flex items-center gap-2 px-3.5 py-2 text-xs font-medium text-white bg-blue-600 hover:bg-blue-500 rounded-lg shadow-lg shadow-blue-600/20 transition-all"
        >
          <Plus className="w-3.5 h-3.5" />
          Initiate Refund
        </button>
      </div>

      {/* Refunds Table */}
      <div className="bg-slate-900/50 border border-slate-800 rounded-xl overflow-hidden backdrop-blur">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-900 border-b border-slate-800 text-slate-400 uppercase font-semibold">
              <tr>
                <th className="px-6 py-3.5">Refund ID</th>
                <th className="px-6 py-3.5">Payment ID</th>
                <th className="px-6 py-3.5">Amount</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5">Reason</th>
                <th className="px-6 py-3.5">Date</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800 text-slate-300">
              {loading ? (
                <tr>
                  <td colSpan={6} className="px-6 py-8 text-center text-slate-500 font-mono">
                    FETCHING REFUNDS...
                  </td>
                </tr>
              ) : refunds.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-8 text-center text-slate-500 font-mono">
                    NO REFUNDS RECORDED
                  </td>
                </tr>
              ) : (
                refunds.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="px-6 py-4 font-mono text-[11px] font-medium text-slate-200">{r.id}</td>
                    <td className="px-6 py-4 font-mono text-[11px] text-slate-500">{r.paymentId}</td>
                    <td className="px-6 py-4 font-semibold text-rose-400">
                      -₹{(r.amountMinor / 100).toFixed(2)} <span className="text-[10px] text-slate-500">{r.currency}</span>
                    </td>
                    <td className="px-6 py-4">
                      <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        {r.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-slate-400 max-w-xs truncate">{r.reason || '-'}</td>
                    <td className="px-6 py-4 text-[11px] text-slate-500">
                      {new Date(r.createdAt).toLocaleString()}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Refund Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 w-full max-w-md shadow-2xl">
            <div className="flex items-center justify-between mb-4 border-b border-slate-800 pb-3">
              <h2 className="text-sm font-semibold text-white uppercase tracking-wider">
                Initiate Refund
              </h2>
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

            <form onSubmit={handleCreateRefund} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Select Payment</label>
                {eligiblePayments.length === 0 ? (
                  <div className="p-3 bg-slate-950 border border-slate-800 rounded-lg text-xs text-amber-400 font-mono">
                    No captured or partially refunded payments available.
                  </div>
                ) : (
                  <select
                    value={selectedPaymentId}
                    onChange={(e) => setSelectedPaymentId(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-100 focus:border-blue-500 outline-none"
                  >
                    {eligiblePayments.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.id.substring(0, 8)}... - ₹{(p.amountMinor / 100).toFixed(2)} ({p.status})
                      </option>
                    ))}
                  </select>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">
                  Refund Amount (₹)
                </label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  required
                  value={amountRupees}
                  onChange={(e) => setAmountRupees(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-100 focus:border-blue-500 outline-none"
                  placeholder="500.00"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Reason</label>
                <textarea
                  rows={2}
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-100 focus:border-blue-500 outline-none"
                  placeholder="Customer requested cancellation"
                />
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
                  disabled={submitting || eligiblePayments.length === 0}
                  className="px-4 py-2 text-xs font-medium text-white bg-blue-600 hover:bg-blue-500 disabled:opacity-50 rounded-lg shadow-md transition-all"
                >
                  {submitting ? 'Posting Refund...' : 'Confirm Refund'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
