import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import PageLayout, { PageHero, Section } from '../components/PageLayout';

const SHOTS = [
  'https://images.unsplash.com/photo-1490481651871-ab68de25d43d?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1445205170230-053b83016050?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1469334031218-e382a71b716b?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1539109136881-3be0616acf4b?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1525507119028-ed4c629a60a3?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1551232864-3f0890e580d9?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1496747611176-843222e1e57c?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1485462537746-965f33f7f6a7?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1509631179647-0177331693ae?auto=format&fit=crop&w=900&q=80',
  'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=900&q=80',
];

export default function LookbookPage() {
  const { t } = useTranslation();
  const tp = (k, opts) => t(`pages.lookbook.${k}`, opts);
  const captions = tp('items', { returnObjects: true });
  const caps = Array.isArray(captions) ? captions : [];

  return (
    <PageLayout>
      <PageHero kicker={tp('heroKicker')} title={tp('heroTitle')} subtitle={tp('heroSubtitle')} />

      <Section>
        <div className="columns-1 sm:columns-2 lg:columns-3 gap-5 [column-fill:_balance]">
          {SHOTS.map((src, i) => (
            <figure key={src} className="mb-5 break-inside-avoid group relative overflow-hidden bg-black">
              <img
                src={src}
                alt={caps[i]?.caption || ''}
                className="w-full object-cover group-hover:opacity-85 transition-opacity duration-300"
                loading="lazy"
              />
              {caps[i]?.caption && (
                <figcaption className="absolute bottom-0 left-0 right-0 p-4 bg-gradient-to-t from-black/70 to-transparent text-white text-[11px] font-bold tracking-[0.2em] uppercase">
                  {caps[i].caption}
                </figcaption>
              )}
            </figure>
          ))}
        </div>

        <div className="text-center mt-8">
          <Link
            to="/shop"
            className="inline-block bg-black text-white text-[11px] font-bold tracking-[0.2em] uppercase px-8 py-4 hover:bg-[#E83354] transition-colors"
          >
            {tp('cta')}
          </Link>
        </div>
      </Section>
    </PageLayout>
  );
}
