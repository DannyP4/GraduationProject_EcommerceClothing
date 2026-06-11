import Turnstile from 'react-turnstile';

const SITE_KEY = import.meta.env.VITE_TURNSTILE_SITE_KEY;

export const captchaEnabled = Boolean(SITE_KEY);

export default function CaptchaWidget({ onToken }) {
  if (!SITE_KEY) return null;
  return (
    <div className="my-1">
      <Turnstile
        sitekey={SITE_KEY}
        theme="light"
        onVerify={(token) => onToken(token)}
        onExpire={() => onToken('')}
        onError={() => onToken('')}
      />
    </div>
  );
}
