"use client";

import { useQuery } from "@tanstack/react-query";
import { useState, useEffect } from "react";
import api from "@/lib/api";
import { format } from "date-fns";
import { vi } from "date-fns/locale";
import Link from "next/link";
import {
  Users,
  Building2,
  MapPin,
  DollarSign,
  TrendingUp,
  AlertOctagon,
  Clock,
  Loader2,
  Calendar as CalendarIcon,
  CheckCircle2,
  XCircle,
  X,
} from "lucide-react";
import { useDateFilter, type DateRange } from "@/app/admin/layout";

interface BookingTrendDto {
  date: string;   // yyyy-MM-dd
  count: number;
}

interface RecentBookingDto {
  bookingId: number;
  customerName: string;
  stadiumName: string;
  totalPrice: number;
  bookingStatus: string;
  bookingDate: string;
  reservationDate: string;
  timeSlot: string;
}

interface AdminDashboardResponse {
  totalUsers: number;
  totalOwners: number;
  totalStadiums: number;
  totalBookings: number;
  totalRevenue: number;
  pendingBookings: number;
  confirmedBookings: number;
  cancelledBookings: number;
  completedBookings: number;
  pendingOwnerApprovals: number;
  openComplaints: number;
  recentBookings: RecentBookingDto[];
  bookingTrend: BookingTrendDto[];
}

interface ApiResponse<T> {
  code: number;
  message: string;
  result: T;
}

const fetchDashboardData = async (dateRange: DateRange | null) => {
  const params = dateRange
    ? { startDate: dateRange.startDate, endDate: dateRange.endDate }
    : {};
  const { data } = await api.get<ApiResponse<AdminDashboardResponse>>("/admin/dashboard", { params });
  return data.result;
};

/** Nhãn ngày trong tuần tiếng Việt — dùng khi hiển thị trend 7 ngày */
const getDayLabel = (dateStr: string): string => {
  const date = new Date(dateStr + "T00:00:00");
  return format(date, "EEE", { locale: vi });
};

const STATUS_MAP: Record<string, { label: string; classes: string }> = {
  PENDING: { label: "Chờ duyệt", classes: "bg-yellow-100 text-yellow-800 border-yellow-200" },
  CONFIRMED: { label: "Đã xác nhận", classes: "bg-blue-100 text-blue-800 border-blue-200" },
  COMPLETED: { label: "Hoàn thành", classes: "bg-emerald-100 text-emerald-800 border-emerald-200" },
  CANCELLED: { label: "Đã hủy", classes: "bg-rose-100 text-rose-800 border-rose-200" },
};

const formatLocalDate = (dateStr: string) => {
  if (!dateStr) return "N/A";
  const [year, month, day] = dateStr.split("-");
  return `${day}/${month}/${year}`;
};

// ── Date helpers ─────────────────────────────────────────────────────────────
const toISO = (d: Date) => d.toISOString().split("T")[0];

function getQuickRanges() {
  const today = new Date();
  const y = today.getFullYear();
  const m = today.getMonth(); // 0-indexed
  const quarter = Math.floor(m / 3);

  return {
    today: { startDate: toISO(today), endDate: toISO(today) },
    last7: { startDate: toISO(new Date(today.getFullYear(), today.getMonth(), today.getDate() - 6)), endDate: toISO(today) },
    last30: { startDate: toISO(new Date(today.getFullYear(), today.getMonth(), today.getDate() - 29)), endDate: toISO(today) },
    thisMonth: { startDate: toISO(new Date(y, m, 1)), endDate: toISO(new Date(y, m + 1, 0)) },
    thisQuarter: { startDate: toISO(new Date(y, quarter * 3, 1)), endDate: toISO(new Date(y, quarter * 3 + 3, 0)) },
    thisYear: { startDate: toISO(new Date(y, 0, 1)), endDate: toISO(new Date(y, 11, 31)) },
  };
}

