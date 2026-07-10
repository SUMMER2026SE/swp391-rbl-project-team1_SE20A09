'use client'

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { ArrowLeft, MapPin, Clock, Calendar, Star, MessageSquare, AlertCircle, Loader2, Info } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { post } from '@/lib/api';
import { fetchBookingDetail, type BookingDetailItem } from '@/lib/bookings-api';
import { initiateVnpayPayment } from '@/lib/payments-api';
import { toast } from 'sonner';
import Image from "next/image";
import { useParams, useRouter } from 'next/navigation';
import { Header } from '@/components/layout/Header';
import { Footer } from '@/components/landing/Footer';
import { chatUrl, createContextualConversation } from '@/lib/contextual-chat';

const STATUS_CONFIG = {
  confirmed: { label: "Đã xác nhận", className: "bg-green-50 text-green-700 border-green-200" },
  pending: { label: "Chờ xác nhận", className: "bg-amber-50 text-amber-700 border-amber-200" },
  pending_payment: { label: "Chờ thanh toán", className: "bg-orange-50 text-orange-700 border-orange-200" },
  completed: { label: "Hoàn thành", className: "bg-slate-50 text-slate-600 border-slate-200" },
  cancelled: { label: "Đã hủy", className: "bg-red-50 text-red-600 border-red-200" },
} as const;

const PAYMENT_STATUS_CONFIG = {
  unpaid: { label: "Chưa thanh toán", className: "bg-rose-50 text-rose-700 border-rose-200" },
  deposited: { label: "Đã đặt cọc", className: "bg-indigo-50 text-indigo-700 border-indigo-200" },
  paid: { label: "Đã thanh toán", className: "bg-emerald-50 text-emerald-700 border-emerald-200" },
  refunded: { label: "Đã hoàn tiền", className: "bg-blue-50 text-blue-700 border-blue-200" },
  awaiting_cash_payment: { label: "Chờ thu tiền mặt", className: "bg-amber-50 text-amber-700 border-amber-200" }
} as const;

function getStatusBadge(status: BookingDetailItem["status"]) {
  const config = STATUS_CONFIG[status] || { label: status, className: "bg-slate-50 text-slate-600" };
  return (
    <Badge variant="outline" className={`${config.className} font-bold px-3 py-1 rounded-full text-[10px] uppercase tracking-wider`}>
      {config.label}
    </Badge>
  );
}

function getPaymentStatusBadge(status: string) {
  const normalized = status.toLowerCase();
  const config = PAYMENT_STATUS_CONFIG[normalized as keyof typeof PAYMENT_STATUS_CONFIG] || { label: status, className: "bg-slate-50 text-slate-600" };
  return (
    <Badge variant="outline" className={`${config.className} font-bold px-3 py-1 rounded-full text-[10px] uppercase tracking-wider`}>
      {config.label}
    </Badge>
  );
}

