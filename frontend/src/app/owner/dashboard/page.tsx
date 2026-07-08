'use client'

import { useState, useEffect } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import {
  TrendingUp,
  TrendingDown,
  Calendar,
  DollarSign,
  AlertCircle,
  CheckCircle,
  Home,
  BarChart3,
  Wallet,
  Bell,
  XCircle,
} from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Textarea } from "@/components/ui/textarea";
import { toast } from "sonner";
import { get, put } from "@/lib/api";

// ── Interfaces ────────────────────────────────────────────────────────
interface ApiResponse<T> {
  code: number;
  message: string;
  result: T;
}

interface CustomerInfo {
  name: string;
  phone: string;
  email: string;
}

interface BookingItem {
  id: number;
  displayId: string;
  customer: CustomerInfo;
  venue: string;
  date: string;
  time: string;
  amount: number;
  paymentStatus: string;
  status: string;
  notes: string;
  playTimeRaw: string;
}

interface RevenueDetail {
  date: string;
  revenue: number;
}

interface VenueRevenue {
  stadiumId: number;
  stadiumName: string;
  totalBookings: number;
  totalRevenue: number;
  occupancy: number;
  trend: string;
}

interface RevenueReportResponse {
  totalRevenue: number;
  totalBookings: number;
  details: RevenueDetail[];
  venueRevenues: VenueRevenue[];
}

interface OwnerDashboardSummaryResponse {
  todayBookingsCount: number;
  currentMonthRevenue: number;
  averageOccupancyRate: number;
  pendingBookingsCount: number;
}

interface KpiItem {
  title: string;
  value: string | number;
  change: string;
  trend: "up" | "down" | "neutral";
  icon: React.ReactNode;
}

