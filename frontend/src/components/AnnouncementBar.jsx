import { useState } from 'react';
import { useTranslation } from 'react-i18next';

export default function AnnouncementBar() {
  const { t } = useTranslation();
  const [dismissed, setDismissed] = useState(false);
  if (dismissed) return null;

  const text = t('announcement.ticker');

  return (
    <div className="bg-black text-white text-[11px] font-semibold tracking-[0.15em] uppercase py-2 overflow-hidden relative flex items-center">
      <div className="flex-1 overflow-hidden">
        <span
          className="inline-block whitespace-nowrap"
          style={{ animation: 'ticker 25s linear infinite' }}
        >
          {text.repeat(4)}
        </span>
      </div>
      <button
        onClick={() => setDismissed(true)}
        className="flex-shrink-0 px-4 text-white/60 hover:text-white transition-colors text-lg leading-none"
        aria-label={t('announcement.dismiss')}
      >
        ×
      </button>
    </div>
  );
}
