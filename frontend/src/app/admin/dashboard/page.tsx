"use client";

import { useQuery } from "@tanstack/react-query";
import api from "@/lib/api";
import { format } from "date-fns";
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
  XCircle
} from "lucide-react";

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
}

interface ApiResponse<T> {
  code: number;
  message: string;
  result: T;
}

const fetchDashboardData = async () => {
  const { data } = await api.get<ApiResponse<AdminDashboardResponse>>("/admin/dashboard");
  return data.result;
};

// Dữ liệu mock cho biểu đồ do backend chưa trả về dữ liệu trend
const CHART_DATA = [
  { day: 'T2', value: 40 }, { day: 'T3', value: 65 }, { day: 'T4', value: 45 },
  { day: 'T5', value: 80 }, { day: 'T6', value: 55 }, { day: 'T7', value: 95 }, { day: 'CN', value: 85 }
];

const STATUS_MAP: Record<string, { label: string, classes: string }> = {
  PENDING: { label: 'Chờ duyệt', classes: 'bg-yellow-100 text-yellow-800 border-yellow-200' },
  CONFIRMED: { label: 'Đã xác nhận', classes: 'bg-blue-100 text-blue-800 border-blue-200' },
  COMPLETED: { label: 'Hoàn thành', classes: 'bg-emerald-100 text-emerald-800 border-emerald-200' },
  CANCELLED: { label: 'Đã hủy', classes: 'bg-rose-100 text-rose-800 border-rose-200' },
};

export default function AdminDashboardPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin-dashboard"],
    queryFn: fetchDashboardData,
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
    { title: 'Tổng người dùng', value: data.totalUsers.toLocaleString("vi-VN"), trend: '+12% tháng trước', icon: Users, color: 'text-blue-600', bg: 'bg-blue-100' },
    { title: 'Tổng chủ sân', value: data.totalOwners.toLocaleString("vi-VN"), trend: '+5% tháng trước', icon: Building2, color: 'text-indigo-600', bg: 'bg-indigo-100' },
    { title: 'Tổng số sân', value: data.totalStadiums.toLocaleString("vi-VN"), trend: '+8% tháng trước', icon: MapPin, color: 'text-purple-600', bg: 'bg-purple-100' },
    { title: 'Tổng lượt đặt', value: data.totalBookings.toLocaleString("vi-VN"), trend: '+24% tháng trước', icon: TrendingUp, color: 'text-emerald-600', bg: 'bg-emerald-100' },
  ];

  const BOOKING_STATUS = [
    { label: 'Chờ duyệt', count: data.pendingBookings, icon: Clock, color: 'text-yellow-600', bg: 'bg-yellow-50', border: 'border-yellow-100' },
    { label: 'Đã xác nhận', count: data.confirmedBookings, icon: CalendarIcon, color: 'text-blue-600', bg: 'bg-blue-50', border: 'border-blue-100' },
    { label: 'Hoàn thành', count: data.completedBookings, icon: CheckCircle2, color: 'text-emerald-600', bg: 'bg-emerald-50', border: 'border-emerald-100' },
    { label: 'Đã hủy', count: data.cancelledBookings, icon: XCircle, color: 'text-rose-600', bg: 'bg-rose-50', border: 'border-rose-100' },
  ];

  return (
    <div className="space-y-8 pb-8">
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
          <div className="text-xs text-emerald-200 font-medium relative z-10">+18% so với tháng trước</div>
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
            <span className="text-xs font-normal px-2 py-0.5 bg-slate-100 text-slate-500 rounded-full">Tổng quan</span>
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
            <Link href="/admin/owner-approvals" className="flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50 hover:bg-slate-100 hover:border-slate-200 transition-colors cursor-pointer group">
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

            <Link href="/admin/complaints" className="flex items-center justify-between p-4 rounded-xl border border-slate-100 bg-slate-50 hover:bg-slate-100 hover:border-slate-200 transition-colors cursor-pointer group">
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
        
        {/* Custom Bar Chart (Tailwind only) */}
        <div className="xl:col-span-1 bg-white rounded-2xl border border-slate-200 shadow-sm p-6 flex flex-col">
          <h2 className="text-lg font-bold text-slate-900 mb-6">Biểu đồ đặt sân (7 ngày)</h2>
          <div className="flex-1 flex items-end justify-between gap-2 h-48 mt-auto pt-4">
            {CHART_DATA.map((data, idx) => (
              <div key={idx} className="flex flex-col items-center gap-2 group w-full">
                <span className="text-xs font-semibold text-slate-500 opacity-0 group-hover:opacity-100 transition-opacity -translate-y-2 group-hover:translate-y-0">
                  {data.value}
                </span>
                <div className="w-full relative flex justify-center bg-slate-100 rounded-t-md h-full">
                  <div 
                    className="absolute bottom-0 w-full bg-emerald-500 rounded-t-md transition-all duration-500 group-hover:bg-emerald-400"
                    style={{ height: `${data.value}%` }}
                  ></div>
                </div>
                <span className="text-xs font-medium text-slate-500">{data.day}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Table */}
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
                    const statusInfo = STATUS_MAP[booking.bookingStatus] || { label: booking.bookingStatus, classes: 'bg-slate-100 text-slate-800 border-slate-200' };
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
                          <div className="text-xs text-slate-500">{format(new Date(booking.reservationDate), "dd/MM/yyyy")}</div>
                        </td>
                        <td className="px-6 py-4 text-sm font-semibold text-slate-900">
                          {new Intl.NumberFormat("vi-VN", {
                            style: "currency",
                            currency: "VND",
                          }).format(booking.totalPrice)}
                        </td>
                        <td className="px-6 py-4">
                          <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium border ${statusInfo.classes}`}>
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
