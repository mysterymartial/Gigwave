import { useState, useEffect, type FormEvent } from 'react';
import { useBankAccounts, useAddBankAccount, useSetDefaultBankAccount, useDeleteBankAccount } from '../../hooks/useBankAccounts';
import { usePaymentBanks } from '../../hooks/usePayments';
import { getAuthErrorMessage } from '../../lib/authErrors';

export default function BankAccountsPage() {
  const { data: bankAccounts, isLoading } = useBankAccounts();
  const { data: banks } = usePaymentBanks();
  const addAccount = useAddBankAccount();
  const setDefault = useSetDefaultBankAccount();
  const deleteAccount = useDeleteBankAccount();

  const [showForm, setShowForm] = useState(false);
  const [bankCode, setBankCode] = useState('');
  const [accountNumber, setAccountNumber] = useState('');
  const [accountName, setAccountName] = useState('');
  const [isPayoutDefault, setIsPayoutDefault] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const hasNoAccounts = !bankAccounts || bankAccounts.length === 0;
  useEffect(() => {
    if (!isLoading && hasNoAccounts) setShowForm(true);
  }, [isLoading, hasNoAccounts]);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    const selectedBank = banks?.find((b) => b.code === bankCode);
    addAccount.mutate(
      {
        bankName: selectedBank?.name || '',
        bankCode,
        accountNumber,
        accountName,
        isPayoutDefault,
      },
      {
        onSuccess: () => {
          setShowForm(false);
          setBankCode('');
          setAccountNumber('');
          setAccountName('');
          setIsPayoutDefault(false);
        },
        onError: (err) => {
          setError(getAuthErrorMessage(err, 'Failed to add bank account. Please try again.'));
        },
      }
    );
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-900 flex items-center justify-center">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-teal-500"></div>
          <p className="text-gray-400 mt-4">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto">
        {hasNoAccounts && (
          <div className="mb-6 p-4 rounded-lg bg-emerald-500/20 border border-emerald-500/40 text-emerald-200">
            Add your bank account to continue using GigWave.
          </div>
        )}
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold text-white">Bank Accounts</h1>
          <button
            onClick={() => setShowForm(!showForm)}
            className="bg-teal-500 hover:bg-teal-400 text-white px-6 py-2 rounded-lg font-semibold transition-colors"
          >
            {showForm ? 'Cancel' : 'Add Bank Account'}
          </button>
        </div>

        {showForm && (
          <div className="bg-emerald-900/40 dark:bg-emerald-900/50 rounded-2xl border border-emerald-700/40 p-8 mb-6 shadow-xl">
            <h2 className="text-xl font-semibold mb-6 text-white">Add Bank Account</h2>
            {error && (
              <div className="mb-4 p-3 rounded-lg bg-red-500/20 border border-red-500/50 text-red-200 text-sm">
                {error}
              </div>
            )}
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-emerald-100 mb-1">Bank</label>
                <select
                  className="w-full px-4 py-3 rounded-lg bg-gray-800/80 border border-emerald-700/50 text-white focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                  value={bankCode}
                  onChange={(e) => setBankCode(e.target.value)}
                  required
                >
                  <option value="">Select Bank</option>
                  {banks?.map((bank) => (
                    <option key={bank.code} value={bank.code}>
                      {bank.name}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-emerald-100 mb-1">Account Number</label>
                <input
                  type="text"
                  className="w-full px-4 py-3 rounded-lg bg-gray-800/80 border border-emerald-700/50 text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                  value={accountNumber}
                  onChange={(e) => setAccountNumber(e.target.value)}
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-emerald-100 mb-1">Account Name</label>
                <input
                  type="text"
                  className="w-full px-4 py-3 rounded-lg bg-gray-800/80 border border-emerald-700/50 text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500"
                  value={accountName}
                  onChange={(e) => setAccountName(e.target.value)}
                  required
                />
              </div>
              <div>
                <label className="flex items-center text-emerald-100">
                  <input
                    type="checkbox"
                    className="mr-2 w-4 h-4 text-teal-500 rounded focus:ring-teal-500"
                    checked={isPayoutDefault}
                    onChange={(e) => setIsPayoutDefault(e.target.checked)}
                  />
                  Set as default payout account
                </label>
              </div>
              <button
                type="submit"
                disabled={addAccount.isPending}
                className="w-full bg-teal-500 hover:bg-teal-400 text-white px-4 py-3 rounded-lg font-semibold disabled:opacity-50 transition-colors"
              >
                {addAccount.isPending ? 'Adding...' : 'Add Account'}
              </button>
            </form>
          </div>
        )}

        <div className="space-y-4">
          {bankAccounts && bankAccounts.length > 0 ? (
            bankAccounts.map((account) => (
              <div key={account.id} className="bg-emerald-900/40 dark:bg-emerald-900/50 rounded-xl border border-emerald-700/40 p-6 shadow-lg">
                <div className="flex justify-between items-start">
                  <div>
                    <h3 className="text-lg font-semibold text-white">{account.bankName}</h3>
                    <p className="text-emerald-200/80">{account.accountName}</p>
                    <p className="text-sm text-emerald-200/60">{account.accountNumber}</p>
                    {account.isPayoutDefault && (
                      <span className="inline-block mt-2 px-3 py-1 bg-emerald-500/30 text-emerald-200 text-xs rounded-full border border-emerald-500/50">
                        Default Payout
                      </span>
                    )}
                  </div>
                  <div className="flex space-x-2">
                    {!account.isPayoutDefault && (
                      <button
                        onClick={() => setDefault.mutate(account.id)}
                        className="text-teal-400 hover:text-teal-300 text-sm font-medium"
                      >
                        Set Default
                      </button>
                    )}
                    <button
                      onClick={() => deleteAccount.mutate(account.id)}
                      className="text-red-400 hover:text-red-300 text-sm font-medium"
                    >
                      Delete
                    </button>
                  </div>
                </div>
              </div>
            ))
          ) : (
            <div className="text-center py-12 text-gray-400">No bank accounts added yet</div>
          )}
        </div>
      </div>
    </div>
  );
}



