import { useEffect, useRef } from 'react';

// scrollbar will be hidden when not scrolling
export default function useAutoHideScrollbar(idleMs = 700) {
  const ref = useRef(null);
  useEffect(() => {
    const el = ref.current;
    if (!el) return undefined;
    let timer;
    const onScroll = () => {
      el.classList.add('is-scrolling');
      clearTimeout(timer);
      timer = setTimeout(() => el.classList.remove('is-scrolling'), idleMs);
    };
    el.addEventListener('scroll', onScroll, { passive: true });
    return () => {
      el.removeEventListener('scroll', onScroll);
      clearTimeout(timer);
    };
  }, [idleMs]);
  return ref;
}
