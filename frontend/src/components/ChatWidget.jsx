import { useEffect, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { sendChatMessage } from '../services/chatService';
import useChatHistory from '../hooks/useChatHistory';
import useAutoHideScrollbar from '../lib/useAutoHideScrollbar';

function formatPrice(value, currency) {
  if (value == null) return '';
  const num = Number(value);
  if (currency === 'USD') return `$${num.toFixed(2)}`;
  return `${num.toLocaleString('vi-VN')} ₫`;
}

function renderInline(text) {
  return text.split('**').map((part, i) =>
    i % 2 === 1 ? <strong key={i}>{part}</strong> : <span key={i}>{part}</span>
  );
}

function renderRich(text) {
  return (text || '').split('\n').map((line, i) => {
    const bullet = line.match(/^\s*[-*•]\s+(.*)$/);
    if (bullet) {
      return (
        <div key={i} className="flex gap-1.5">
          <span className="text-[#E83354] leading-5">•</span>
          <span className="flex-1">{renderInline(bullet[1])}</span>
        </div>
      );
    }
    if (line.trim() === '') return <div key={i} className="h-1.5" />;
    return <p key={i}>{renderInline(line)}</p>;
  });
}

function Bubble({ role, error, children }) {
  const isUser = role === 'user';
  const tone = isUser
    ? 'bg-[#0A0A0A] text-white'
    : error
      ? 'bg-[#E83354]/10 text-[#E83354] border border-[#E83354]/30'
      : 'bg-white text-black border border-black/10';
  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div className={`max-w-[85%] px-3 py-2 text-sm whitespace-pre-wrap break-words ${tone}`}>
        {children}
      </div>
    </div>
  );
}

function ProductRow({ product }) {
  const price = product.salePrice != null ? product.salePrice : product.basePrice;
  return (
    <Link
      to={`/product/${product.slug || product.id}`}
      className="flex gap-3 p-2 bg-white border border-black/10 hover:border-[#E83354] transition-colors"
    >
      <div className="w-12 h-14 flex-shrink-0 overflow-hidden bg-black/5">
        {product.primaryImageUrl ? (
          <img src={product.primaryImageUrl} alt={product.name} className="w-full h-full object-cover" />
        ) : null}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs font-bold uppercase tracking-wide truncate">{product.name}</p>
        <p className="text-[10px] text-black/40 truncate">{product.categoryName}</p>
        <p className="text-xs font-bold text-[#E83354] mt-0.5">{formatPrice(price, product.currency)}</p>
      </div>
    </Link>
  );
}

export default function ChatWidget() {
  const { pathname } = useLocation();
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const { messages, append, clear } = useChatHistory();
  const endRef = useRef(null);
  const inputRef = useRef(null);
  const msgScrollRef = useAutoHideScrollbar();

  useEffect(() => {
    if (open) endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading, open]);

  useEffect(() => {
    if (open) inputRef.current?.focus();
  }, [open]);

  if (pathname.startsWith('/admin')) return null;

  const send = async () => {
    const text = input.trim();
    if (!text || loading) return;
    const priorHistory = messages.map((m) => ({ role: m.role, content: m.content }));
    append({ role: 'user', content: text });
    setInput('');
    setLoading(true);
    try {
      const data = await sendChatMessage(text, priorHistory);
      append({ role: 'assistant', content: data?.reply || '', products: data?.products || [] });
    } catch {
      append({
        role: 'assistant',
        content: t('chat.error'),
        error: true,
      });
    } finally {
      setLoading(false);
    }
  };

  const onKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  };

  return (
    <>
      <div
        aria-hidden={!open}
        className={`fixed bottom-24 right-6 z-[95] w-[384px] max-w-[calc(100vw-3rem)] h-[600px] max-h-[calc(100vh-7rem)] flex flex-col bg-white border border-black/10 shadow-2xl origin-bottom-right transition-all duration-200 ease-out ${open ? 'scale-100 opacity-100' : 'scale-90 opacity-0 pointer-events-none'}`}
      >
          <div className="flex items-center justify-between px-4 py-3 bg-[#0A0A0A] text-white">
            <div>
              <p className="text-[11px] font-bold tracking-[0.18em] uppercase">{t('chat.title')}</p>
              <p className="text-[10px] text-white/50">{t('chat.subtitle')}</p>
            </div>
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={clear}
                aria-label={t('chat.clear')}
                title={t('chat.clear')}
                className="text-white/60 hover:text-white transition-colors"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <polyline points="3 6 5 6 21 6" />
                  <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                </svg>
              </button>
              <button
                type="button"
                onClick={() => setOpen(false)}
                aria-label={t('chat.minimize')}
                title={t('chat.minimize')}
                className="text-white/60 hover:text-white transition-colors"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <line x1="5" y1="12" x2="19" y2="12" />
                </svg>
              </button>
            </div>
          </div>

          <div ref={msgScrollRef} className="flex-1 overflow-y-auto scrollbar-subtle px-3 py-3 space-y-3 bg-[#F7F7F7]">
            {messages.length === 0 && (
              <Bubble role="assistant">
                {t('chat.intro')}
              </Bubble>
            )}
            {messages.map((m) => (
              <div key={m.id} className="space-y-2">
                <Bubble role={m.role} error={m.error}>{renderRich(m.content)}</Bubble>
                {m.role === 'assistant' && m.products && m.products.length > 0 && (
                  <div className="space-y-2">
                    {m.products.map((p) => (
                      <ProductRow key={p.id} product={p} />
                    ))}
                  </div>
                )}
              </div>
            ))}
            {loading && (
              <Bubble role="assistant">
                <span className="inline-flex gap-1 py-1">
                  <span className="w-1.5 h-1.5 bg-black/40 rounded-full animate-bounce" />
                  <span className="w-1.5 h-1.5 bg-black/40 rounded-full animate-bounce [animation-delay:0.15s]" />
                  <span className="w-1.5 h-1.5 bg-black/40 rounded-full animate-bounce [animation-delay:0.3s]" />
                </span>
              </Bubble>
            )}
            <div ref={endRef} />
          </div>

          <div className="flex items-center gap-2 border-t border-black/10 p-2 bg-white">
            <input
              ref={inputRef}
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={onKeyDown}
              placeholder={t('chat.placeholder')}
              maxLength={1000}
              className="flex-1 px-3 py-2 text-sm border border-black/10 focus:outline-none focus:border-[#E83354]"
            />
            <button
              type="button"
              onClick={send}
              disabled={loading || !input.trim()}
              aria-label={t('chat.send')}
              className="w-9 h-9 flex-shrink-0 flex items-center justify-center bg-[#0A0A0A] text-white hover:bg-[#E83354] disabled:opacity-40 transition-colors"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="22" y1="2" x2="11" y2="13" />
                <polygon points="22 2 15 22 11 13 2 9 22 2" />
              </svg>
            </button>
          </div>
        </div>

      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-label={open ? t('chat.close') : t('chat.open')}
        title={t('chat.launcher')}
        className="fixed bottom-6 right-6 z-[95] w-14 h-14 rounded-full flex items-center justify-center bg-[#0A0A0A] text-white shadow-lg hover:bg-[#E83354] hover:-translate-y-0.5 transition-all duration-300"
      >
        {open ? (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        ) : (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
        </svg>
        )}
      </button>
    </>
  );
}
