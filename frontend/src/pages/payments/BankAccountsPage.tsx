import { useState, type FormEvent } from 'react';
import { useBankAccounts, useAddBankAccount, useSetDefaultBankAccount, useDeleteBankAccount } from '../../hooks/useBankAccounts';
import { usePaymentBanks } from '../../hooks/usePayments';

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

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const selectedBank = banks?.find((b) => b.code === bankCode);
    await addAccount.mutateAsync({
      bankName: selectedBank?.name || '',
      bankCode,
      accountNumber,
      accountName,
      isPayoutDefault,
    });
    setShowForm(false);
    setBankCode('');
    setAccountNumber('');
    setAccountName('');
    setIsPayoutDefault(false);
  };

  if (isLoading) {
    return <div className="text-center py-8">Loading...</div>;
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Bank Accounts</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700"
        >
          {showForm ? 'Cancel' : 'Add Bank Account'}
        </button>
      </div>

      {showForm && (
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <h2 className="text-xl font-semibold mb-4">Add Bank Account</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Bank</label>
              <select
                className="w-full px-3 py-2 border border-gray-300 rounded-md"
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
              <label className="block text-sm font-medium text-gray-700 mb-1">Account Number</label>
              <input
                type="text"
                className="w-full px-3 py-2 border border-gray-300 rounded-md"
                value={accountNumber}
                onChange={(e) => setAccountNumber(e.target.value)}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Account Name</label>
              <input
                type="text"
                className="w-full px-3 py-2 border border-gray-300 rounded-md"
                value={accountName}
                onChange={(e) => setAccountName(e.target.value)}
                required
              />
            </div>
            <div>
              <label className="flex items-center">
                <input
                  type="checkbox"
                  className="mr-2"
                  checked={isPayoutDefault}
                  onChange={(e) => setIsPayoutDefault(e.target.checked)}
                />
                Set as default payout account
              </label>
            </div>
            <button
              type="submit"
              disabled={addAccount.isPending}
              className="w-full bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 disabled:opacity-50"
            >
              {addAccount.isPending ? 'Adding...' : 'Add Account'}
            </button>
          </form>
        </div>
      )}

      <div className="space-y-4">
        {bankAccounts && bankAccounts.length > 0 ? (
          bankAccounts.map((account) => (
            <div key={account.id} className="bg-white rounded-lg shadow-md p-6">
              <div className="flex justify-between items-start">
                <div>
                  <h3 className="text-lg font-semibold">{account.bankName}</h3>
                  <p className="text-gray-600">{account.accountName}</p>
                  <p className="text-sm text-gray-500">{account.accountNumber}</p>
                  {account.isPayoutDefault && (
                    <span className="inline-block mt-2 px-2 py-1 bg-green-100 text-green-800 text-xs rounded">
                      Default Payout
                    </span>
                  )}
                </div>
                <div className="flex space-x-2">
                  {!account.isPayoutDefault && (
                    <button
                      onClick={() => setDefault.mutate(account.id)}
                      className="text-blue-600 hover:text-blue-700 text-sm"
                    >
                      Set Default
                    </button>
                  )}
                  <button
                    onClick={() => deleteAccount.mutate(account.id)}
                    className="text-red-600 hover:text-red-700 text-sm"
                  >
                    Delete
                  </button>
                </div>
              </div>
            </div>
          ))
        ) : (
          <div className="text-center py-8 text-gray-500">No bank accounts added yet</div>
        )}
      </div>
    </div>
  );
}



