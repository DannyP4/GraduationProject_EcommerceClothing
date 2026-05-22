export default function StatCard({ label, value, currency, delta, deltaLabel, accent = false }) {
  const isPositive = typeof delta === 'number' && delta >= 0;
  const deltaPct = typeof delta === 'number' ? `${isPositive ? '+' : ''}${(delta * 100).toFixed(1)}%` : null;

  return (
    <div className={`p-5 border ${accent ? 'bg-[#0A0A0A] text-white border-black' : 'bg-white border-black/10'}`}>
      <p className={`text-[10px] font-bold tracking-[0.2em] uppercase mb-3 ${accent ? 'text-white/50' : 'text-black/40'}`}>
        {label}
      </p>
      <p className={`font-['Anton'] text-4xl tracking-tight ${accent ? 'text-white' : 'text-black'}`}>
        {currency ? formatVnd(value) : value.toLocaleString('vi-VN')}
      </p>
      {deltaPct && (
        <p className="mt-3 flex items-center gap-2 text-[11px] tracking-wider">
          <span className={`font-bold ${isPositive ? 'text-green-600' : 'text-[#E83354]'}`}>
            {isPositive ? '▲' : '▼'} {deltaPct}
          </span>
          <span className={accent ? 'text-white/40' : 'text-black/40'}>{deltaLabel}</span>
        </p>
      )}
    </div>
  );
}

function formatVnd(value) {
  return `${Number(value).toLocaleString('vi-VN')} ₫`;
}
