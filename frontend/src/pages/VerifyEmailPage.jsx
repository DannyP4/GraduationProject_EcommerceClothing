import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import * as authService from '../services/authService';
import AuthCard from '../components/AuthCard';
import { useAuth } from '../context/AuthContext';

export default function VerifyEmailPage() {
  const { t } = useTranslation();
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const [state, setState] = useState('verifying'); // 'verifying' | 'success' | 'error'
  const [message, setMessage] = useState('');
  const auth = useAuth();
  const ran = useRef(false);

  useEffect(() => {
    if (ran.current) return; // single-use token

    ran.current = true;

    if (!token) {
      setState('error');
      setMessage(t('authFlow.verify.missingToken'));
      return;
    }
    authService
      .verifyEmail(token)
      .then(() => {
        setState('success');
        if (auth.status === 'authenticated') auth.refreshUser().catch(() => {});
      })
      .catch((err) => {
        setState('error');
        setMessage(err.message || t('authFlow.errors.linkExpired'));
      });
  }, [token, auth, t]);

  if (state === 'verifying') {
    return (
      <AuthCard eyebrow={t('authFlow.verify.verifyingEyebrow')} title={t('authFlow.verify.verifyingTitle')}>
        <p className="text-sm text-black/50">{t('authFlow.verify.verifyingLead')}</p>
      </AuthCard>
    );
  }

  if (state === 'success') {
    return (
      <AuthCard eyebrow={t('authFlow.verify.successEyebrow')} title={t('authFlow.verify.successTitle')}>
        <p className="text-sm text-black/60 mb-7 leading-relaxed">
          {t('authFlow.verify.successLead')}
        </p>
        <Link
          to={auth.status === 'authenticated' ? '/account/profile' : '/login'}
          className="block text-center w-full bg-black text-white text-[12px] font-bold tracking-[0.15em] uppercase py-4 hover:bg-[#E83354] transition-colors"
        >
          {auth.status === 'authenticated' ? t('authFlow.verify.goToAccount') : t('authFlow.signIn')}
        </Link>
      </AuthCard>
    );
  }

  return (
    <AuthCard eyebrow={t('authFlow.verify.failedEyebrow')} title={t('authFlow.verify.failedTitle')}>
      <p className="text-sm text-black/60 mb-7 leading-relaxed">{message}</p>
      <Link
        to="/"
        className="block text-center w-full bg-black text-white text-[12px] font-bold tracking-[0.15em] uppercase py-4 hover:bg-[#E83354] transition-colors"
      >
        {t('authFlow.backToStore')}
      </Link>
    </AuthCard>
  );
}
