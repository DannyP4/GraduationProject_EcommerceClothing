import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext';
import * as authService from '../services/authService';
import { useToast } from './Toast';

export default function EmailVerificationBanner() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const toast = useToast();
  const [sending, setSending] = useState(false);
  const [dismissed, setDismissed] = useState(false);

  if (!user || user.emailVerified || dismissed) return null;

  const resend = async () => {
    setSending(true);
    try {
      await authService.resendVerification();
      toast.success(t('auth.verifyBanner.sent'));
    } catch (e) {
      toast.error(e.message || t('auth.verifyBanner.sendError'));
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="bg-[#E83354]/5 border border-[#E83354]/30 px-4 py-3 mb-6 flex items-center gap-3">
      <span className="w-2 h-2 rounded-full bg-[#E83354] flex-shrink-0" />
      <p className="flex-1 text-xs text-black/70">
        {t('auth.verifyBanner.message')}
      </p>
      <button
        onClick={resend}
        disabled={sending}
        className="text-[11px] font-bold tracking-[0.12em] uppercase text-[#E83354] hover:underline disabled:opacity-50 flex-shrink-0"
      >
        {sending ? t('auth.verifyBanner.sending') : t('auth.verifyBanner.resend')}
      </button>
      <button
        onClick={() => setDismissed(true)}
        aria-label={t('auth.verifyBanner.dismiss')}
        className="text-black/30 hover:text-black transition-colors flex-shrink-0"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>
    </div>
  );
}
