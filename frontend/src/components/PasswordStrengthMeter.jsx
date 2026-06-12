import { useTranslation } from 'react-i18next';

// UX hint only — actual policy is enforced by BE @Pattern. Inline scoring avoids zxcvbn (~30 KB gzip).
export default function PasswordStrengthMeter({ value }) {
  const { t } = useTranslation();
  if (!value) return null;
  const score = computeScore(value);
  const labels = [
    t('auth.strength.tooWeak'),
    t('auth.strength.weak'),
    t('auth.strength.fair'),
    t('auth.strength.strong'),
    t('auth.strength.veryStrong'),
  ];
  const colors = ['bg-[#E83354]', 'bg-orange-500', 'bg-yellow-500', 'bg-green-500', 'bg-green-700'];
  return (
    <div className="mt-2">
      <div className="flex gap-1 mb-1">
        {[0, 1, 2, 3, 4].map((i) => (
          <div
            key={i}
            className={`h-1 flex-1 ${i <= score ? colors[score] : 'bg-black/10'}`}
          />
        ))}
      </div>
      <p className="text-[10px] text-black/50 tracking-wider">{labels[score]}</p>
    </div>
  );
}

function computeScore(pw) {
  let s = 0;
  if (pw.length >= 8) s += 1;
  if (pw.length >= 12) s += 1;
  if (/[A-Z]/.test(pw) && /[a-z]/.test(pw)) s += 1;
  if (/\d/.test(pw)) s += 1;
  if (/[^A-Za-z0-9]/.test(pw)) s += 1;
  return Math.min(4, Math.max(0, s - 1));
}
