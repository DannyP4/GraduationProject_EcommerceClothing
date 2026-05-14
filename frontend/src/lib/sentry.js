import * as Sentry from '@sentry/react';

const DSN = import.meta.env.VITE_SENTRY_DSN_FRONTEND;

export function initSentry() {
  if (!DSN) {
    if (import.meta.env.DEV) {
      console.info('[sentry] DSN missing — error reporting disabled.');
    }
    return;
  }
  Sentry.init({
    dsn: DSN,
    environment: import.meta.env.MODE,
    tracesSampleRate: 0,
    sendDefaultPii: false,
  });
}

export { Sentry };
