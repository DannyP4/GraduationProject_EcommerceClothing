import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import * as authService from '../services/authService';
import AuthCard from '../components/AuthCard';
import CaptchaWidget, { captchaEnabled } from '../components/CaptchaWidget';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function ForgotPasswordPage() {
  const { t } = useTranslation();
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [sent, setSent] = useState(false);
  const [captchaToken, setCaptchaToken] = useState('');
  const [captchaKey, setCaptchaKey] = useState(0);
  const resetCaptcha = () => { setCaptchaToken(''); setCaptchaKey((k) => k + 1); };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!EMAIL_REGEX.test(email.trim())) {
      setError(t('authFlow.errors.invalidEmail'));
      return;
    }
    if (captchaEnabled && !captchaToken) {
      setError(t('authFlow.errors.captcha'));
      return;
    }
    setSubmitting(true);
    try {
      await authService.forgotPassword(email.trim(), captchaToken);
      setSent(true);
    } catch (err) {
      resetCaptcha();
      setError(err.message || t('authFlow.errors.generic'));
    } finally {
      setSubmitting(false);
    }
  };

  if (sent) {
    return (
      <AuthCard eyebrow={t('authFlow.eyebrow.account')} title={t('authFlow.forgot.sentTitle')}>
        <p className="text-sm text-black/60 mb-7 leading-relaxed">
          {t('authFlow.forgot.sentLead.before')}
          <strong className="text-black break-all">{email.trim()}</strong>
          {t('authFlow.forgot.sentLead.after')}
        </p>
        <Link
          to="/login"
          className="block text-center w-full bg-black text-white text-[12px] font-bold tracking-[0.15em] uppercase py-4 hover:bg-[#E83354] transition-colors"
        >
          {t('authFlow.backToSignIn')}
        </Link>
      </AuthCard>
    );
  }

  return (
    <AuthCard eyebrow={t('authFlow.eyebrow.account')} title={t('authFlow.forgot.title')}>
      <p className="text-sm text-black/50 mb-7">
        {t('authFlow.forgot.lead')}
      </p>
      <form onSubmit={handleSubmit} className="space-y-5">
        {error && (
          <div className="border border-[#E83354]/40 bg-[#E83354]/5 px-4 py-3 text-[12px] text-[#E83354]">
            {error}
          </div>
        )}
        <div>
          <label className="block text-[10px] font-bold tracking-[0.15em] uppercase text-black/50 mb-1.5">
            {t('authFlow.fields.email')}
          </label>
          <input
            type="email"
            placeholder="you@gmail.com"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full border border-black/15 px-4 py-3 text-sm focus:outline-none focus:border-black transition-colors"
            required
          />
        </div>
        <CaptchaWidget key={captchaKey} onToken={setCaptchaToken} />
        <button
          type="submit"
          disabled={submitting || (captchaEnabled && !captchaToken)}
          className="w-full bg-black text-white text-[12px] font-bold tracking-[0.15em] uppercase py-4 hover:bg-[#E83354] transition-colors disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:bg-black"
        >
          {submitting ? t('authFlow.forgot.sending') : t('authFlow.forgot.submit')}
        </button>
      </form>
      <p className="text-center text-[11px] text-black/40 mt-7">
        <Link to="/login" className="font-bold text-black hover:text-[#E83354] transition-colors">
          {t('authFlow.backToSignIn')}
        </Link>
      </p>
    </AuthCard>
  );
}
