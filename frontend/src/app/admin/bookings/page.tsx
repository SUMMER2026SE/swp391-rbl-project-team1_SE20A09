"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { format } from "date-fns";
import { Search, Loader2, Calendar, DollarSign, Activity, CalendarDays, Eye, AlertTriangle } from "lucide-react";
import api from "@/lib/api";

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { useDebounceValue } from "usehooks-ts";

export interface AdminBookingResponse {
  bookingId: number;
  customerName: string;
  customerEmail: string;
  stadiumName: string;
  ownerName: string;
  totalPrice: number;
  serviceFee: number;
  bookingStatus: string;
  paymentStatus: string;
  bookingDate: string;
  reservationDate: string;
  timeSlot: string;
  note?: string;
  cancelReason?: string;
}

interface AdminBookingStatsResponse {
  totalBookings: number;
  totalGMV: number;
  totalServiceFee: number;
}

interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  pageNumber: number;
  pageSize: number;
}

interface ApiResponse<T> {
  code: number;
  message: string;
  result: T;
}

interface AdminBookingListResponse {
  bookings: PageResponse<AdminBookingResponse>;
  stats: AdminBookingStatsResponse;
}

const fetchBookings = async (
  page: number,
  size: number,
  search: string,
  bookingStatus: string,
  paymentStatus: string,
  startDate: string,
  endDate: string
) => {
  const params: Record<string, string | number> = { page, pageSize: size };
  if (search) params.search = search;
  if (bookingStatus && bookingStatus !== "ALL") params.bookingStatus = bookingStatus;
  if (paymentStatus && paymentStatus !== "ALL") params.paymentStatus = paymentStatus;
  if (startDate) params.startDate = startDate;
  if (endDate) params.endDate = endDate;

  const { data } = await api.get<ApiResponse<AdminBookingListResponse>>("/admin/bookings", { params });
  return data.result;
};

