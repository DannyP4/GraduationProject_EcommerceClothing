import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { getNotifications } from '../../services/adminNotificationService';
import useAutoHideScrollbar from '../../lib/useAutoHideScrollbar';

const TYPE_META = {
  ORDER:  { label: 'Order',  cls: 'text-blue-700   bg-blue-50    border-blue-300',   icon: IconBox },
  STOCK:  { label: 'Stock',  cls: 'text-amber-700  bg-amber-50   border-amber-300',  icon: IconAlert },
  REVIEW: { label: 'Review', cls: 'text-purple-700 bg-purple-50  border-purple-300', icon: IconStar },
};

const SEEN_KEY = 'vesta_admin_notif_seen';
const POLL_MS = 60000;

function loadSeen() {
  try { return new Set(JSON.parse(localStorage.getItem(SEEN_KEY)) || []); }
  catch { return new Set(); }
}
function saveSeen(set) {
  try { localStorage.setItem(SEEN_KEY, JSON.stringify([...set])); } catch { /* ignore */ }
}

export default function AdminNotificationBell() {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [seen, setSeen] = useState(loadSeen);
  const wrapRef = useRef(null);
  const closeTimerRef = useRef(null);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const data = await getNotifications();
        if (cancelled) return;
        const list = Array.isArray(data) ? data : [];
        setItems(list);
        setSeen((prev) => {
          const ids = new Set(list.map((n) => n.id));
          const pruned = new Set([...prev].filter((id) => ids.has(id)));
          saveSeen(pruned);
          return pruned;
        });
      } catch { /* keep last feed on transient error */ }
    };
    load();
    const timer = setInterval(load, POLL_MS);
    return () => { cancelled = true; clearInterval(timer); };
  }, []);

  const unreadCount = items.filter((n) => !seen.has(n.id)).length;

  const cancelClose = () => {
    if (closeTimerRef.current) { clearTimeout(closeTimerRef.current); closeTimerRef.current = null; }
  };
  const scheduleClose = () => {
    cancelClose();
    closeTimerRef.current = setTimeout(() => setOpen(false), 200);
  };
  const openPanel = () => { cancelClose(); setOpen(true); };
  const markSeen = (id) => {
    setSeen((prev) => {
      if (prev.has(id)) return prev;
      const next = new Set(prev);
      next.add(id);
      saveSeen(next);
      return next;
    });
  };
  const markAllSeen = () => {
    setSeen((prev) => {
      const next = new Set([...prev, ...items.map((n) => n.id)]);
      saveSeen(next);
      return next;
    });
  };

  useEffect(() => () => cancelClose(), []);

  return (
    <div
      ref={wrapRef}
      className="relative hidden sm:block"
      onMouseEnter={openPanel}
      onMouseLeave={scheduleClose}
    >
      <button
        type="button"
        aria-label="Admin notifications"
        title="Notifications · Refreshes every minute"
        className="relative text-black/70 hover:text-[#E83354] transition-all hover:-translate-y-0.5 block"
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>
        {unreadCount > 0 && (
          <span className="absolute -top-2 -right-2 bg-[#E83354] text-white text-[9px] font-bold w-4 h-4 rounded-full flex items-center justify-center">
            {unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full mt-2 w-96 bg-white border border-black/10 shadow-xl z-50 max-h-[480px] flex flex-col">
          <div className="px-4 py-3 border-b border-black/5 flex items-center justify-between flex-shrink-0">
            <p className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40">Notifications</p>
            <span className="flex items-center gap-1.5">
              {unreadCount > 0 && (
                <span className="text-[10px] font-bold tracking-wider uppercase bg-[#E83354] text-white px-2 py-0.5">
                  {unreadCount} new
                </span>
              )}
              <span className="text-[10px] font-bold tracking-wider uppercase bg-black text-white px-2 py-0.5">
                {items.length}
              </span>
              {unreadCount > 0 && (
                <button
                  type="button"
                  onClick={markAllSeen}
                  className="text-[10px] font-bold tracking-wider uppercase text-[#E83354] hover:underline ml-1"
                >
                  Mark all read
                </button>
              )}
            </span>
          </div>

          {items.length === 0 ? (
            <p className="px-4 py-8 text-center text-xs text-black/40 tracking-wide flex-1">
              You're all caught up.
            </p>
          ) : (
            <NotificationList
              items={items}
              seen={seen}
              onItemClick={(id) => { markSeen(id); setOpen(false); }}
            />
          )}
        </div>
      )}
    </div>
  );
}

function NotificationList({ items, seen, onItemClick }) {
  const listRef = useAutoHideScrollbar();
  return (
    <ul ref={listRef} className="overflow-y-auto divide-y divide-black/5 flex-1 scrollbar-subtle">
      {items.map((n) => {
        const meta = TYPE_META[n.type] ?? { label: n.type, cls: 'text-black/60 bg-black/5 border-black/15', icon: IconBox };
        const Icon = meta.icon;
        const isUnread = !seen.has(n.id);
        return (
          <li key={n.id}>
            <Link
              to={n.href}
              onClick={() => onItemClick(n.id)}
              className={`flex gap-3 px-4 py-3 hover:bg-black/5 transition-colors ${isUnread ? '' : 'opacity-60'}`}
            >
              <span className={`w-7 h-7 flex-shrink-0 border flex items-center justify-center ${meta.cls}`}>
                <Icon />
              </span>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-0.5">
                  <span className={`text-[9px] font-bold tracking-wider uppercase px-1.5 py-0.5 border ${meta.cls}`}>
                    {meta.label}
                  </span>
                  {isUnread && <span className="w-1.5 h-1.5 rounded-full bg-[#E83354]" />}
                </div>
                <p className="text-xs text-black/80 leading-snug">{n.message}</p>
                {n.at && <p className="text-[10px] text-black/40 mt-1 tracking-wider">{timeAgo(n.at)}</p>}
              </div>
            </Link>
          </li>
        );
      })}
    </ul>
  );
}

function timeAgo(iso) {
  if (!iso) return '';
  const now = Date.now();
  const t = new Date(iso).getTime();
  const diffMin = Math.max(1, Math.round((now - t) / 60000));
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffH = Math.round(diffMin / 60);
  if (diffH < 24) return `${diffH}h ago`;
  const diffD = Math.round(diffH / 24);
  return `${diffD}d ago`;
}

function IconBox() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
      <polyline points="3.27 6.96 12 12.01 20.73 6.96" /><line x1="12" y1="22.08" x2="12" y2="12" />
    </svg>
  );
}
function IconAlert() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
      <line x1="12" y1="9" x2="12" y2="13" /><line x1="12" y1="17" x2="12.01" y2="17" />
    </svg>
  );
}
function IconStar() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
    </svg>
  );
}
