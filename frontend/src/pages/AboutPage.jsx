import { useTranslation } from 'react-i18next';
import PageLayout, { PageHero, Section } from '../components/PageLayout';

const HERO_IMAGE = 'https://images.unsplash.com/photo-1490481651871-ab68de25d43d?auto=format&fit=crop&w=1600&q=80';
const STORY_IMAGE = 'https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=1200&q=80';

export default function AboutPage() {
  const { t } = useTranslation();
  const tp = (k, opts) => t(`pages.about.${k}`, opts);
  const values = tp('values', { returnObjects: true }) || [];

  return (
    <PageLayout>
      <PageHero kicker={tp('heroKicker')} title={tp('heroTitle')} subtitle={tp('heroSubtitle')} image={HERO_IMAGE} />

      <Section kicker={tp('storyKicker')} title={tp('storyTitle')}>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-10 items-center">
          <div className="space-y-4 text-sm md:text-base text-black/70 leading-relaxed">
            <p>{tp('storyP1')}</p>
            <p>{tp('storyP2')}</p>
          </div>
          <img src={STORY_IMAGE} alt="" className="w-full aspect-[4/5] object-cover" loading="lazy" />
        </div>
      </Section>

      <div className="bg-white">
        <Section kicker={tp('valuesKicker')} title={tp('valuesTitle')}>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {Array.isArray(values) && values.map((v, i) => (
              <div key={i} className="border border-black/10 p-6">
                <span className="font-['Anton'] text-4xl text-[#E83354]">{String(i + 1).padStart(2, '0')}</span>
                <h3 className="font-bold uppercase tracking-wider text-sm mt-3 mb-2">{v.title}</h3>
                <p className="text-sm text-black/60 leading-relaxed">{v.desc}</p>
              </div>
            ))}
          </div>
        </Section>
      </div>

      <Section kicker={tp('projectKicker')} title={tp('projectTitle')}>
        <div className="border border-black/10 bg-white p-6 md:p-8 max-w-2xl">
          <p className="text-sm text-black/70 leading-relaxed mb-6">{tp('projectBody')}</p>
          <dl className="space-y-3 text-sm">
            <Row label={tp('authorLabel')} value="Phạm Đức Long" />
            <Row label={tp('studentIdLabel')} value="20225737" />
            <Row label={tp('schoolLabel')} value={tp('school')} />
            <Row
              label={tp('emailLabel')}
              value={<a href="mailto:longpd1911@gmail.com" className="text-[#E83354] hover:underline">longpd1911@gmail.com</a>}
            />
          </dl>
        </div>
      </Section>
    </PageLayout>
  );
}

function Row({ label, value }) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-baseline gap-1 sm:gap-4">
      <dt className="w-40 flex-shrink-0 text-[10px] font-bold tracking-[0.15em] uppercase text-black/40">{label}</dt>
      <dd className="font-medium text-black/80">{value}</dd>
    </div>
  );
}