export default function AdminBookingsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [bookingStatus, setBookingStatus] = useState("ALL");
  const [paymentStatus, setPaymentStatus] = useState("ALL");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [selectedBooking, setSelectedBooking] = useState<AdminBookingResponse | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState(false);

  const [debouncedSearch] = useDebounceValue(search, 500);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin-bookings", page, debouncedSearch, bookingStatus, paymentStatus, startDate, endDate],
    queryFn: () => fetchBookings(page, 10, debouncedSearch, bookingStatus, paymentStatus, startDate, endDate),
  });

  const formatVND = (amount: number) => {
    return (amount ?? 0).toLocaleString("vi-VN") + " đ";
  };

  const getBookingStatusBadge = (status: string) => {
    switch (status) {
      case "PENDING_PAYMENT":
        return <Badge variant="outline" className="border-amber-500 text-amber-600 bg-amber-50">Chờ thanh toán</Badge>;
      case "PENDING":
        return <Badge variant="secondary" className="bg-sky-500 hover:bg-sky-600 text-white">Chờ duyệt</Badge>;
      case "CONFIRMED":
        return <Badge variant="default" className="bg-emerald-500 hover:bg-emerald-600 text-white">Đã xác nhận</Badge>;
      case "COMPLETED":
        return <Badge variant="outline" className="border-slate-300 text-slate-600 bg-slate-50">Hoàn thành</Badge>;
      case "CANCELLED":
        return <Badge variant="destructive" className="bg-rose-500">Đã hủy</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  const getPaymentStatusBadge = (status: string) => {
    switch (status) {
      case "UNPAID":
        return <Badge variant="outline" className="border-rose-300 text-rose-600 bg-rose-50">Chưa thanh toán</Badge>;
      case "PAID":
        return <Badge variant="default" className="bg-emerald-500 hover:bg-emerald-600 text-white">Đã thanh toán</Badge>;
      case "REFUNDED":
        return <Badge variant="secondary" className="bg-indigo-500 hover:bg-indigo-600 text-white">Đã hoàn tiền</Badge>;
      case "DEPOSITED":
        return <Badge variant="secondary" className="bg-amber-500 hover:bg-amber-600 text-white">Đã cọc</Badge>;
      case "AWAITING_CASH_PAYMENT":
        return <Badge variant="secondary" className="bg-yellow-500 hover:bg-yellow-600 text-white">Chờ tiền mặt</Badge>;
      default:
        return <Badge variant="outline">{status}</Badge>;
    }
  };

  const handleResetFilters = () => {
    setSearch("");
    setBookingStatus("ALL");
    setPaymentStatus("ALL");
    setStartDate("");
    setEndDate("");
    setPage(0);
  };

  return (
    <div className="p-8 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold tracking-tight text-gray-900">Quản lý Đặt sân</h1>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="p-3 bg-blue-50 rounded-lg text-blue-600 shrink-0">
            <CalendarDays className="h-6 w-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Tổng đơn đặt sân</p>
            <h3 className="text-2xl font-bold text-slate-900 mt-1">
              {isLoading ? (
                <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
              ) : (
                data?.stats?.totalBookings ?? 0
              )}
            </h3>
            <p className="text-[11px] text-slate-400 mt-1">Tất cả trạng thái (gồm đơn hủy)</p>
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="p-3 bg-emerald-50 rounded-lg text-emerald-600 shrink-0">
            <DollarSign className="h-6 w-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Tổng GMV</p>
            <h3 className="text-2xl font-bold text-slate-900 mt-1">
              {isLoading ? (
                <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
              ) : (
                formatVND(data?.stats?.totalGMV ?? 0)
              )}
            </h3>
            <p className="text-[11px] text-slate-400 mt-1">Đơn Chờ duyệt/Xác nhận/Hoàn thành</p>
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100 flex items-center gap-4">
          <div className="p-3 bg-indigo-50 rounded-lg text-indigo-600 shrink-0">
            <Activity className="h-6 w-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Phí dịch vụ Platform</p>
            <h3 className="text-2xl font-bold text-slate-900 mt-1">
              {isLoading ? (
                <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
              ) : (
                formatVND(data?.stats?.totalServiceFee ?? 0)
              )}
            </h3>
            <p className="text-[11px] text-slate-400 mt-1">Chỉ tính các đơn Hoàn thành</p>
          </div>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="bg-white p-4 rounded-xl shadow-sm border border-slate-100 space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-12 gap-4 items-end">
          <div className="md:col-span-4 space-y-1.5">
            <label className="text-xs font-semibold text-slate-500 uppercase">Tìm kiếm</label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
              <Input
                placeholder="Tên khách hàng, email, tên sân..."
                className="pl-9 bg-slate-50 border-slate-200 focus-visible:ring-emerald-500 focus-visible:border-emerald-500"
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
              />
            </div>
          </div>

          <div className="md:col-span-2 space-y-1.5">
            <label className="text-xs font-semibold text-slate-500 uppercase">Trạng thái đặt</label>
            <Select
              value={bookingStatus}
              onValueChange={(val) => {
                setBookingStatus(val);
                setPage(0);
              }}
            >
              <SelectTrigger className="bg-slate-50 border-slate-200">
                <SelectValue placeholder="Trạng thái" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">Tất cả</SelectItem>
                <SelectItem value="PENDING_PAYMENT">Chờ thanh toán</SelectItem>
                <SelectItem value="PENDING">Chờ duyệt</SelectItem>
                <SelectItem value="CONFIRMED">Đã xác nhận</SelectItem>
                <SelectItem value="COMPLETED">Hoàn thành</SelectItem>
                <SelectItem value="CANCELLED">Đã hủy</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="md:col-span-2 space-y-1.5">
            <label className="text-xs font-semibold text-slate-500 uppercase">Thanh toán</label>
            <Select
              value={paymentStatus}
              onValueChange={(val) => {
                setPaymentStatus(val);
                setPage(0);
              }}
            >
              <SelectTrigger className="bg-slate-50 border-slate-200">
                <SelectValue placeholder="Thanh toán" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">Tất cả</SelectItem>
                <SelectItem value="UNPAID">Chưa thanh toán</SelectItem>
                <SelectItem value="PAID">Đã thanh toán</SelectItem>
                <SelectItem value="REFUNDED">Đã hoàn tiền</SelectItem>
                <SelectItem value="DEPOSITED">Đã cọc</SelectItem>
                <SelectItem value="AWAITING_CASH_PAYMENT">Chờ tiền mặt</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="md:col-span-2 space-y-1.5">
            <label className="text-xs font-semibold text-slate-500 uppercase">Từ ngày (Chơi)</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => {
                setStartDate(e.target.value);
                setPage(0);
              }}
              max={endDate || undefined}
              className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-emerald-400 focus:border-transparent bg-slate-50"
            />
          </div>

          <div className="md:col-span-2 space-y-1.5">
            <label className="text-xs font-semibold text-slate-500 uppercase">Đến ngày (Chơi)</label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => {
                setEndDate(e.target.value);
                setPage(0);
              }}
              min={startDate || undefined}
              className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-emerald-400 focus:border-transparent bg-slate-50"
            />
          </div>
        </div>

        <div className="flex justify-end">
          <Button variant="outline" size="sm" onClick={handleResetFilters} className="text-slate-600 hover:text-slate-900 border-slate-200">
            Đặt lại bộ lọc
          </Button>
        </div>
      </div>

      {/* Main Table */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <Table>
          <TableHeader className="bg-slate-50/80">
            <TableRow>
              <TableHead className="font-semibold text-slate-900">ID</TableHead>
              <TableHead className="font-semibold text-slate-900">Khách hàng</TableHead>
              <TableHead className="font-semibold text-slate-900">Sân & Chủ sân</TableHead>
              <TableHead className="font-semibold text-slate-900">Ngày chơi & Giờ</TableHead>
              <TableHead className="font-semibold text-slate-900">Tổng tiền</TableHead>
              <TableHead className="font-semibold text-slate-900">Phí DV</TableHead>
              <TableHead className="font-semibold text-slate-900">Trạng thái đặt</TableHead>
              <TableHead className="font-semibold text-slate-900">Thanh toán</TableHead>
              <TableHead className="font-semibold text-slate-900 text-right">Chi tiết</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={9} className="h-48 text-center">
                  <Loader2 className="h-6 w-6 animate-spin mx-auto text-emerald-600" />
                </TableCell>
              </TableRow>
            ) : isError ? (
              <TableRow>
                <TableCell colSpan={9} className="h-48 text-center text-red-500 font-medium">
                  Đã xảy ra lỗi khi tải dữ liệu! Vui lòng thử lại.
                </TableCell>
              </TableRow>
            ) : data?.bookings?.content?.length === 0 ? (
              <TableRow>
                <TableCell colSpan={9} className="h-48 text-center text-slate-500">
                  Không tìm thấy lịch đặt sân nào phù hợp với bộ lọc.
                </TableCell>
              </TableRow>
            ) : (
              data?.bookings?.content?.map((booking) => (
                <TableRow key={booking.bookingId} className="hover:bg-slate-50/50 transition-colors">
                  <TableCell className="font-semibold text-slate-700">#{booking.bookingId}</TableCell>
                  <TableCell>
                    <div className="flex flex-col">
                      <span className="font-medium text-slate-950">{booking.customerName}</span>
                      <span className="text-xs text-slate-500">{booking.customerEmail}</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-col">
                      <span className="font-medium text-slate-900">{booking.stadiumName}</span>
                      <span className="text-xs text-slate-500">Chủ: {booking.ownerName}</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-col">
                      <span className="text-slate-900">
                        {booking.reservationDate
                          ? format(new Date(booking.reservationDate), "dd/MM/yyyy")
                          : "N/A"}
                      </span>
                      <span className="text-xs font-medium text-emerald-700">{booking.timeSlot}</span>
                    </div>
                  </TableCell>
                  <TableCell className="font-medium text-slate-950">
                    {formatVND(booking.totalPrice)}
                  </TableCell>
                  <TableCell className="text-slate-600">
                    {formatVND(booking.serviceFee)}
                  </TableCell>
                  <TableCell>{getBookingStatusBadge(booking.bookingStatus)}</TableCell>
                  <TableCell>{getPaymentStatusBadge(booking.paymentStatus)}</TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => {
                        setSelectedBooking(booking);
                        setIsDetailOpen(true);
                      }}
                      className="h-8 w-8 text-slate-500 hover:text-slate-900 hover:bg-slate-100"
                    >
                      <Eye className="h-4 w-4" />
                    </Button>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* Pagination */}
      {!isLoading && !isError && data && data.bookings && (
        <div className="flex justify-between items-center pt-2">
          <span className="text-sm text-slate-500 font-medium">
            Hiển thị {data.bookings.content.length} / {data.bookings.totalElements} đơn đặt
          </span>
          <div className="flex items-center gap-4">
            <span className="text-sm text-slate-500 font-medium">
              Trang {page + 1} / {data.bookings.totalPages || 1}
            </span>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
                className="w-20 border-slate-200"
              >
                Trước
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={page >= (data.bookings.totalPages - 1)}
                onClick={() => setPage((p) => p + 1)}
                className="w-20 border-slate-200"
              >
                Sau
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Slide-out Drawer Detail View */}
      <Sheet open={isDetailOpen} onOpenChange={setIsDetailOpen}>
        <SheetContent className="w-full sm:max-w-md overflow-y-auto scrollbar-thin bg-background border-l border-border">
          <SheetHeader className="border-b border-border pb-4 mb-4">
            <SheetTitle className="text-lg font-bold text-foreground flex items-center gap-2">
              Chi tiết Đặt sân #{selectedBooking?.bookingId}
            </SheetTitle>
            <SheetDescription>
              Xem chi tiết toàn bộ thông tin đơn hàng và trạng thái.
            </SheetDescription>
          </SheetHeader>

          {selectedBooking && (
            <div className="space-y-5 text-sm px-4 pb-6">
              {/* Customer Info */}
              <div className="space-y-1">
                <h4 className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Khách hàng</h4>
                <p className="font-semibold text-foreground">{selectedBooking.customerName}</p>
                <p className="text-xs text-muted-foreground">{selectedBooking.customerEmail}</p>
              </div>

              {/* Venue & Owner Info */}
              <div className="space-y-1">
                <h4 className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Sân & Đối tác</h4>
                <p className="font-semibold text-foreground">{selectedBooking.stadiumName}</p>
                <p className="text-xs text-muted-foreground">Chủ sân: {selectedBooking.ownerName}</p>
              </div>

              {/* Time & Reservation Info */}
              <div className="space-y-2">
                <h4 className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Lịch ra sân</h4>
                <div className="space-y-1.5">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Ngày chơi:</span>
                    <span className="font-semibold text-foreground">
                      {selectedBooking.reservationDate
                        ? format(new Date(selectedBooking.reservationDate), "dd/MM/yyyy")
                        : "N/A"}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Khung giờ:</span>
                    <span className="font-semibold text-primary">{selectedBooking.timeSlot}</span>
                  </div>
                  <div className="flex justify-between border-t border-border pt-1.5">
                    <span className="text-muted-foreground">Ngày tạo đơn:</span>
                    <span className="text-foreground">
                      {selectedBooking.bookingDate
                        ? format(new Date(selectedBooking.bookingDate), "dd/MM/yyyy HH:mm")
                        : "N/A"}
                    </span>
                  </div>
                </div>
              </div>

              {/* Financial Info — highlighted, đây là thông tin Admin cần nhất */}
              <div className="space-y-2">
                <h4 className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Chi phí</h4>
                <div className="bg-primary/5 p-3 rounded-lg border border-primary/20 space-y-2">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Tổng tiền thu hộ (GMV):</span>
                    <span className="font-semibold text-foreground">{formatVND(selectedBooking.totalPrice)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Phí dịch vụ platform:</span>
                    <span className="font-bold text-primary">{formatVND(selectedBooking.serviceFee)}</span>
                  </div>
                </div>
              </div>

              {/* Status Info */}
              <div className="space-y-2">
                <h4 className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Trạng thái</h4>
                <div className="bg-muted/50 p-3 rounded-lg border border-border space-y-2">
                  <div className="flex justify-between items-center">
                    <span className="text-muted-foreground">Đặt sân:</span>
                    <span>{getBookingStatusBadge(selectedBooking.bookingStatus)}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-muted-foreground">Thanh toán:</span>
                    <span>{getPaymentStatusBadge(selectedBooking.paymentStatus)}</span>
                  </div>
                </div>
              </div>

              {/* Note / Cancel Reason */}
              {selectedBooking.note && (
                <div className="space-y-2">
                  <h4 className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Ghi chú của khách</h4>
                  <div className="bg-muted/50 p-3 rounded-lg border border-border text-foreground whitespace-pre-wrap">
                    {selectedBooking.note}
                  </div>
                </div>
              )}

              {selectedBooking.bookingStatus === "CANCELLED" && (
                <div className="space-y-2">
                  <h4 className="text-xs font-bold text-destructive uppercase tracking-wider flex items-center gap-1">
                    <AlertTriangle className="h-3.5 w-3.5" /> Lý do hủy đơn
                  </h4>
                  <div className="bg-destructive/10 border border-destructive/20 text-destructive p-3 rounded-lg font-medium whitespace-pre-wrap">
                    {selectedBooking.cancelReason || "Không có lý do cụ thể."}
                  </div>
                </div>
              )}
            </div>
          )}
        </SheetContent>
      </Sheet>
    </div>
  );
}
