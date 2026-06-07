import { useCallback, useState } from 'react';

const KEY = 'chat.history';
let seq = 0;

function nextId() {
  seq += 1;
  return `${Date.now()}-${seq}`;
}

function load() {
  try {
    const raw = sessionStorage.getItem(KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function persist(messages) {
  try {
    sessionStorage.setItem(KEY, JSON.stringify(messages));
  } catch {
    /* ignore quota, serialization errors */
  }
}

export default function useChatHistory() {
  const [messages, setMessages] = useState(load);

  const append = useCallback((message) => {
    setMessages((prev) => {
      const next = [...prev, { id: nextId(), ...message }];
      persist(next);
      return next;
    });
  }, []);

  const clear = useCallback(() => {
    setMessages([]);
    persist([]);
  }, []);

  return { messages, append, clear };
}
