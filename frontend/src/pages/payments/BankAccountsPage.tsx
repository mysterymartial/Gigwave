import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useBankAccounts, useAddBankAccount, useSetDefaultBankAccount, useDeleteBankAccount } from '../../hooks/useBankAccounts';
import { usePaymentBanks, useSetupOrganizerMandate, useSetupMusicianMandate } from '../../hooks/usePayments';
import { useMusicianProfile } from '../../hooks/useProfiles';
import { useAuth } from '../../hooks/useAuth';
import { UserRole } from '../../types';
import { getAuthErrorMessage } from '../../lib/authErrors';

export default function BankAccountsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { data: bankAccounts, isLoading } = useBankAccounts();
  const { data: banks } = usePaymentBanks();
  const { data: musicianProfile } = useMusicianProfile({ enabled: user?.role === UserRole.MUSICIAN });
  const addAccount = useAddBankAccount();
  const setDefault = useSetDefaultBankAccount();
  const deleteAccount = useDeleteBankAccount();
  const setupOrganizerMandate = useSetupOrganizerMandate();
  const setupMusicianMandate = useSetupMusicianMandate();

  const [showForm, setShowForm] = useState(false);
  const [showMandateForm, setShowMandateForm] = useState<string | null>(null); // accountId
  const [bankCode, setBankCode] = useState('');
  const [accountNumber, setAccountNumber] = useState('');
  const [accountName, setAccountName] = useState('');
  const [isPayoutDefault, setIsPayoutDefault] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const MANDATE_MAX_AMOUNT_NGN = 5_000_000;

  const hasNoAccounts = !bankAccounts || bankAccounts.length === 0;
  useEffect(() => {
    if (!isLoading && hasNoAccounts) setShowForm(true);
  }, [isLoading, hasNoAccounts]);

  // Show mandate prompt after first account is added
  useEffect(() => {
    if (bankAccounts && bankAccounts.length === 1 && !showMandateForm && !showForm) {
      // Small delay to let user see the account was added
      const timer = setTimeout(() => {
        setShowMandateForm(bankAccounts[0].id);
      }, 1000);
      return () => clearTimeout(timer);
    }
  }, [bankAccounts, showMandateForm, showForm]);

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
          // After adding account, prompt to set up mandate
          if (bankAccounts && bankAccounts.length === 0) {
            // First account added - show mandate prompt
            setTimeout(() => {
              const newAccount = bankAccounts?.[0] || bankAccounts?.[bankAccounts.length - 1];
              if (newAccount) {
                setShowMandateForm(newAccount.id);
              }
            }, 500);
          }
        },
        onError: (err) => {
          setError(getAuthErrorMessage(err, 'Failed to add bank account. Please try again.'));
        },
      }
    );
  };

  const handleSetupMandate = (accountId: string) => {
    setError(null);
    const idempotencyKey = `mandate-${accountId}-${Date.now()}`;
    const mandateFn = user?.role === UserRole.EVENT_OWNER ? setupOrganizerMandate : setupMusicianMandate;
    mandateFn.mutate(
      {
        bankAccountId: accountId,
        maxAmount: MANDATE_MAX_AMOUNT_NGN,
        idempotencyKey,
      },
      {
        onSuccess: (response) => {
          if (response.authorizationUrl) {
            // Redirect to OnePipe authorization page
            // After authorization, user will return and Layout will enforce 3 videos if musician
            window.location.href = response.authorizationUrl;
          } else {
            setError('Mandate setup initiated. Please check your email or SMS for authorization.');
            setShowMandateForm(null);
            // For musicians: after mandate setup, check if they need to add videos
            if (user?.role === UserRole.MUSICIAN) {
              const videoCount = musicianProfile?.performanceVideoUrls?.length || 0;
              if (videoCount < 3) {
                setTimeout(() => navigate('/profile'), 2000);
              }
            }
          }
        },
        onError: (err) => {
          setError(getAuthErrorMessage(err, 'Failed to set up mandate. Please try again.'));
        },
      }
    );
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-teal-500"></div>
          <p className="text-gray-600 dark:text-gray-400 mt-4">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto">
        {hasNoAccounts && (
          <div className="mb-6 p-4 rounded-lg bg-emerald-500/20 border border-emerald-500/40 text-emerald-200">
            Add your bank account to continue using GigWave.
          </div>
        )}
        {!hasNoAccounts && bankAccounts && bankAccounts.length > 0 && (
          <div className="mb-6 p-4 rounded-lg bg-teal-500/20 border border-teal-500/40 text-teal-200">
            <div className="flex justify-between items-center">
              <div>
                <p className="font-semibold">Ready to start using GigWave?</p>
                <p className="text-sm mt-1">
                  {user?.role === UserRole.EVENT_OWNER
                    ? 'Set up a payment mandate to enable secure payments for your gigs.'
                    : 'Set up a payment mandate to receive payouts securely.'}
                </p>
              </div>
              <div className="flex gap-2 ml-4">
                {user?.role === UserRole.EVENT_OWNER ? (
                  <button
                    onClick={() => navigate('/gigs/create')}
                    className="bg-teal-500 hover:bg-teal-400 text-white px-4 py-2 rounded-lg font-semibold transition-colors"
                  >
                    Post a Gig
                  </button>
                ) : (
                  <button
                    onClick={() => navigate('/gigs')}
                    className="bg-teal-500 hover:bg-teal-400 text-white px-4 py-2 rounded-lg font-semibold transition-colors"
                  >
                    Find Gigs
                  </button>
                )}
              </div>
            </div>
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
                <label className="block text-sm font-medium text-gray-700 dark:text-emerald-100 mb-1">Account Number</label>
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
                  <div className="flex flex-col items-end gap-2">
                    <div className="flex space-x-2">
                      <button
                        onClick={() => setShowMandateForm(account.id)}
                        className="bg-teal-500 hover:bg-teal-400 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors"
                      >
                        Set Up Mandate
                      </button>
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
              </div>
            ))
          ) : (
            <div className="text-center py-12 text-gray-500 dark:text-gray-400">No bank accounts added yet</div>
          )}
        </div>

        {/* Mandate Setup Modal - max amount fixed at ₦5,000,000; BVN not collected */}
        {showMandateForm && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-emerald-900/95 dark:bg-emerald-900/95 rounded-2xl border border-emerald-700/50 p-8 max-w-md w-full shadow-xl">
              <h2 className="text-2xl font-semibold mb-4 text-white">Set Up Payment Mandate</h2>
              <p className="text-emerald-200/80 mb-2 text-sm">
                {user?.role === UserRole.EVENT_OWNER
                  ? 'Authorize GigWave to debit your account for secure payments when booking musicians.'
                  : 'Authorize GigWave to pay you securely for completed gigs.'}
              </p>
              <p className="text-emerald-200/60 mb-6 text-xs">
                Maximum transaction amount: ₦5,000,000
              </p>
              {error && (
                <div className="mb-4 p-3 rounded-lg bg-red-500/20 border border-red-500/50 text-red-200 text-sm">
                  {error}
                </div>
              )}
              <div className="flex space-x-3">
                <button
                  type="button"
                  onClick={() => {
                    setShowMandateForm(null);
                    setError(null);
                  }}
                  className="flex-1 bg-gray-700 hover:bg-gray-600 text-white px-4 py-3 rounded-lg font-semibold transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={() => handleSetupMandate(showMandateForm)}
                  disabled={setupOrganizerMandate.isPending || setupMusicianMandate.isPending}
                  className="flex-1 bg-teal-500 hover:bg-teal-400 text-white px-4 py-3 rounded-lg font-semibold disabled:opacity-50 transition-colors"
                >
                  {setupOrganizerMandate.isPending || setupMusicianMandate.isPending
                    ? 'Setting up...'
                    : 'Set Up Mandate'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}



