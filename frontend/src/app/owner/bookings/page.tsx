'use client'

import { useState, useEffect, useCallback } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
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
  CheckCircle, 
  XCircle, 
  ChevronDown, 
  ChevronUp, 
  RotateCcw, 
  Clock, 
  DollarSign, 
  Info, 
  User, 
  Calendar,
  AlertCircle,
  HelpCircle,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { toast } from "sonner";
import { get, post } from "@/lib/api";
import { cancelBooking } from "@/lib/bookings-api";
import type { PageResponse } from "@/types/common";

interface BookingItem {
  id: number;
  displayId: string;
  customer: {
    name: string;
    phone: string;
    email: string;
  };
  venue: string;
  date: string;
  time: string;
  amount: number;
  refundAmount: number;
  paymentStatus: string;
  status: string;
  notes: string;
  playTimeRaw: string;
}

function BookingManagementPage() {
  const [expandedRow, setExpandedRow] = useState<number | null>(null);
  const [bookingList, setBookingList] = useState<BookingItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // States phục vụ Hoàn Tiền (UC-OWN-12)
  const [selectedBooking, setSelectedBooking] = useState<BookingItem | null>(null);
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState("");
  const [reasonType, setReasonType] = useState("CUSTOMER_REQUEST");
  const [proofUrl, setProofUrl] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Xem trước tiền hoàn từ Backend (Tránh clock skew của client)
  const [previewData, setPreviewData] = useState<any>(null);
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);

  // States phục vụ hiển thị kết quả thành công Premium
  const [successData, setSuccessData] = useState<any>(null);
  const [isSuccessModalOpen, setIsSuccessModalOpen] = useState(false);

  // Hủy đơn chưa thu tiền (vd: đang chờ thu tiền mặt) — không có gì để hoàn nên
  // dùng thẳng luồng cancelBooking() thay vì modal "Hủy & Hoàn Tiền" (yêu cầu
  // có giao dịch PAID/DEPOSITED để tính % hoàn).
  const [cancelOnlyBooking, setCancelOnlyBooking] = useState<BookingItem | null>(null);
  const [cancelOnlyReason, setCancelOnlyReason] = useState("");
  const [isCancelOnlySubmitting, setIsCancelOnlySubmitting] = useState(false);

  const [activeTab, setActiveTab] = useState("all");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Fetch danh sách đặt sân thực tế từ Backend
  const fetchBookings = useCallback(async () => {
    try {
      setIsLoading(true);
      const query = new URLSearchParams({
        page: String(page),
        size: "20",
      });
      if (activeTab !== "all") {
        query.set("status", activeTab.toUpperCase());
      }
      const data = await get<any>(
        `/owner/bookings?${query.toString()}`
      );

      // Defensive: hỗ trợ cả mảng phẳng (API cũ) và PageResponse (API mới),
      // đồng thời không đưa dữ liệu sai kiểu vào state.
      const list: BookingItem[] = Array.isArray(data)
        ? data
        : Array.isArray(data?.content)
          ? data.content
          : [];
      setBookingList(list);
      setTotalPages(data?.totalPages ?? 0);
      setTotalElements(data?.totalElements ?? list.length);
    } catch (error: any) {
      console.error("Error fetching bookings:", error);
      toast.error("Không thể tải danh sách đặt sân từ máy chủ: " + error.message);
    } finally {
      setIsLoading(false);
    }
  }, [activeTab, page]);

  useEffect(() => {
    fetchBookings();
  }, [fetchBookings]);

  const handleTabChange = (value: string) => {
    setActiveTab(value);
    setPage(0);
    setExpandedRow(null);
  };

  // Hàm tính toán dự phóng hoàn tiền ở Frontend (Hiển thị dự báo nhanh)
  const calculateExpectedRefund = (playTimeStr: string, price: number) => {
    const playTime = new Date(playTimeStr);
    const now = new Date();
    const diffMs = playTime.getTime() - now.getTime();
    const diffHours = diffMs / (1000 * 60 * 60);

    if (diffHours >= 24) {
      return { 
        percentage: 100, 
        amount: price, 
        label: "Hoàn 100%", 
        desc: "Hủy trước giờ chơi >= 24 giờ. Khách hàng nhận lại toàn bộ tiền sân.",
        badgeClass: "bg-emerald-100 text-emerald-800 dark:bg-emerald-950/30 dark:text-emerald-400"
      };
    } else if (diffHours >= 12) {
      return { 
        percentage: 50, 
        amount: price * 0.5, 
        label: "Hoàn 50%", 
        desc: "Hủy trước giờ chơi từ 12 giờ đến dưới 24 giờ. Khách hàng nhận lại 50% tiền sân.",
        badgeClass: "bg-amber-100 text-amber-800 dark:bg-amber-950/30 dark:text-amber-400"
      };
    } else {
      return { 
        percentage: 0, 
        amount: 0, 
        label: "Hoàn 0%", 
        desc: "Hủy quá sát giờ chơi (< 12 giờ hoặc đã diễn ra). Khách hàng không được hoàn tiền theo điều khoản.",
        badgeClass: "bg-rose-100 text-rose-800 dark:bg-rose-950/30 dark:text-rose-400"
      };
    }
  };

  const getStatusBadge = (status: string) => {
    const config = {
      pending: { label: "Chờ xác nhận", className: "bg-yellow-100 text-yellow-700 dark:bg-yellow-950/30 dark:text-yellow-400" },
      confirmed: { label: "Đã xác nhận", className: "bg-blue-100 text-blue-700 dark:bg-blue-950/30 dark:text-blue-400" },
      rejected: { label: "Đã từ chối", className: "bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-400" },
      completed: { label: "Hoàn thành", className: "bg-green-100 text-green-700 dark:bg-green-950/30 dark:text-green-400" },
      cancelled: { label: "Đã hủy", className: "bg-rose-100 text-rose-700 dark:bg-rose-950/30 dark:text-rose-400" },
    };
    const item = config[status.toLowerCase() as keyof typeof config] || { label: status, className: "bg-gray-100 text-gray-700" };
    return <Badge className={`${item.className} border-none shadow-none font-medium px-2.5 py-0.5`}>{item.label}</Badge>;
  };

  const getPaymentBadge = (status: string) => {
    const config = {
      unpaid: { label: "Chưa thanh toán", className: "bg-orange-100 text-orange-700 dark:bg-orange-950/30 dark:text-orange-400" },
      paid: { label: "Đã thanh toán", className: "bg-emerald-100 text-emerald-700 dark:bg-emerald-950/30 dark:text-emerald-400" },
      refunded: { label: "Đã hoàn tiền", className: "bg-purple-100 text-purple-700 dark:bg-purple-950/30 dark:text-purple-400" },
      awaiting_cash_payment: { label: "Chờ thu tiền mặt", className: "bg-amber-100 text-amber-700 dark:bg-amber-950/30 dark:text-amber-400" },
    };
    const item = config[status.toLowerCase() as keyof typeof config] || { label: status, className: "bg-gray-100 text-gray-700" };
    return <Badge className={`${item.className} border-none shadow-none font-medium px-2.5 py-0.5`}>{item.label}</Badge>;
  };

  const filterBookings = (status?: string) => {
    // Defensive: không bao giờ gọi .filter trên null/undefined/object.
    const list = Array.isArray(bookingList) ? bookingList : [];
    if (!status) return list;
    return list.filter((b) => b.status && b.status.toLowerCase() === status.toLowerCase());
  };

  // Load preview data mỗi khi mở modal hoặc thay đổi loại nguyên nhân hủy
  const fetchRefundPreview = useCallback(async (bookingId: number, type: string) => {
    setIsPreviewLoading(true);
    try {
      const data = await get<any>(`/owner/bookings/${bookingId}/refund/preview?reasonType=${type}`);
      setPreviewData(data);
    } catch (error: any) {
      console.error("Error fetching refund preview:", error);
      toast.error("Không tải được thông tin xem trước hoàn tiền.");
      setPreviewData(null);
    } finally {
      setIsPreviewLoading(false);
    }
  }, []);

  const handleOpenRefundModal = (booking: BookingItem) => {
    setSelectedBooking(booking);
    setCancelReason("");
    setReasonType("CUSTOMER_REQUEST");
    setProofUrl("");
    setIsCancelModalOpen(true);
    setPreviewData(null);
    fetchRefundPreview(booking.id, "CUSTOMER_REQUEST");
  };

  // Lắng nghe thay đổi của reasonType để fetch lại preview
  useEffect(() => {
    if (selectedBooking && isCancelModalOpen) {
      fetchRefundPreview(selectedBooking.id, reasonType);
    }
  }, [reasonType, selectedBooking, isCancelModalOpen, fetchRefundPreview]);

  // Hàm gửi Request thực tế lên Backend để thực hiện hoàn tiền
  const handleConfirmRefund = async () => {
    if (!selectedBooking) return;
    if (!cancelReason.trim()) {
      toast.error("Vui lòng nhập lý do hủy đặt sân!");
      return;
    }

    if (reasonType === "OWNER_FAULT" && !proofUrl.trim()) {
      toast.error("Vui lòng cung cấp bằng chứng (link ảnh/mô tả) cho sự cố từ phía sân!");
      return;
    }

    try {
      setIsSubmitting(true);
      toast.loading("Đang kết nối backend xử lý hoàn tiền...", { id: "refund-process" });

      // Gọi API thật
      const response = await post<any>(`/owner/bookings/${selectedBooking.id}/refund`, {
        reason: cancelReason.trim(),
        reasonType,
        proofUrl: reasonType === "OWNER_FAULT" ? proofUrl.trim() : null
      });

      // Thành công: Cập nhật state ở local
      setBookingList(prev => prev.map(b => {
        if (b.id === selectedBooking.id) {
          return {
            ...b,
            status: "cancelled",
            paymentStatus: "refunded"
          };
        }
        return b;
      }));

      // Đưa dữ liệu kết quả từ server trả về vào State thành công để show Popup Premium
      setSuccessData(response);
      setIsCancelModalOpen(false);
      setIsSuccessModalOpen(true);
      toast.success("Đã hoàn tiền và giải phóng sân thành công!", { id: "refund-process" });
    } catch (error: any) {
      console.error(error);
      toast.error(error.message || "Không thể thực hiện hoàn tiền. Vui lòng kiểm tra lại quyền sở hữu hoặc trạng thái đơn hàng!", { id: "refund-process" });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleConfirmCancelOnly = async () => {
    if (!cancelOnlyBooking) return;
    try {
      setIsCancelOnlySubmitting(true);
      await cancelBooking(cancelOnlyBooking.id, cancelOnlyReason.trim() || undefined);
      setBookingList(prev => prev.map(b =>
        b.id === cancelOnlyBooking.id ? { ...b, status: "cancelled" } : b
      ));
      toast.success("Đã hủy đơn thành công");
      setCancelOnlyBooking(null);
      setCancelOnlyReason("");
    } catch (error: any) {
      toast.error(error.message || "Không thể hủy đơn, vui lòng thử lại");
    } finally {
      setIsCancelOnlySubmitting(false);
    }
  };

  const BookingRow = ({ booking }: { booking: BookingItem }) => {
    const isExpanded = expandedRow === booking.id;
    const canRefund = booking.status.toLowerCase() === "confirmed" && booking.paymentStatus.toLowerCase() === "paid";
    const canCancelUnpaid = booking.status.toLowerCase() === "confirmed"
      && booking.paymentStatus.toLowerCase() === "awaiting_cash_payment";

    return (
      <>
        <tr className="border-b hover:bg-muted/40 transition-colors duration-150">
          <td className="p-4 align-middle">
            <Checkbox />
          </td>
          <td className="p-4 align-middle font-mono text-sm font-semibold text-primary">{booking.displayId}</td>
          <td className="p-4 align-middle font-medium">{booking.customer.name}</td>
          <td className="p-4 align-middle text-muted-foreground">{booking.venue}</td>
          <td className="p-4 align-middle text-sm">{booking.date}</td>
          <td className="p-4 align-middle text-sm font-medium">{booking.time}</td>
          <td className="p-4 align-middle text-right font-semibold text-slate-900 dark:text-slate-100">
            {booking.amount.toLocaleString('vi-VN')}đ
          </td>
          <td className="p-4 align-middle text-right text-rose-600 dark:text-rose-400 font-medium">
            {booking.refundAmount > 0 ? `-${booking.refundAmount.toLocaleString('vi-VN')}đ` : "0đ"}
          </td>
          <td className="p-4 align-middle text-right text-emerald-600 dark:text-emerald-400 font-bold">
            {(() => {
              const isPaidType = booking.paymentStatus.toLowerCase() === "paid" || booking.paymentStatus.toLowerCase() === "refunded";
              const netVal = isPaidType ? (booking.amount - booking.refundAmount) : 0;
              return `${netVal.toLocaleString('vi-VN')}đ`;
            })()}
          </td>
          <td className="p-4 align-middle">
            <div className="flex flex-col gap-1 items-start">
              {getStatusBadge(booking.status)}
              {getPaymentBadge(booking.paymentStatus)}
            </div>
          </td>
          <td className="p-4 align-middle">
            <div className="flex gap-2 items-center justify-end">
              {canRefund && (
                <Button
                  size="sm"
                  variant="destructive"
                  className="bg-rose-600 hover:bg-rose-700 text-white font-medium shadow-sm transition-all duration-200"
                  onClick={() => handleOpenRefundModal(booking)}
                >
                  <RotateCcw className="h-3.5 w-3.5 mr-1.5 animate-spin-reverse" />
                  Hủy & Hoàn Tiền
                </Button>
              )}
              {canCancelUnpaid && (
                <Button
                  size="sm"
                  variant="outline"
                  className="border-amber-300 text-amber-700 hover:bg-amber-50 dark:border-amber-800 dark:text-amber-400"
                  onClick={() => setCancelOnlyBooking(booking)}
                >
                  <XCircle className="h-3.5 w-3.5 mr-1.5" />
                  Hủy đơn
                </Button>
              )}
              <Button
                size="sm"
                variant="ghost"
                onClick={() =>
                  setExpandedRow(isExpanded ? null : booking.id)
                }
                className="hover:bg-muted"
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

        {isExpanded && (
          <tr className="bg-muted/30">
            <td colSpan={11} className="p-6">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6 rounded-lg border bg-card p-4 shadow-sm animate-in fade-in slide-in-from-top-1 duration-200">
                <div className="space-y-3">
                  <h4 className="font-semibold text-sm flex items-center gap-1.5 text-primary border-b pb-1.5">
                    <User className="h-4 w-4 text-blue-500" />
                    Thông tin khách hàng
                  </h4>
                  <div className="space-y-1.5 text-sm">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Họ tên:</span>
                      <span className="font-medium">{booking.customer.name}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Số điện thoại:</span>
                      <span className="font-mono">{booking.customer.phone}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Email:</span>
                      <span className="text-blue-600 dark:text-blue-400 font-mono text-xs">{booking.customer.email}</span>
                    </div>
                  </div>
                </div>

                <div className="space-y-3">
                  <h4 className="font-semibold text-sm flex items-center gap-1.5 text-primary border-b pb-1.5">
                    <Calendar className="h-4 w-4 text-emerald-500" />
                    Chi tiết sân đặt
                  </h4>
                  <div className="space-y-1.5 text-sm">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Sân chơi:</span>
                      <span className="font-medium">{booking.venue}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Khung giờ:</span>
                      <span className="font-medium">{booking.time}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Thời gian gốc:</span>
                      <span className="font-mono text-xs">{new Date(booking.playTimeRaw).toLocaleString('vi-VN')}</span>
                    </div>
                  </div>
                </div>

                <div className="space-y-3">
                  <h4 className="font-semibold text-sm flex items-center gap-1.5 text-primary border-b pb-1.5">
                    <Info className="h-4 w-4 text-violet-500" />
                    Chính sách hoàn tiền (Ước tính)
                  </h4>
                  {booking.status.toLowerCase() === "cancelled" ? (
                    <div className="text-sm text-slate-500 italic bg-slate-100 dark:bg-slate-800 p-2.5 rounded border">
                      Đơn hàng đã được xử lý hủy. Khung giờ chơi đã được giải phóng trở lại trạng thái Sẵn Sàng (AVAILABLE).
                    </div>
                  ) : (
                    <div className="space-y-2">
                      {(() => {
                        const estimate = calculateExpectedRefund(booking.playTimeRaw, booking.amount);
                        return (
                          <>
                            <div className="flex justify-between items-center">
                              <span className="text-xs text-muted-foreground">Khả năng hoàn:</span>
                              <Badge className={`${estimate.badgeClass} border-none`}>{estimate.label}</Badge>
                            </div>
                            <div className="flex justify-between items-center">
                              <span className="text-xs text-muted-foreground">Số tiền hoàn trả:</span>
                              <span className="font-bold text-slate-900 dark:text-slate-100">{estimate.amount.toLocaleString('vi-VN')}đ</span>
                            </div>
                            <p className="text-[11px] text-muted-foreground leading-relaxed bg-slate-50 dark:bg-slate-900 p-1.5 rounded border border-slate-100 dark:border-slate-800">{estimate.desc}</p>
                            <p className="text-[10px] text-amber-600 dark:text-amber-400 italic mt-1 leading-normal">* Số tiền hoàn ước tính theo giờ máy khách, có thể thay đổi tùy thuộc vào giờ máy chủ khi xử lý giao dịch thực tế.</p>
                          </>
                        );
                      })()}
                    </div>
                  )}
                </div>

                {booking.notes && (
                  <div className="col-span-full bg-slate-50 dark:bg-slate-900/60 p-2.5 rounded border text-xs text-slate-600 dark:text-slate-300">
                    <span className="font-semibold text-primary block mb-0.5">Ghi chú:</span>
                    <p className="italic">{booking.notes}</p>
                  </div>
                )}
              </div>
            </td>
          </tr>
        )}
      </>
    );
  };

  const getActiveBookings = () => {
    switch (activeTab) {
      case "pending":
        return filterBookings("pending");
      case "confirmed":
        return filterBookings("confirmed");
      case "completed":
        return filterBookings("completed");
      case "cancelled":
        return filterBookings("cancelled");
      default:
        return bookingList;
    }
  };

  const activeBookings = getActiveBookings();
  const totalGrossAmount = activeBookings.reduce((sum, b) => {
    const isPaidType = b.paymentStatus.toLowerCase() === "paid" || b.paymentStatus.toLowerCase() === "refunded";
    return sum + (isPaidType ? b.amount : 0);
  }, 0);
  const totalRefundedAmount = activeBookings.reduce((sum, b) => sum + (b.refundAmount || 0), 0);
  const totalNetAmount = totalGrossAmount - totalRefundedAmount;

  return (
    <>
      <div className="container mx-auto px-4 py-8 max-w-7xl">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between mb-8 gap-4">
          <div>
            <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white">Quản lý Đơn đặt sân</h1>
            <p className="text-muted-foreground mt-1 text-sm">Xem danh sách, xác nhận đặt sân hoặc hủy hoàn tiền nhanh chóng.</p>
          </div>
          
          {/* Hủy nhanh đã loại bỏ trong Production */}
        </div>

        {/* Filters */}
        <Card className="mb-6 shadow-sm border border-slate-200/80 dark:border-slate-800">
          <CardContent className="p-5">
            <div className="flex gap-4 flex-wrap">
              <div className="flex-1 min-w-[200px]">
                <label className="block text-xs font-semibold text-muted-foreground mb-1.5 uppercase tracking-wide">Từ ngày</label>
                <input
                  type="date"
                  className="w-full border rounded-lg px-3 py-2 bg-background focus:ring-1 focus:ring-primary focus:outline-none"
                  placeholder="Từ ngày"
                />
              </div>
              <div className="flex-1 min-w-[200px]">
                <label className="block text-xs font-semibold text-muted-foreground mb-1.5 uppercase tracking-wide">Đến ngày</label>
                <input
                  type="date"
                  className="w-full border rounded-lg px-3 py-2 bg-background focus:ring-1 focus:ring-primary focus:outline-none"
                  placeholder="Đến ngày"
                />
              </div>
              <div className="w-full md:w-56">
                <label className="block text-xs font-semibold text-muted-foreground mb-1.5 uppercase tracking-wide">Sân thể thao</label>
                <Select defaultValue="all">
                  <SelectTrigger className="w-full h-[42px]">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">Tất cả các sân</SelectItem>
                    <SelectItem value="san1">Sân Bóng Đá Thủ Đức</SelectItem>
                    <SelectItem value="san2">Sân Cầu Lông Bình Thạnh</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="flex items-end">
                <Button variant="outline" className="h-[42px] px-6 font-semibold shadow-sm hover:bg-muted">Lọc kết quả</Button>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Doanh thu Summary Widget */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
          <Card className="bg-slate-50/50 dark:bg-slate-900/40 border border-slate-200/80 dark:border-slate-800 shadow-sm">
            <CardContent className="p-5 flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Doanh thu gộp (Gross)</p>
                <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-1">
                  {totalGrossAmount.toLocaleString('vi-VN')}đ
                </h3>
                <p className="text-[11px] text-muted-foreground mt-1">Tổng tiền của các đơn hàng trong danh sách</p>
              </div>
              <div className="bg-blue-100 text-blue-600 p-3 rounded-lg dark:bg-blue-950/40 dark:text-blue-400">
                <DollarSign className="h-6 w-6" />
              </div>
            </CardContent>
          </Card>

          <Card className="bg-slate-50/50 dark:bg-slate-900/40 border border-slate-200/80 dark:border-slate-800 shadow-sm">
            <CardContent className="p-5 flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Tiền đã hoàn trả (Refunded)</p>
                <h3 className="text-2xl font-bold text-rose-600 dark:text-rose-400 mt-1">
                  {totalRefundedAmount.toLocaleString('vi-VN')}đ
                </h3>
                <p className="text-[11px] text-muted-foreground mt-1">Số tiền đã trả lại cho các đơn hủy hoàn tiền</p>
              </div>
              <div className="bg-rose-100 text-rose-600 p-3 rounded-lg dark:bg-rose-950/40 dark:text-rose-400">
                <RotateCcw className="h-6 w-6" />
              </div>
            </CardContent>
          </Card>

          <Card className="bg-emerald-50/30 dark:bg-emerald-950/10 border border-emerald-100/80 dark:border-emerald-950/40 shadow-sm">
            <CardContent className="p-5 flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-emerald-800 dark:text-emerald-400 uppercase tracking-wider">Thực thu sau cùng (Net)</p>
                <h3 className="text-2xl font-bold text-emerald-600 dark:text-emerald-400 mt-1">
                  {totalNetAmount.toLocaleString('vi-VN')}đ
                </h3>
                <p className="text-[11px] text-emerald-800 dark:text-emerald-400 mt-1">Số tiền thực tế chủ sân thu về sau hủy hoàn</p>
              </div>
              <div className="bg-emerald-100 text-emerald-600 p-3 rounded-lg dark:bg-emerald-950/30 dark:text-emerald-400">
                <CheckCircle className="h-6 w-6" />
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Tabs */}
        <Tabs value={activeTab} onValueChange={handleTabChange} className="space-y-6">
          <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 border-b pb-2">
            <TabsList className="bg-muted/70 p-1 rounded-lg w-full lg:w-auto overflow-x-auto flex-nowrap whitespace-nowrap justify-start">
              <TabsTrigger value="all" className="font-semibold text-sm">
                Tất cả
              </TabsTrigger>
              <TabsTrigger value="pending" className="font-semibold text-sm">
                Chờ duyệt
              </TabsTrigger>
              <TabsTrigger value="confirmed" className="font-semibold text-sm">
                Đã xác nhận
              </TabsTrigger>
              <TabsTrigger value="completed" className="font-semibold text-sm">
                Hoàn thành
              </TabsTrigger>
              <TabsTrigger value="cancelled" className="font-semibold text-sm">
                Đã hủy
              </TabsTrigger>
            </TabsList>

            <div className="flex gap-2 self-end">
              <Button variant="outline" size="sm" className="font-medium shadow-sm">
                Xuất danh sách Excel
              </Button>
            </div>
          </div>

          <Card className="shadow-md border border-slate-200/80 dark:border-slate-800 overflow-hidden">
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                  <thead>
                    <tr className="bg-muted/50 border-b text-muted-foreground text-xs uppercase font-bold tracking-wider">
                      <th className="p-4 text-left w-12">
                        <Checkbox />
                      </th>
                      <th className="p-4 text-left">Mã đơn</th>
                      <th className="p-4 text-left">Khách hàng</th>
                      <th className="p-4 text-left">Sân chơi</th>
                      <th className="p-4 text-left">Ngày</th>
                      <th className="p-4 text-left">Khung giờ</th>
                      <th className="p-4 text-right">Tổng tiền</th>
                      <th className="p-4 text-right">Tiền đã hoàn</th>
                      <th className="p-4 text-right">Thực thu</th>
                      <th className="p-4 text-left">Trạng thái</th>
                      <th className="p-4 text-right">Hành động</th>
                    </tr>
                  </thead>
                  <tbody>
                    <TabsContent value="all" className="m-0" asChild>
                      <>
                        {isLoading ? (
                          <tr>
                            <td colSpan={11} className="text-center p-12 text-muted-foreground">
                              <span className="animate-spin inline-block rounded-full h-6 w-6 border-2 border-primary border-t-transparent mr-2 align-middle"></span> 
                              Đang tải danh sách đặt sân từ máy chủ...
                            </td>
                          </tr>
                        ) : bookingList.length === 0 ? (
                          <tr><td colSpan={11} className="text-center p-8 text-muted-foreground">Không có đơn đặt sân nào.</td></tr>
                        ) : (
                          bookingList.map((booking) => (
                            <BookingRow key={booking.id} booking={booking} />
                          ))
                        )}
                      </>
                    </TabsContent>
                    <TabsContent value="pending" className="m-0" asChild>
                      <>
                        {isLoading ? (
                          <tr>
                            <td colSpan={11} className="text-center p-12 text-muted-foreground">
                              <span className="animate-spin inline-block rounded-full h-6 w-6 border-2 border-primary border-t-transparent mr-2 align-middle"></span> 
                              Đang tải...
                            </td>
                          </tr>
                        ) : filterBookings("pending").length === 0 ? (
                          <tr><td colSpan={11} className="text-center p-8 text-muted-foreground">Không có đơn đặt sân chờ duyệt.</td></tr>
                        ) : (
                          filterBookings("pending").map((booking) => (
                            <BookingRow key={booking.id} booking={booking} />
                          ))
                        )}
                      </>
                    </TabsContent>
                    <TabsContent value="confirmed" className="m-0" asChild>
                      <>
                        {isLoading ? (
                          <tr>
                            <td colSpan={11} className="text-center p-12 text-muted-foreground">
                              <span className="animate-spin inline-block rounded-full h-6 w-6 border-2 border-primary border-t-transparent mr-2 align-middle"></span> 
                              Đang tải...
                            </td>
                          </tr>
                        ) : filterBookings("confirmed").length === 0 ? (
                          <tr><td colSpan={11} className="text-center p-8 text-muted-foreground">Không có đơn đã xác nhận.</td></tr>
                        ) : (
                          filterBookings("confirmed").map((booking) => (
                            <BookingRow key={booking.id} booking={booking} />
                          ))
                        )}
                      </>
                    </TabsContent>
                    <TabsContent value="completed" className="m-0" asChild>
                      <>
                        {isLoading ? (
                          <tr>
                            <td colSpan={11} className="text-center p-12 text-muted-foreground">
                              <span className="animate-spin inline-block rounded-full h-6 w-6 border-2 border-primary border-t-transparent mr-2 align-middle"></span> 
                              Đang tải...
                            </td>
                          </tr>
                        ) : filterBookings("completed").length === 0 ? (
                          <tr><td colSpan={11} className="text-center p-8 text-muted-foreground">Không có đơn hoàn thành.</td></tr>
                        ) : (
                          filterBookings("completed").map((booking) => (
                            <BookingRow key={booking.id} booking={booking} />
                          ))
                        )}
                      </>
                    </TabsContent>
                    <TabsContent value="cancelled" className="m-0" asChild>
                      <>
                        {isLoading ? (
                          <tr>
                            <td colSpan={11} className="text-center p-12 text-muted-foreground">
                              <span className="animate-spin inline-block rounded-full h-6 w-6 border-2 border-primary border-t-transparent mr-2 align-middle"></span> 
                              Đang tải...
                            </td>
                          </tr>
                        ) : filterBookings("cancelled").length === 0 ? (
                          <tr><td colSpan={11} className="text-center p-8 text-muted-foreground">Không có đơn đã hủy.</td></tr>
                        ) : (
                          filterBookings("cancelled").map((booking) => (
                            <BookingRow key={booking.id} booking={booking} />
                          ))
                        )}
                      </>
                    </TabsContent>
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-3">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setPage(current => Math.max(0, current - 1))}
                disabled={page === 0 || isLoading}
              >
                <ChevronLeft className="mr-1 h-4 w-4" /> Trang trước
              </Button>
              <span className="text-sm font-medium text-muted-foreground">
                Trang {page + 1} / {totalPages} · {totalElements} đơn
              </span>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setPage(current => Math.min(totalPages - 1, current + 1))}
                disabled={page >= totalPages - 1 || isLoading}
              >
                Trang sau <ChevronRight className="ml-1 h-4 w-4" />
              </Button>
            </div>
          )}
        </Tabs>
      </div>

      {/* ──────────────────────────────────────────────────────────────────────────
          POPUP 1: ĐỐI THOẠI XÁC NHẬN HỦY VÀ ƯỚC TÍNH HOÀN TIỀN (CONFIRM REFUND DIALOG)
          ────────────────────────────────────────────────────────────────────────── */}
      <Dialog open={isCancelModalOpen} onOpenChange={setIsCancelModalOpen}>
        <DialogContent className="max-w-md border dark:border-slate-800 shadow-2xl rounded-xl">
          <DialogHeader>
            <DialogTitle className="text-xl font-bold flex items-center gap-2 text-rose-600">
              <RotateCcw className="h-5.5 w-5.5" />
              Yêu Cầu Hủy & Hoàn Tiền
            </DialogTitle>
            <DialogDescription className="text-slate-500 dark:text-slate-400 text-xs">
              Vui lòng xem xét các điều khoản hoàn tiền và nhập lý do trước khi thực hiện. Hành động này không thể hoàn tác.
            </DialogDescription>
          </DialogHeader>

          {selectedBooking && (
            <div className="space-y-4 py-3">
              {/* Thông tin đơn */}
              <div className="bg-slate-50 dark:bg-slate-900/60 p-4 rounded-xl border space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Mã đơn:</span>
                  <span className="font-mono font-bold text-primary">{selectedBooking.displayId}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Khách hàng:</span>
                  <span className="font-medium text-slate-800 dark:text-slate-200">{selectedBooking.customer.name}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Thời gian chơi:</span>
                  <span className="font-medium text-slate-800 dark:text-slate-200">{new Date(selectedBooking.playTimeRaw).toLocaleString('vi-VN')}</span>
                </div>
                <div className="flex justify-between border-t pt-2 mt-2">
                  <span className="text-muted-foreground font-semibold">Tiền thanh toán:</span>
                  <span className="font-bold text-primary">{selectedBooking.amount.toLocaleString('vi-VN')}đ</span>
                </div>
              </div>

              {/* Tính toán hoàn tiền thực tế (Truy vấn động từ Máy Chủ) */}
              <div className="border border-violet-100 dark:border-violet-950/50 bg-violet-50/20 dark:bg-violet-950/10 p-4 rounded-xl space-y-2.5">
                <h5 className="font-semibold text-xs text-violet-800 dark:text-violet-400 uppercase tracking-wider flex items-center gap-1.5">
                  <Clock className="h-3.5 w-3.5" />
                  Chính sách áp dụng tự động (Giờ Máy Chủ)
                </h5>
                
                {isPreviewLoading ? (
                  <div className="text-center py-4 text-xs text-muted-foreground flex items-center justify-center gap-2">
                    <span className="animate-spin rounded-full h-4 w-4 border-2 border-indigo-600 border-t-transparent mr-2"></span>
                    Đang tính toán tiền hoàn chính xác từ máy chủ...
                  </div>
                ) : previewData ? (
                  <>
                    <div className="flex justify-between items-center text-sm">
                      <span className="text-slate-600 dark:text-slate-400">Tỷ lệ hoàn trả:</span>
                      <Badge className={`${
                        previewData.refundPercentage === 100 
                          ? "bg-emerald-100 text-emerald-800 dark:bg-emerald-950/30 dark:text-emerald-400" 
                          : previewData.refundPercentage === 50 
                          ? "bg-amber-100 text-amber-800 dark:bg-amber-950/30 dark:text-amber-400" 
                          : "bg-rose-100 text-rose-800 dark:bg-rose-950/30 dark:text-rose-400"
                      } border-none font-bold px-2 py-0.5`}>
                        Hoàn {previewData.refundPercentage}%
                      </Badge>
                    </div>
                    <div className="flex justify-between items-center text-sm">
                      <span className="text-slate-600 dark:text-slate-400">Tiền trả khách:</span>
                      <span className="font-extrabold text-lg text-slate-900 dark:text-slate-100">{previewData.refundAmount.toLocaleString('vi-VN')}đ</span>
                    </div>
                    <p className="text-[11px] leading-relaxed text-muted-foreground border-t pt-2 mt-1 italic">
                      {previewData.refundPercentage === 100 
                        ? "Hủy trước giờ chơi >= 24 giờ. Khách hàng nhận lại toàn bộ tiền sân." 
                        : previewData.refundPercentage === 50 
                        ? "Hủy trước giờ chơi từ 12 giờ đến dưới 24 giờ. Khách hàng nhận lại 50% tiền sân." 
                        : "Hủy quá sát giờ chơi (< 12 giờ). Khách hàng không được hoàn tiền theo điều khoản."
                      }
                    </p>
                  </>
                ) : (
                  <div className="text-center py-4 text-xs text-rose-600 flex items-center justify-center gap-1.5">
                    <AlertCircle className="h-4 w-4" />
                    Không thể kết nối máy chủ để xem trước hoàn tiền.
                  </div>
                )}
              </div>

              {/* Chọn người chịu trách nhiệm */}
              <div className="space-y-1.5 mt-2">
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">Nguyên nhân hủy <span className="text-rose-500">*</span></label>
                <Select value={reasonType} onValueChange={setReasonType}>
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="Chọn nguyên nhân" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="CUSTOMER_REQUEST">Khách hàng yêu cầu hủy</SelectItem>
                    <SelectItem value="OWNER_FAULT">Sự cố từ phía sân (Mưa ngập, hỏng hóc...)</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {reasonType === "OWNER_FAULT" && (
                <div className="space-y-1.5 mt-2 p-3 bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-900 rounded-md">
                  <label className="block text-xs font-semibold text-rose-700 dark:text-rose-400">Bằng chứng sự cố <span className="text-rose-500">*</span></label>
                  <p className="text-[11px] text-rose-600 mb-2">Bằng chứng này sẽ được lưu lại để Admin đối soát. Nếu khai báo sai, bạn có thể bị phạt.</p>
                  <Textarea 
                    value={proofUrl}
                    onChange={(e) => setProofUrl(e.target.value)}
                    placeholder="Mô tả chi tiết sự cố hoặc dán link ảnh bằng chứng vào đây..."
                    className="min-h-[60px] focus:ring-1 focus:ring-rose-500 focus:outline-none"
                  />
                </div>
              )}

              {/* Lý do hủy */}
              <div className="space-y-1.5 mt-2">
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">Ghi chú hủy sân</label>
                <Textarea 
                  value={cancelReason}
                  onChange={(e) => setCancelReason(e.target.value)}
                  placeholder="Ghi chú thêm (không bắt buộc)..."
                  className="min-h-[60px] focus:ring-1 focus:ring-primary focus:outline-none"
                />
              </div>
            </div>
          )}

          <DialogFooter className="gap-2">
            <Button 
              variant="outline" 
              onClick={() => setIsCancelModalOpen(false)}
              disabled={isSubmitting}
            >
              Hủy bỏ
            </Button>
            <Button 
              className="bg-rose-600 hover:bg-rose-700 text-white font-medium shadow-sm transition-all duration-200"
              onClick={handleConfirmRefund}
              disabled={isSubmitting || isPreviewLoading || !previewData}
            >
              {isSubmitting ? "Đang xử lý..." : "Xác nhận hoàn tiền"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ──────────────────────────────────────────────────────────────────────────
          POPUP 2: THÔNG BÁO HOÀN TIỀN THÀNH CÔNG RỰC RỠ (SUCCESS DETAIL DIALOG)
          ────────────────────────────────────────────────────────────────────────── */}
      <Dialog open={isSuccessModalOpen} onOpenChange={setIsSuccessModalOpen}>
        <DialogContent className="max-w-md border border-emerald-100 dark:border-emerald-950/40 bg-card shadow-2xl rounded-xl overflow-hidden p-0 animate-in zoom-in-95 duration-200">
          
          {/* Header rực rỡ tích xanh */}
          <div className="bg-gradient-to-r from-emerald-500 to-teal-600 p-6 text-center text-white space-y-2">
            <div className="mx-auto w-16 h-16 bg-white/20 rounded-full flex items-center justify-center border border-white/30 animate-pulse">
              <CheckCircle className="h-10 w-10 text-white stroke-[2.5]" />
            </div>
            <h3 className="text-xl font-bold tracking-tight">Hoàn Tiền Thành Công!</h3>
            <p className="text-xs text-white/85">Giao dịch đã được hệ thống ghi nhận và đồng bộ thời gian chơi.</p>
          </div>

          {successData && (
            <div className="p-6 space-y-5">
              {/* Summary Info */}
              <div className="text-center space-y-1">
                <span className="text-xs text-muted-foreground uppercase font-bold tracking-wider">Số tiền đã hoàn trả</span>
                <h4 className="text-3xl font-black text-emerald-600 tracking-tight">
                  {successData.refundAmount.toLocaleString('vi-VN')}đ
                </h4>
                <div className="inline-flex items-center gap-1.5 mt-1.5">
                  <Badge className="bg-emerald-100 text-emerald-800 border-none font-semibold px-2 py-0.5">
                    Đã hoàn {successData.refundPercentage}%
                  </Badge>
                </div>
              </div>

              {/* Phân rã dữ liệu từ API */}
              <div className="space-y-3">
                <h5 className="font-semibold text-xs text-muted-foreground uppercase tracking-wide border-b pb-1">Chi tiết giao dịch hoàn trả</h5>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Mã đơn hàng:</span>
                    <span className="font-mono font-bold text-slate-800 dark:text-slate-200">BK{String(successData.bookingId).padStart(6, '0')}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Khách hàng:</span>
                    <span className="font-medium text-slate-800 dark:text-slate-200">{successData.customerName}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Sân chơi:</span>
                    <span className="font-medium text-slate-800 dark:text-slate-200">{successData.stadiumName}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Tiền thanh toán gốc:</span>
                    <span className="font-medium text-slate-800 dark:text-slate-200">{successData.originalPrice.toLocaleString('vi-VN')}đ</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Trạng thái mới đơn:</span>
                    <Badge className="bg-rose-100 text-rose-800 border-none font-semibold">{successData.bookingStatus}</Badge>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Trạng thái dòng tiền:</span>
                    <Badge className="bg-purple-100 text-purple-800 border-none font-semibold">{successData.paymentStatus}</Badge>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Thời gian xử lý:</span>
                    <span className="font-mono text-xs text-slate-800 dark:text-slate-200">{new Date(successData.processedAt).toLocaleString('vi-VN')}</span>
                  </div>
                </div>
              </div>

              {/* Thông báo giải phóng sân */}
              <div className="bg-slate-50 dark:bg-slate-900/60 border rounded-lg p-3 flex gap-2 items-start text-xs text-slate-600 dark:text-slate-300">
                <AlertCircle className="h-4.5 w-4.5 text-blue-500 shrink-0 mt-0.5" />
                <div>
                  <span className="font-semibold block text-slate-800 dark:text-slate-100 mb-0.5">Khung giờ đặt sân đã trống!</span>
                  Hệ thống đã giải phóng khung giờ chơi <span className="font-semibold text-primary">{new Date(successData.playTime).toLocaleString('vi-VN')}</span> sang trạng thái **Sẵn sàng (AVAILABLE)** để đón nhận các lượt đặt sân mới.
                </div>
              </div>
            </div>
          )}

          <div className="bg-slate-50 dark:bg-slate-900/60 p-4 border-t flex justify-end">
            <Button 
              className="bg-emerald-600 hover:bg-emerald-700 text-white font-medium"
              onClick={() => setIsSuccessModalOpen(false)}
            >
              Đóng và tiếp tục
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      <Dialog
        open={cancelOnlyBooking !== null}
        onOpenChange={(open) => {
          if (!open) {
            setCancelOnlyBooking(null);
            setCancelOnlyReason("");
          }
        }}
      >
        <DialogContent className="max-w-md border dark:border-slate-800 shadow-2xl rounded-xl">
          <DialogHeader>
            <DialogTitle className="text-xl font-bold flex items-center gap-2 text-amber-600">
              <XCircle className="h-5 w-5" /> Hủy đơn đặt sân
            </DialogTitle>
            <DialogDescription className="text-slate-500 dark:text-slate-400 text-xs">
              Đơn <span className="font-mono font-semibold">{cancelOnlyBooking?.displayId}</span> đang chờ thu tiền mặt tại sân —
              chưa thu tiền nên hủy sẽ không phát sinh hoàn tiền. Giờ chơi sẽ được giải phóng ngay.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-2 py-2">
            <label className="text-sm font-medium text-slate-700 dark:text-slate-300">
              Lý do hủy (tùy chọn)
            </label>
            <Textarea
              value={cancelOnlyReason}
              onChange={(e) => setCancelOnlyReason(e.target.value)}
              placeholder="Ví dụ: khách báo không tới, đặt trùng lịch..."
              rows={3}
              maxLength={255}
              disabled={isCancelOnlySubmitting}
            />
          </div>

          <DialogFooter className="gap-2">
            <Button
              type="button"
              variant="outline"
              className="rounded-xl"
              onClick={() => setCancelOnlyBooking(null)}
              disabled={isCancelOnlySubmitting}
            >
              Quay lại
            </Button>
            <Button
              type="button"
              variant="destructive"
              className="rounded-xl"
              onClick={handleConfirmCancelOnly}
              disabled={isCancelOnlySubmitting}
            >
              {isCancelOnlySubmitting ? "Đang xử lý..." : "Xác nhận hủy"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}

export default BookingManagementPage;