// ── Inline Date Filter Panel ─────────────────────────────────────────────────
function DateFilterPanel() {
  const { dateRange, isFilterOpen, setIsFilterOpen, applyDateRange } = useDateFilter();

  const [from, setFrom] = useState(dateRange?.startDate ?? "");
  const [to, setTo] = useState(dateRange?.endDate ?? "");

  // Sync local state when external dateRange changes
  useEffect(() => {
    setFrom(dateRange?.startDate ?? "");
    setTo(dateRange?.endDate ?? "");
  }, [dateRange]);

  if (!isFilterOpen) return null;

  const ranges = getQuickRanges();

  const QUICK_BUTTONS = [
    { label: "Hôm nay", range: ranges.today },
    { label: "7 ngày qua", range: ranges.last7 },
    { label: "30 ngày qua", range: ranges.last30 },
    { label: "Tháng này", range: ranges.thisMonth },
    { label: "Quý này", range: ranges.thisQuarter },
    { label: "Năm nay", range: ranges.thisYear },
  ];

  const handleQuick = (range: DateRange) => {
    setFrom(range.startDate);
    setTo(range.endDate);
  };

  const handleApply = () => {
    if (!from || !to) return;
    applyDateRange({ startDate: from, endDate: to });
  };

  return (
    <div className="bg-white border border-slate-200 rounded-2xl shadow-sm p-5 mb-6 animate-in fade-in slide-in-from-top-2 duration-200">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold text-slate-900 flex items-center gap-2">
          <CalendarIcon className="h-4 w-4 text-emerald-500" />
          Lọc theo khoảng thời gian
        </h3>
        <button
          type="button"
          onClick={() => setIsFilterOpen(false)}
          className="text-xs text-slate-400 hover:text-slate-600 flex items-center gap-1 transition-colors"
          aria-label="Đóng bộ lọc"
        >
          <X className="h-3.5 w-3.5" />
          Đóng
        </button>
      </div>

      {/* Quick select buttons */}
      <div className="flex flex-wrap gap-2 mb-5">
        {QUICK_BUTTONS.map(({ label, range }) => {
          const isActive = from === range.startDate && to === range.endDate;
          return (
            <button
              key={label}
              type="button"
              onClick={() => handleQuick(range)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors
                ${isActive
                  ? "bg-emerald-50 border-emerald-400 text-emerald-700"
                  : "bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100 hover:border-slate-300"
                }`}
            >
              {label}
            </button>
          );
        })}
      </div>

      {/* Custom date inputs */}
      <div className="flex flex-col sm:flex-row items-start sm:items-end gap-3">
        <div className="flex flex-col gap-1">
          <label htmlFor="date-from" className="text-xs font-medium text-slate-500">Từ ngày</label>
          <input
            id="date-from"
            type="date"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            max={to || undefined}
            className="border border-slate-200 rounded-lg px-3 py-2 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-emerald-400 focus:border-transparent bg-white"
          />
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="date-to" className="text-xs font-medium text-slate-500">Đến ngày</label>
          <input
            id="date-to"
            type="date"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            min={from || undefined}
            className="border border-slate-200 rounded-lg px-3 py-2 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-emerald-400 focus:border-transparent bg-white"
          />
        </div>
        <button
          type="button"
          onClick={handleApply}
          disabled={!from || !to}
          className="px-5 py-2 bg-emerald-600 text-white text-sm font-semibold rounded-lg hover:bg-emerald-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
        >
          Áp dụng
        </button>
      </div>
    </div>
  );
}

// ── Main Dashboard Page ───────────────────────────────────────────────────────
export default function AdminDashboardPage() {
  const { dateRange } = useDateFilter();

  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin-dashboard", dateRange],
    queryFn: () => fetchDashboardData(dateRange),
    staleTime: dateRange ? 0 : 5 * 60 * 1000, // No cache when filtered
  });

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-emerald-600" />
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="py-20 text-center text-rose-500 font-medium">
        Đã có lỗi xảy ra khi tải dữ liệu dashboard.
      </div>
    );
  }

  const KPI_DATA = [
    { title: "Tổng người dùng", value: data.totalUsers.toLocaleString("vi-VN"), trend: "Số liệu thời gian thực", icon: Users, color: "text-blue-600", bg: "bg-blue-100" },
    { title: "Tổng chủ sân", value: data.totalOwners.toLocaleString("vi-VN"), trend: "Số liệu thời gian thực", icon: Building2, color: "text-indigo-600", bg: "bg-indigo-100" },
    { title: "Tổng số sân", value: data.totalStadiums.toLocaleString("vi-VN"), trend: "Số liệu thời gian thực", icon: MapPin, color: "text-purple-600", bg: "bg-purple-100" },
    { title: "Tổng lượt đặt", value: data.totalBookings.toLocaleString("vi-VN"), trend: dateRange ? "Trong khoảng ngày đã chọn" : "Số liệu thời gian thực", icon: TrendingUp, color: "text-emerald-600", bg: "bg-emerald-100" },
  ];

  const BOOKING_STATUS = [
    { label: "Chờ duyệt", count: data.pendingBookings, icon: Clock, color: "text-yellow-600", bg: "bg-yellow-50", border: "border-yellow-100" },
    { label: "Đã xác nhận", count: data.confirmedBookings, icon: CalendarIcon, color: "text-blue-600", bg: "bg-blue-50", border: "border-blue-100" },
    { label: "Hoàn thành", count: data.completedBookings, icon: CheckCircle2, color: "text-emerald-600", bg: "bg-emerald-50", border: "border-emerald-100" },
    { label: "Đã hủy", count: data.cancelledBookings, icon: XCircle, color: "text-rose-600", bg: "bg-rose-50", border: "border-rose-100" },
  ];

  // Tính max để scale bar chart đúng tỷ lệ
  const trendData = data.bookingTrend ?? [];
  const maxTrendCount = Math.max(...trendData.map((item) => item.count), 1);

  // Dynamic chart title based on date range
  const chartDays = trendData.length;
  const chartTitle = dateRange
    ? `Biểu đồ đặt sân (${chartDays} ngày)`
    : "Biểu đồ đặt sân (7 ngày)";

  return (
    <div className="space-y-8 pb-8">
      {/* DATE FILTER PANEL — rendered inline at top of dashboard */}
      <DateFilterPanel />

      {/* KPI SECTION */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-5">
        {/* Main Revenue Card */}
        <div className="lg:col-span-1 bg-emerald-600 rounded-2xl p-6 text-white shadow-lg shadow-emerald-600/20 relative overflow-hidden group">
          <div className="absolute -right-6 -top-6 bg-white/10 w-24 h-24 rounded-full blur-2xl group-hover:bg-white/20 transition-all"></div>
          <div className="flex justify-between items-start mb-4 relative z-10">
            <div className="p-2 bg-white/20 rounded-xl">
              <DollarSign className="h-6 w-6 text-white" />
            </div>
          </div>
          <h3 className="text-emerald-100 text-sm font-medium mb-1 relative z-10">Doanh thu hệ thống</h3>
          <div className="text-2xl font-bold tracking-tight mb-2 relative z-10">
            {new Intl.NumberFormat("vi-VN", {
              style: "currency",
              currency: "VND",
              maximumFractionDigits: 0,
            }).format(data.totalRevenue)}
          </div>
          <div className="text-xs text-emerald-200 font-medium relative z-10">
            {dateRange ? "Trong khoảng ngày đã chọn" : "Số liệu thời gian thực"}
          </div>
        </div>

        {/* Other KPI Cards */}
        {KPI_DATA.map((kpi, idx) => (
          <div key={idx} className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-md transition-shadow group">
            <div className="flex justify-between items-start mb-4">
              <div className={`p-2.5 rounded-xl ${kpi.bg} ${kpi.color} group-hover:scale-110 transition-transform`}>
                <kpi.icon className="h-5 w-5" />
              </div>
            </div>
            <h3 className="text-slate-500 text-sm font-medium mb-1">{kpi.title}</h3>
            <div className="text-2xl font-bold text-slate-900 tracking-tight mb-2">{kpi.value}</div>
            <div className="text-xs text-emerald-600 font-medium">{kpi.trend}</div>
          </div>
        ))}
      </div>

      {/* MIDDLE SECTION: Booking Status & Pending Actions */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {/* Booking Status Overview */}
        <div className="lg:col-span-2 bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
          <h2 className="text-lg font-bold text-slate-900 mb-6 flex items-center gap-2">
            Trạng thái đặt sân
            <span className="text-xs font-normal px-2 py-0.5 bg-slate-100 text-slate-500 rounded-full">
              {dateRange ? "Theo bộ lọc" : "Tổng quan"}
            </span>
          </h2>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            {BOOKING_STATUS.map((status, idx) => (
              <div key={idx} className={`p-4 rounded-xl border ${status.bg} ${status.border} transition-all hover:-translate-y-1`}>
                <status.icon className={`h-6 w-6 mb-3 ${status.color}`} />
                <div className="text-2xl font-bold text-slate-900 mb-1">{status.count}</div>
                <div className={`text-sm font-medium ${status.color}`}>{status.label}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Actionable Items */}
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 flex flex-col">
          <h2 className="text-lg font-bold text-slate-900 mb-6">Yêu cầu xử lý</h2>
          <div className="space-y-4 flex-1">
            <Link
              href="/admin/owner-approvals"
              className="flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50 hover:bg-slate-100 hover:border-slate-200 transition-colors cursor-pointer group"
            >
              <div className="flex items-center gap-3">
                <div className="p-2 bg-white rounded-lg border border-slate-200 text-slate-500 group-hover:text-emerald-600 transition-colors">
                  <Building2 className="h-5 w-5" />
                </div>
                <div>
                  <div className="font-semibold text-slate-900 text-sm mb-0.5">Phê duyệt chủ sân</div>
                  <div className="text-xs text-slate-500">Các tài khoản chủ sân mới chờ duyệt</div>
                </div>
              </div>
              <div className="bg-rose-100 text-rose-700 font-bold text-xs px-2.5 py-1 rounded-full">
                {data.pendingOwnerApprovals}
              </div>
            </Link>

            <Link
              href="/admin/complaints"
              className="flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50 hover:bg-slate-100 hover:border-slate-200 transition-colors cursor-pointer group"
            >
              <div className="flex items-center gap-3">
                <div className="p-2 bg-white rounded-lg border border-slate-200 text-slate-500 group-hover:text-emerald-600 transition-colors">
                  <AlertOctagon className="h-5 w-5" />
                </div>
                <div>
                  <div className="font-semibold text-slate-900 text-sm mb-0.5">Khiếu nại chưa xử lý</div>
                  <div className="text-xs text-slate-500">Khiếu nại từ khách hàng cần giải quyết</div>
                </div>
              </div>
              <div className="bg-rose-100 text-rose-700 font-bold text-xs px-2.5 py-1 rounded-full">
                {data.openComplaints}
              </div>
            </Link>
          </div>
        </div>
      </div>

      {/* CHARTS & RECENT BOOKINGS */}
      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">

        {/* Bar Chart */}
        <div className="xl:col-span-1 bg-white rounded-2xl border border-slate-200 shadow-sm p-6 flex flex-col">
          <h2 className="text-lg font-bold text-slate-900 mb-6">{chartTitle}</h2>
          {trendData.length === 0 ? (
            <div className="flex-1 flex items-center justify-center text-slate-400 text-sm">
              Chưa có dữ liệu
            </div>
          ) : (
            <div className="flex items-stretch justify-between gap-1.5" style={{ height: 180 }}>
              {trendData.map((item, idx) => (
                <div key={idx} className="flex flex-col items-center gap-1 group w-full h-full">
                  {/* count tooltip */}
                  <span className="text-[11px] font-semibold text-emerald-600 opacity-0 group-hover:opacity-100 transition-opacity" style={{ minHeight: 16, lineHeight: '16px' }}>
                    {item.count > 0 ? item.count : ''}
                  </span>
                  {/* gray track */}
                  <div className="w-full relative bg-slate-100 rounded-t-md flex-1 min-h-0">
                    <div
                      className="absolute bottom-0 w-full bg-emerald-500 rounded-t-md transition-all duration-700 ease-out group-hover:bg-emerald-400"
                      style={{ height: `${(item.count / maxTrendCount) * 100}%` }}
                    />
                  </div>
                  {/* day label */}
                  <span className="text-[11px] font-medium text-slate-500 leading-none mt-0.5 flex-shrink-0">
                    {getDayLabel(item.date)}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Recent Bookings Table */}
        <div className="xl:col-span-2 bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
          <div className="p-6 border-b border-slate-100 flex items-center justify-between">
            <h2 className="text-lg font-bold text-slate-900">Đơn đặt sân gần đây</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-100">
                  <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Mã Đơn</th>
                  <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Khách hàng / Sân</th>
                  <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Thời gian chơi</th>
                  <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Tổng tiền</th>
                  <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Trạng thái</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.recentBookings.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-6 py-8 text-center text-slate-500 text-sm">
                      Chưa có lịch sử đặt sân nào.
                    </td>
                  </tr>
                ) : (
                  data.recentBookings.map((booking) => {
                    const statusInfo = STATUS_MAP[booking.bookingStatus] || {
                      label: booking.bookingStatus,
                      classes: "bg-slate-100 text-slate-800 border-slate-200",
                    };
                    return (
                      <tr key={booking.bookingId} className="hover:bg-slate-50/80 transition-colors">
                        <td className="px-6 py-4">
                          <span className="font-mono text-sm font-medium text-slate-900">#{booking.bookingId}</span>
                        </td>
                        <td className="px-6 py-4">
                          <div className="text-sm font-semibold text-slate-900 mb-0.5">{booking.customerName}</div>
                          <div className="text-xs text-slate-500">{booking.stadiumName}</div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="text-sm text-slate-900 font-medium mb-0.5">{booking.timeSlot}</div>
                          <div className="text-xs text-slate-500">{formatLocalDate(booking.reservationDate)}</div>
                        </td>
                        <td className="px-6 py-4 text-sm font-semibold text-slate-900">
                          {new Intl.NumberFormat("vi-VN", {
                            style: "currency",
                            currency: "VND",
                          }).format(booking.totalPrice)}
                        </td>
                        <td className="px-6 py-4">
                          <span
                            className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium border ${statusInfo.classes}`}
                          >
                            {statusInfo.label}
                          </span>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
