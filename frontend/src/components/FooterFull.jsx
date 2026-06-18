import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

const SHOP_LINKS = ['newArrivals', 'tops', 'bottoms', 'outerwear', 'accessories', 'sale'];
const INFO_LINKS = [
  { key: 'aboutUs', to: '/about' },
  { key: 'sustainability' },
  { key: 'sizeGuide' },
  { key: 'shippingReturns', to: '/shipping-returns' },
  { key: 'faq', to: '/faq' },
  { key: 'contact', to: '/contact' },
];
const LEGAL_LINKS = ['privacy', 'terms', 'cookies'];

export default function FooterFull() {
  const { t } = useTranslation();
  return (
    <footer className="bg-[#0A0A0A] text-white pt-16 pb-8">
      <div className="max-w-[1440px] mx-auto px-6">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-10 mb-14">
          {/* Brand col */}
          <div className="col-span-2 md:col-span-1">
            <div className="font-['Anton'] text-3xl tracking-widest mb-4">VESTA</div>
            <p className="text-white/50 text-sm leading-relaxed mb-6">
              {t('footer.tagline')}
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
            <h4 className="text-[10px] font-bold tracking-[0.2em] uppercase text-white/40 mb-5">{t('footer.shop')}</h4>
            <ul className="space-y-3">
              {SHOP_LINKS.map((k) => (
                <li key={k}>
                  <Link to="/shop" className="text-sm text-white/60 hover:text-white transition-colors">{t(`footer.links.${k}`)}</Link>
                </li>
              ))}
            </ul>
          </div>

          {/* Info col */}
          <div>
            <h4 className="text-[10px] font-bold tracking-[0.2em] uppercase text-white/40 mb-5">{t('footer.info')}</h4>
            <ul className="space-y-3">
              {INFO_LINKS.map(({ key, to }) => (
                <li key={key}>
                  {to ? (
                    <Link to={to} className="text-sm text-white/60 hover:text-white transition-colors">{t(`footer.links.${key}`)}</Link>
                  ) : (
                    <a href="#" className="text-sm text-white/60 hover:text-white transition-colors">{t(`footer.links.${key}`)}</a>
                  )}
                </li>
              ))}
            </ul>
          </div>

          {/* Newsletter col */}
          <div>
            <h4 className="text-[10px] font-bold tracking-[0.2em] uppercase text-white/40 mb-5">{t('footer.stayUpdated')}</h4>
            <p className="text-sm text-white/50 mb-4 leading-relaxed">
              {t('footer.newsletterHint')}
            </p>
            <div className="flex">
              <input
                type="email"
                placeholder={t('footer.emailPlaceholder')}
                className="flex-1 bg-white/10 border border-white/20 text-white text-sm px-3 py-2.5 focus:outline-none focus:border-white/50 placeholder:text-white/30"
              />
              <button className="bg-[#E83354] text-white text-[10px] font-bold tracking-wider px-4 hover:bg-[#c82244] transition-colors">
                {t('footer.subscribe')}
              </button>
            </div>
          </div>
        </div>

        {/* Bottom row */}
        <div className="border-t border-white/10 pt-6 flex flex-col sm:flex-row justify-between items-center gap-3">
          <p className="text-[11px] text-white/30 tracking-wider">
            {t('footer.rights')}
          </p>
          <div className="flex gap-6">
            {LEGAL_LINKS.map((k) => (
              <a key={k} href="#" className="text-[11px] text-white/30 hover:text-white/60 tracking-wider transition-colors">
                {t(`footer.links.${k}`)}
              </a>
            ))}
          </div>
        </div>
      </div>
    </footer>
  );
}
