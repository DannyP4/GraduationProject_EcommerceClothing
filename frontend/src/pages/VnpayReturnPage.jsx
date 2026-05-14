import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import AnnouncementBar from '../components/AnnouncementBar';
import NavbarGlass from '../components/NavbarGlass';
import FooterFull from '../components/FooterFull';
import * as paymentService from '../services/paymentService';

export default function VnpayReturnPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [state, setState] = useState({ status: 'verifying', message: 'Verifying your payment…', order: null });

  useEffect(() => {
    let cancelled = false;
    const params = {};
    for (const [k, v] of searchParams.entries()) params[k] = v;

    if (Object.keys(params).length === 0) {
      setState({ status: 'failed', message: 'Missing payment parameters.', order: null });
      return;
    }

    paymentService.verifyVnpayReturn(params)
      .then((result) => {
        if (cancelled) return;
        setState({
          status: result?.success ? 'success' : 'failed',
          message: result?.message || (result?.success ? 'Payment confirmed.' : 'Payment failed.'),
          order: result?.order ?? null,
        });
      })
      .catch((err) => {
        if (!cancelled) {
          setState({
            status: 'failed',
            message: err?.message || 'Verification failed.',
            order: null,
          });
        }
      });
    return () => { cancelled = true; };
  }, [searchParams]);

  const orderNumber = state.order?.orderNumber ?? searchParams.get('vnp_TxnRef');

  return (
    <div className="min-h-screen bg-[#E8E8E8]">
      <AnnouncementBar />
      <NavbarGlass />
      <div className="max-w-[720px] mx-auto px-6 py-20">
        <div className="bg-white p-10 text-center">
          <p className="text-[10px] font-bold tracking-[0.25em] uppercase text-black/40 mb-3">VNPAY · Sandbox</p>
          <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight mb-6">
            {state.status === 'verifying' && 'Verifying…'}
            {state.status === 'success' && 'Payment Confirmed'}
            {state.status === 'failed' && 'Payment Not Completed'}
          </h1>

          {state.status === 'verifying' && (
            <p className="text-sm text-black/60">Please wait while we confirm with VNPAY.</p>
          )}

          {state.status === 'success' && (
            <>
              <p className="text-sm text-black/70 mb-2">{state.message}</p>
              {orderNumber && (
                <p className="text-xs text-black/50 mb-8">
                  Order <span className="font-bold">{orderNumber}</span> is now paid.
                </p>
              )}
              <div className="flex gap-3 justify-center">
                <button
                  onClick={() => navigate(`/account/orders/${orderNumber}`, { replace: true })}
                  className="bg-[#E83354] text-white text-[12px] font-bold tracking-[0.15em] uppercase px-8 py-3 hover:bg-[#c82244] transition-colors"
                >
                  View Order
                </button>
                <Link
                  to="/shop"
                  className="border border-black text-black text-[12px] font-bold tracking-[0.15em] uppercase px-8 py-3 hover:bg-black hover:text-white transition-colors"
                >
                  Continue Shopping
                </Link>
              </div>
            </>
          )}

          {state.status === 'failed' && (
            <>
              <p className="bg-[#E83354]/15 border-l-4 border-[#E83354] px-4 py-3 text-sm text-left mb-6">
                {state.message}
              </p>
              {orderNumber && (
                <p className="text-xs text-black/50 mb-8">
                  Order <span className="font-bold">{orderNumber}</span> is still pending. You can retry payment or cancel from your account.
                </p>
              )}
              <div className="flex gap-3 justify-center">
                {orderNumber && (
                  <Link
                    to={`/account/orders/${orderNumber}`}
                    className="bg-black text-white text-[12px] font-bold tracking-[0.15em] uppercase px-8 py-3 hover:bg-[#E83354] transition-colors"
                  >
                    View Order
                  </Link>
                )}
                <Link
                  to="/cart"
                  className="border border-black text-black text-[12px] font-bold tracking-[0.15em] uppercase px-8 py-3 hover:bg-black hover:text-white transition-colors"
                >
                  Back to Cart
                </Link>
              </div>
            </>
          )}
        </div>
      </div>
      <FooterFull />
    </div>
  );
}
