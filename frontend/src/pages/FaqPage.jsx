import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import PageLayout, { PageHero, Section } from '../components/PageLayout';

export default function FaqPage() {
  const { t } = useTranslation();
  const tp = (k, opts) => t(`pages.faq.${k}`, opts);
  const items = tp('items', { returnObjects: true });
  const list = Array.isArray(items) ? items : [];
  const [open, setOpen] = useState(0);

  return (
    <PageLayout>
      <PageHero kicker={tp('heroKicker')} title={tp('heroTitle')} subtitle={tp('heroSubtitle')} />

      <Section className="max-w-3xl">
        <ul className="divide-y divide-black/10 border-y border-black/10">
          {list.map((it, i) => {
            const isOpen = open === i;
            return (
              <li key={i}>
                <button
                  type="button"
                  onClick={() => setOpen(isOpen ? -1 : i)}
                  className="w-full flex items-center justify-between gap-4 py-5 text-left"
                  aria-expanded={isOpen}
                >
                  <span className="font-bold uppercase tracking-wider text-sm">{it.q}</span>
                  <span className={`text-[#E83354] text-xl leading-none transition-transform ${isOpen ? 'rotate-45' : ''}`} aria-hidden>+</span>
                </button>
                {isOpen && (
                  <p className="pb-5 text-sm text-black/65 leading-relaxed whitespace-pre-line">{it.a}</p>
                )}
              </li>
            );
          })}
        </ul>
      </Section>
    </PageLayout>
  );
}
