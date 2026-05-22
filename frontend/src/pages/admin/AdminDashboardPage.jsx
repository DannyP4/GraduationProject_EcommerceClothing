import { Link } from 'react-router-dom';
import {
  LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid,
} from 'recharts';
import StatCard from '../../components/admin/StatCard';
import OrderStatusBadge from '../../components/admin/OrderStatusBadge';
import {
  DASHBOARD_WIDGETS,
  LAST_7_DAYS_REVENUE,
  RECENT_ORDERS_PREVIEW,
} from '../../mocks/adminDashboard';

export default function AdminDashboardPage() {
  const w = DASHBOARD_WIDGETS;

  return (
    <div className="space-y-8">
      <div>
        <p className="text-[10px] font-bold tracking-[0.25em] uppercase text-black/40 mb-1">Today · 21 May 2026</p>
        <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight">Dashboard</h1>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          label="Today's Revenue"
          value={w.todayRevenue.value}
          currency={w.todayRevenue.currency}
          delta={w.todayRevenue.delta}
          deltaLabel={w.todayRevenue.deltaLabel}
          accent
        />
        <StatCard
          label="Pending Orders"
          value={w.pendingOrders.value}
          delta={w.pendingOrders.delta}
          deltaLabel={w.pendingOrders.deltaLabel}
        />
        <StatCard
          label="Out of Stock"
          value={w.outOfStockVariants.value}
          delta={w.outOfStockVariants.delta}
          deltaLabel={w.outOfStockVariants.deltaLabel}
        />
        <StatCard
          label="7-Day Revenue"
          value={w.weekRevenue.value}
          currency={w.weekRevenue.currency}
          delta={w.weekRevenue.delta}
          deltaLabel={w.weekRevenue.deltaLabel}
        />
      </div>

      <section className="border border-black/10 bg-white p-5">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="font-['Anton'] text-xl uppercase tracking-wider">Revenue · Last 7 Days</h2>
            <p className="text-[11px] text-black/40 tracking-wider">Mock data — Phase 4b will wire real API.</p>
          </div>
          <span className="text-[10px] font-bold tracking-[0.2em] uppercase bg-black/5 text-black/40 px-2 py-1">
            VND
          </span>
        </div>
        <div className="h-72 -ml-2">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={LAST_7_DAYS_REVENUE} margin={{ top: 10, right: 20, bottom: 0, left: 10 }}>
              <CartesianGrid stroke="rgba(0,0,0,0.06)" vertical={false} />
              <XAxis
                dataKey="label"
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
                formatter={(v) => [`${v.toLocaleString('vi-VN')} ₫`, 'Revenue']}
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
        <ul className="divide-y divide-black/5">
          {RECENT_ORDERS_PREVIEW.map((o) => (
            <li key={o.orderNumber}>
              <Link
                to={`/admin/orders/${o.orderNumber}`}
                className="flex items-center gap-4 py-3 hover:bg-black/5 -mx-2 px-2 transition-colors"
              >
                <span className="font-bold text-sm tracking-wider flex-1 min-w-0 truncate">{o.orderNumber}</span>
                <span className="hidden sm:inline text-xs text-black/60 flex-1 truncate">{o.customer}</span>
                <OrderStatusBadge status={o.status} />
                <span className="font-['Anton'] text-lg w-32 text-right">
                  {o.grandTotal.toLocaleString('vi-VN')} ₫
                </span>
              </Link>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
