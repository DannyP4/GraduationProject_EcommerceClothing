import { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import * as authService from '../services/authService';
import AuthCard from '../components/AuthCard';
import { useAuth } from '../context/AuthContext';

export default function OAuthCallbackPage() {
  const [params] = useSearchParams();
  const code = params.get('code') || '';
  const navigate = useNavigate();
  const { adoptSession } = useAuth();
  const ran = useRef(false);

  useEffect(() => {
    if (ran.current) return;
    ran.current = true;

    if (!code) {
      navigate('/login?oauth_error=google_login_failed', { replace: true });
      return;
    }
    authService
      .oauthExchange(code)
      .then((data) => {
        const user = adoptSession(data);
        navigate(user?.role === 'admin' ? '/admin' : '/', { replace: true });
      })
      .catch((err) => {
        navigate(`/login?oauth_error=${encodeURIComponent(err.message || 'google_login_failed')}`, { replace: true });
      });
  }, [code, navigate, adoptSession]);

  return (
    <AuthCard eyebrow="Signing in" title="Almost there...">
      <p className="text-sm text-black/50">Completing your Google sign-in.</p>
    </AuthCard>
  );
}
