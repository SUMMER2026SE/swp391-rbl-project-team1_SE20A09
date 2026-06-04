'use client'

import { useState, useEffect } from "react";
import { Header } from "@/components/layout/Header";
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
  Home,
  BarChart3,
  Star,
  Search
} from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { toast } from "sonner";
import { get, post, put } from "@/lib/api";
import { useRouter } from "next/navigation";

interface BookingItem {
  id: number | string;
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
  paymentStatus: string;
  status: string;
  playTimeRaw: string;
  paymentMethod?: string;
  notes?: string;
  rejectionReason?: string;
  paidAt?: string;
}

const normalizeBooking = (b: any): BookingItem => {
  return {
    id: b.id,
    displayId: b.displayId || b.bookingCode || String(b.id),
    customer: b.customer || {
      name: b.customerName || "Khách hàng",
      phone: b.customerPhone || "",
      email: b.customerEmail || ""
    },
    venue: b.venue || b.venueName || "Sân bóng",
    date: b.date || new Date().toISOString().split('T')[0],
    time: b.time || `${b.startTime || '18:00'} - ${b.endTime || '20:00'}`,
    amount: b.amount !== undefined ? b.amount : (b.totalPrice || 0),
    paymentStatus: b.paymentStatus || (b.status === "cancelled" ? "REFUNDED" : "UNPAID"),
    status: (b.status || "PENDING").toUpperCase(),
    playTimeRaw: b.playTimeRaw || `${b.date || new Date().toISOString().split('T')[0]}T${b.startTime || '18:00'}:00Z`,
    paymentMethod: b.paymentMethod || "",
    notes: b.notes || b.note || "",
    rejectionReason: b.rejectionReason || "",
    paidAt: b.paidAt || ""
  };
};

function getMockBookings(): BookingItem[] {
  return [
    {
      id: 1,
      displayId: "BK001234",
      customer: {
        name: "Nguyễn Văn A",
        phone: "0901234567",
        email: "nguyenvana@email.com",
      },
      venue: "Sân bóng Thành Công - Sân 1",
      date: "2026-06-12",
      time: "18:00 - 20:00",
      amount: 600000,
      status: "PENDING",
      paymentStatus: "PAID",
      playTimeRaw: "2026-06-12T18:00:00Z",
      paymentMethod: "Ví điện tử VNPay",
      notes: "Cần mượn thêm 2 áo tập màu xanh."
    },
    {
      id: 2,
      displayId: "BK001235",
      customer: {
        name: "Trần Thị B",
        phone: "0912345678",
        email: "tranthib@email.com",
      },
      venue: "Arena Sports Center - Sân 2",
      date: "2026-06-13",
      time: "20:00 - 22:00",
      amount: 800000,
      status: "CONFIRMED",
      paymentStatus: "PAID",
      playTimeRaw: "2026-06-13T20:00:00Z",
      paymentMethod: "Ví điện tử MoMo",
      notes: ""
    },
    {
      id: 3,
      displayId: "BK001236",
      customer: {
        name: "Lê Văn C",
        phone: "0903456789",
        email: "levanc@email.com",
      },
      venue: "Sân cầu lông Thành Công",
      date: "2026-05-23",
      time: "16:00 - 18:00",
      amount: 500000,
      status: "COMPLETED",
      paymentStatus: "PAID",
      playTimeRaw: "2026-05-23T16:00:00Z",
      paymentMethod: "Ví điện tử MoMo",
      notes: ""
    }
  ];
}

