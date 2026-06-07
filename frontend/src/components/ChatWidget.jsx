import { useEffect, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { sendChatMessage } from '../services/chatService';
import useChatHistory from '../hooks/useChatHistory';

function formatPrice(value, currency) {
  if (value == null) return '';
  const num = Number(value);
  if (currency === 'USD') return `$${num.toFixed(2)}`;
  return `${num.toLocaleString('vi-VN')} ₫`;
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
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const { messages, append, clear } = useChatHistory();
  const endRef = useRef(null);
  const inputRef = useRef(null);

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
        content: "Sorry, I'm having trouble right now. Please try again in a moment.",
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
      {open && (
        <div className="fixed bottom-24 right-6 z-[95] w-[360px] max-w-[calc(100vw-3rem)] max-h-[70vh] flex flex-col bg-white border border-black/10 shadow-2xl">
          <div className="flex items-center justify-between px-4 py-3 bg-[#0A0A0A] text-white">
            <div>
              <p className="text-[11px] font-bold tracking-[0.18em] uppercase">Vesta Assistant</p>
              <p className="text-[10px] text-white/50">Ask about our products</p>
            </div>
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={clear}
                aria-label="Clear chat"
                title="Clear chat"
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
                aria-label="Close chat"
                title="Close"
                className="text-white/60 hover:text-white transition-colors"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
          </div>

          <div className="flex-1 overflow-y-auto px-3 py-3 space-y-3 bg-[#F7F7F7]">
            {messages.length === 0 && (
              <Bubble role="assistant">
                Hi! I&apos;m Vesta&apos;s shopping assistant. Ask about our products - e.g. &quot;black jacket under 500k&quot; - or our shipping &amp; returns policy.
              </Bubble>
            )}
            {messages.map((m) => (
              <div key={m.id} className="space-y-2">
                <Bubble role={m.role} error={m.error}>{m.content}</Bubble>
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
              placeholder="Type your message…"
              maxLength={1000}
              className="flex-1 px-3 py-2 text-sm border border-black/10 focus:outline-none focus:border-[#E83354]"
            />
            <button
              type="button"
              onClick={send}
              disabled={loading || !input.trim()}
              aria-label="Send"
              className="w-9 h-9 flex-shrink-0 flex items-center justify-center bg-[#0A0A0A] text-white hover:bg-[#E83354] disabled:opacity-40 transition-colors"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="22" y1="2" x2="11" y2="13" />
                <polygon points="22 2 15 22 11 13 2 9 22 2" />
              </svg>
            </button>
          </div>
        </div>
      )}

      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-label={open ? 'Close chat' : 'Open chat'}
        title="Chat with Vesta"
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
