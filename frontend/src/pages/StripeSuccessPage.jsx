import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import AnnouncementBar from '../components/AnnouncementBar';
import NavbarGlass from '../components/NavbarGlass';
import FooterFull from '../components/FooterFull';
import * as orderService from '../services/orderService';

const POLL_INTERVAL_MS = 2000;
const POLL_TIMEOUT_MS = 20000;

export default function StripeSuccessPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [state, setState] = useState({
    status: 'polling',
    messageKey: 'paymentReturn.stripe.messages.confirming',
    order: null,
  });
  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    // poll until webhook flips order to PAID, or timeout.
    const orderNumber = searchParams.get('order') || searchParams.get('orderNumber');
    if (!orderNumber) {
      setState({ status: 'failed', messageKey: 'paymentReturn.stripe.messages.missingOrder', order: null });
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
            setState({ status: 'success', messageKey: 'paymentReturn.stripe.messages.confirmed', order });
            return;
          }
          if (order?.status === 'CANCELLED') {
            setState({ status: 'failed', messageKey: 'paymentReturn.stripe.messages.cancelledOrder', order });
            return;
          }
        } catch {
          if (cancelled) return;
          setState({ status: 'failed', messageKey: 'paymentReturn.stripe.messages.checkFailed', order: null });
          return;
        }
        await new Promise((r) => setTimeout(r, POLL_INTERVAL_MS));
      }
      if (cancelled) return;
      setState((s) => ({
        status: 'pending',
        messageKey: 'paymentReturn.stripe.messages.stillProcessing',
        order: s.order,
      }));
    };
    poll();

    return () => { cancelled = true; };
  }, [searchParams]);

  const orderNumber = state.order?.orderNumber ?? searchParams.get('order');
  const titleKeyByStatus = {
    polling: 'paymentReturn.stripe.title.polling',
    success: 'paymentReturn.stripe.title.success',
    pending: 'paymentReturn.stripe.title.pending',
    failed: 'paymentReturn.stripe.title.failed',
  };

  return (
    <div className="min-h-screen bg-[#E8E8E8]">
      <AnnouncementBar />
      <NavbarGlass />
      <div className="max-w-[720px] mx-auto px-6 py-20">
        <div className="bg-white p-10 text-center">
          <p className="text-[10px] font-bold tracking-[0.25em] uppercase text-black/40 mb-3">
            {t('paymentReturn.stripe.eyebrow')}
          </p>
          <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight mb-6">
            {t(titleKeyByStatus[state.status])}
          </h1>

          <p className="text-sm text-black/70 mb-2">{t(state.messageKey)}</p>
          {orderNumber && (
            <p className="text-xs text-black/50 mb-8">
              {t('paymentReturn.common.order')} <span className="font-bold">{orderNumber}</span>
            </p>
          )}

          <div className="flex gap-3 justify-center">
            {orderNumber && (
              <button
                onClick={() => navigate(`/account/orders/${orderNumber}`, { replace: true })}
                className="bg-[#E83354] text-white text-[12px] font-bold tracking-[0.15em] uppercase px-8 py-3 hover:bg-[#c82244] transition-colors"
              >
                {t('paymentReturn.common.viewOrder')}
              </button>
            )}
            <Link
              to="/shop"
              className="border border-black text-black text-[12px] font-bold tracking-[0.15em] uppercase px-8 py-3 hover:bg-black hover:text-white transition-colors"
            >
              {t('paymentReturn.common.continueShopping')}
            </Link>
          </div>
        </div>
      </div>
      <FooterFull />
    </div>
  );
}
