"use client";

import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  AlertTriangle,
  BarChart3,
  CheckCircle2,
  Clock3,
  Loader2,
  ShieldAlert,
  Users,
} from "lucide-react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import api from "@/lib/api";

type BreakdownSource = "REPORT" | "COMPLAINT";

interface Summary {
  totalSignals: number;
  totalReports: number;
  totalComplaints: number;
  openSignals: number;
  resolvedSignals: number;
  resolveRate: number;
  averageResolutionHours: number;
}

interface TopUser {
  userId: number;
  fullName: string;
  email: string;
  roleName: string;
  reportCount: number;
  complaintCount: number;
  totalCount: number;
}

interface Breakdown {
  source: BreakdownSource;
  key: string;
  count: number;
}

interface TrendPoint {
  date: string;
  reportCount: number;
  complaintCount: number;
  totalCount: number;
}

interface ModerationAnalyticsResponse {
  summary: Summary;
  topUsers: TopUser[];
  categoryBreakdown: Breakdown[];
  statusBreakdown: Breakdown[];
  trend: TrendPoint[];
}

const REPORT_CATEGORIES = [
  "NO_SHOW",
  "PROPERTY_DAMAGE",
  "HARASSMENT",
  "FRAUD",
  "PAYMENT_ABUSE",
  "FAKE_LISTING",
  "OTHER",
];

const COMPLAINT_PRIORITIES = ["LOW", "MEDIUM", "HIGH"];

const REPORT_STATUSES = ["OPEN", "UNDER_REVIEW", "ACTION_TAKEN", "DISMISSED"];

const COMPLAINT_STATUSES = [
  "OPEN",
  "IN_PROGRESS",
  "RESOLVED",
  "ESCALATED",
  "AWAITING_CUSTOMER_RESPONSE",
  "CUSTOMER_WITHDRAWN",
];

const LABELS: Record<string, string> = {
  REPORT: "Report",
  COMPLAINT: "Complaint",
  Customer: "Khach hang",
  Owner: "Chu san",
  NO_SHOW: "Khong den",
  PROPERTY_DAMAGE: "Pha hoai tai san",
  HARASSMENT: "Quay roi",
  FRAUD: "Gian lan",
  PAYMENT_ABUSE: "Lam dung thanh toan",
  FAKE_LISTING: "Listing gia",
  OTHER: "Khac",
  LOW: "Thap",
  MEDIUM: "Trung binh",
  HIGH: "Cao",
  OPEN: "Moi mo",
  UNDER_REVIEW: "Dang xem xet",
  ACTION_TAKEN: "Da xu ly",
  DISMISSED: "Da bac bo",
  IN_PROGRESS: "Dang xu ly",
  RESOLVED: "Da giai quyet",
  ESCALATED: "Da escalate",
  AWAITING_CUSTOMER_RESPONSE: "Cho Admin review",
  CUSTOMER_WITHDRAWN: "Khach rut don",
};

const todayIso = () => new Date().toISOString().slice(0, 10);

const daysAgoIso = (days: number) => {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return date.toISOString().slice(0, 10);
};

const formatNumber = (value: number) => new Intl.NumberFormat("vi-VN").format(value);

const formatDate = (date: string) => {
  const [year, month, day] = date.split("-");
  return `${day}/${month}/${year}`;
};

const labelOf = (key: string) => LABELS[key] ?? key;

async function fetchAnalytics(params: {
  from: string;
  to: string;
  role: string;
  category: string;
  status: string;
}) {
  const { data } = await api.get<ModerationAnalyticsResponse>("/admin/moderation-analytics", {
    params: {
      from: params.from,
      to: params.to,
      role: params.role === "ALL" ? undefined : params.role,
      category: params.category === "ALL" ? undefined : params.category,
      status: params.status === "ALL" ? undefined : params.status,
      topLimit: 10,
    },
  });
  return data;
}

function KpiCard({
  label,
  value,
  detail,
  icon: Icon,
  tone,
}: {
  label: string;
  value: string;
  detail: string;
  icon: typeof ShieldAlert;
  tone: string;
}) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-slate-500">{label}</p>
          <p className="mt-2 text-2xl font-bold text-slate-900">{value}</p>
          <p className="mt-1 text-xs text-slate-500">{detail}</p>
        </div>
        <div className={`rounded-lg p-2 ${tone}`}>
          <Icon className="h-5 w-5" />
        </div>
      </div>
    </div>
  );
}

function TrendTooltip({
  active,
  payload,
}: {
  active?: boolean;
  payload?: Array<{ payload: TrendPoint }>;
}) {
  if (!active || !payload?.length) return null;
  const item = payload[0].payload;
  return (
    <div className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm shadow-lg">
      <p className="font-semibold text-slate-700">{formatDate(item.date)}</p>
      <p className="mt-1 text-rose-600">Report: {formatNumber(item.reportCount)}</p>
      <p className="text-amber-600">Complaint: {formatNumber(item.complaintCount)}</p>
    </div>
  );
}

