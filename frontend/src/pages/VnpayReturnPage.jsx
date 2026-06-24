import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import AnnouncementBar from '../components/AnnouncementBar';
import NavbarGlass from '../components/NavbarGlass';
import FooterFull from '../components/FooterFull';
import * as paymentService from '../services/paymentService';

export default function VnpayReturnPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [state, setState] = useState({
    status: 'verifying',
    messageKey: 'paymentReturn.vnpay.messages.verifying',
    order: null,
  });

  useEffect(() => {
    let cancelled = false;
    const params = {};
    for (const [k, v] of searchParams.entries()) params[k] = v;

    if (Object.keys(params).length === 0) {
      setState({ status: 'failed', messageKey: 'paymentReturn.vnpay.messages.missingParams', order: null });
      return;
    }

    paymentService.verifyVnpayReturn(params)
      .then((result) => {
        if (cancelled) return;
        setState({
          status: result?.success ? 'success' : 'failed',
          messageKey: result?.success
            ? 'paymentReturn.vnpay.messages.confirmed'
            : 'paymentReturn.vnpay.messages.failed',
          order: result?.order ?? null,
        });
      })
      .catch(() => {
        if (!cancelled) {
          setState({
            status: 'failed',
            messageKey: 'paymentReturn.vnpay.messages.verificationFailed',
            order: null,
          });
        }
      });
    return () => { cancelled = true; };
  }, [searchParams]);

  const orderNumber = state.order?.orderNumber ?? searchParams.get('vnp_TxnRef');
  const titleKeyByStatus = {
    verifying: 'paymentReturn.vnpay.title.verifying',
    success: 'paymentReturn.vnpay.title.success',
    failed: 'paymentReturn.vnpay.title.failed',
  };

  return (
    <div className="min-h-screen bg-[#E8E8E8]">
      <AnnouncementBar />
      <NavbarGlass />
      <div className="max-w-[720px] mx-auto px-6 py-20">
        <div className="bg-white p-10 text-center">
          <p className="text-[10px] font-bold tracking-[0.25em] uppercase text-black/40 mb-3">
            {t('paymentReturn.vnpay.eyebrow')}
          </p>
          <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight mb-6">
            {t(titleKeyByStatus[state.status])}
          </h1>

          {state.status === 'verifying' && (
            <p className="text-sm text-black/60">{t('paymentReturn.vnpay.messages.waiting')}</p>
          )}

          {state.status === 'success' && (
            <>
              <p className="text-sm text-black/70 mb-2">{t(state.messageKey)}</p>
              {orderNumber && (
                <p className="text-xs text-black/50 mb-8">
                  {t('paymentReturn.vnpay.orderPaid', { orderNumber })}
                </p>
              )}
              <div className="flex gap-3 justify-center">
                <button
                  onClick={() => navigate(`/account/orders/${orderNumber}`, { replace: true })}
                  className="bg-[#E83354] text-white text-[12px] font-bold tracking-[0.15em] uppercase px-8 py-3 hover:bg-[#c82244] transition-colors"
                >
                  {t('paymentReturn.common.viewOrder')}
                </button>
                <Link
                  to="/shop"
                  className="border border-black text-black text-[12px] font-bold tracking-[0.15em] uppercase px-8 py-3 hover:bg-black hover:text-white transition-colors"
                >
                  {t('paymentReturn.common.continueShopping')}
                </Link>
              </div>
            </>
          )}

          {state.status === 'failed' && (
            <>
              <p className="bg-[#E83354]/15 border-l-4 border-[#E83354] px-4 py-3 text-sm text-left mb-6">
                {t(state.messageKey)}
              </p>
              {orderNumber && (
                <p className="text-xs text-black/50 mb-8">
                  {t('paymentReturn.vnpay.orderPending', { orderNumber })}
                </p>
              )}
              <div className="flex gap-3 justify-center">
                {orderNumber && (
                  <Link
                    to={`/account/orders/${orderNumber}`}
                    className="bg-black text-white text-[12px] font-bold tracking-[0.15em] uppercase px-8 py-3 hover:bg-[#E83354] transition-colors"
                  >
                    {t('paymentReturn.common.viewOrder')}
                  </Link>
                )}
                <Link
                  to="/cart"
                  className="border border-black text-black text-[12px] font-bold tracking-[0.15em] uppercase px-8 py-3 hover:bg-black hover:text-white transition-colors"
                >
                  {t('paymentReturn.common.backToCart')}
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
