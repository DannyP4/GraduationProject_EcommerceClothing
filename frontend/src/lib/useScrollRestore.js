import { useEffect, useRef } from 'react';
import { useLocation, useNavigationType } from 'react-router-dom';

export default function useScrollRestore(ready) {
  const { key, pathname } = useLocation();
  const navType = useNavigationType();
  const storageKey = `scroll:${pathname}:${key}`;
  const restoring = useRef(navType === 'POP');
  const done = useRef(false);

  useEffect(() => {
    let ticking = false;
    const save = () => {
      if (restoring.current || ticking) return;
      ticking = true;
      requestAnimationFrame(() => {
        try { sessionStorage.setItem(storageKey, String(window.scrollY)); } catch {}
        ticking = false;
      });
    };
    window.addEventListener('scroll', save, { passive: true });
    return () => window.removeEventListener('scroll', save);
  }, [storageKey]);

  useEffect(() => {
    if (navType !== 'POP') { restoring.current = false; return; }
    if (!ready || done.current) return;
    done.current = true;
    const saved = sessionStorage.getItem(storageKey);
    const y = saved != null ? Number(saved) : 0;
    requestAnimationFrame(() => {
      window.scrollTo(0, y);
      requestAnimationFrame(() => { restoring.current = false; });
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ready, navType, storageKey]);
}
