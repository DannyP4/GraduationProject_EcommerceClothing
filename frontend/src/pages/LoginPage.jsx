import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useLocation, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import PasswordStrengthMeter from '../components/PasswordStrengthMeter';
import CaptchaWidget, { captchaEnabled } from '../components/CaptchaWidget';
import { useToast } from '../components/Toast';
import { API_BASE_URL } from '../lib/api';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
// Mirrors BE @Pattern in RegisterRequest — at least one letter and one digit.
const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d).+$/;

function friendlyOAuthError(code) {
  return code === 'google_login_failed' ? 'Google sign-in failed. Please try again.' : code;
}

function GoogleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
      <path fill="#4285F4" d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844a4.14 4.14 0 0 1-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615z" />
      <path fill="#34A853" d="M9 18c2.43 0 4.467-.806 5.956-2.184l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18z" />
      <path fill="#FBBC05" d="M3.964 10.706A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.706V4.962H.957A8.997 8.997 0 0 0 0 9c0 1.452.348 2.827.957 4.038l3.007-2.332z" />
      <path fill="#EA4335" d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.962L3.964 7.294C4.672 5.167 6.656 3.58 9 3.58z" />
    </svg>
  );
}

export default function LoginPage() {
  const [mode, setMode] = useState('login'); // 'login' | 'signup'
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [captchaToken, setCaptchaToken] = useState('');
  const [captchaKey, setCaptchaKey] = useState(0);
  const resetCaptcha = () => { setCaptchaToken(''); setCaptchaKey((k) => k + 1); };

  const auth = useAuth();
  const toast = useToast();
  const [searchParams, setSearchParams] = useSearchParams();
  const oauthErrorShown = useRef(false);
  const handleGoogleLogin = () => {
    window.location.href = `${API_BASE_URL}/oauth2/authorization/google`;
  };
  const layer1 = useRef(null);
  const layer2 = useRef(null);
  const layer3 = useRef(null);
  const navigate = useNavigate();
  const location = useLocation();
  const fromPath = location.state?.from;
  const redirectFor = (role) => fromPath || (role === 'admin' ? '/admin' : '/');

  // Already signed in
  useEffect(() => {
    if (auth.status === 'authenticated') {
      navigate(redirectFor(auth.user?.role), { replace: true });
    }
  }, [auth.status, auth.user?.role, navigate, fromPath]);

  useEffect(() => {
    if (oauthErrorShown.current) return;
    const oauthError = searchParams.get('oauth_error');
    if (oauthError) {
      oauthErrorShown.current = true;
      toast.error(friendlyOAuthError(oauthError));
      const next = new URLSearchParams(searchParams);
      next.delete('oauth_error');
      setSearchParams(next, { replace: true });
    }
  }, [searchParams, setSearchParams, toast]);

  const switchMode = (m) => {
    setMode(m);
    setError('');
    setConfirmPassword('');
    resetCaptcha();
  };

  useEffect(() => {
    const handler = (e) => {
      const cx = window.innerWidth / 2;
      const cy = window.innerHeight / 2;
      const dx = (e.clientX - cx) / cx;
      const dy = (e.clientY - cy) / cy;

      if (layer1.current) layer1.current.style.transform = `translate(${dx * 20}px, ${dy * 15}px)`;
      if (layer2.current) layer2.current.style.transform = `translate(${dx * -30}px, ${dy * 20}px)`;
      if (layer3.current) layer3.current.style.transform = `translate(${dx * 15}px, ${dy * -10}px) rotate(${dx * 3}deg)`;
    };
    window.addEventListener('mousemove', handler);
    return () => window.removeEventListener('mousemove', handler);
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!EMAIL_REGEX.test(email.trim())) {
      setError('Please enter a valid email address.');
      return;
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    if (mode === 'signup') {
      if (!fullName.trim()) {
        setError('Please enter your full name.');
        return;
      }
      if (!PASSWORD_RULE.test(password)) {
        setError('Password must contain at least one letter and one digit.');
        return;
      }
      if (password !== confirmPassword) {
        setError('Passwords do not match.');
        return;
      }
    }

    if (captchaEnabled && !captchaToken) {
      setError('Please complete the captcha.');
      return;
    }

    setSubmitting(true);
    try {
      const loggedInUser = mode === 'login'
        ? await auth.login(email.trim(), password, captchaToken)
        : await auth.register({ email: email.trim(), password, fullName: fullName.trim(), captchaToken });
      navigate(redirectFor(loggedInUser?.role));
    } catch (err) {
      const msg = (err.message || '').toLowerCase();
      if (mode === 'login' && (msg.includes('suspended') || msg.includes('deleted'))) {
        const status = msg.includes('deleted') ? 'DELETED' : 'SUSPENDED';
        navigate('/auth/account-status', { state: { status, email: email.trim() } });
        return;
      }
      resetCaptcha();
      setError(err.message || 'Something went wrong. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex">
      {/* LEFT — Form Panel */}
      <div className="flex-1 bg-white flex flex-col justify-center px-10 md:px-16 py-12 relative">
        {/* Back link */}
        <Link
          to="/"
          className="absolute top-8 left-10 text-[11px] font-bold tracking-[0.15em] uppercase text-black/40 hover:text-black flex items-center gap-2 transition-colors"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M19 12H5M12 19l-7-7 7-7" />
          </svg>
          Back
        </Link>

        <div className="max-w-sm w-full mx-auto">
          {/* Logo */}
          <Link to="/" className="font-['Anton'] text-3xl tracking-widest text-black block mb-10">
            VESTA
          </Link>

          {/* Toggle */}
          <div className="flex mb-8 border-b border-black/10">
            {['login', 'signup'].map((m) => (
              <button
                key={m}
                type="button"
                onClick={() => switchMode(m)}
                className={`pb-3 mr-8 text-[11px] font-bold tracking-[0.15em] uppercase transition-all border-b-2 -mb-[1px] ${mode === m ? 'border-black text-black' : 'border-transparent text-black/30 hover:text-black/60'
                  }`}
              >
                {m === 'login' ? 'Sign In' : 'Create Account'}
              </button>
            ))}
          </div>

          <h1 className="text-3xl font-black tracking-tight mb-2">
            {mode === 'login' ? 'Welcome back.' : 'Join the movement.'}
          </h1>
          <p className="text-sm text-black/50 mb-8">
            {mode === 'login'
              ? 'Sign in to access your orders and wishlist.'
              : 'Get early drops, campus-exclusive deals, and more.'}
          </p>

          <form onSubmit={handleSubmit} className="space-y-5">
            {error && (
              <div className="border border-[#E83354]/40 bg-[#E83354]/5 px-4 py-3 text-[12px] text-[#E83354]">
                {error}
              </div>
            )}
            {mode === 'signup' && (
              <div>
                <label className="block text-[10px] font-bold tracking-[0.15em] uppercase text-black/50 mb-1.5">
                  Full Name
                </label>
                <input
                  type="text"
                  placeholder="Long Pham"
                  autoComplete="name"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="w-full border border-black/15 px-4 py-3 text-sm focus:outline-none focus:border-black transition-colors"
                  required
                />
              </div>
            )}
            <div>
              <label className="block text-[10px] font-bold tracking-[0.15em] uppercase text-black/50 mb-1.5">
                Email
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
            <div>
              <label className="block text-[10px] font-bold tracking-[0.15em] uppercase text-black/50 mb-1.5">
                Password
              </label>
              <input
                type="password"
                placeholder="••••••••"
                // Tells the browser/password manager whether to offer save vs autofill on this field.
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full border border-black/15 px-4 py-3 text-sm focus:outline-none focus:border-black transition-colors"
                required
                minLength={8}
              />
              {mode === 'signup' && <PasswordStrengthMeter value={password} />}
            </div>

            {mode === 'signup' && (
              <div>
                <label className="block text-[10px] font-bold tracking-[0.15em] uppercase text-black/50 mb-1.5">
                  Confirm Password
                </label>
                <input
                  type="password"
                  placeholder="••••••••"
                  autoComplete="new-password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="w-full border border-black/15 px-4 py-3 text-sm focus:outline-none focus:border-black transition-colors"
                  required
                  minLength={8}
                />
                {confirmPassword && confirmPassword !== password && (
                  <p className="text-[10px] text-[#E83354] mt-1 tracking-wider">Passwords do not match</p>
                )}
              </div>
            )}

            {mode === 'login' && (
              <div className="text-right">
                <Link to="/auth/forgot-password" className="text-[13px] font-medium text-black/60 hover:text-[#E83354] transition-colors">
                  Forgot password?
                </Link>
              </div>
            )}

            <CaptchaWidget key={captchaKey} onToken={setCaptchaToken} />

            <button
              type="submit"
              disabled={submitting || (captchaEnabled && !captchaToken)}
              className="w-full bg-black text-white text-[12px] font-bold tracking-[0.15em] uppercase py-4 hover:bg-[#E83354] transition-colors disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:bg-black"
            >
              {submitting
                ? mode === 'login' ? 'Signing in...' : 'Creating account...'
                : mode === 'login' ? 'Sign In' : 'Create Account'}
            </button>
          </form>

          <div className="my-6 flex items-center gap-4">
            <div className="flex-1 h-px bg-black/10" />
            <span className="text-[10px] text-black/30 tracking-wider uppercase">or</span>
            <div className="flex-1 h-px bg-black/10" />
          </div>

          <button
            type="button"
            onClick={handleGoogleLogin}
            className="w-full border border-black/20 text-[13px] font-semibold text-black/80 py-3.5 hover:border-black hover:bg-black/[0.02] transition-all flex items-center justify-center gap-3"
          >
            <GoogleIcon />
            Continue with Google
          </button>

          <p className="text-center text-[13px] text-black/55 mt-8">
            {mode === 'login' ? "Don't have an account? " : 'Already have an account? '}
            <button
              type="button"
              onClick={() => switchMode(mode === 'login' ? 'signup' : 'login')}
              className="font-bold text-[#E83354] hover:underline underline-offset-2 transition-colors"
            >
              {mode === 'login' ? 'Sign up' : 'Sign in'}
            </button>
          </p>
        </div>
      </div>

      {/* RIGHT — Decorative Panel */}
      <div className="hidden md:block w-1/2 bg-[#0A0A0A] relative overflow-hidden">
        {/* Ticker background text */}
        <div
          className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 whitespace-nowrap font-['Anton'] pointer-events-none select-none"
          style={{
            fontSize: 'clamp(80px, 10vw, 160px)',
            color: 'transparent',
            WebkitTextStroke: '1px rgba(255,255,255,0.06)',
            letterSpacing: '-0.02em',
            textTransform: 'uppercase',
            animation: 'ticker 20s linear infinite',
          }}
        >
          VESTA CAMPUS LEGEND STREETWEAR
        </div>

        {/* Parallax layers */}
        <div ref={layer1} className="absolute inset-0 flex items-center justify-center" style={{ transition: 'transform 0.1s ease-out' }}>
          <div
            className="w-80 h-80 rounded-full"
            style={{ background: 'radial-gradient(circle, rgba(232,51,84,0.3) 0%, transparent 70%)' }}
          />
        </div>

        <div ref={layer2} className="absolute inset-0 flex items-center justify-center" style={{ transition: 'transform 0.1s ease-out' }}>
          <img
            src="https://images.unsplash.com/photo-1509631179647-0177331693ae?w=700&q=80"
            alt="Fashion"
            className="w-64 h-80 object-cover opacity-60"
            style={{ filter: 'grayscale(30%) contrast(1.1)' }}
          />
        </div>

        <div ref={layer3} className="absolute bottom-12 right-12" style={{ transition: 'transform 0.1s ease-out' }}>
          <div className="border border-white/20 p-6 max-w-xs backdrop-blur-sm">
            <p className="text-[10px] font-bold tracking-[0.2em] uppercase text-[#E83354] mb-2">SS 2026</p>
            <p className="font-['Anton'] text-3xl text-white tracking-wide leading-tight uppercase">
              Campus<br />Collection
            </p>
            <p className="text-white/50 text-xs mt-3 leading-relaxed">
              200+ pieces. One mission. Dress like the legend you are.
            </p>
          </div>
        </div>

        {/* Top-left decorative circles */}
        <div className="absolute top-8 left-8 w-20 h-20 border border-white/10 rounded-full" />
        <div className="absolute top-12 left-12 w-10 h-10 border border-[#E83354]/30 rounded-full" />
        <div className="absolute bottom-8 left-8 w-16 h-16 bg-[#F5C842] opacity-20 rotate-12" />
      </div>
    </div>
  );
}
