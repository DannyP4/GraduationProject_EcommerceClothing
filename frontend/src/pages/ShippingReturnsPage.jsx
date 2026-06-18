import { useTranslation } from 'react-i18next';
import PageLayout, { PageHero, Section } from '../components/PageLayout';

export default function ShippingReturnsPage() {
  const { t } = useTranslation();
  const tp = (k, opts) => t(`pages.shipping.${k}`, opts);
  const shipItems = tp('shipItems', { returnObjects: true });
  const returnItems = tp('returnItems', { returnObjects: true });

  return (
    <PageLayout>
      <PageHero kicker={tp('heroKicker')} title={tp('heroTitle')} subtitle={tp('heroSubtitle')} />

      <Section title={tp('shipTitle')} kicker={tp('shipKicker')} className="max-w-4xl">
        <Blocks items={shipItems} />
      </Section>

      <div className="bg-white">
        <Section title={tp('returnTitle')} kicker={tp('returnKicker')} className="max-w-4xl">
          <Blocks items={returnItems} />
        </Section>
      </div>
    </PageLayout>
  );
}

function Blocks({ items }) {
  const list = Array.isArray(items) ? items : [];
  return (
    <div className="space-y-6">
      {list.map((it, i) => (
        <div key={i} className="border-l-2 border-[#E83354] pl-5">
          <h3 className="font-bold uppercase tracking-wider text-sm mb-1.5">{it.title}</h3>
          <p className="text-sm text-black/65 leading-relaxed whitespace-pre-line">{it.desc}</p>
        </div>
      ))}
    </div>
  );
}
