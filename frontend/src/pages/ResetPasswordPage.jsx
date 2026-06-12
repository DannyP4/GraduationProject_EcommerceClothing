import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import * as authService from '../services/authService';
import AuthCard from '../components/AuthCard';
import PasswordStrengthMeter from '../components/PasswordStrengthMeter';
import { useToast } from '../components/Toast';

const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d).+$/;

export default function ResetPasswordPage() {
  const { t } = useTranslation();
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();
  const toast = useToast();

  if (!token) {
    return (
      <AuthCard eyebrow={t('authFlow.eyebrow.account')} title={t('authFlow.reset.invalidTitle')}>
        <p className="text-sm text-black/60 mb-7 leading-relaxed">
          {t('authFlow.reset.invalidLead')}
        </p>
        <Link
          to="/auth/forgot-password"
          className="block text-center w-full bg-black text-white text-[12px] font-bold tracking-[0.15em] uppercase py-4 hover:bg-[#E83354] transition-colors"
        >
          {t('authFlow.reset.requestNew')}
        </Link>
      </AuthCard>
    );
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (password.length < 8 || !PASSWORD_RULE.test(password)) {
      setError(t('authFlow.errors.passwordRule'));
      return;
    }
    if (password !== confirm) {
      setError(t('authFlow.errors.passwordMismatch'));
      return;
    }
    setSubmitting(true);
    try {
      await authService.resetPassword({ token, newPassword: password });
      toast.success(t('authFlow.reset.success'));
      navigate('/login');
    } catch (err) {
      setError(err.message || t('authFlow.errors.linkExpired'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthCard eyebrow={t('authFlow.eyebrow.account')} title={t('authFlow.reset.title')}>
      <p className="text-sm text-black/50 mb-7">{t('authFlow.reset.lead')}</p>
      <form onSubmit={handleSubmit} className="space-y-5">
        {error && (
          <div className="border border-[#E83354]/40 bg-[#E83354]/5 px-4 py-3 text-[12px] text-[#E83354]">
            {error}
          </div>
        )}
        <div>
          <label className="block text-[10px] font-bold tracking-[0.15em] uppercase text-black/50 mb-1.5">
            {t('authFlow.fields.newPassword')}
          </label>
          <input
            type="password"
            placeholder="••••••••"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full border border-black/15 px-4 py-3 text-sm focus:outline-none focus:border-black transition-colors"
            required
            minLength={8}
          />
          <PasswordStrengthMeter value={password} />
        </div>
        <div>
          <label className="block text-[10px] font-bold tracking-[0.15em] uppercase text-black/50 mb-1.5">
            {t('authFlow.fields.confirmPassword')}
          </label>
          <input
            type="password"
            placeholder="••••••••"
            autoComplete="new-password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            className="w-full border border-black/15 px-4 py-3 text-sm focus:outline-none focus:border-black transition-colors"
            required
            minLength={8}
          />
          {confirm && confirm !== password && (
            <p className="text-[10px] text-[#E83354] mt-1 tracking-wider">{t('authFlow.fields.mismatchHint')}</p>
          )}
        </div>
        <button
          type="submit"
          disabled={submitting}
          className="w-full bg-black text-white text-[12px] font-bold tracking-[0.15em] uppercase py-4 hover:bg-[#E83354] transition-colors disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:bg-black"
        >
          {submitting ? t('authFlow.reset.updating') : t('authFlow.reset.submit')}
        </button>
      </form>
    </AuthCard>
  );
}
