'use client'

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { CheckCircle2, XCircle, Loader2, ArrowRight, RefreshCw, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { fetchBookingDetail, type BookingDetailItem } from "@/lib/bookings-api";

/**
 * UC-CUS-02: Trang kết quả thanh toán VNPay.
 *
 * VNPay redirect về `VNPAY_RETURN_URL=http://localhost:3000/payments/result?success=true&bookingId=X`
 * sau khi khách thanh toán tại cổng. Trang này:
 *   1. Đọc query params (`success`, `bookingId`, `reason`) do VNPay + backend thêm vào.
 *   2. Nếu success=true → fetch lại booking detail, hiển thị trạng thái CONFIRMED.
 *   3. Nếu success=false → hiển thị thông báo lỗi + nút "Thử lại" quay lại booking detail.
 */
function PaymentResultContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const success = searchParams.get("success") === "true";
  const bookingId = searchParams.get("bookingId");
  const reason = searchParams.get("reason");

  const [booking, setBooking] = useState<BookingDetailItem | null>(null);
  const [loading, setLoading] = useState(success);

  useEffect(() => {
    if (!success || !bookingId) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        let data = null;
        for (let i = 0; i < 3; i++) {
          await new Promise((r) => setTimeout(r, 500 * Math.pow(2, i))); // 500ms, 1000ms, 2000ms
          data = await fetchBookingDetail(bookingId);
          if (cancelled) return;
          if (data.status === 'confirmed' || data.paymentStatus === 'paid' || data.paymentStatus === 'deposited') {
            break;
          }
        }
        if (!cancelled && data) {
          setBooking(data);
          setLoading(false);
        }
      } catch {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [success, bookingId]);

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Header />
        <div className="flex-1 flex items-center justify-center">
          <div className="text-center space-y-4">
            <Loader2 className="h-12 w-12 animate-spin text-primary mx-auto" />
            <p className="text-slate-500 font-medium">Đang xác nhận thanh toán...</p>
          </div>
        </div>
        <Footer />
      </div>
    );
  }

  if (success) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <Header />
        <main className="flex-1 container mx-auto px-4 py-12 max-w-2xl">
          <Card className="rounded-3xl border-none shadow-sm bg-white overflow-hidden">
            <div className="bg-gradient-to-br from-emerald-500 to-emerald-700 p-8 text-center">
              <div className="inline-flex items-center justify-center w-20 h-20 rounded-full bg-white/20 backdrop-blur-md mb-4">
                <CheckCircle2 className="h-12 w-12 text-white" />
              </div>
              <h1 className="text-2xl md:text-3xl font-bold text-white mb-2">
                Thanh toán thành công!
              </h1>
              <p className="text-emerald-50 font-medium">
                Đơn đặt sân của bạn đã được xác nhận
              </p>
            </div>

            <CardContent className="p-8 space-y-6">
              {booking && (
                <div className="space-y-4">
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-slate-500 font-medium">Mã đơn</span>
                    <span className="font-mono font-bold text-slate-900">
                      {booking.displayId}
                    </span>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-slate-500 font-medium">Sân</span>
                    <span className="font-semibold text-slate-900 text-right">
                      {booking.venueName}
                    </span>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-slate-500 font-medium">Ngày chơi</span>
                    <span className="font-semibold text-slate-900">{booking.playDate}</span>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-slate-500 font-medium">Khung giờ</span>
                    <span className="font-semibold text-emerald-700">
                      {booking.startTime} - {booking.endTime}
                    </span>
                  </div>
                  <div className="flex items-center justify-between text-sm pt-3 border-t border-slate-100">
                    <span className="text-slate-500 font-medium">Tổng giá trị đơn</span>
                    <span className="text-xl font-bold text-primary">
                      {booking.totalPrice.toLocaleString("vi-VN")}đ
                    </span>
                  </div>
                  {booking.paymentStatus === "deposited" && booking.paidAmount !== null && (
                    <>
                      <div className="flex items-center justify-between text-sm">
                        <span className="text-slate-500 font-medium">Đã thanh toán (đặt cọc 30%)</span>
                        <span className="font-bold text-indigo-700">
                          {booking.paidAmount.toLocaleString("vi-VN")}đ
                        </span>
                      </div>
                      <div className="flex items-center justify-between text-xs">
                        <span className="text-slate-400">Còn lại khi đến sân</span>
                        <span className="font-semibold text-slate-500">
                          {Math.max(0, booking.totalPrice - booking.paidAmount).toLocaleString("vi-VN")}đ
                        </span>
                      </div>
                    </>
                  )}
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-slate-500 font-medium">Trạng thái</span>
                    <span className="font-semibold text-emerald-700">
                      {booking.paymentStatus === "deposited" ? "Đã đặt cọc" : "Đã xác nhận"}
                    </span>
                  </div>
                </div>
              )}

              <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-4 text-sm text-emerald-800">
                <p className="font-semibold mb-1">📧 Email xác nhận đã được gửi</p>
                <p>
                  Vui lòng kiểm tra email để nhận thông tin chi tiết về đơn đặt sân.
                  Chúc bạn có trải nghiệm tuyệt vời!
                </p>
              </div>

              <div className="flex flex-col gap-3 pt-2">
                <Button
                  asChild
                  className="w-full rounded-2xl h-12 bg-indigo-500 hover:bg-indigo-600 font-bold"
                >
                  <Link href={booking ? `/community?action=create&bookingId=${booking.id}` : "/community"}>
                    <Users className="h-4 w-4 mr-2" />
                    Tìm đối thủ (Tạo kèo ghép)
                  </Link>
                </Button>
                <div className="flex flex-col sm:flex-row gap-3">
                  <Button
                    asChild
                    className="flex-1 rounded-2xl h-12 bg-emerald-600 hover:bg-emerald-700 font-bold"
                  >
                    <Link href={booking ? `/booking/${booking.id}` : "/profile?tab=bookings"}>
                      Xem chi tiết đơn
                      <ArrowRight className="h-4 w-4 ml-2" />
                    </Link>
                  </Button>
                  <Button
                    asChild
                    variant="outline"
                    className="flex-1 rounded-2xl h-12 border-slate-200 font-bold"
                  >
                    <Link href="/profile?tab=bookings">Lịch sử đặt sân</Link>
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </main>
        <Footer />
      </div>
    );
  }

  // Failure case
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Header />
      <main className="flex-1 container mx-auto px-4 py-12 max-w-2xl">
        <Card className="rounded-3xl border-none shadow-sm bg-white overflow-hidden">
          <div className="bg-gradient-to-br from-rose-500 to-rose-700 p-8 text-center">
            <div className="inline-flex items-center justify-center w-20 h-20 rounded-full bg-white/20 backdrop-blur-md mb-4">
              <XCircle className="h-12 w-12 text-white" />
            </div>
            <h1 className="text-2xl md:text-3xl font-bold text-white mb-2">
              Thanh toán thất bại
            </h1>
            <p className="text-rose-50 font-medium">
              Đơn đặt sân chưa được thanh toán thành công
            </p>
          </div>

          <CardContent className="p-8 space-y-6">
            <div className="bg-rose-50 border border-rose-200 rounded-2xl p-4 text-sm text-rose-800">
              <p className="font-semibold mb-1">⚠️ Lý do</p>
              <p>
                {reason === "invalid_hash"
                  ? "Chữ ký không hợp lệ — vui lòng thử lại hoặc liên hệ hỗ trợ."
                  : reason
                    ? `Mã lỗi: ${reason}`
                    : "Giao dịch đã bị huỷ hoặc không thành công."}
              </p>
            </div>

            <p className="text-slate-600 text-sm leading-relaxed">
              Đơn đặt sân của bạn vẫn được giữ trong thời gian giữ chỗ.
              Bạn có thể thử thanh toán lại hoặc huỷ đơn nếu không muốn tiếp tục.
            </p>

            <div className="flex flex-col sm:flex-row gap-3 pt-2">
              {bookingId && (
                <Button
                  onClick={() => router.push(`/booking/${bookingId}`)}
                  className="flex-1 rounded-2xl h-12 bg-rose-600 hover:bg-rose-700 font-bold"
                >
                  <RefreshCw className="h-4 w-4 mr-2" />
                  Thử thanh toán lại
                </Button>
              )}
              <Button
                asChild
                variant="outline"
                className="flex-1 rounded-2xl h-12 border-slate-200 font-bold"
              >
                <Link href="/profile?tab=bookings">Về lịch sử đặt sân</Link>
              </Button>
            </div>
          </CardContent>
        </Card>
      </main>
      <Footer />
    </div>
  );
}

export default function PaymentResultPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-slate-50 flex flex-col">
          <Header />
          <div className="flex-1 flex items-center justify-center">
            <Loader2 className="h-12 w-12 animate-spin text-primary" />
          </div>
          <Footer />
        </div>
      }
    >
      <PaymentResultContent />
    </Suspense>
  );
}