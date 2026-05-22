export const DASHBOARD_WIDGETS = {
  todayRevenue: { value: 8_640_000, currency: 'VND', delta: +0.182, deltaLabel: 'vs yesterday' },
  pendingOrders: { value: 3, delta: +0.5, deltaLabel: 'vs avg last 7d' },
  outOfStockVariants: { value: 7, delta: -0.143, deltaLabel: 'vs last week' },
  weekRevenue: { value: 41_290_000, currency: 'VND', delta: +0.094, deltaLabel: 'vs prior 7d' },
};

export const LAST_7_DAYS_REVENUE = [
  { date: '2026-05-15', label: 'May 15', revenue: 4_120_000 },
  { date: '2026-05-16', label: 'May 16', revenue: 5_870_000 },
  { date: '2026-05-17', label: 'May 17', revenue: 3_210_000 },
  { date: '2026-05-18', label: 'May 18', revenue: 6_950_000 },
  { date: '2026-05-19', label: 'May 19', revenue: 7_400_000 },
  { date: '2026-05-20', label: 'May 20', revenue: 5_100_000 },
  { date: '2026-05-21', label: 'May 21', revenue: 8_640_000 },
];

export const RECENT_ORDERS_PREVIEW = [
  { orderNumber: 'UNF-20260521-0007', customer: 'Nguyễn Minh An', status: 'PENDING', grandTotal: 890000 },
  { orderNumber: 'UNF-20260521-0006', customer: 'Phạm Hoàng Long', status: 'PENDING', grandTotal: 1290000 },
  { orderNumber: 'UNF-20260520-0014', customer: 'Đỗ Quang Huy', status: 'PAID', grandTotal: 1780000 },
  { orderNumber: 'UNF-20260520-0011', customer: 'Lê Bảo Châu', status: 'PAID', grandTotal: 2890000 },
  { orderNumber: 'UNF-20260520-0008', customer: 'Vũ Thanh Hà', status: 'PROCESSING', grandTotal: 1640000 },
];
