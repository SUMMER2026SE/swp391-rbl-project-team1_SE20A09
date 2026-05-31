'use client'

import { useState, useEffect, useCallback } from "react";
import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Checkbox } from "@/components/ui/checkbox";
import {
  CheckCircle, XCircle, ChevronDown, ChevronUp,
  Loader2, AlertCircle, RefreshCw
} from "lucide-react";
import { get, put } from "@/lib/api";

// ── Types ────────────────────────────────────────────────────
interface CustomerInfo {
  userId: number;
  fullName: string;
  email: string;
  phoneNumber: string;
  avatarUrl: string | null;
}

interface StadiumInfo {
  stadiumId: number;
  stadiumName: string;
  address: string;
  sportType: string;
}

interface SlotInfo {
  slotId: number;
  startTime: string;
  endTime: string;
}

interface BookingData {
  bookingId: number;
  customer: CustomerInfo;
  stadium: StadiumInfo;
  slot: SlotInfo;
  totalPrice: number;
  bookingStatus: string;
  paymentStatus: string;
  note: string | null;
  bookingDate: string;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// ── Component ────────────────────────────────────────────────
function BookingManagementPage() {
  const [bookings, setBookings] = useState<BookingData[]>([]);
  const [expandedRow, setExpandedRow] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState("all");
  const [processingId, setProcessingId] = useState<number | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const [showRejectDialog, setShowRejectDialog] = useState<number | null>(null);
  const [totalElements, setTotalElements] = useState(0);

  const fetchBookings = useCallback(async (status?: string) => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      params.set("size", "50");
      if (status && status !== "all") {
        params.set("status", status.toUpperCase());
      }
      const data = await get<PageResponse<BookingData>>(
        `/owner/bookings?${params.toString()}`
      );
      setBookings(data.content);
      setTotalElements(data.totalElements);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : "Không thể tải danh sách đặt sân";
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchBookings(activeTab);
  }, [activeTab, fetchBookings]);

  const handleConfirm = async (bookingId: number) => {
    setProcessingId(bookingId);
    try {
      await put(`/owner/bookings/${bookingId}/action`, {
        action: "CONFIRM",
      });
      await fetchBookings(activeTab);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : "Không thể xác nhận đơn";
      alert(errorMessage);
    } finally {
      setProcessingId(null);
    }
  };

  const handleReject = async (bookingId: number) => {
    if (!rejectReason.trim()) {
      alert("Vui lòng nhập lý do từ chối.");
      return;
    }
    setProcessingId(bookingId);
    try {
      await put(`/owner/bookings/${bookingId}/action`, {
        action: "REJECT",
        reason: rejectReason,
      });
      setShowRejectDialog(null);
      setRejectReason("");
      await fetchBookings(activeTab);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : "Không thể từ chối đơn";
      alert(errorMessage);
    } finally {
      setProcessingId(null);
    }
  };

  const getStatusBadge = (status: string) => {
    const config: Record<string, { label: string; className: string }> = {
      PENDING: { label: "Chờ xác nhận", className: "bg-yellow-100 text-yellow-700" },
      CONFIRMED: { label: "Đã xác nhận", className: "bg-green-100 text-green-700" },
      CANCELLED: { label: "Đã từ chối", className: "bg-red-100 text-red-700" },
      COMPLETED: { label: "Hoàn thành", className: "bg-blue-100 text-blue-700" },
    };
    const item = config[status] || { label: status, className: "bg-gray-100 text-gray-700" };
    return <Badge className={item.className}>{item.label}</Badge>;
  };

  const getPaymentBadge = (status: string) => {
    const config: Record<string, { label: string; className: string }> = {
      UNPAID: { label: "Chưa thanh toán", className: "bg-orange-100 text-orange-700" },
      PAID: { label: "Đã thanh toán", className: "bg-green-100 text-green-700" },
      REFUNDED: { label: "Đã hoàn tiền", className: "bg-purple-100 text-purple-700" },
    };
    const item = config[status] || { label: status, className: "bg-gray-100 text-gray-700" };
    return <Badge className={item.className}>{item.label}</Badge>;
  };