const COLORS = ["#2563EB", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6"];

function OwnerDashboardPage() {
  const [isLoading, setIsLoading] = useState(true);
  const [report, setReport] = useState<RevenueReportResponse | null>(null);
  const [summary, setSummary] = useState<OwnerDashboardSummaryResponse | null>(null);
  const [pendingBookings, setPendingBookings] = useState<BookingItem[]>([]);

  // Rejection Dialog states
  const [selectedBookingId, setSelectedBookingId] = useState<number | null>(null);
  const [isRejectDialogOpen, setIsRejectDialogOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [isSubmittingAction, setIsSubmittingAction] = useState(false);

  // Confirm (Approve) AlertDialog states
  const [selectedConfirmId, setSelectedConfirmId] = useState<number | null>(null);
  const [isConfirmDialogOpen, setIsConfirmDialogOpen] = useState(false);

  // Fetch all necessary dashboard data
  const loadDashboardData = async () => {
    try {
      setIsLoading(true);

      // 1. Tính toán ngày (30 ngày trước đến nay)
      const end = new Date();
      const start = new Date();
      start.setDate(end.getDate() - 30);

      const formatDate = (d: Date) => d.toISOString().split("T")[0];
      const startDateStr = formatDate(start);
      const endDateStr = formatDate(end);

      // 2. Fetch Báo cáo doanh thu, Booking và Dashboard Summary
      const [reportRes, bookingsResponse, summaryRes] = await Promise.all([
        get<ApiResponse<RevenueReportResponse>>(`/owner/reports/revenue?startDate=${startDateStr}&endDate=${endDateStr}`),
        get<BookingItem[] | { content?: BookingItem[] }>(
          "/owner/bookings?page=0&size=100&status=PENDING"
        ),
        get<ApiResponse<OwnerDashboardSummaryResponse>>("/owner/reports/summary"),
      ]);

      setReport(reportRes.result);
      setSummary(summaryRes.result);

      // Hỗ trợ cả API cũ trả mảng phẳng và API mới trả PageResponse.
      // Query đã lọc PENDING ở server; filter phía client giữ tương thích với API cũ.
      const bookingsList: BookingItem[] = Array.isArray(bookingsResponse)
        ? bookingsResponse
        : Array.isArray(bookingsResponse?.content)
          ? bookingsResponse.content
          : [];
      const pending = bookingsList.filter(
        (booking) => booking.status?.toLowerCase() === "pending"
      );
      setPendingBookings(pending);

    } catch (error: unknown) {
      const err = error as Error;
      console.error("Lỗi khi tải dữ liệu dashboard:", err);
      toast.error(err.message || "Không thể tải dữ liệu dashboard. Vui lòng thử lại.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadDashboardData();
  }, []);

  // Mở AlertDialog xác nhận duyệt
  const handleOpenConfirmDialog = (bookingId: number) => {
    setSelectedConfirmId(bookingId);
    setIsConfirmDialogOpen(true);
  };

  // Xử lý duyệt đặt sân (sau khi đã xác nhận qua AlertDialog)
  const handleConfirmBooking = async () => {
    if (!selectedConfirmId) return;
    try {
      setIsConfirmDialogOpen(false);
      toast.loading("Đang xử lý duyệt đơn...", { id: "booking-action" });
      await put(`/owner/bookings/${selectedConfirmId}/action`, { action: "CONFIRM" });
      toast.success("Đã duyệt đơn đặt sân thành công!", { id: "booking-action" });
      setSelectedConfirmId(null);
      loadDashboardData();
    } catch (error: unknown) {
      const err = error as Error;
      toast.error(err.message || "Không thể duyệt đơn.", { id: "booking-action" });
    }
  };

  // Mở Dialog từ chối
  const handleOpenRejectDialog = (bookingId: number) => {
    setSelectedBookingId(bookingId);
    setRejectReason("");
    setIsRejectDialogOpen(true);
  };

  // Xử lý từ chối đặt sân
  const handleRejectBooking = async () => {
    if (!selectedBookingId) return;
    if (!rejectReason.trim()) {
      toast.error("Vui lòng nhập lý do từ chối.");
      return;
    }

    try {
      setIsSubmittingAction(true);
      toast.loading("Đang xử lý từ chối...", { id: "booking-action" });
      await put(`/owner/bookings/${selectedBookingId}/action`, {
        action: "REJECT",
        reason: rejectReason.trim(),
      });
      toast.success("Đã từ chối đơn đặt sân thành công!", { id: "booking-action" });
      setIsRejectDialogOpen(false);
      loadDashboardData();
    } catch (error: unknown) {
      const err = error as Error;
      toast.error(err.message || "Không thể từ chối đơn.", { id: "booking-action" });
    } finally {
      setIsSubmittingAction(false);
    }
  };

  // Xử lý định dạng biểu đồ phân bổ doanh thu theo sân
  const getVenuePieData = () => {
    if (!report || !report.venueRevenues || report.venueRevenues.length === 0) {
      return [];
    }
    return report.venueRevenues.map((v) => ({
      name: v.stadiumName,
      value: v.totalRevenue,
    }));
  };

  // Điền dữ liệu đầy đủ 30 ngày (padding với doanh thu = 0 cho ngày không có phát sinh giao dịch)
  const getPaddedDetails = () => {
    if (!report || !report.details) return [];
    const detailsMap = new Map(report.details.map((d) => [d.date, d.revenue]));
    const list = [];
    const end = new Date();
    const current = new Date();
    current.setDate(end.getDate() - 30);

    while (current <= end) {
      const dateStr = current.toISOString().split("T")[0];
      list.push({
        date: dateStr,
        revenue: detailsMap.get(dateStr) || 0,
      });
      current.setDate(current.getDate() + 1);
    }
    return list;
  };

  const kpisData: KpiItem[] = [
    {
      title: "Đặt sân hôm nay",
      value: summary?.todayBookingsCount ?? 0,
      change: "",
      trend: "neutral",
      icon: <Calendar className="h-6 w-6" />,
    },
    {
      title: "Doanh thu tháng này",
      value: summary ? `${summary.currentMonthRevenue.toLocaleString("vi-VN")}đ` : "0đ",
      change: "",
      trend: "neutral",
      icon: <DollarSign className="h-6 w-6" />,
    },
    {
      title: "Tỷ lệ lấp đầy trung bình",
      value: summary ? `${summary.averageOccupancyRate}%` : "0%",
      change: "",
      trend: "neutral",
      icon: <BarChart3 className="h-6 w-6" />,
    },
    {
      title: "Chờ xác nhận",
      value: summary?.pendingBookingsCount ?? 0,
      change: "",
      trend: "neutral",
      icon: <AlertCircle className="h-6 w-6" />,
    },
  ];


  return (
    <div className="p-6 lg:p-8">
      {/* Page Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-extrabold text-slate-950 dark:text-white">Tổng quan hiệu suất</h1>
          <p className="text-muted-foreground text-sm mt-1">Dữ liệu được cập nhật dựa trên hoạt động thực tế của hệ thống.</p>
        </div>
        <Button variant="outline" onClick={loadDashboardData} disabled={isLoading} size="sm">
          Tải lại trang
        </Button>
      </div>

      {/* KPIs Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {isLoading
          ? Array.from({ length: 4 }).map((_, idx) => (
            <Card key={idx}>
              <CardContent className="p-6">
                <div className="flex justify-between items-center mb-4">
                  <Skeleton className="h-12 w-12 rounded-lg" />
                  <Skeleton className="h-4 w-12" />
                </div>
                <Skeleton className="h-8 w-24 mb-2" />
                <Skeleton className="h-4 w-32" />
              </CardContent>
            </Card>
          ))
          : kpisData.map((kpi, idx) => (
            <Card key={idx} className="border border-slate-200/80 dark:border-slate-800 shadow-sm hover:shadow-md transition-shadow">
              <CardContent className="p-6">
                <div className="flex items-center justify-between mb-4">
                  <div className="text-primary bg-primary/10 p-3 rounded-lg dark:bg-primary/20">
                    {kpi.icon}
                  </div>
                  {kpi.change && (
                    <div
                      className={`flex items-center text-sm ${kpi.trend === "up"
                          ? "text-green-600 dark:text-green-400"
                          : "text-red-600 dark:text-red-400"
                        }`}
                    >
                      {kpi.trend === "up" ? (
                        <TrendingUp className="h-4 w-4 mr-1" />
                      ) : (
                        <TrendingDown className="h-4 w-4 mr-1" />
                      )}
                      {kpi.change}
                    </div>
                  )}
                </div>
                <div className="text-3xl font-bold tracking-tight text-slate-900 dark:text-white mb-1">
                  {kpi.value}
                </div>
                <div className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                  {kpi.title}
                </div>
              </CardContent>
            </Card>
          ))}
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
        {/* Revenue Chart */}
        <Card className="lg:col-span-2 border border-slate-200/80 dark:border-slate-800 shadow-sm">
          <CardHeader className="pb-2">
            <h3 className="font-bold text-slate-900 dark:text-white">Doanh thu 30 ngày qua</h3>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <Skeleton className="h-[300px] w-full" />
            ) : !report || report.details.length === 0 ? (
              <div className="h-[300px] flex items-center justify-center text-muted-foreground text-sm">
                Chưa có dữ liệu doanh thu trong khoảng thời gian này.
              </div>
            ) : (
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={getPaddedDetails()}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis
                    dataKey="date"
                    tickLine={false}
                    axisLine={false}
                    tickMargin={8}
                    tickFormatter={(value) => {
                      const parts = value.split("-");
                      return parts.length === 3 ? `${parts[2]}/${parts[1]}` : value;
                    }}
                  />
                  <YAxis
                    tickLine={false}
                    axisLine={false}
                    tickMargin={8}
                    tickFormatter={(value) => `${Number(value) / 1000}k`}
                  />
                  <Tooltip
                    formatter={(value) => [`${Number(value).toLocaleString("vi-VN")}đ`, "Doanh thu"]}
                  />
                  <Line
                    type="monotone"
                    dataKey="revenue"
                    stroke="#2563EB"
                    strokeWidth={2.5}
                    dot={{ r: 2 }}
                    activeDot={{ r: 6 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        {/* Sport/Stadium Distribution Chart */}
        <Card className="border border-slate-200/80 dark:border-slate-800 shadow-sm">
          <CardHeader className="pb-2">
            <h3 className="font-bold text-slate-900 dark:text-white">Doanh thu theo sân</h3>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <Skeleton className="h-[300px] w-full" />
            ) : getVenuePieData().length === 0 ? (
              <div className="h-[300px] flex items-center justify-center text-muted-foreground text-sm">
                Không có dữ liệu đóng góp từ các sân.
              </div>
            ) : (
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={getVenuePieData()}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ percent }) => percent > 0.05 ? `${(percent * 100).toFixed(0)}%` : ""}
                    outerRadius={75}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {getVenuePieData().map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value) => `${Number(value).toLocaleString("vi-VN")}đ`} />
                  <Legend
                    verticalAlign="bottom"
                    height={36}
                    iconType="circle"
                    wrapperStyle={{ fontSize: "12px" }}
                  />
                </PieChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Pending Bookings Table */}
      <Card className="border border-slate-200/80 dark:border-slate-800 shadow-sm overflow-hidden">
        <CardHeader className="border-b bg-slate-50/50 dark:bg-slate-950/20 py-4">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-slate-900 dark:text-white">Đơn đặt sân chờ duyệt</h3>
            <Badge variant={pendingBookings.length > 0 ? "destructive" : "secondary"}>
              {pendingBookings.length}
            </Badge>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-8 space-y-4">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
            </div>
          ) : pendingBookings.length === 0 ? (
            <div className="text-center py-12 text-muted-foreground text-sm flex flex-col items-center gap-2">
              <CheckCircle className="h-8 w-8 text-emerald-500" />
              Tuyệt vời! Bạn không còn đơn đặt sân nào cần duyệt.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full border-collapse">
                <thead>
                  <tr className="border-b bg-slate-50/50 dark:bg-slate-950/20 text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                    <th className="text-left p-4">Mã đơn</th>
                    <th className="text-left p-4">Khách hàng</th>
                    <th className="text-left p-4">Sân</th>
                    <th className="text-left p-4">Ngày chơi</th>
                    <th className="text-left p-4">Khung giờ</th>
                    <th className="text-right p-4">Số tiền</th>
                    <th className="text-right p-4">Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {pendingBookings.map((booking) => (
                    <tr key={booking.id} className="border-b hover:bg-slate-50 dark:hover:bg-slate-950/40 transition-colors">
                      <td className="p-4 font-mono text-sm font-semibold text-primary">{booking.displayId}</td>
                      <td className="p-4">
                        <div className="font-medium text-slate-900 dark:text-white">{booking.customer.name}</div>
                        <div className="text-xs text-muted-foreground">{booking.customer.phone}</div>
                      </td>
                      <td className="p-4 text-slate-700 dark:text-slate-300">{booking.venue}</td>
                      <td className="p-4 text-slate-700 dark:text-slate-300">{booking.date}</td>
                      <td className="p-4 text-slate-700 dark:text-slate-300 font-medium">{booking.time}</td>
                      <td className="p-4 text-right font-semibold text-slate-900 dark:text-white">
                        {booking.amount.toLocaleString("vi-VN")}đ
                      </td>
                      <td className="p-4 text-right">
                        <div className="flex gap-2 justify-end">
                          <Button
                            size="sm"
                            className="bg-emerald-600 hover:bg-emerald-700 text-white font-medium shadow-sm transition-all"
                            onClick={() => handleOpenConfirmDialog(booking.id)}
                          >
                            <CheckCircle className="h-4 w-4 mr-1" />
                            Duyệt
                          </Button>
                          <Button
                            size="sm"
                            variant="destructive"
                            className="font-medium shadow-sm transition-all"
                            onClick={() => handleOpenRejectDialog(booking.id)}
                          >
                            <XCircle className="h-4 w-4 mr-1" />
                            Từ chối
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>


      {/* Confirm (Approve) AlertDialog */}
      <AlertDialog open={isConfirmDialogOpen} onOpenChange={setIsConfirmDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle className="flex items-center gap-2 text-emerald-700 dark:text-emerald-400">
              <CheckCircle className="h-5 w-5" />
              Xác nhận duyệt đơn đặt sân
            </AlertDialogTitle>
            <AlertDialogDescription>
              Bạn có chắc chắn muốn <span className="font-semibold text-foreground">duyệt</span> đơn đặt sân này không?
              Khách hàng sẽ nhận được thông báo xác nhận ngay lập tức.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction
              className="bg-emerald-600 hover:bg-emerald-700 text-white"
              onClick={handleConfirmBooking}
            >
              <CheckCircle className="h-4 w-4 mr-1" />
              Xác nhận duyệt
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Rejection Reason Dialog */}
      <Dialog open={isRejectDialogOpen} onOpenChange={setIsRejectDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="text-xl font-bold text-rose-600 flex items-center gap-2">
              <XCircle className="h-6 w-6" />
              Từ chối đơn đặt sân
            </DialogTitle>
            <DialogDescription>
              Vui lòng nhập lý do từ chối đơn đặt sân này. Lý do này sẽ được gửi đến khách hàng qua ghi chú đơn hàng.
            </DialogDescription>
          </DialogHeader>
          <div className="py-4">
            <Textarea
              placeholder="Nhập lý do từ chối (ví dụ: Sân đang bảo trì, Trùng lịch đột xuất...)"
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              className="min-h-[100px] focus:ring-1 focus:ring-rose-500 focus:outline-none"
            />
          </div>
          <DialogFooter className="gap-2">
            <Button variant="outline" onClick={() => setIsRejectDialogOpen(false)} disabled={isSubmittingAction}>
              Hủy
            </Button>
            <Button
              variant="destructive"
              onClick={handleRejectBooking}
              disabled={isSubmittingAction || !rejectReason.trim()}
            >
              {isSubmittingAction ? "Đang xử lý..." : "Xác nhận từ chối"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default OwnerDashboardPage;