export default function BookingDetailPage() {
  const params = useParams();
  const router = useRouter();
  const [booking, setBooking] = useState<BookingDetailItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const [complaintOpen, setComplaintOpen] = useState(false);
  const [complaintText, setComplaintText] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [paying, setPaying] = useState(false);
  const [chatStarting, setChatStarting] = useState(false);

  const handleMessageOwner = async () => {
    if (!booking?.ownerUserId) return toast.error('Không tìm thấy tài khoản chủ sân');
    try {
      setChatStarting(true);
      const conversationId = await createContextualConversation(booking.ownerUserId, {
        action: 'booking_referral', bookingId: Number(booking.id), stadiumName: booking.venueName,
        playDate: booking.playDate, time: `${booking.startTime} - ${booking.endTime}`,
      });
      router.push(chatUrl(conversationId));
    } catch { toast.error('Không thể bắt đầu cuộc trò chuyện'); }
    finally { setChatStarting(false); }
  };

  useEffect(() => {
    const loadData = async () => {
      if (!params?.id) return;
      try {
        setLoading(true);
        const data = await fetchBookingDetail(params.id as string);
        setBooking(data);
      } catch (err: any) {
        setError(err.message || 'Không thể tải chi tiết đặt sân');
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [params?.id]);

  const handlePayWithVnpay = async (option: string = "FULL") => {
    if (!booking) return;
    if (paying) return;
    try {
      setPaying(true);
      const { paymentUrl } = await initiateVnpayPayment(parseInt(booking.id, 10), option);
      // Rời app — phải dùng window.location (không dùng được router.push)
      window.location.href = paymentUrl;
    } catch (err: any) {
      toast.error(err.message || 'Không thể tạo liên kết thanh toán VNPay');
      setPaying(false);
    }
  };

  const handleSubmitComplaint = async () => {
    if (!complaintText.trim() || !booking) return;
    try {
      setSubmitting(true);
      const response = await post<{ complaintId: number }>(`/complaints`, {
        bookingId: parseInt(booking.id),
        subject: "Khiếu nại từ đơn đặt sân #" + booking.displayId,
        description: complaintText.trim() 
      });
      toast.success("Đã gửi khiếu nại thành công! Chủ sân sẽ sớm phản hồi.");
      setComplaintOpen(false);
      setComplaintText("");
      router.push(`/complaints?complaintId=${response.complaintId}`);
    } catch (err: any) {
      toast.error(err.message || "Có lỗi xảy ra khi gửi khiếu nại.");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Header />
        <div className="flex-1 flex items-center justify-center">
          <div className="text-center space-y-4">
            <Loader2 className="h-12 w-12 animate-spin text-primary mx-auto" />
            <p className="text-slate-500 font-medium">Đang tải chi tiết đơn hàng...</p>
          </div>
        </div>
        <Footer />
      </div>
    );
  }

  if (error || !booking) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Header />
        <div className="flex-1 flex flex-col items-center justify-center gap-6 p-4">
          <div className="bg-red-50 p-6 rounded-full">
            <AlertCircle className="h-16 w-16 text-red-500" />
          </div>
          <div className="text-center">
            <h2 className="text-2xl font-bold text-slate-900 mb-2">Lỗi tải dữ liệu</h2>
            <p className="text-slate-500 font-medium max-w-xs">{error || 'Không tìm thấy đơn đặt sân'}</p>
          </div>
          <Button onClick={() => router.push('/profile?tab=bookings')} variant="outline" className="rounded-xl px-8 border-slate-200">
            Quay lại danh sách
          </Button>
        </div>
        <Footer />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Header />

      <main className="flex-1 container mx-auto px-4 py-8 max-w-3xl">
        {/* Breadcrumb & Navigation */}
        <div className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div className="flex items-center gap-2 text-sm text-slate-500 font-semibold">
            <Link href="/profile?tab=bookings" className="hover:text-primary transition-colors">Lịch sử đặt sân</Link>
            <span className="text-slate-300">/</span>
            <span className="text-slate-800">{booking.displayId}</span>
          </div>
          <div className="text-left sm:text-right">
            <p className="text-[10px] uppercase tracking-widest text-slate-400 font-bold">Mã đơn hàng</p>
            <p className="font-mono font-bold text-slate-900 text-lg">{booking.displayId}</p>
          </div>
        </div>

        <div className="grid grid-cols-1 gap-6">
          {/* Main Card: Venue & Status */}
          <Card className="overflow-hidden rounded-3xl border-none shadow-sm bg-white">
            <div className="relative h-48 md:h-64">
              <Image
                src={booking.imageUrl}
                alt={booking.venueName}
                fill
                className="object-cover"
                unoptimized
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
              <div className="absolute bottom-6 left-6 right-6">
                <div className="flex flex-wrap items-center justify-between gap-4">
                  <div>
                    <Badge className="bg-white/20 hover:bg-white/30 text-white border-white/40 backdrop-blur-md mb-2">
                      {booking.sportType}
                    </Badge>
                    <h1 className="text-2xl md:text-3xl font-bold text-white leading-tight">
                      {booking.venueName}
                    </h1>
                  </div>
                  <div className="flex items-center gap-2 bg-white/10 backdrop-blur-md p-1.5 rounded-2xl border border-white/20">
                    {getStatusBadge(booking.status)}
                    {getPaymentStatusBadge(booking.paymentStatus)}
                  </div>
                </div>
              </div>
            </div>
            
            <CardContent className="p-6">
              <div className="flex items-start gap-3">
                <div className="bg-slate-50 p-2 rounded-xl">
                  <MapPin className="h-5 w-5 text-primary" />
                </div>
                <div>
                  <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-0.5">Địa chỉ</p>
                  <p className="text-slate-700 font-medium leading-relaxed">{booking.address}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Booking Details */}
            <Card className="rounded-3xl border-none shadow-sm bg-white p-6">
              <h3 className="text-lg font-bold text-slate-900 mb-6 flex items-center gap-2">
                <Calendar className="h-5 w-5 text-primary" />
                Thông tin đặt sân
              </h3>
              
              <div className="space-y-6">
                {/* Highlighted time and date display */}
                <div className="flex items-center rounded-2xl bg-slate-50 px-4 py-3 text-xs sm:text-sm border border-slate-100">
                  <div className="flex-1">
                    <div className="text-[10px] sm:text-xs font-semibold text-slate-400 tracking-[0.08em]">
                      NGÀY RA SÂN
                    </div>
                    <div className="mt-1 text-sm sm:text-base font-semibold text-slate-800">
                      {booking.playDate}
                    </div>
                  </div>

                  <div className="w-px mx-4 bg-slate-200 self-stretch" />

                  <div className="flex-1 text-right">
                    <div className="text-[10px] sm:text-xs font-semibold text-slate-400 tracking-[0.08em]">
                      KHUNG GIỜ CHƠI
                    </div>
                    <div className="mt-1 text-sm sm:text-base font-medium text-emerald-700 whitespace-nowrap">
                      {booking.startTime} - {booking.endTime}
                    </div>
                  </div>
                </div>

                <Separator className="bg-slate-50" />

                <div className="flex items-start gap-3">
                  <Info className="h-4 w-4 text-slate-400 mt-0.5" />
                  <div>
                    <p className="text-[10px] uppercase font-bold text-slate-400 tracking-wider mb-1">Ghi chú</p>
                    <p className="text-sm text-slate-600 italic">
                      {booking.note || "Không có ghi chú nào từ khách hàng."}
                    </p>
                  </div>
                </div>
              </div>
            </Card>

            {/* Payment Summary */}
            <Card className="rounded-3xl border-none shadow-sm bg-white p-6 flex flex-col justify-between">
              <div>
                <h3 className="text-lg font-bold text-slate-900 mb-6">Thanh toán</h3>
                
                <div className="space-y-4">
                  <div className="flex justify-between items-center text-sm">
                    <span className="text-slate-500 font-medium">Giá sân</span>
                    <span className="font-bold text-slate-700">
                      {Math.max(0, booking.totalPrice - booking.serviceFee).toLocaleString('vi-VN')}đ
                    </span>
                  </div>
                  <div className="flex justify-between items-center text-sm">
                    <span className="text-slate-500 font-medium">Phí dịch vụ</span>
                    <span className="font-bold text-slate-700">{booking.serviceFee.toLocaleString('vi-VN')}đ</span>
                  </div>
                  
                  <Separator className="bg-slate-100" />
                  
                  <div className="flex justify-between items-end">
                    <div>
                      <p className="text-[10px] uppercase font-bold text-slate-400 tracking-wider mb-1">Tổng cộng</p>
                      <p className="text-xl font-bold text-primary">
                        {booking.totalPrice.toLocaleString('vi-VN')}đ
                      </p>
                    </div>
                    {getPaymentStatusBadge(booking.paymentStatus)}
                  </div>
                </div>
              </div>

              {/* Thanh toán ngay button flow */}
              <div className="pt-6 space-y-2">
                {(booking.status === 'pending' || booking.status === 'pending_payment' || booking.status === 'confirmed') && booking.paymentStatus === 'unpaid' && (
                  <div className="flex flex-col gap-2">
                    <Button
                      className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-2xl h-11 disabled:opacity-60"
                      onClick={() => handlePayWithVnpay("FULL")}
                      disabled={paying}
                    >
                      {paying ? "Đang chuyển hướng..." : "Thanh toán toàn bộ"}
                    </Button>
                    <Button
                      variant="outline"
                      className="w-full text-emerald-700 border-emerald-200 hover:bg-emerald-50 font-bold rounded-2xl h-11 disabled:opacity-60"
                      onClick={() => handlePayWithVnpay("DEPOSIT")}
                      disabled={paying}
                    >
                      Thanh toán cọc 30%
                    </Button>
                  </div>
                )}
                <p className="text-[10px] text-slate-400 text-center">
                  Đặt lúc: {booking.createdAt}
                </p>
              </div>
            </Card>
          </div>

          {/* Action Buttons */}
          <Card className="rounded-3xl border-none shadow-sm bg-slate-900 p-6">
            <div className="flex flex-col sm:flex-row gap-4">
              {(booking.status === 'confirmed' || booking.status === 'pending') && (
                <>
                  <Button variant="secondary" className="rounded-2xl flex-1 font-bold h-12" onClick={handleMessageOwner} disabled={chatStarting}>
                    <MessageSquare className="h-4 w-4 mr-2" />
                    {chatStarting ? 'Đang mở...' : 'Liên hệ chủ sân'}
                  </Button>
                  <Button asChild variant="destructive" className="rounded-2xl flex-1 font-bold h-12">
                    <Link href={`/booking/${booking.id}/cancel`}>Huỷ đơn đặt</Link>
                  </Button>
                </>
              )}
              {booking.status === 'completed' && (
                <>
                  <Button asChild className="rounded-2xl flex-1 font-bold h-12 bg-emerald-500 hover:bg-emerald-600">
                    <Link href={`/booking/${booking.id}/review`}>
                      <Star className="h-4 w-4 mr-2" />
                      Viết đánh giá
                    </Link>
                  </Button>
                  <Button 
                    variant="outline" 
                    className="rounded-2xl flex-1 font-bold h-12 bg-transparent text-white border-white/20 hover:bg-white/10"
                    onClick={() => setComplaintOpen(true)}
                  >
                    <AlertCircle className="h-4 w-4 mr-2 text-red-400" />
                    Gửi khiếu nại
                  </Button>
                </>
              )}
              {booking.status === 'cancelled' && (
                <>
                  <Button 
                    variant="outline" 
                    className="rounded-2xl flex-1 font-bold h-12 bg-transparent text-white border-white/20 hover:bg-white/10"
                    onClick={() => setComplaintOpen(true)}
                  >
                    <AlertCircle className="h-4 w-4 mr-2 text-red-400" />
                    Gửi khiếu nại
                  </Button>
                  <Button asChild variant="secondary" className="rounded-2xl flex-1 font-bold h-12">
                    <Link href="/search">Đặt sân khác</Link>
                  </Button>
                </>
              )}
            </div>
          </Card>
        </div>
      </main>

      <Footer />

      {/* Complaint Dialog */}
      <Dialog open={complaintOpen} onOpenChange={setComplaintOpen}>
        <DialogContent className="rounded-3xl sm:max-w-md p-6">
          <DialogHeader>
            <DialogTitle className="text-xl font-bold">Gửi khiếu nại</DialogTitle>
            <DialogDescription className="text-slate-500 font-medium">
              Vui lòng mô tả vấn đề bạn gặp phải. Chúng tôi và chủ sân sẽ hỗ trợ bạn sớm nhất.
            </DialogDescription>
          </DialogHeader>
          <div className="py-4">
            <Textarea
              placeholder="Ví dụ: Sân không đúng mô tả, chủ sân không cho vào, ..."
              value={complaintText}
              onChange={(e) => setComplaintText(e.target.value)}
              className="min-h-[120px] rounded-2xl border-slate-200 focus-visible:ring-primary"
            />
          </div>
          <DialogFooter className="flex-col sm:flex-row gap-2">
            <Button variant="ghost" onClick={() => setComplaintOpen(false)} className="rounded-xl flex-1">Hủy</Button>
            <Button 
              onClick={handleSubmitComplaint} 
              disabled={!complaintText.trim() || submitting} 
              className="rounded-xl flex-1 bg-red-600 hover:bg-red-700 font-bold"
            >
              {submitting ? "Đang gửi..." : "Gửi khiếu nại"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