  const formatDateTime = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleString("vi-VN", {
      day: "2-digit", month: "2-digit", year: "numeric",
      hour: "2-digit", minute: "2-digit",
    });
  };

  const formatTime = (startStr: string, endStr: string) => {
    const start = new Date(startStr);
    const end = new Date(endStr);
    const dateStr = start.toLocaleDateString("vi-VN");
    const startTime = start.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
    const endTime = end.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
    return `${dateStr} | ${startTime} - ${endTime}`;
  };

  const filterBookings = (status: string) => {
    if (status === "all") return bookings;
    return bookings.filter((b) => b.bookingStatus === status.toUpperCase());
  };

  const BookingRow = ({ booking }: { booking: BookingData }) => {
    const isExpanded = expandedRow === booking.bookingId;
    const isProcessing = processingId === booking.bookingId;

    return (
      <>
        <tr className="border-b hover:bg-muted transition-colors">
          <td className="p-3">
            <Checkbox />
          </td>
          <td className="p-3 font-mono text-sm">BK{String(booking.bookingId).padStart(6, '0')}</td>
          <td className="p-3">{booking.customer.fullName}</td>
          <td className="p-3">
            <div>
              <div className="font-medium">{booking.stadium.stadiumName}</div>
              <div className="text-xs text-muted-foreground">{booking.stadium.sportType}</div>
            </div>
          </td>
          <td className="p-3 text-sm">{formatTime(booking.slot.startTime, booking.slot.endTime)}</td>
          <td className="p-3 text-right font-medium">
            {booking.totalPrice.toLocaleString('vi-VN')}đ
          </td>
          <td className="p-3">{getStatusBadge(booking.bookingStatus)}</td>
          <td className="p-3">
            <div className="flex gap-2 items-center">
              {booking.bookingStatus === "PENDING" && (
                <>
                  <Button
                    size="sm"
                    variant="default"
                    onClick={() => handleConfirm(booking.bookingId)}
                    disabled={isProcessing}
                  >
                    {isProcessing ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <CheckCircle className="h-4 w-4 mr-1" />
                    )}
                    Duyệt
                  </Button>
                  <Button
                    size="sm"
                    variant="destructive"
                    onClick={() => setShowRejectDialog(booking.bookingId)}
                    disabled={isProcessing}
                  >
                    <XCircle className="h-4 w-4 mr-1" />
                    Từ chối
                  </Button>
                </>
              )}
              <Button
                size="sm"
                variant="ghost"
                onClick={() =>
                  setExpandedRow(isExpanded ? null : booking.bookingId)
                }
              >
                {isExpanded ? (
                  <ChevronUp className="h-4 w-4" />
                ) : (
                  <ChevronDown className="h-4 w-4" />
                )}
              </Button>
            </div>
          </td>
        </tr>

        {/* Reject dialog inline */}
        {showRejectDialog === booking.bookingId && (
          <tr className="bg-red-50 dark:bg-red-950/20">
            <td colSpan={8} className="p-4">
              <div className="flex items-center gap-3">
                <input
                  type="text"
                  placeholder="Nhập lý do từ chối..."
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                  className="flex-1 border rounded-lg px-3 py-2 text-sm"
                />
                <Button
                  size="sm"
                  variant="destructive"
                  onClick={() => handleReject(booking.bookingId)}
                  disabled={isProcessing}
                >
                  {isProcessing ? (
                    <Loader2 className="h-4 w-4 animate-spin mr-1" />
                  ) : null}
                  Xác nhận từ chối
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => {
                    setShowRejectDialog(null);
                    setRejectReason("");
                  }}
                >
                  Hủy
                </Button>
              </div>
            </td>
          </tr>
        )}

        {/* Expanded details */}
        {isExpanded && (
          <tr className="bg-muted/50">
            <td colSpan={8} className="p-6">
              <div className="grid grid-cols-2 gap-6">
                <div>
                  <h4 className="font-semibold mb-3">Thông tin khách hàng</h4>
                  <div className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Họ tên</span>
                      <span>{booking.customer.fullName}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Số điện thoại</span>
                      <span>{booking.customer.phoneNumber}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Email</span>
                      <span>{booking.customer.email}</span>
                    </div>
                  </div>
                </div>

                <div>
                  <h4 className="font-semibold mb-3">Chi tiết đặt sân</h4>
                  <div className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Địa chỉ</span>
                      <span className="text-right max-w-[200px]">{booking.stadium.address}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Thanh toán</span>
                      {getPaymentBadge(booking.paymentStatus)}
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Ngày đặt</span>
                      <span>{formatDateTime(booking.bookingDate)}</span>
                    </div>
                    {booking.note && (
                      <div>
                        <span className="text-muted-foreground block mb-1">
                          Ghi chú
                        </span>
                        <p className="bg-background p-2 rounded text-sm">{booking.note}</p>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </td>
          </tr>
        )}
      </>
    );
  };

  const renderBookingTable = (filteredBookings: BookingData[]) => (
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead className="bg-muted">
          <tr>
            <th className="p-3 text-left w-10">
              <Checkbox />
            </th>
            <th className="p-3 text-left">Mã đơn</th>
            <th className="p-3 text-left">Khách hàng</th>
            <th className="p-3 text-left">Sân</th>
            <th className="p-3 text-left">Thời gian</th>
            <th className="p-3 text-right">Số tiền</th>
            <th className="p-3 text-left">Trạng thái</th>
            <th className="p-3 text-left">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {filteredBookings.length === 0 ? (
            <tr>
              <td colSpan={8} className="p-8 text-center text-muted-foreground">
                Không có đơn đặt sân nào.
              </td>
            </tr>
          ) : (
            filteredBookings.map((booking) => (
              <BookingRow key={booking.bookingId} booking={booking} />
            ))
          )}
        </tbody>
      </table>
    </div>
  );

  if (error) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <div className="container mx-auto px-4 py-8">
          <Card className="p-8">
            <div className="text-center space-y-4">
              <AlertCircle className="h-12 w-12 mx-auto text-red-500" />
              <p className="text-lg text-red-600">{error}</p>
              <Button onClick={() => fetchBookings(activeTab)}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Thử lại
              </Button>
            </div>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl font-bold">Quản lý đặt sân</h1>
          <Button
            variant="outline"
            onClick={() => fetchBookings(activeTab)}
            disabled={loading}
          >
            <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </Button>
        </div>

        {/* Tabs */}
        <Tabs value={activeTab} onValueChange={setActiveTab}>
          <div className="flex items-center justify-between mb-6">
            <TabsList>
              <TabsTrigger value="all">
                Tất cả ({totalElements})
              </TabsTrigger>
              <TabsTrigger value="pending">
                Chờ xác nhận ({filterBookings("pending").length})
              </TabsTrigger>
              <TabsTrigger value="confirmed">
                Đã xác nhận ({filterBookings("confirmed").length})
              </TabsTrigger>
              <TabsTrigger value="cancelled">
                Đã từ chối ({filterBookings("cancelled").length})
              </TabsTrigger>
              <TabsTrigger value="completed">
                Hoàn thành ({filterBookings("completed").length})
              </TabsTrigger>
            </TabsList>
          </div>

          <Card>
            <CardContent className="p-0">
              {loading ? (
                <div className="flex items-center justify-center p-12">
                  <Loader2 className="h-8 w-8 animate-spin text-primary" />
                  <span className="ml-3 text-muted-foreground">Đang tải...</span>
                </div>
              ) : (
                <>
                  <TabsContent value="all" className="m-0">
                    {renderBookingTable(bookings)}
                  </TabsContent>
                  <TabsContent value="pending" className="m-0">
                    {renderBookingTable(filterBookings("pending"))}
                  </TabsContent>
                  <TabsContent value="confirmed" className="m-0">
                    {renderBookingTable(filterBookings("confirmed"))}
                  </TabsContent>
                  <TabsContent value="cancelled" className="m-0">
                    {renderBookingTable(filterBookings("cancelled"))}
                  </TabsContent>
                  <TabsContent value="completed" className="m-0">
                    {renderBookingTable(filterBookings("completed"))}
                  </TabsContent>
                </>
              )}
            </CardContent>
          </Card>
        </Tabs>
      </div>
    </div>
  );
}

export default BookingManagementPage;
