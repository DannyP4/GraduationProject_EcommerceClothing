import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import PageLayout, { PageHero, Section } from '../components/PageLayout';

const SHOP_EMAIL = 'longpd1911@gmail.com';

export default function ContactPage() {
  const { t } = useTranslation();
  const tp = (k) => t(`pages.contact.${k}`);
  const [form, setForm] = useState({ name: '', email: '', message: '' });

  const onChange = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const onSubmit = (e) => {
    e.preventDefault();
    const subject = `[Vesta] ${tp('mailSubject')} — ${form.name}`.trim();
    const body = `${form.message}\n\n— ${form.name} (${form.email})`;
    window.location.href = `mailto:${SHOP_EMAIL}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
  };

  return (
    <PageLayout>
      <PageHero kicker={tp('heroKicker')} title={tp('heroTitle')} subtitle={tp('heroSubtitle')} />

      <Section>
        <div className="grid grid-cols-1 lg:grid-cols-[340px_minmax(0,1fr)] gap-10">
          <div className="space-y-6">
            <InfoBlock label={tp('emailLabel')}>
              <a href={`mailto:${SHOP_EMAIL}`} className="text-[#E83354] hover:underline">{SHOP_EMAIL}</a>
            </InfoBlock>
            <InfoBlock label={tp('addressLabel')}>
              <p className="text-black/70 leading-relaxed">{tp('address')}</p>
            </InfoBlock>
            <InfoBlock label={tp('hoursLabel')}>
              <p className="text-black/70 leading-relaxed whitespace-pre-line">{tp('hours')}</p>
            </InfoBlock>
          </div>

          <form onSubmit={onSubmit} className="bg-white border border-black/10 p-6 md:p-8 space-y-4">
            <h2 className="font-['Anton'] text-2xl uppercase tracking-tight">{tp('formTitle')}</h2>
            <Field label={tp('nameLabel')} value={form.name} onChange={onChange('name')} required />
            <Field label={tp('emailFieldLabel')} type="email" value={form.email} onChange={onChange('email')} required />
            <div>
              <label className="block text-[10px] font-bold tracking-[0.15em] uppercase text-black/50 mb-1.5">
                {tp('messageLabel')}<span className="text-[#E83354]"> *</span>
              </label>
              <textarea
                required
                rows={5}
                value={form.message}
                onChange={onChange('message')}
                className="w-full border border-black/15 px-3 py-2.5 text-sm focus:outline-none focus:border-black transition-colors resize-y"
              />
            </div>
            <button
              type="submit"
              className="w-full bg-black text-white text-[11px] font-bold tracking-[0.2em] uppercase py-3.5 hover:bg-[#E83354] transition-colors"
            >
              {tp('send')}
            </button>
            <p className="text-[11px] text-black/40 leading-relaxed">{tp('mailHint')}</p>
          </form>
        </div>
      </Section>
    </PageLayout>
  );
}

function InfoBlock({ label, children }) {
  return (
    <div>
      <p className="text-[10px] font-bold tracking-[0.2em] uppercase text-black/40 mb-1.5">{label}</p>
      <div className="text-sm">{children}</div>
    </div>
  );
}

function Field({ label, type = 'text', value, onChange, required }) {
  return (
    <div>
      <label className="block text-[10px] font-bold tracking-[0.15em] uppercase text-black/50 mb-1.5">
        {label}{required && <span className="text-[#E83354]"> *</span>}
      </label>
      <input
        type={type}
        required={required}
        value={value}
        onChange={onChange}
        className="w-full border border-black/15 px-3 py-2.5 text-sm focus:outline-none focus:border-black transition-colors"
      />
    </div>
  );
}
