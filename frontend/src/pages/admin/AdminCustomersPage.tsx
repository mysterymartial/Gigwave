import { useState } from 'react';
import { useAllCustomers, useDebitCustomer } from '../../hooks/useAdmin';
import { useAuth } from '../../hooks/useAuth';
import { UserRole } from '../../types';
import { format } from 'date-fns';

export default function AdminCustomersPage() {
  const { user } = useAuth();
  const { data: customers, isLoading } = useAllCustomers();
  const debitCustomer = useDebitCustomer();

  const [selectedCustomerId, setSelectedCustomerId] = useState<string | null>(null);
  const [amount, setAmount] = useState('');
  const [reason, setReason] = useState('');

  // Redirect if not admin
  if (user?.role !== UserRole.ADMIN) {
    return (
      <div className="min-h-screen bg-gray-900 flex items-center justify-center">
        <div className="bg-red-500/20 border border-red-500/50 rounded-lg p-6">
          <p className="text-red-200">Access denied. Admin privileges required.</p>
        </div>
      </div>
    );
  }

  const handleDebit = async (customerId: string) => {
    if (!amount || !reason || parseFloat(amount) <= 0) {
      alert('Please enter a valid amount and reason');
      return;
    }

    try {
      await debitCustomer.mutateAsync({
        customerId,
        data: {
          amount: parseFloat(amount),
          reason,
        },
      });
      alert('Debit initiated successfully');
      setSelectedCustomerId(null);
      setAmount('');
      setReason('');
    } catch (error: any) {
      if (error?.response?.data?.message) {
        alert(error.response.data.message);
      } else {
        alert('Failed to debit customer');
      }
    }
  };

  if (isLoading) {
    return <div className="text-center py-8">Loading customers...</div>;
  }

  return (
    <div className="max-w-7xl mx-auto p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Customer Management</h1>
        <p className="text-gray-600 dark:text-gray-400 mt-2">Manage customers and process admin debits</p>
      </div>

      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
            <thead className="bg-emerald-800/60">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-emerald-100 uppercase tracking-wider">
                  Customer
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-emerald-100 uppercase tracking-wider">
                  Role
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-emerald-100 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-emerald-100 uppercase tracking-wider">
                  Mandate
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-emerald-100 uppercase tracking-wider">
                  Created
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-emerald-100 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-emerald-700/30">
              {customers?.map((customer) => (
                <tr key={customer.id} className="hover:bg-emerald-900/30">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-white">
                      {customer.phone}
                    </div>
                    {customer.email && (
                      <div className="text-sm text-emerald-200/80">{customer.email}</div>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-teal-500/30 text-teal-200 border border-teal-500/50">
                      {customer.role}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {customer.isDisabled ? (
                      <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-red-500/30 text-red-200 border border-red-500/50">
                        Disabled
                      </span>
                    ) : (
                      <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-500/30 text-green-200 border border-green-500/50">
                        Active
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-emerald-200/90">
                    {customer.hasActiveMandate ? (
                      <span className="text-green-400">Active</span>
                    ) : (
                      <span className="text-red-400">None</span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-emerald-200/80">
                    {format(new Date(customer.createdAt), 'MMM dd, yyyy')}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    {customer.hasActiveMandate && !customer.isDisabled && (
                      <button
                        onClick={() => setSelectedCustomerId(customer.id)}
                        className="text-red-400 hover:text-red-300 transition-colors"
                      >
                        Debit
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Debit Modal */}
      {selectedCustomerId && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-emerald-900/95 dark:bg-emerald-900/95 rounded-2xl border border-emerald-700/50 p-6 max-w-md w-full shadow-xl">
            <h2 className="text-2xl font-bold text-white mb-4">Debit Customer</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-emerald-100 mb-1">
                  Amount (₦)
                </label>
                <input
                  type="number"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  className="w-full px-4 py-3 border border-emerald-700/50 rounded-lg bg-gray-800/80 text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500"
                  placeholder="0.00"
                  min="1"
                  step="0.01"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-emerald-100 mb-1">
                  Reason *
                </label>
                <textarea
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  className="w-full px-4 py-3 border border-emerald-700/50 rounded-lg bg-gray-800/80 text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500"
                  placeholder="e.g., Fraudulent activity detected"
                  rows={3}
                  required
                />
              </div>
              <div className="bg-yellow-500/20 border border-yellow-500/50 rounded-lg p-3">
                <p className="text-sm text-yellow-200">
                  <strong>Note:</strong> Money will be debited from the customer's account and sent to the configured settlement account.
                </p>
              </div>
              <div className="flex space-x-3">
                <button
                  onClick={() => {
                    setSelectedCustomerId(null);
                    setAmount('');
                    setReason('');
                  }}
                  className="flex-1 px-4 py-2 border border-emerald-700/50 rounded-lg text-emerald-200 hover:bg-emerald-900/60 transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={() => handleDebit(selectedCustomerId)}
                  disabled={debitCustomer.isPending || !amount || !reason}
                  className="flex-1 px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-400 disabled:opacity-50 disabled:cursor-not-allowed font-semibold transition-colors"
                >
                  {debitCustomer.isPending ? 'Processing...' : 'Debit Customer'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
