import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import PageLayout, { PageHero, Section } from '../components/PageLayout';

const COLLECTIONS = [
  { q: 'jacket', image: 'https://images.unsplash.com/photo-1551232864-3f0890e580d9?auto=format&fit=crop&w=1000&q=80' },
  { q: 'shirt', image: 'https://images.unsplash.com/photo-1469334031218-e382a71b716b?auto=format&fit=crop&w=1000&q=80' },
  { q: 'dress', image: 'https://images.unsplash.com/photo-1539109136881-3be0616acf4b?auto=format&fit=crop&w=1000&q=80' },
  { q: 'jeans', image: 'https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&w=1000&q=80' },
];

export default function CollectionsPage() {
  const { t } = useTranslation();
  const tp = (k, opts) => t(`pages.collections.${k}`, opts);
  const items = tp('items', { returnObjects: true });
  const list = Array.isArray(items) ? items : [];

  return (
    <PageLayout>
      <PageHero kicker={tp('heroKicker')} title={tp('heroTitle')} subtitle={tp('heroSubtitle')} />

      <Section>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
          {COLLECTIONS.map((c, i) => {
            const meta = list[i] || {};
            return (
              <Link
                key={c.q}
                to={`/shop?q=${encodeURIComponent(c.q)}`}
                className="group relative block aspect-[16/10] overflow-hidden bg-black"
              >
                <img
                  src={c.image}
                  alt={meta.title || c.q}
                  className="absolute inset-0 w-full h-full object-cover opacity-80 group-hover:opacity-60 group-hover:scale-105 transition-all duration-500"
                  loading="lazy"
                />
                <div className="absolute inset-0 flex flex-col justify-end p-6 text-white">
                  <h3 className="font-['Anton'] text-3xl md:text-4xl uppercase tracking-tight">{meta.title}</h3>
                  <p className="text-sm text-white/70 mt-1">{meta.desc}</p>
                  <span className="mt-3 text-[11px] font-bold tracking-[0.2em] uppercase inline-flex items-center gap-1">
                    {tp('shopNow')} <span aria-hidden>→</span>
                  </span>
                </div>
              </Link>
            );
          })}
        </div>
      </Section>
    </PageLayout>
  );
}
