import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid,
} from 'recharts';
import StatCard from '../../components/admin/StatCard';
import OrderStatusBadge from '../../components/admin/OrderStatusBadge';
import * as statsSvc from '../../services/adminStatsService';
import { listOrders } from '../../services/adminOrderService';

function todayISO() {
  return new Date().toISOString().slice(0, 10);
}

function daysAgoISO(n) {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d.toISOString().slice(0, 10);
}

// StatCard expects a fraction (0.18 -> +18%); the API returns a percentage (18.0).
function pctToDelta(pct) {
  return pct != null ? pct / 100 : undefined;
}

export default function AdminDashboardPage() {
  const [today, setToday] = useState(null);
  const [week, setWeek] = useState(null);
  const [ops, setOps] = useState(null);
  const [revenue, setRevenue] = useState([]);
  const [recentOrders, setRecentOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    const t = todayISO();
    const weekAgo = daysAgoISO(6);
    Promise.all([
      statsSvc.getSummary({ from: t, to: t }),
      statsSvc.getSummary({ from: weekAgo, to: t }),
      statsSvc.getOps(),
      statsSvc.getRevenue({ from: weekAgo, to: t, granularity: 'DAY' }),
      listOrders({ page: 0, size: 5 }),
    ])
      .then(([todaySummary, weekSummary, opsData, revenueData, ordersPage]) => {
        if (cancelled) return;
        setToday(todaySummary);
        setWeek(weekSummary);
        setOps(opsData);
        setRevenue(revenueData ?? []);
        setRecentOrders(ordersPage?.content ?? []);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Failed to load dashboard');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, []);

  const headerDate = new Date().toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });

  return (
    <div className="space-y-8">
      <div>
        <p className="text-[10px] font-bold tracking-[0.25em] uppercase text-black/40 mb-1">Today · {headerDate}</p>
        <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight">Dashboard</h1>
      </div>

      {error && (
        <div className="border border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354] px-4 py-3 text-xs">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          label="Today's Revenue"
          value={today?.current?.revenue ?? 0}
          currency="VND"
          delta={pctToDelta(today?.changes?.revenuePct)}
          deltaLabel="vs yesterday"
          accent
        />
        <StatCard
          label="Open Orders"
          value={ops?.openOrders ?? 0}
        />
        <StatCard
          label={`Low Stock (≤${ops?.lowStockThreshold ?? 5})`}
          value={ops?.lowStock ?? 0}
        />
        <StatCard
          label="7-Day Revenue"
          value={week?.current?.revenue ?? 0}
          currency="VND"
          delta={pctToDelta(week?.changes?.revenuePct)}
          deltaLabel="vs prior 7d"
        />
      </div>

      <section className="border border-black/10 bg-white p-5">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="font-['Anton'] text-xl uppercase tracking-wider">Revenue · Last 7 Days</h2>
            <p className="text-[11px] text-black/40 tracking-wider">Revenue-status orders, bucketed by day.</p>
          </div>
          <span className="text-[10px] font-bold tracking-[0.2em] uppercase bg-black/5 text-black/40 px-2 py-1">
            VND
          </span>
        </div>
        <div className="h-72 -ml-2">
          {loading && revenue.length === 0 ? (
            <div className="h-full flex items-center justify-center text-sm text-black/30">Loading...</div>
          ) : revenue.length === 0 ? (
            <div className="h-full flex items-center justify-center text-sm text-black/40">No revenue in the last 7 days.</div>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={revenue} margin={{ top: 10, right: 20, bottom: 0, left: 10 }}>
                <CartesianGrid stroke="rgba(0,0,0,0.06)" vertical={false} />
                <XAxis
                  dataKey="bucket"
                  tickFormatter={(v) => (v ? v.slice(5) : v)}
                  tick={{ fontSize: 11, fill: 'rgba(0,0,0,0.4)' }}
                  axisLine={{ stroke: 'rgba(0,0,0,0.08)' }}
                  tickLine={false}
                />
                <YAxis
                  tick={{ fontSize: 11, fill: 'rgba(0,0,0,0.4)' }}
                  axisLine={false}
                  tickLine={false}
                  tickFormatter={(v) => `${(v / 1_000_000).toFixed(1)}M`}
                />
                <Tooltip
                  contentStyle={{ border: '1px solid rgba(0,0,0,0.1)', borderRadius: 0, fontSize: 12 }}
                  formatter={(v) => [`${Number(v).toLocaleString('vi-VN')} ₫`, 'Revenue']}
                />
                <Line
                  type="monotone"
                  dataKey="revenue"
                  stroke="#E83354"
                  strokeWidth={2.5}
                  dot={{ r: 4, fill: '#E83354', strokeWidth: 0 }}
                  activeDot={{ r: 6, fill: '#0A0A0A' }}
                />
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>
      </section>

      <section className="border border-black/10 bg-white p-5">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-['Anton'] text-xl uppercase tracking-wider">Recent Orders</h2>
          <Link
            to="/admin/orders"
            className="text-[11px] font-bold tracking-[0.15em] uppercase text-black/50 hover:text-[#E83354] transition-colors"
          >
            View all →
          </Link>
        </div>
        {loading && recentOrders.length === 0 ? (
          <p className="text-sm text-black/30 py-6 text-center">Loading...</p>
        ) : recentOrders.length === 0 ? (
          <p className="text-sm text-black/40 py-6 text-center">No orders yet.</p>
        ) : (
          <ul className="divide-y divide-black/5">
            {recentOrders.map((o) => (
              <li key={o.orderNumber}>
                <Link
                  to={`/admin/orders/${o.orderNumber}`}
                  className="flex items-center gap-4 py-3 hover:bg-black/5 -mx-2 px-2 transition-colors"
                >
                  <span className="font-bold text-sm tracking-wider flex-1 min-w-0 truncate">{o.orderNumber}</span>
                  <span className="hidden sm:inline text-xs text-black/60 flex-1 truncate">
                    {o.customer?.fullName || o.customer?.email || '—'}
                  </span>
                  <OrderStatusBadge status={o.status} />
                  <span className="font-['Anton'] text-lg w-32 text-right">
                    {Number(o.grandTotal ?? 0).toLocaleString('vi-VN')} ₫
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
