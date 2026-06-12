import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import * as authService from '../services/authService';
import AuthCard from '../components/AuthCard';
import PasswordStrengthMeter from '../components/PasswordStrengthMeter';
import { useToast } from '../components/Toast';
import { useAuth } from '../context/AuthContext';

const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d).+$/;

export default function AcceptInvitePage() {
  const { t } = useTranslation();
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const [state, setState] = useState('loading'); // 'loading' | 'ready' | 'invalid'
  const [email, setEmail] = useState('');
  const [fullName, setFullName] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();
  const toast = useToast();
  const { adoptSession } = useAuth();
  const ran = useRef(false);

  useEffect(() => {
    if (ran.current) return;
    ran.current = true;

    if (!token) {
      setState('invalid');
      return;
    }
    authService
      .previewInvite(token)
      .then((data) => {
        setEmail(data.email || '');
        if (data.fullName) setFullName(data.fullName);
        setState('ready');
      })
      .catch(() => setState('invalid'));
  }, [token]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!fullName.trim()) {
      setError(t('authFlow.errors.nameRequired'));
      return;
    }
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
      const data = await authService.acceptInvite({ token, fullName: fullName.trim(), password });
      adoptSession(data);
      toast.success(t('authFlow.invite.success'));
      navigate('/admin');
    } catch (err) {
      setError(err.message || t('authFlow.invite.invalidError'));
      setSubmitting(false);
    }
  };

  if (state === 'loading') {
    return (
      <AuthCard eyebrow={t('authFlow.invite.eyebrow')} title={t('authFlow.invite.loadingTitle')}>
        <p className="text-sm text-black/50">{t('authFlow.invite.loadingLead')}</p>
      </AuthCard>
    );
  }

  if (state === 'invalid') {
    return (
      <AuthCard eyebrow={t('authFlow.invite.eyebrow')} title={t('authFlow.invite.invalidTitle')}>
        <p className="text-sm text-black/60 mb-7 leading-relaxed">
          {t('authFlow.invite.invalidLead')}
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
    <AuthCard eyebrow={t('authFlow.invite.eyebrow')} title={t('authFlow.invite.title')}>
      <p className="text-sm text-black/60 mb-7 leading-relaxed">
        {t('authFlow.invite.lead.before')}
        <strong className="text-black break-all">{email}</strong>
        {t('authFlow.invite.lead.after')}
      </p>
      <form onSubmit={handleSubmit} className="space-y-5">
        {error && (
          <div className="border border-[#E83354]/40 bg-[#E83354]/5 px-4 py-3 text-[12px] text-[#E83354]">
            {error}
          </div>
        )}
        <div>
          <label className="block text-[10px] font-bold tracking-[0.15em] uppercase text-black/50 mb-1.5">
            {t('authFlow.fields.fullName')}
          </label>
          <input
            type="text"
            placeholder={t('authFlow.fields.fullNamePlaceholder')}
            autoComplete="name"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            className="w-full border border-black/15 px-4 py-3 text-sm focus:outline-none focus:border-black transition-colors"
            required
            maxLength={150}
          />
        </div>
        <div>
          <label className="block text-[10px] font-bold tracking-[0.15em] uppercase text-black/50 mb-1.5">
            {t('authFlow.fields.password')}
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
          {submitting ? t('authFlow.invite.settingUp') : t('authFlow.invite.submit')}
        </button>
      </form>
    </AuthCard>
  );
}
