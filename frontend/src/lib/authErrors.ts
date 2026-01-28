/**
 * Extract a user-friendly error message from axios/fetch errors (login/register).
 * Shows real backend message, validation errors, or network/unknown errors.
 */
export function getAuthErrorMessage(
  err: unknown,
  fallback: string = 'Something went wrong. Please try again.'
): string {
  if (err && typeof err === 'object' && 'response' in err) {
    const res = (err as { response?: { data?: unknown; status?: number } }).response;
    if (!res) return fallback;

    const data = res.data;
    const status = res.status;

    // Backend ErrorResponse shape: { message: string, error?: string }
    if (data && typeof data === 'object' && 'message' in data && typeof (data as { message: unknown }).message === 'string') {
      const msg = (data as { message: string }).message;
      if (msg) return msg;
    }

    // Validation: { message: string, errors: Record<string, string> }
    if (status === 400 && data && typeof data === 'object' && 'errors' in data) {
      const errors = (data as { errors?: Record<string, string> }).errors;
      if (errors && typeof errors === 'object') {
        const parts = Object.entries(errors).map(([field, text]) => `${field}: ${text}`);
        if (parts.length) return parts.join('. ');
      }
      if (data && typeof data === 'object' && 'message' in data && typeof (data as { message: unknown }).message === 'string') {
        return (data as { message: string }).message;
      }
    }

    // Generic by status
    if (status === 401) return 'Invalid phone or password.';
    if (status === 403) return 'Access denied.';
    if (status >= 500) return 'Server error. Please try again later.';
    if (status >= 400) return 'Invalid request. Check your input.';
  }

  // Network / no response
  if (err instanceof Error) {
    if (err.message === 'Network Error' || err.name === 'NetworkError')
      return 'Network error. Is the backend running? Check the URL.';
    if (err.message) return err.message;
  }

  return fallback;
}
