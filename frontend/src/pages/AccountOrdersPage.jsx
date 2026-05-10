export default function AccountOrdersPage() {
  return (
    <div className="text-center py-20">
      <h2 className="font-['Anton'] text-4xl uppercase tracking-tight mb-3">Order History</h2>
      <p className="text-sm text-black/50 max-w-md mx-auto">
        Your past orders will appear here once checkout is available. We're still wiring up the
        order &amp; payment flow — check back soon.
      </p>
      <div className="mt-8 inline-block border border-black/10 px-6 py-4">
        <p className="text-[10px] font-bold tracking-[0.2em] uppercase text-black/40 mb-1">Coming next</p>
        <p className="text-xs text-black/70">Phase 3b · Checkout, COD, VNPAY, Stripe</p>
      </div>
    </div>
  );
}
