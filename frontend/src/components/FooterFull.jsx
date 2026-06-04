export default function FooterFull() {
  return (
    <footer className="bg-[#0A0A0A] text-white pt-16 pb-8">
      <div className="max-w-[1440px] mx-auto px-6">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-10 mb-14">
          {/* Brand col */}
          <div className="col-span-2 md:col-span-1">
            <div className="font-['Anton'] text-3xl tracking-widest mb-4">VESTA</div>
            <p className="text-white/50 text-sm leading-relaxed mb-6">
              Campus-born streetwear for the generation that refuses to dress like everyone else.
            </p>
            <div className="flex gap-4">
              {['IG', 'TK', 'TW', 'YT'].map((s) => (
                <a
                  key={s}
                  href="#"
                  className="w-8 h-8 border border-white/20 flex items-center justify-center text-[10px] font-bold tracking-wider text-white/50 hover:border-white/60 hover:text-white transition-all"
                >
                  {s}
                </a>
              ))}
            </div>
          </div>

          {/* Shop col */}
          <div>
            <h4 className="text-[10px] font-bold tracking-[0.2em] uppercase text-white/40 mb-5">Shop</h4>
            <ul className="space-y-3">
              {['New Arrivals', 'Tops', 'Bottoms', 'Outerwear', 'Accessories', 'Sale'].map((l) => (
                <li key={l}>
                  <a href="/shop" className="text-sm text-white/60 hover:text-white transition-colors">{l}</a>
                </li>
              ))}
            </ul>
          </div>

          {/* Info col */}
          <div>
            <h4 className="text-[10px] font-bold tracking-[0.2em] uppercase text-white/40 mb-5">Info</h4>
            <ul className="space-y-3">
              {['About Us', 'Sustainability', 'Size Guide', 'Shipping & Returns', 'FAQ', 'Contact'].map((l) => (
                <li key={l}>
                  <a href="#" className="text-sm text-white/60 hover:text-white transition-colors">{l}</a>
                </li>
              ))}
            </ul>
          </div>

          {/* Newsletter col */}
          <div>
            <h4 className="text-[10px] font-bold tracking-[0.2em] uppercase text-white/40 mb-5">Stay Updated</h4>
            <p className="text-sm text-white/50 mb-4 leading-relaxed">
              Get early access to drops and exclusive campus deals.
            </p>
            <div className="flex">
              <input
                type="email"
                placeholder="your@email.com"
                className="flex-1 bg-white/10 border border-white/20 text-white text-sm px-3 py-2.5 focus:outline-none focus:border-white/50 placeholder:text-white/30"
              />
              <button className="bg-[#E83354] text-white text-[10px] font-bold tracking-wider px-4 hover:bg-[#c82244] transition-colors">
                GO
              </button>
            </div>
          </div>
        </div>

        {/* Bottom row */}
        <div className="border-t border-white/10 pt-6 flex flex-col sm:flex-row justify-between items-center gap-3">
          <p className="text-[11px] text-white/30 tracking-wider">
            © 2026 VESTA. All rights reserved.
          </p>
          <div className="flex gap-6">
            {['Privacy Policy', 'Terms of Service', 'Cookie Policy'].map((l) => (
              <a key={l} href="#" className="text-[11px] text-white/30 hover:text-white/60 tracking-wider transition-colors">
                {l}
              </a>
            ))}
          </div>
        </div>
      </div>
    </footer>
  );
}
