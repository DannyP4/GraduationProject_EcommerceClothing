import { useState } from 'react';

function Star({ size, className }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" className={className}>
      <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
    </svg>
  );
}

export default function StarRating({ value = 0, max = 5, size = 16, interactive = false, onChange, className = '' }) {
  const [hover, setHover] = useState(0);

  if (interactive) {
    const shown = hover || value;
    return (
      <div className={`inline-flex items-center gap-0.5 ${className}`} onMouseLeave={() => setHover(0)}>
        {Array.from({ length: max }, (_, i) => {
          const idx = i + 1;
          return (
            <button
              key={idx}
              type="button"
              onClick={() => onChange?.(idx)}
              onMouseEnter={() => setHover(idx)}
              className="p-0.5 leading-none"
              aria-label={`${idx} star${idx > 1 ? 's' : ''}`}
            >
              <Star size={size} className={idx <= shown ? 'text-amber-500' : 'text-black/15'} />
            </button>
          );
        })}
      </div>
    );
  }

  return (
    <span className={`inline-flex items-center gap-0.5 ${className}`} aria-label={`${value} out of ${max} stars`}>
      {Array.from({ length: max }, (_, i) => {
        const fill = Math.max(0, Math.min(1, value - i)) * 100;
        return (
          <span key={i} className="relative inline-block leading-none" style={{ width: size, height: size }}>
            <Star size={size} className="text-black/15 absolute inset-0" />
            <span className="absolute inset-0 overflow-hidden" style={{ width: `${fill}%` }}>
              <Star size={size} className="text-amber-500" />
            </span>
          </span>
        );
      })}
    </span>
  );
}
