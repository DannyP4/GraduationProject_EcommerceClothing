import { useEffect, useState } from 'react';
import {
  Area, AreaChart, Bar, BarChart, CartesianGrid, Cell, Pie, PieChart,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import * as statsSvc from '../../services/adminStatsService';

const ACCENT = '#E83354';

const PROVIDER_COLORS = {
  COD: '#71717a',
  VNPAY: '#ef4444',
  STRIPE: '#635BFF',
  BANK_TRANSFER: '#0891b2',
};

const STATUS_COLORS = {
  PENDING: '#d97706',
  PAID: '#2563eb',
  PROCESSING: '#3b82f6',
  SHIPPED: '#4f46e5',
  DELIVERED: '#16a34a',
  CANCELLED: '#E83354',
  REFUNDED: '#9333ea',
};

const ORDER_STATUSES = ['PENDING', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED'];

const PRESETS = [
  { label: '7d', days: 6 },
  { label: '30d', days: 29 },
  { label: '90d', days: 89 },
];

function todayISO() {
  return new Date().toISOString().slice(0, 10);
}

function daysAgoISO(n) {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d.toISOString().slice(0, 10);
}

function formatVnd(value) {
  if (value == null) return '0 ₫';
  return `${Number(value).toLocaleString('vi-VN')} ₫`;
}

function formatVndShort(value) {
  if (value == null) return '0';
  const n = Number(value);
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(0)}K`;
  return n.toString();
}

function formatPct(pct) {
  if (pct == null) return null;
  const n = Number(pct);
  const sign = n >= 0 ? '+' : '';
  return `${sign}${n.toFixed(1)}%`;
}

export default function AdminStatsPage() {
  const [from, setFrom] = useState(daysAgoISO(29));
  const [to, setTo] = useState(todayISO());
  const [granularity, setGranularity] = useState('DAY');
  const [refreshKey, setRefreshKey] = useState(0);

  const [summary, setSummary] = useState(null);
  const [revenue, setRevenue] = useState(null);
  const [payment, setPayment] = useState(null);
  const [ordersByStatus, setOrdersByStatus] = useState(null);
  const [topProducts, setTopProducts] = useState(null);
  const [topCustomers, setTopCustomers] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    const params = { from, to };
    Promise.all([
      statsSvc.getSummary(params),
      statsSvc.getRevenue({ ...params, granularity }),
      statsSvc.getPaymentBreakdown(params),
      statsSvc.getOrdersByStatus(params),
      statsSvc.getTopProducts(params),
      statsSvc.getTopCustomers(params),
    ])
      .then(([s, r, p, o, tp, tc]) => {
        if (cancelled) return;
        setSummary(s);
        setRevenue(r);
        setPayment(p);
        setOrdersByStatus(o);
        setTopProducts(tp);
        setTopCustomers(tc);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Failed to load stats');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, [from, to, granularity, refreshKey]);

  const applyPreset = (days) => {
    setFrom(daysAgoISO(days));
    setTo(todayISO());
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight">Stats</h1>
        <p className="text-xs text-black/50 mt-1">Revenue, payments, and orders analytics.</p>
      </div>

      <DateRangeBar
        from={from} to={to} granularity={granularity}
        onFromChange={setFrom} onToChange={setTo}
        onGranularityChange={setGranularity}
        onPreset={applyPreset}
        onRefresh={() => setRefreshKey((k) => k + 1)}
        loading={loading}
      />

      {error && (
        <div className="border border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354] px-4 py-3 text-xs">
          {error}
        </div>
      )}

      <KpiGrid summary={summary} loading={loading} />

      <RevenueChartCard data={revenue ?? []} granularity={granularity} loading={loading} />

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <PaymentBreakdownCard data={payment ?? []} loading={loading} />
        <OrdersByStatusCard data={ordersByStatus ?? []} loading={loading} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <TopProductsCard data={topProducts ?? []} loading={loading} />
        <TopCustomersCard data={topCustomers ?? []} loading={loading} />
      </div>
    </div>
  );
}

function DateRangeBar({ from, to, granularity, onFromChange, onToChange, onGranularityChange, onPreset, onRefresh, loading }) {
  return (
    <div className="bg-white border border-black/10 p-3">
      <div className="flex flex-wrap items-center gap-2">
        <div className="flex items-center gap-2">
          <label className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40">From</label>
          <input
            type="date"
            value={from}
            max={to}
            onChange={(e) => onFromChange(e.target.value)}
            className="border border-black/15 px-2 py-1.5 text-sm focus:border-black focus:outline-none"
          />
        </div>
        <div className="flex items-center gap-2">
          <label className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40">To</label>
          <input
            type="date"
            value={to}
            min={from}
            max={todayISO()}
            onChange={(e) => onToChange(e.target.value)}
            className="border border-black/15 px-2 py-1.5 text-sm focus:border-black focus:outline-none"
          />
        </div>
        <div className="flex gap-1">
          {PRESETS.map((p) => (
            <button
              key={p.label}
              type="button"
              onClick={() => onPreset(p.days)}
              className="text-[10px] font-bold tracking-[0.15em] uppercase border border-black/15 px-3 py-1.5 hover:border-black hover:bg-black hover:text-white transition-colors"
            >
              {p.label}
            </button>
          ))}
        </div>
        <div className="flex items-center gap-2 md:ml-auto">
          <label className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40">Granularity</label>
          <select
            value={granularity}
            onChange={(e) => onGranularityChange(e.target.value)}
            className="border border-black/15 px-2 py-1.5 text-sm focus:border-black focus:outline-none bg-white"
          >
            <option value="DAY">Day</option>
            <option value="WEEK">Week</option>
            <option value="MONTH">Month</option>
          </select>
          <button
            type="button"
            onClick={onRefresh}
            disabled={loading}
            className="text-[10px] font-bold tracking-[0.15em] uppercase border-2 border-black px-3 py-1.5 hover:bg-black hover:text-white transition-colors disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-black"
          >
            {loading ? 'Loading...' : 'Refresh'}
          </button>
        </div>
      </div>
    </div>
  );
}

function KpiGrid({ summary, loading }) {
  const current = summary?.current;
  const changes = summary?.changes;
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <KpiCard
        label="Revenue"
        value={current ? formatVnd(current.revenue) : '—'}
        change={changes?.revenuePct}
        accent
        loading={loading}
      />
      <KpiCard
        label="Orders"
        value={current ? current.orders.toLocaleString('vi-VN') : '—'}
        change={changes?.ordersPct}
        loading={loading}
      />
      <KpiCard
        label="Avg. Order Value"
        value={current ? formatVnd(current.avgOrderValue) : '—'}
        change={changes?.avgOrderValuePct}
        loading={loading}
      />
      <KpiCard
        label="New Customers"
        value={current ? current.newCustomers.toLocaleString('vi-VN') : '—'}
        change={changes?.newCustomersPct}
        loading={loading}
      />
    </div>
  );
}

function KpiCard({ label, value, change, accent, loading }) {
  const pctLabel = formatPct(change);
  const positive = change != null && change >= 0;
  return (
    <div className={`bg-white border ${accent ? 'border-black' : 'border-black/10'} p-4`}>
      <p className="text-[10px] font-bold tracking-[0.2em] uppercase text-black/40">{label}</p>
      <p className={`font-['Anton'] mt-2 ${accent ? 'text-3xl' : 'text-2xl'} tracking-tight`}>
        {loading ? '...' : value}
      </p>
      <div className="mt-2 h-4 text-[11px] tracking-wider">
        {pctLabel != null ? (
          <span className={positive ? 'text-emerald-600' : 'text-[#E83354]'}>
            {positive ? '▲' : '▼'} {pctLabel} vs prev
          </span>
        ) : (
          <span className="text-black/30">— vs prev</span>
        )}
      </div>
    </div>
  );
}

function RevenueChartCard({ data, granularity, loading }) {
  return (
    <section className="border border-black/10 bg-white p-5">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="font-['Anton'] text-xl uppercase tracking-wider">Revenue Over Time</h2>
          <p className="text-[11px] text-black/40 tracking-wider">
            Bucketed by {granularity.toLowerCase()} · revenue-status orders only
          </p>
        </div>
        <span className="text-[10px] font-bold tracking-[0.2em] uppercase bg-black/5 text-black/40 px-2 py-1">VND</span>
      </div>
      <div className="h-72 -ml-2">
        {loading && data.length === 0 ? (
          <div className="h-full flex items-center justify-center text-sm text-black/30">Loading...</div>
        ) : data.length === 0 ? (
          <div className="h-full flex items-center justify-center text-sm text-black/40">No revenue in this range.</div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={data} margin={{ top: 10, right: 20, bottom: 0, left: 10 }}>
              <defs>
                <linearGradient id="revGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor={ACCENT} stopOpacity={0.3} />
                  <stop offset="100%" stopColor={ACCENT} stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid stroke="rgba(0,0,0,0.06)" vertical={false} />
              <XAxis
                dataKey="bucket"
                tick={{ fontSize: 11, fill: 'rgba(0,0,0,0.4)' }}
                axisLine={{ stroke: 'rgba(0,0,0,0.08)' }}
                tickLine={false}
              />
              <YAxis
                tick={{ fontSize: 11, fill: 'rgba(0,0,0,0.4)' }}
                axisLine={false}
                tickLine={false}
                tickFormatter={formatVndShort}
              />
              <Tooltip
                contentStyle={{ border: '1px solid rgba(0,0,0,0.1)', borderRadius: 0, fontSize: 12 }}
                formatter={(v, name) => name === 'revenue' ? [formatVnd(v), 'Revenue'] : [v, 'Orders']}
              />
              <Area
                type="monotone"
                dataKey="revenue"
                stroke={ACCENT}
                strokeWidth={2.5}
                fill="url(#revGrad)"
              />
            </AreaChart>
          </ResponsiveContainer>
        )}
      </div>
    </section>
  );
}

function PaymentBreakdownCard({ data, loading }) {
  return (
    <section className="border border-black/10 bg-white p-5">
      <h2 className="font-['Anton'] text-xl uppercase tracking-wider mb-4">Payment Providers</h2>
      <div className="h-64">
        {loading && data.length === 0 ? (
          <div className="h-full flex items-center justify-center text-sm text-black/30">Loading...</div>
        ) : data.length === 0 ? (
          <div className="h-full flex items-center justify-center text-sm text-black/40">No payments in this range.</div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={data}
                dataKey="revenue"
                nameKey="provider"
                cx="50%"
                cy="50%"
                innerRadius={50}
                outerRadius={90}
                paddingAngle={2}
              >
                {data.map((d) => (
                  <Cell key={d.provider} fill={PROVIDER_COLORS[d.provider] ?? '#999'} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{ border: '1px solid rgba(0,0,0,0.1)', borderRadius: 0, fontSize: 12 }}
                formatter={(v) => [formatVnd(v), 'Revenue']}
              />
            </PieChart>
          </ResponsiveContainer>
        )}
      </div>
      {data.length > 0 && (
        <ul className="mt-3 space-y-1.5">
          {data.map((d) => (
            <li key={d.provider} className="flex items-center gap-2 text-xs">
              <span className="w-3 h-3 inline-block" style={{ background: PROVIDER_COLORS[d.provider] ?? '#999' }} />
              <span className="font-bold tracking-wider uppercase text-[10px] flex-1">{d.provider}</span>
              <span className="text-black/70">{formatVnd(d.revenue)}</span>
              <span className="text-black/40 w-12 text-right">{Number(d.pct).toFixed(1)}%</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function OrdersByStatusCard({ data, loading }) {
  const sorted = ORDER_STATUSES.map((s) => data.find((d) => d.status === s)).filter(Boolean);
  return (
    <section className="border border-black/10 bg-white p-5">
      <h2 className="font-['Anton'] text-xl uppercase tracking-wider mb-4">Orders by Status</h2>
      <div className="h-64">
        {loading && sorted.length === 0 ? (
          <div className="h-full flex items-center justify-center text-sm text-black/30">Loading...</div>
        ) : sorted.length === 0 ? (
          <div className="h-full flex items-center justify-center text-sm text-black/40">No orders in this range.</div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={sorted} margin={{ top: 10, right: 10, bottom: 0, left: 0 }}>
              <CartesianGrid stroke="rgba(0,0,0,0.06)" vertical={false} />
              <XAxis
                dataKey="status"
                tick={{ fontSize: 9, fill: 'rgba(0,0,0,0.5)' }}
                axisLine={{ stroke: 'rgba(0,0,0,0.08)' }}
                tickLine={false}
                interval={0}
              />
              <YAxis
                allowDecimals={false}
                tick={{ fontSize: 11, fill: 'rgba(0,0,0,0.4)' }}
                axisLine={false}
                tickLine={false}
              />
              <Tooltip
                contentStyle={{ border: '1px solid rgba(0,0,0,0.1)', borderRadius: 0, fontSize: 12 }}
                formatter={(v, _name, payload) => [`${v} orders · ${formatVnd(payload?.payload?.revenue)}`, payload?.payload?.status]}
              />
              <Bar dataKey="count" radius={[2, 2, 0, 0]}>
                {sorted.map((d) => (
                  <Cell key={d.status} fill={STATUS_COLORS[d.status] ?? '#999'} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>
    </section>
  );
}

function TopProductsCard({ data, loading }) {
  return (
    <section className="border border-black/10 bg-white p-5">
      <h2 className="font-['Anton'] text-xl uppercase tracking-wider mb-4">Top Products · By Revenue</h2>
      {loading && data.length === 0 ? (
        <p className="text-sm text-black/30 py-8 text-center">Loading...</p>
      ) : data.length === 0 ? (
        <p className="text-sm text-black/40 py-8 text-center">No sales in this range.</p>
      ) : (
        <ul className="divide-y divide-black/5">
          {data.map((p, i) => (
            <li key={p.productId} className="flex items-center gap-3 py-2.5">
              <span className="font-['Anton'] text-lg text-black/30 w-6">{i + 1}</span>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-bold truncate">{p.productName}</p>
                <p className="text-[11px] text-black/40">{p.unitsSold} units sold</p>
              </div>
              <span className="font-['Anton'] text-base">{formatVnd(p.revenue)}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function TopCustomersCard({ data, loading }) {
  return (
    <section className="border border-black/10 bg-white p-5">
      <h2 className="font-['Anton'] text-xl uppercase tracking-wider mb-4">Top Customers · By Spent</h2>
      {loading && data.length === 0 ? (
        <p className="text-sm text-black/30 py-8 text-center">Loading...</p>
      ) : data.length === 0 ? (
        <p className="text-sm text-black/40 py-8 text-center">No customer activity in this range.</p>
      ) : (
        <ul className="divide-y divide-black/5">
          {data.map((c, i) => (
            <li key={c.userId} className="flex items-center gap-3 py-2.5">
              <span className="font-['Anton'] text-lg text-black/30 w-6">{i + 1}</span>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-bold truncate">{c.fullName || c.email}</p>
                <p className="text-[11px] text-black/40 truncate">{c.email} · {c.orderCount} orders</p>
              </div>
              <span className="font-['Anton'] text-base">{formatVnd(c.totalSpent)}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
