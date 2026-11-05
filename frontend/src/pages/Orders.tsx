import React, { useEffect, useState } from 'react';
import api from '../api/client';
import { Plus, X, AlertCircle } from 'lucide-react';
import type { Order, ApiResponse } from '../types';

export const Orders: React.FC = () => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);

  // Form State
  const [orderReference, setOrderReference] = useState('');
  const [amountRupees, setAmountRupees] = useState('');
  const [currency, setCurrency] = useState('INR');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchOrders = async () => {
    try {
      setLoading(true);
      const res = await api.get<ApiResponse<Order[]>>('/orders');
      if (res.data.success) {
        setOrders(res.data.data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, []);

  const handleCreateOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);

    try {
      const amountMinor = Math.round(parseFloat(amountRupees) * 100);
      if (isNaN(amountMinor) || amountMinor <= 0) {
        throw new Error('Please enter a valid positive amount');
      }

      const res = await api.post<ApiResponse<Order>>('/orders', {
        orderReference,
        amountMinor,
        currency,
        description,
      });

      if (res.data.success) {
        setShowModal(false);
        setOrderReference('');
        setAmountRupees('');
        setDescription('');
        fetchOrders();
      }
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to create order');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancelOrder = async (id: string) => {
    if (!confirm('Are you sure you want to cancel this order?')) return;
    try {
      await api.post(`/orders/${id}/cancel`);
      fetchOrders();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to cancel order');
    }
  };

  const getStatusBadge = (status: Order['status']) => {
    switch (status) {
      case 'COMPLETED':
        return <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">COMPLETED</span>;
      case 'PROCESSING':
        return <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-blue-500/10 text-blue-400 border border-blue-500/20">PROCESSING</span>;
      case 'CANCELLED':
        return <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-slate-500/10 text-slate-400 border border-slate-500/20">CANCELLED</span>;
      case 'FAILED':
        return <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-rose-500/10 text-rose-400 border border-rose-500/20">FAILED</span>;
      default:
        return <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-amber-500/10 text-amber-400 border border-amber-500/20">CREATED</span>;
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Orders</h1>
          <p className="text-xs text-slate-400 mt-1">Manage payment intents and customer orders</p>
        </div>
        <button
          onClick={() => {
            setOrderReference(`ORD-${Math.floor(1000 + Math.random() * 9000)}`);
            setShowModal(true);
          }}
          className="flex items-center gap-2 px-3.5 py-2 text-xs font-medium text-white bg-blue-600 hover:bg-blue-500 rounded-lg shadow-lg shadow-blue-600/20 transition-all"
        >
          <Plus className="w-3.5 h-3.5" />
          Create Order
        </button>
      </div>

      {/* Orders Table */}
      <div className="bg-slate-900/50 border border-slate-800 rounded-xl overflow-hidden backdrop-blur">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-900 border-b border-slate-800 text-slate-400 uppercase font-semibold">
              <tr>
                <th className="px-6 py-3.5">Order Ref</th>
                <th className="px-6 py-3.5">Order ID</th>
                <th className="px-6 py-3.5">Amount</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5">Description</th>
                <th className="px-6 py-3.5">Date</th>
                <th className="px-6 py-3.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800 text-slate-300">
              {loading ? (
                <tr>
                  <td colSpan={7} className="px-6 py-8 text-center text-slate-500 font-mono">
                    FETCHING ORDERS...
                  </td>
                </tr>
              ) : orders.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-6 py-8 text-center text-slate-500 font-mono">
                    NO ORDERS CREATED YET
                  </td>
                </tr>
              ) : (
                orders.map((o) => (
                  <tr key={o.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="px-6 py-4 font-mono font-medium text-slate-200">{o.orderReference}</td>
                    <td className="px-6 py-4 font-mono text-[11px] text-slate-500">{o.id}</td>
                    <td className="px-6 py-4 font-semibold text-slate-100">
                      ₹{(o.amountMinor / 100).toFixed(2)} <span className="text-[10px] text-slate-500">{o.currency}</span>
                    </td>
                    <td className="px-6 py-4">{getStatusBadge(o.status)}</td>
                    <td className="px-6 py-4 text-slate-400 max-w-xs truncate">{o.description || '-'}</td>
                    <td className="px-6 py-4 text-[11px] text-slate-500">
                      {new Date(o.createdAt).toLocaleString()}
                    </td>
                    <td className="px-6 py-4 text-right">
                      {o.status === 'CREATED' && (
                        <button
                          onClick={() => handleCancelOrder(o.id)}
                          className="text-[11px] font-medium text-rose-400 hover:text-rose-300 transition-colors"
                        >
                          Cancel
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create Order Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 w-full max-w-md shadow-2xl">
            <div className="flex items-center justify-between mb-4 border-b border-slate-800 pb-3">
              <h2 className="text-sm font-semibold text-white uppercase tracking-wider">New Order</h2>
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

            <form onSubmit={handleCreateOrder} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Order Reference</label>
                <input
                  type="text"
                  required
                  value={orderReference}
                  onChange={(e) => setOrderReference(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-100 font-mono focus:border-blue-500 outline-none"
                  placeholder="ORD-1001"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-400 mb-1">Amount (₹)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="1"
                    required
                    value={amountRupees}
                    onChange={(e) => setAmountRupees(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-100 focus:border-blue-500 outline-none"
                    placeholder="1000.00"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-400 mb-1">Currency</label>
                  <select
                    value={currency}
                    onChange={(e) => setCurrency(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-100 focus:border-blue-500 outline-none"
                  >
                    <option value="INR">INR (₹)</option>
                    <option value="USD">USD ($)</option>
                    <option value="EUR">EUR (€)</option>
                    <option value="GBP">GBP (£)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Description</label>
                <textarea
                  rows={2}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-100 focus:border-blue-500 outline-none"
                  placeholder="E.g. SaaS Monthly Subscription"
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
                  disabled={submitting}
                  className="px-4 py-2 text-xs font-medium text-white bg-blue-600 hover:bg-blue-500 disabled:opacity-50 rounded-lg shadow-md transition-all"
                >
                  {submitting ? 'Creating...' : 'Create Order'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
