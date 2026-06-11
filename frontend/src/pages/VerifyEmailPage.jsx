import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import * as authService from '../services/authService';
import AuthCard from '../components/AuthCard';
import { useAuth } from '../context/AuthContext';

export default function VerifyEmailPage() {
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
      setMessage('This verification link is missing its token.');
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
        setMessage(err.message || 'This link is invalid or has expired.');
      });
  }, [token, auth]);

  if (state === 'verifying') {
    return (
      <AuthCard eyebrow="One last step" title="Verifying...">
        <p className="text-sm text-black/50">Confirming your email address.</p>
      </AuthCard>
    );
  }

  if (state === 'success') {
    return (
      <AuthCard eyebrow="Verified" title="Email confirmed">
        <p className="text-sm text-black/60 mb-7 leading-relaxed">
          Your email is verified - your Vesta account is all set.
        </p>
        <Link
          to={auth.status === 'authenticated' ? '/account/profile' : '/login'}
          className="block text-center w-full bg-black text-white text-[12px] font-bold tracking-[0.15em] uppercase py-4 hover:bg-[#E83354] transition-colors"
        >
          {auth.status === 'authenticated' ? 'Go to my account' : 'Sign in'}
        </Link>
      </AuthCard>
    );
  }

  return (
    <AuthCard eyebrow="Verification failed" title="Link not valid">
      <p className="text-sm text-black/60 mb-7 leading-relaxed">{message}</p>
      <Link
        to="/"
        className="block text-center w-full bg-black text-white text-[12px] font-bold tracking-[0.15em] uppercase py-4 hover:bg-[#E83354] transition-colors"
      >
        Back to store
      </Link>
    </AuthCard>
  );
}
