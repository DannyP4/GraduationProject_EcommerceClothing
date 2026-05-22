const STYLES = {
  PENDING:    'bg-amber-50    text-amber-700  border-amber-300',
  PAID:       'bg-blue-50     text-blue-700   border-blue-300',
  PROCESSING: 'bg-blue-50     text-blue-700   border-blue-300',
  SHIPPED:    'bg-indigo-50   text-indigo-700 border-indigo-300',
  DELIVERED:  'bg-green-50    text-green-700  border-green-400',
  CANCELLED:  'bg-[#E83354]/10 text-[#E83354] border-[#E83354]/50',
  REFUNDED:   'bg-purple-50   text-purple-700 border-purple-300',
  CAPTURED:   'bg-green-50    text-green-700  border-green-400',
  FAILED:     'bg-[#E83354]/10 text-[#E83354] border-[#E83354]/50',
};

export default function OrderStatusBadge({ status, size = 'sm' }) {
  const cls = STYLES[status] ?? 'bg-black/5 text-black/60 border-black/15';
  const sizeCls = size === 'md'
    ? 'text-[11px] px-2.5 py-1'
    : 'text-[10px] px-2 py-0.5';
  return (
    <span className={`font-bold tracking-wider uppercase border ${sizeCls} ${cls}`}>
      {status}
    </span>
  );
}