function BreakdownChart({ title, data }: { title: string; data: Breakdown[] }) {
  const chartData = data.map((item) => ({
    name: `${labelOf(item.key)} (${labelOf(item.source)})`,
    count: item.count,
    source: item.source,
  }));

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
      <h2 className="text-base font-semibold text-slate-900">{title}</h2>
      <div className="mt-4 h-72">
        {chartData.length === 0 ? (
          <div className="flex h-full items-center justify-center text-sm text-slate-400">Chua co du lieu</div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} layout="vertical" margin={{ left: 24, right: 8 }}>
              <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#e2e8f0" />
              <XAxis type="number" allowDecimals={false} tickLine={false} axisLine={false} />
              <YAxis
                type="category"
                dataKey="name"
                width={132}
                tickLine={false}
                axisLine={false}
                tick={{ fontSize: 11, fill: "#64748b" }}
              />
              <Tooltip formatter={(value) => [formatNumber(Number(value)), "So luong"]} />
              <Bar dataKey="count" radius={[0, 6, 6, 0]} fill="#0f766e" />
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>
    </section>
  );
}

export default function AdminModerationAnalyticsPage() {
  const [from, setFrom] = useState(daysAgoIso(29));
  const [to, setTo] = useState(todayIso());
  const [role, setRole] = useState("ALL");
  const [category, setCategory] = useState("ALL");
  const [status, setStatus] = useState("ALL");

  const queryParams = useMemo(() => ({ from, to, role, category, status }), [from, to, role, category, status]);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["admin-moderation-analytics", queryParams],
    queryFn: () => fetchAnalytics(queryParams),
  });

  const summary = data?.summary;

  return (
    <div className="space-y-6">
      <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
        <div className="grid gap-4 md:grid-cols-5">
          <label className="space-y-1.5 text-sm font-medium text-slate-600">
            Tu ngay
            <input
              type="date"
              value={from}
              onChange={(event) => setFrom(event.target.value)}
              className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-900 outline-none focus:border-emerald-500"
            />
          </label>
          <label className="space-y-1.5 text-sm font-medium text-slate-600">
            Den ngay
            <input
              type="date"
              value={to}
              onChange={(event) => setTo(event.target.value)}
              className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-900 outline-none focus:border-emerald-500"
            />
          </label>
          <label className="space-y-1.5 text-sm font-medium text-slate-600">
            Vai tro
            <select
              value={role}
              onChange={(event) => setRole(event.target.value)}
              className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-900 outline-none focus:border-emerald-500"
            >
              <option value="ALL">Tat ca</option>
              <option value="Customer">Khach hang</option>
              <option value="Owner">Chu san</option>
            </select>
          </label>
          <label className="space-y-1.5 text-sm font-medium text-slate-600">
            Category
            <select
              value={category}
              onChange={(event) => setCategory(event.target.value)}
              className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-900 outline-none focus:border-emerald-500"
            >
              <option value="ALL">Tat ca</option>
              <optgroup label="Report">
                {REPORT_CATEGORIES.map((item) => (
                  <option key={item} value={item}>
                    {labelOf(item)}
                  </option>
                ))}
              </optgroup>
              <optgroup label="Complaint">
                {COMPLAINT_PRIORITIES.map((item) => (
                  <option key={item} value={item}>
                    {labelOf(item)}
                  </option>
                ))}
              </optgroup>
            </select>
          </label>
          <label className="space-y-1.5 text-sm font-medium text-slate-600">
            Status
            <select
              value={status}
              onChange={(event) => setStatus(event.target.value)}
              className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-900 outline-none focus:border-emerald-500"
            >
              <option value="ALL">Tat ca</option>
              <optgroup label="Report">
                {REPORT_STATUSES.map((item) => (
                  <option key={item} value={item}>
                    {labelOf(item)}
                  </option>
                ))}
              </optgroup>
              <optgroup label="Complaint">
                {COMPLAINT_STATUSES.map((item) => (
                  <option key={item} value={item}>
                    {labelOf(item)}
                  </option>
                ))}
              </optgroup>
            </select>
          </label>
        </div>
      </section>

      {isLoading && (
        <div className="flex h-64 items-center justify-center rounded-lg border border-slate-200 bg-white">
          <Loader2 className="h-6 w-6 animate-spin text-emerald-600" />
        </div>
      )}

      {isError && (
        <div className="rounded-lg border border-rose-200 bg-rose-50 p-5">
          <div className="flex items-center justify-between gap-4">
            <p className="text-sm font-medium text-rose-700">Khong tai duoc thong ke moderation.</p>
            <button
              type="button"
              onClick={() => refetch()}
              className="rounded-lg bg-rose-600 px-3 py-2 text-sm font-semibold text-white hover:bg-rose-700"
            >
              Thu lai
            </button>
          </div>
        </div>
      )}

      {data && summary && (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <KpiCard
              label="Tin hieu moderation"
              value={formatNumber(summary.totalSignals)}
              detail={`${formatNumber(summary.totalReports)} report, ${formatNumber(summary.totalComplaints)} complaint`}
              icon={ShieldAlert}
              tone="bg-rose-50 text-rose-600"
            />
            <KpiCard
              label="Dang mo"
              value={formatNumber(summary.openSignals)}
              detail="Can Admin tiep tuc theo doi"
              icon={AlertTriangle}
              tone="bg-amber-50 text-amber-600"
            />
            <KpiCard
              label="Ty le resolve"
              value={`${summary.resolveRate}%`}
              detail={`${formatNumber(summary.resolvedSignals)} tin hieu da xu ly`}
              icon={CheckCircle2}
              tone="bg-emerald-50 text-emerald-600"
            />
            <KpiCard
              label="TG xu ly TB"
              value={`${summary.averageResolutionHours}h`}
              detail="Tinh tren report/complaint da co moc xu ly"
              icon={Clock3}
              tone="bg-sky-50 text-sky-600"
            />
          </div>

          <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
            <div className="mb-4 flex items-center gap-2">
              <BarChart3 className="h-5 w-5 text-emerald-600" />
              <h2 className="text-base font-semibold text-slate-900">Bien dong theo thoi gian</h2>
            </div>
            <div className="h-80 overflow-x-auto">
              <div style={{ minWidth: Math.max(data.trend.length * 36, 720), height: "100%" }}>
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={data.trend} margin={{ top: 10, right: 18, left: 0, bottom: 8 }}>
                    <defs>
                      <linearGradient id="reportFill" x1="0" x2="0" y1="0" y2="1">
                        <stop offset="5%" stopColor="#e11d48" stopOpacity={0.25} />
                        <stop offset="95%" stopColor="#e11d48" stopOpacity={0.02} />
                      </linearGradient>
                      <linearGradient id="complaintFill" x1="0" x2="0" y1="0" y2="1">
                        <stop offset="5%" stopColor="#d97706" stopOpacity={0.25} />
                        <stop offset="95%" stopColor="#d97706" stopOpacity={0.02} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                    <XAxis
                      dataKey="date"
                      tickFormatter={formatDate}
                      tickLine={false}
                      axisLine={false}
                      minTickGap={24}
                      tick={{ fontSize: 11, fill: "#64748b" }}
                    />
                    <YAxis allowDecimals={false} tickLine={false} axisLine={false} tick={{ fontSize: 11, fill: "#64748b" }} />
                    <Tooltip content={<TrendTooltip />} />
                    <Area
                      type="monotone"
                      dataKey="reportCount"
                      name="Report"
                      stroke="#e11d48"
                      fill="url(#reportFill)"
                      strokeWidth={2}
                    />
                    <Area
                      type="monotone"
                      dataKey="complaintCount"
                      name="Complaint"
                      stroke="#d97706"
                      fill="url(#complaintFill)"
                      strokeWidth={2}
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </div>
          </section>

          <div className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
            <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
              <div className="border-b border-slate-100 p-5">
                <div className="flex items-center gap-2">
                  <Users className="h-5 w-5 text-emerald-600" />
                  <h2 className="text-base font-semibold text-slate-900">User bi report/complain nhieu nhat</h2>
                </div>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[720px] text-left text-sm">
                  <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                    <tr>
                      <th className="px-5 py-3 font-semibold">User</th>
                      <th className="px-5 py-3 font-semibold">Role</th>
                      <th className="px-5 py-3 text-right font-semibold">Report</th>
                      <th className="px-5 py-3 text-right font-semibold">Complaint</th>
                      <th className="px-5 py-3 text-right font-semibold">Tong</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {data.topUsers.length === 0 ? (
                      <tr>
                        <td colSpan={5} className="px-5 py-8 text-center text-slate-400">
                          Chua co du lieu
                        </td>
                      </tr>
                    ) : (
                      data.topUsers.map((user) => (
                        <tr key={user.userId} className="hover:bg-slate-50">
                          <td className="px-5 py-4">
                            <p className="font-semibold text-slate-900">{user.fullName || "N/A"}</p>
                            <p className="text-xs text-slate-500">{user.email}</p>
                          </td>
                          <td className="px-5 py-4">
                            <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-700">
                              {labelOf(user.roleName)}
                            </span>
                          </td>
                          <td className="px-5 py-4 text-right font-medium text-rose-600">{formatNumber(user.reportCount)}</td>
                          <td className="px-5 py-4 text-right font-medium text-amber-600">
                            {formatNumber(user.complaintCount)}
                          </td>
                          <td className="px-5 py-4 text-right font-bold text-slate-900">{formatNumber(user.totalCount)}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </section>

            <div className="grid gap-6">
              <BreakdownChart title="Theo category / priority" data={data.categoryBreakdown} />
              <BreakdownChart title="Theo status" data={data.statusBreakdown} />
            </div>
          </div>
        </>
      )}
    </div>
  );
}
