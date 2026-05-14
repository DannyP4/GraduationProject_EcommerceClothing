import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import AnnouncementBar from '../components/AnnouncementBar';
import NavbarGlass from '../components/NavbarGlass';
import FooterFull from '../components/FooterFull';
import * as orderService from '../services/orderService';

const POLL_INTERVAL_MS = 2000;
const POLL_TIMEOUT_MS = 20000;

export default function StripeSuccessPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [state, setState] = useState({ status: 'polling', message: 'Confirming payment with Stripe…', order: null });
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    // poll until webhook flips order to PAID, or timeout.
    const orderNumber = searchParams.get('order') || searchParams.get('orderNumber');
    if (!orderNumber) {
      setState({ status: 'failed', message: 'Missing order reference in redirect URL.', order: null });
      return;
    }

    const deadline = Date.now() + POLL_TIMEOUT_MS;
    let cancelled = false;

    const poll = async () => {
      while (!cancelled && Date.now() < deadline) {
        try {
          const order = await orderService.getOrder(orderNumber);
          if (cancelled) return;
          if (order?.status === 'PAID') {
            setState({ status: 'success', message: 'Payment confirmed.', order });
            return;
          }
          if (order?.status === 'CANCELLED') {
            setState({ status: 'failed', message: 'Order was cancelled.', order });
            return;
          }
        } catch (err) {
          if (cancelled) return;
          setState({ status: 'failed', message: err?.message || 'Could not check order status.', order: null });
          return;
        }
        await new Promise((r) => setTimeout(r, POLL_INTERVAL_MS));
      }
      if (cancelled) return;
      setState((s) => ({
        status: 'pending',
        message: 'Stripe is still processing. Refresh your order in a minute.',
        order: s.order,
      }));
    };
    poll();

    return () => { cancelled = true; };
  }, [searchParams]);

  const orderNumber = state.order?.orderNumber ?? searchParams.get('order');

  return (
    <div className="min-h-screen bg-[#E8E8E8]">
      <AnnouncementBar />
      <NavbarGlass />
      <div className="max-w-[720px] mx-auto px-6 py-20">
        <div className="bg-white p-10 text-center">
          <p className="text-[10px] font-bold tracking-[0.25em] uppercase text-black/40 mb-3">Stripe · Test Mode</p>
          <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight mb-6">
            {state.status === 'polling' && 'Confirming…'}
            {state.status === 'success' && 'Payment Successful'}
            {state.status === 'pending' && 'Still Processing'}
            {state.status === 'failed' && 'Verification Failed'}
          </h1>

          <p className="text-sm text-black/70 mb-2">{state.message}</p>
          {orderNumber && (
            <p className="text-xs text-black/50 mb-8">
              Order <span className="font-bold">{orderNumber}</span>
            </p>
          )}

          <div className="flex gap-3 justify-center">
            {orderNumber && (
              <button
                onClick={() => navigate(`/account/orders/${orderNumber}`, { replace: true })}
                className="bg-[#E83354] text-white text-[12px] font-bold tracking-[0.15em] uppercase px-8 py-3 hover:bg-[#c82244] transition-colors"
              >
                View Order
              </button>
            )}
            <Link
              to="/shop"
              className="border border-black text-black text-[12px] font-bold tracking-[0.15em] uppercase px-8 py-3 hover:bg-black hover:text-white transition-colors"
            >
              Continue Shopping
            </Link>
          </div>
        </div>
      </div>
      <FooterFull />
    </div>
  );
}