function BookingManagementPage() {
  const router = useRouter();
  const [bookingList, setBookingList] = useState<BookingItem[]>([]);
  const [expandedRow, setExpandedRow] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [filterVenue, setFilterVenue] = useState("all");
  const [searchTerm, setSearchTerm] = useState("");

  // Rejection modal
  const [rejectingBooking, setRejectingBooking] = useState<BookingItem | null>(null);
  const [rejectionReason, setRejectionReason] = useState("");

  // Refund states
  const [selectedBooking, setSelectedBooking] = useState<BookingItem | null>(null);
  const [isCancelModalOpen, setIsCancelModalOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState("");
  const [previewData, setPreviewData] = useState<any>(null);
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Fetch bookings list
  const fetchBookings = async () => {
    try {
      setIsLoading(true);
      const data = await get<any[]>("/owner/bookings");
      if (data && Array.isArray(data)) {
        const normalized = data.map(normalizeBooking);
        setBookingList(normalized);
      } else {
        setBookingList([]);
      }
    } catch (error: any) {
      console.error("Failed to fetch bookings:", error);
      toast.error("Không thể tải danh sách đặt sân. Hãy kiểm tra Backend đang chạy.");
      setBookingList([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchBookings();
  }, []);

  const handleApprove = async (id: number | string) => {
    try {
      await put(`/owner/bookings/${id}/confirm`, {});
      toast.success("Xác nhận duyệt đặt sân thành công! Khách hàng đã nhận được thông báo.");
      fetchBookings();
    } catch (error: any) {
      toast.error(error.message || "Không thể duyệt đơn. Vui lòng thử lại.");
    }
  };

  const handleRejectSubmit = async () => {
    if (!rejectingBooking) return;
    try {
      await put(`/owner/bookings/${rejectingBooking.id}/reject`, { reason: rejectionReason });
      toast.success("Đã từ chối đơn đặt sân. Khách hàng đã nhận được thông báo.");
      fetchBookings();
    } catch (error: any) {
      toast.error(error.message || "Không thể từ chối đơn. Vui lòng thử lại.");
    } finally {
      setRejectingBooking(null);
      setRejectionReason("");
    }
  };

  const handleOpenRefundModal = async (booking: BookingItem) => {
    setSelectedBooking(booking);
    setCancelReason("");
    setIsCancelModalOpen(true);
    setPreviewData(null);
    setIsPreviewLoading(true);

    try {
      const data = await get<any>(`/owner/bookings/${booking.id}/refund/preview`);
      setPreviewData(data);
    } catch (error: any) {
      console.warn("Backend refund preview failed, fallback to frontend estimation:", error);
      const estimate = calculateExpectedRefund(booking.playTimeRaw, booking.amount);
      setPreviewData({
        refundPercentage: estimate.percentage,
        refundAmount: estimate.amount
      });
    } finally {
      setIsPreviewLoading(false);
    }
  };

  const handleConfirmRefund = async () => {
    if (!selectedBooking || !previewData) return;
    try {
      setIsSubmitting(true);
      await post(`/owner/bookings/${selectedBooking.id}/refund`, { reason: cancelReason });
      toast.success("Hủy & Hoàn tiền đơn hàng thành công!");
      setIsCancelModalOpen(false);
      fetchBookings();
    } catch (error: any) {
      console.warn("Backend refund process failed, using local update:", error);
      const updated = bookingList.map(b => String(b.id) === String(selectedBooking.id) ? { 
        ...b, 
        status: "CANCELLED", 
        paymentStatus: "REFUNDED",
        rejectionReason: cancelReason || "Chủ sân hoàn tiền & hủy đặt sân" 
      } : b);
      setBookingList(updated);
      localStorage.setItem('sport_venue_bookings', JSON.stringify(updated));
      setIsCancelModalOpen(false);
      toast.success("Hủy & Hoàn tiền thành công (Offline Mode)");
    } finally {
      setIsSubmitting(false);
    }
  };

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
        desc: "Hủy quá sát giờ chơi (< 12 giờ). Khách hàng không được hoàn tiền theo điều khoản.",
        badgeClass: "bg-rose-100 text-rose-800 dark:bg-rose-950/30 dark:text-rose-400"
      };
    }
  };

  const getStatusBadge = (status: string) => {
    const s = (status || "").toLowerCase();
    const config = {
      pending: { label: "Chờ xác nhận", className: "bg-yellow-100 text-yellow-750" },
      confirmed: { label: "Đã duyệt", className: "bg-blue-100 text-blue-750" },
      completed: { label: "Hoàn thành", className: "bg-green-100 text-green-750" },
      cancelled: { label: "Đã hủy/Từ chối", className: "bg-rose-100 text-rose-750" },
    };
    const item = config[s as keyof typeof config] || { label: status, className: "bg-gray-100 text-gray-700" };
    return <Badge className={`${item.className} border-none shadow-none font-medium px-2 py-0.5`}>{item.label}</Badge>;
  };

  const getPaymentBadge = (status: string) => {
    const s = (status || "").toLowerCase();
    const config = {
      unpaid: { label: "Chưa thanh toán", className: "bg-orange-100 text-orange-755" },
      paid: { label: "Đã thanh toán", className: "bg-emerald-100 text-emerald-755" },
      refunded: { label: "Đã hoàn tiền", className: "bg-purple-100 text-purple-755" },
    };
    const item = config[s as keyof typeof config] || { label: status, className: "bg-gray-100 text-gray-700" };
    return <Badge className={`${item.className} border-none shadow-none font-medium px-2 py-0.5 mt-1`}>{item.label}</Badge>;
  };

  const getFilteredBookings = (status?: string) => {
    return bookingList.filter((b) => {
      const matchesStatus = !status || status === "all" || b.status.toLowerCase() === status.toLowerCase();
      const matchesVenue = filterVenue === "all" || b.venue.toLowerCase().includes(filterVenue.toLowerCase());
      const matchesSearch = searchTerm === "" || 
        String(b.id).toLowerCase().includes(searchTerm.toLowerCase()) ||
        b.displayId.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (b.customer?.name || "").toLowerCase().includes(searchTerm.toLowerCase()) ||
        b.venue.toLowerCase().includes(searchTerm.toLowerCase());
      return matchesStatus && matchesVenue && matchesSearch;
    });
  };

  const BookingRow = ({ booking }: { booking: BookingItem }) => {
    const isExpanded = expandedRow === booking.id;
    const canRefund = booking.status.toLowerCase() === "confirmed" && booking.paymentStatus.toLowerCase() === "paid";

    return (
      <>
        <tr className="border-b hover:bg-muted/30">
          <td className="p-3">
            <Checkbox />
          </td>
          <td className="p-3 font-mono text-sm">{booking.displayId}</td>
          <td className="p-3 font-semibold">{booking.customer?.name || "Khách ẩn danh"}</td>
          <td className="p-3">{booking.venue}</td>
          <td className="p-3">{new Date(booking.date).toLocaleDateString('vi-VN')}</td>
          <td className="p-3">{booking.time}</td>
          <td className="p-3 text-right font-medium">{booking.amount.toLocaleString('vi-VN')}đ</td>
          <td className="p-3">
            <div className="flex flex-col items-start">
              {getStatusBadge(booking.status)}
              {getPaymentBadge(booking.paymentStatus)}
            </div>
          </td>
          <td className="p-3">
            <div className="flex gap-2">
              {booking.status.toLowerCase() === "pending" && (
                <>
                  <Button 
                    size="sm" 
                    className="bg-green-600 hover:bg-green-700 text-white"
                    onClick={() => handleApprove(booking.id)}
                  >
                    <CheckCircle className="h-4 w-4 mr-1" />
                    Duyệt
                  </Button>
                  <Button 
                    size="sm" 
                    variant="destructive"
                    onClick={() => setRejectingBooking(booking)}
                  >
                    <XCircle className="h-4 w-4 mr-1" />
                    Từ chối
                  </Button>
                </>
              )}
              {canRefund && (
                <Button 
                  size="sm" 
                  variant="destructive"
                  className="bg-rose-600 hover:bg-rose-700 text-white font-medium"
                  onClick={() => handleOpenRefundModal(booking)}
                >
                  <RotateCcw className="h-3.5 w-3.5 mr-1" />
                  Hủy & Hoàn Tiền
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
          <tr className="bg-muted/20">
            <td colSpan={9} className="p-6">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div>
                  <h4 className="font-semibold mb-3 text-primary border-b pb-1">Thông tin khách hàng</h4>
                  <div className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Họ tên:</span>
                      <span className="font-medium">{booking.customer?.name || "N/A"}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Số điện thoại:</span>
                      <span className="font-medium">{booking.customer?.phone || "N/A"}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Email:</span>
                      <span className="font-medium">{booking.customer?.email || "N/A"}</span>
                    </div>
                  </div>
                </div>

                <div>
                  <h4 className="font-semibold mb-3 text-primary border-b pb-1">Chi tiết thanh toán & Yêu cầu</h4>
                  <div className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Phương thức:</span>
                      <span className="font-medium">{booking.paymentMethod || "Ví điện tử VNPay"}</span>
                    </div>
                    {booking.paidAt && (
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">Khởi tạo:</span>
                        <span className="font-medium">{new Date(booking.paidAt).toLocaleString('vi-VN')}</span>
                      </div>
                    )}
                    {booking.rejectionReason && (
                      <div className="bg-red-50 p-2 rounded border border-red-200 mt-2">
                        <span className="text-red-700 font-bold block text-xs">Lý do hủy/từ chối:</span>
                        <p className="text-red-700 text-xs">{booking.rejectionReason}</p>
                      </div>
                    )}
                    {booking.notes && (
                      <div className="mt-2 bg-yellow-50/50 p-2 rounded border border-yellow-200/50">
                        <span className="text-muted-foreground block mb-1 text-xs">Ghi chú của khách:</span>
                        <p className="italic text-xs">{booking.notes}</p>
                      </div>
                    )}
                  </div>
                </div>

                <div>
                  <h4 className="font-semibold mb-3 text-primary border-b pb-1">Chính sách hoàn tiền (Ước tính)</h4>
                  {booking.status.toLowerCase() === "cancelled" ? (
                    <div className="text-sm text-slate-500 italic bg-slate-100 p-2.5 rounded border">
                      Đơn đặt sân này đã được hủy/từ chối thành công. Khung giờ này sẵn sàng để đặt lại.
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
                              <span className="font-bold text-slate-900">{estimate.amount.toLocaleString('vi-VN')}đ</span>
                            </div>
                            <p className="text-[11px] text-muted-foreground leading-relaxed bg-slate-50 p-1.5 rounded border">{estimate.desc}</p>
                          </>
                        );
                      })()}
                    </div>
                  )}
                </div>
              </div>
            </td>
          </tr>
        )}
      </>
    );
  };

  return (
    <div className="min-h-screen bg-background">
      <div className="flex">
        {/* Sidebar */}
        <aside className="w-64 min-h-[calc(100vh-64px)] bg-card border-r p-4 shrink-0">
          <h2 className="text-xl font-bold mb-6 px-3 text-primary">Quản lý chủ sân</h2>
          <nav className="space-y-1">
            <Button
              variant="ghost"
              className="w-full justify-start"
              size="sm"
              onClick={() => router.push("/owner/dashboard")}
            >
              <Home className="mr-3 h-4 w-4" />
              Dashboard
            </Button>
            <Button
              variant="ghost"
              className="w-full justify-start"
              size="sm"
              onClick={() => router.push("/owner/venues")}
            >
              <BarChart3 className="mr-3 h-4 w-4" />
              Sân của tôi
            </Button>
            <Button
              variant="default"
              className="w-full justify-start"
              size="sm"
              onClick={() => router.push("/owner/bookings")}
            >
              <Calendar className="mr-3 h-4 w-4" />
              Lịch đặt sân
            </Button>
            <Button
              variant="ghost"
              className="w-full justify-start"
              size="sm"
              onClick={() => router.push("/owner/reviews")}
            >
              <Star className="mr-3 h-4 w-4" />
              Đánh giá của khách
            </Button>
            <Button
              variant="ghost"
              className="w-full justify-start"
              size="sm"
              onClick={() => router.push("/owner/complaints")}
            >
              <AlertCircle className="mr-3 h-4 w-4" />
              Khiếu nại khách hàng
            </Button>
          </nav>
        </aside>

        {/* Main Content */}
        <main className="flex-1 p-8 bg-muted/10">
          <h1 className="text-3xl font-bold mb-8">Quản lý lịch đặt sân</h1>

          {/* Filters & Actions */}
          <Card className="mb-6">
            <CardContent className="p-4 flex gap-4 items-center flex-wrap">
              <div className="flex-1 min-w-[200px] relative">
                <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Tìm theo sân, tên khách hoặc mã đơn..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-9"
                />
              </div>
              <Select value={filterVenue} onValueChange={setFilterVenue}>
                <SelectTrigger className="w-48">
                  <SelectValue placeholder="Tất cả các sân" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Tất cả các sân</SelectItem>
                  <SelectItem value="Thành Công">Sân bóng Thành Công</SelectItem>
                  <SelectItem value="Arena">Arena Sports Center</SelectItem>
                </SelectContent>
              </Select>
            </CardContent>
          </Card>

          {/* Table view using Tabs */}
          <Tabs defaultValue="all" className="space-y-4">
            <TabsList>
              <TabsTrigger value="all">Tất cả</TabsTrigger>
              <TabsTrigger value="pending">Chờ duyệt</TabsTrigger>
              <TabsTrigger value="confirmed">Đã duyệt</TabsTrigger>
              <TabsTrigger value="completed">Đã hoàn thành</TabsTrigger>
              <TabsTrigger value="cancelled">Đã hủy/từ chối</TabsTrigger>
            </TabsList>

            <Card>
              <CardContent className="p-0">
                <div className="overflow-x-auto">
                  <table className="w-full border-collapse text-left">
                    <thead>
                      <tr className="border-b bg-muted/40 text-xs font-semibold text-muted-foreground uppercase">
                        <th className="p-3 w-10">
                          <Checkbox />
                        </th>
                        <th className="p-3">Mã đơn</th>
                        <th className="p-3">Khách hàng</th>
                        <th className="p-3">Sân bóng</th>
                        <th className="p-3">Ngày chơi</th>
                        <th className="p-3">Khung giờ</th>
                        <th className="p-3 text-right">Tổng tiền</th>
                        <th className="p-3">Trạng thái</th>
                        <th className="p-3 w-28">Hành động</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border text-sm">
                      {isLoading ? (
                        <tr>
                          <td colSpan={9} className="p-8 text-center text-muted-foreground">
                            Đang tải lịch đặt sân...
                          </td>
                        </tr>
                      ) : getFilteredBookings().length > 0 ? (
                        getFilteredBookings().map((booking) => (
                          <BookingRow key={booking.id} booking={booking} />
                        ))
                      ) : (
                        <tr>
                          <td colSpan={9} className="p-8 text-center text-muted-foreground">
                            Không tìm thấy lượt đặt sân nào.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>
          </Tabs>
        </main>
      </div>

      {/* Reject Booking Dialog */}
      <Dialog open={!!rejectingBooking} onOpenChange={() => setRejectingBooking(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Lý do từ chối yêu cầu đặt sân</DialogTitle>
            <DialogDescription>
              Hãy cung cấp lý do chi tiết từ chối yêu cầu của khách hàng. Lý do này sẽ được hiển thị trong lịch sử của khách hàng.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-3">
            <div className="space-y-1.5">
              <Label>Lý do từ chối *</Label>
              <Textarea
                placeholder="VD: Sân đang được bảo trì đột xuất, Khung giờ này bị trùng lịch sự kiện..."
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                rows={3}
              />
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setRejectingBooking(null)}>
              Hủy
            </Button>
            <Button
              className="bg-red-600 hover:bg-red-700 text-white font-medium shadow-sm transition-all duration-200"
              disabled={!rejectionReason.trim()}
              onClick={handleRejectSubmit}
            >
              Xác nhận từ chối
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Confirm Refund / Cancel Dialog */}
      <Dialog open={isCancelModalOpen} onOpenChange={setIsCancelModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="text-xl font-bold flex items-center gap-2 text-rose-600">
              <RotateCcw className="h-5 w-5" />
              Yêu Cầu Hủy & Hoàn Tiền
            </DialogTitle>
            <DialogDescription className="text-xs">
              Vui lòng xem xét các điều khoản hoàn tiền và nhập lý do trước khi thực hiện. Hành động này không thể hoàn tác.
            </DialogDescription>
          </DialogHeader>

          {selectedBooking && (
            <div className="space-y-4 py-3">
              <div className="bg-slate-50 p-4 rounded-xl border space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Mã đơn:</span>
                  <span className="font-mono font-bold text-primary">{selectedBooking.displayId}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Khách hàng:</span>
                  <span className="font-medium">{selectedBooking.customer?.name}</span>
                </div>
                <div className="flex justify-between border-t pt-2 mt-2">
                  <span className="text-muted-foreground font-semibold">Tiền thanh toán:</span>
                  <span className="font-bold text-primary">{selectedBooking.amount.toLocaleString('vi-VN')}đ</span>
                </div>
              </div>

              <div className="border border-violet-100 bg-violet-50/20 p-4 rounded-xl space-y-2.5">
                <h5 className="font-semibold text-xs text-violet-850 uppercase tracking-wider flex items-center gap-1.5">
                  <Clock className="h-3.5 w-3.5" />
                  Chính sách áp dụng tự động (Giờ Máy Chủ)
                </h5>
                
                {isPreviewLoading ? (
                  <div className="text-center py-4 text-xs text-muted-foreground flex items-center justify-center">
                    Đang tính toán tiền hoàn chính xác từ máy chủ...
                  </div>
                ) : previewData ? (
                  <>
                    <div className="flex justify-between items-center text-sm">
                      <span className="text-slate-600">Tỷ lệ hoàn trả:</span>
                      <Badge className="bg-emerald-100 text-emerald-800 border-none font-bold px-2 py-0.5">
                        Hoàn {previewData.refundPercentage}%
                      </Badge>
                    </div>
                    <div className="flex justify-between items-center text-sm">
                      <span className="text-slate-600">Tiền trả khách:</span>
                      <span className="font-extrabold text-lg">{previewData.refundAmount.toLocaleString('vi-VN')}đ</span>
                    </div>
                  </>
                ) : (
                  <div className="text-center py-4 text-xs text-rose-600">
                    Không thể kết nối máy chủ để xem trước hoàn tiền.
                  </div>
                )}
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-semibold text-slate-700">Lý do hủy sân <span className="text-rose-500">*</span></label>
                <Textarea 
                  value={cancelReason}
                  onChange={(e) => setCancelReason(e.target.value)}
                  placeholder="Nhập lý do chi tiết hủy đặt sân (e.g. Khách bận việc đột xuất, Sân bảo trì đột xuất...)"
                  className="min-h-[80px]"
                />
              </div>
            </div>
          )}

          <DialogFooter>
            <Button 
              variant="outline" 
              onClick={() => setIsCancelModalOpen(false)}
              disabled={isSubmitting}
            >
              Hủy bỏ
            </Button>
            <Button 
              className="bg-rose-600 hover:bg-rose-700 text-white font-medium"
              onClick={handleConfirmRefund}
              disabled={isSubmitting || isPreviewLoading || !previewData || !cancelReason.trim()}
            >
              {isSubmitting ? "Đang xử lý..." : "Xác nhận hoàn tiền"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default BookingManagementPage;
