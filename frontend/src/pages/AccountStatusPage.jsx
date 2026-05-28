import { Link, useLocation } from 'react-router-dom';

const SUPPORT_EMAIL = 'longpd1911@gmail.com';

export default function AccountStatusPage() {
  const location = useLocation();
  const state = location.state ?? {};
  const status = (state.status || '').toUpperCase();
  const email = state.email;

  const isDeleted = status === 'DELETED';
  const tag = isDeleted ? '410 · Account deleted' : '423 · Account suspended';
  const headline = isDeleted ? 'Account deleted' : 'Account suspended';
  const intro = isDeleted
    ? 'This account has been removed by an administrator. Order history is kept for audit purposes.'
    : 'This account has been temporarily suspended. You can request a review by contacting support.';

  const mailto = `mailto:${SUPPORT_EMAIL}?subject=${encodeURIComponent(headline + ' — request')}${email ? `&body=${encodeURIComponent('Account: ' + email)}` : ''}`;

  return (
    <div className="min-h-screen bg-[#E8E8E8] flex items-center justify-center px-6 py-16">
      <div className="max-w-md w-full bg-white border border-black/10 p-8 text-center">
        <p className="text-[10px] font-bold tracking-[0.3em] uppercase text-[#E83354] mb-3">{tag}</p>
        <h1 className="font-['Anton'] text-5xl uppercase tracking-tight mb-3">{headline}</h1>
        <p className="text-sm text-black/60 mb-6">{intro}</p>

        {email && (
          <div className="border border-black/10 px-4 py-2 mb-6 text-xs text-left">
            <span className="text-black/40 tracking-wider uppercase mr-2">Account</span>
            <span className="font-mono break-all">{email}</span>
          </div>
        )}

        <div className="text-xs text-black/60 mb-8 leading-relaxed">
          Need help? Email{' '}
          <a href={mailto} className="font-bold text-black underline hover:text-[#E83354] transition-colors">
            {SUPPORT_EMAIL}
          </a>{' '}
          with your account email and a short message.
        </div>

        <div className="flex flex-col sm:flex-row gap-3">
          <Link
            to="/"
            className="flex-1 text-[11px] font-bold tracking-[0.15em] uppercase bg-black text-white py-3 hover:bg-[#E83354] transition-colors"
          >
            Back to Store
          </Link>
          <Link
            to="/login"
            className="flex-1 text-[11px] font-bold tracking-[0.15em] uppercase border border-black/20 py-3 hover:border-black transition-colors"
          >
            Try different account
          </Link>
        </div>
      </div>
    </div>
  );
}
