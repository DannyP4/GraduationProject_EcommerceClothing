import { Link, useSearchParams } from 'react-router-dom';
import AnnouncementBar from '../components/AnnouncementBar';
import NavbarGlass from '../components/NavbarGlass';
import FooterFull from '../components/FooterFull';

export default function StripeCancelPage() {
  const [searchParams] = useSearchParams();
  const orderNumber = searchParams.get('order');

  return (
    <div className="min-h-screen bg-[#E8E8E8]">
      <AnnouncementBar />
      <NavbarGlass />
      <div className="max-w-[720px] mx-auto px-6 py-20">
        <div className="bg-white p-10 text-center">
          <p className="text-[10px] font-bold tracking-[0.25em] uppercase text-black/40 mb-3">Stripe · Test Mode</p>
          <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight mb-6">Payment Cancelled</h1>

          <p className="bg-black/5 border-l-4 border-black/30 px-4 py-3 text-sm text-left mb-6">
            You cancelled the Stripe payment. Your order is still saved as <b>pending</b> — you can retry payment or
            cancel it from your account. Stock has been temporarily reserved.
          </p>

          {orderNumber && (
            <p className="text-xs text-black/50 mb-8">
              Order <span className="font-bold">{orderNumber}</span>
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
        </div>
      </div>
      <FooterFull />
    </div>
  );
}
