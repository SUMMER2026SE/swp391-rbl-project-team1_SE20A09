'use client'

import { useState, useEffect, Suspense } from "react";
import { useRouter } from "next/navigation";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { CreditCard, Banknote, Shield, Clock } from "lucide-react";
import Image from "next/image";
import { toast } from "sonner";
import { calculatePlatformFee } from "@/lib/utils";
import { createBooking } from "@/lib/bookings-api";
import { initiateVnpayPayment } from "@/lib/payments-api";
import { post } from "@/lib/api";

interface BookingSummary {
  venueId: number;
  stadiumName: string;
  imageUrl: string;
  address: string;
  sportName: string;
  date: string;
  time: string;
  venuePrice: number;
  accessories: { accessoryId?: number; name: string; quantity: number; price: number }[];
  accessoryTotal: number;
  total: number;
  slotId: number;
}

/**
 * UC-CUS-01: Parse ngày ISO yyyy-MM-dd theo LOCAL time thay vì UTC.
 *
 * <p>{@code new Date("2026-06-25")} bị JS hiểu là UTC midnight, nên ở GMT-7 hiển thị
 * ngày 24 tháng 6. Dùng {@code new Date(y, m-1, d)} để hiển thị đúng ngày local.</p>
 */
function parseIsoDateAsLocal(iso: string): Date {
  const parts = iso.split("-").map(Number);
  const y = parts[0];
  const m = parts[1];
  const d = parts[2];
  return new Date(y, m - 1, d);
}

function PaymentContent() {
  const router = useRouter();
  const [paymentMethod, setPaymentMethod] = useState("cash");
  const [summary, setSummary] = useState<BookingSummary | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [bookingId, setBookingId] = useState<number | null>(null);
  const [expiredAt, setExpiredAt] = useState<string | null>(null);
  const [countdown, setCountdown] = useState<number | null>(null);

  useEffect(() => {
    const data = sessionStorage.getItem("booking_summary");
    if (data) {
      try {
        setSummary(JSON.parse(data));
      } catch (err) {
        console.error("Failed to parse booking summary", err);
      }
    }
  }, []);

  // Countdown driven by expiredAt từ backend — KHÔNG dùng local timer cứng.
  useEffect(() => {
    if (!expiredAt) {
      return;
    }
    const updateCountdown = () => {
      const ms = new Date(expiredAt).getTime() - Date.now();
      const secs = Math.max(0, Math.floor(ms / 1000));
      setCountdown(secs);
      if (secs <= 0) {
        toast.error("Hết thời gian giữ sân! Vui lòng chọn lại.");
        sessionStorage.removeItem("booking_summary");
        router.push("/search");
      }
    };
    updateCountdown();
    const timer = setInterval(updateCountdown, 1000);
    return () => clearInterval(timer);
  }, [expiredAt, router]);

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  };

  const handlePaymentSubmit = async () => {
    if (!activeSummary) return;

    if (!activeSummary.slotId) {
      toast.error("Không tìm thấy thông tin khung giờ đã chọn. Vui lòng thử lại.");
      return;
    }

    try {
      setIsSubmitting(true);
      const accessoriesPayload = activeSummary.accessories
        .filter((a) => typeof a.accessoryId === "number")
        .map((a) => ({
          accessoryId: a.accessoryId as number,
          quantity: a.quantity,
        }));

      const created = await createBooking({
        stadiumId: activeSummary.venueId,
        slotId: activeSummary.slotId,
        reservationDate: activeSummary.date,
        note: `Đặt qua web - PTTT: ${
          paymentMethod === "cash"
            ? "Tiền mặt tại sân"
            : paymentMethod === "vnpay"
            ? "VNPay (Toàn bộ)"
            : paymentMethod === "vnpay_deposit"
            ? "VNPay (Cọc 30%)"
            : "Khác"
        }`,
        accessories: accessoriesPayload.length > 0 ? accessoriesPayload : undefined,
      });

      const newBookingId = created.bookingId;
      setBookingId(newBookingId);
      
      const apiExpiredAt = (created as { expiredAt?: string }).expiredAt;
      if (apiExpiredAt) {
        setExpiredAt(apiExpiredAt);
      }

      // Xử lý thanh toán ngay lập tức
      if (paymentMethod === "vnpay" || paymentMethod === "vnpay_deposit") {
        const option = paymentMethod === "vnpay_deposit" ? "DEPOSIT" : "FULL";
        const { paymentUrl } = await initiateVnpayPayment(newBookingId, option);
        if (paymentUrl) {
          window.location.href = paymentUrl;
          return;
        } else {
          toast.error("Không tạo được URL thanh toán VNPay.");
        }
      } else {
        await post(`/bookings/${newBookingId}/confirm-payment`, {});
        toast.success("Đặt sân thành công! Vui lòng thanh toán tiền mặt tại sân khi đến chơi.");
        sessionStorage.removeItem("booking_summary");
        router.push("/profile?tab=bookings");
      }
    } catch (err: any) {
      const serverMsg = err?.message ?? "Có lỗi xảy ra. Vui lòng thử lại.";
      toast.error(serverMsg);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Default fallback if session data is missing
  const activeSummary = summary;
  const platformFee = activeSummary ? calculatePlatformFee(activeSummary.venuePrice) : 0;

  if (!activeSummary) {
    return (
      <div className="min-h-screen bg-background flex flex-col justify-between">
        <Header />
        <div className="flex-grow flex flex-col items-center justify-center p-8">
          <h2 className="text-xl font-bold mb-4">Không tìm thấy thông tin thanh toán</h2>
          <Button onClick={() => router.push("/search")}>Quay lại tìm kiếm</Button>
        </div>
        <Footer />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background flex flex-col justify-between">
      <Header />

      <div className="container mx-auto px-4 py-8 flex-grow">
        <div className="max-w-3xl mx-auto">
          <h1 className="text-3xl font-bold mb-8">Thanh toán</h1>

          {/* Timer Notice — driven by expiredAt from API */}
          {countdown !== null && (
            <Card className="mb-6 border-emerald-200 bg-emerald-50/50 shadow-none">
              <CardContent className="p-4">
                <div className="flex items-center gap-3 text-emerald-800">
                  <Clock className="h-5 w-5 text-emerald-600 animate-pulse" />
                  <span className="text-sm md:text-base font-medium">
                    Sân sẽ được giữ trong{" "}
                    <strong className="text-lg text-emerald-600 font-bold">{formatTime(countdown)}</strong>
                  </span>
                </div>
              </CardContent>
            </Card>
          )}

          {/* Order Summary */}
          <Card className="mb-6 shadow-sm border-gray-150">
            <CardHeader className="bg-gray-50/50 pb-4">
              <h3 className="text-lg font-bold text-gray-800">Chi tiết đơn hàng</h3>
            </CardHeader>
            <CardContent className="space-y-4 pt-6">
              <div className="flex justify-between items-start">
                <div>
                  <h4 className="font-semibold text-gray-800">{activeSummary.stadiumName}</h4>
                  <p className="text-xs text-muted-foreground mt-0.5">{activeSummary.address}</p>
                </div>
                <span className="font-semibold text-gray-800">{activeSummary.venuePrice.toLocaleString("vi-VN")}đ</span>
              </div>
              <div className="flex flex-col gap-1 text-sm bg-gray-50 p-3 rounded-lg border border-gray-100">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Ngày đặt:</span>
                  <span className="font-medium">{parseIsoDateAsLocal(activeSummary.date).toLocaleDateString("vi-VN", { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Khung giờ:</span>
                  <span className="font-medium text-emerald-700">{activeSummary.time}</span>
                </div>
              </div>

              {activeSummary.accessories.length > 0 && (
                <>
                  <Separator />
                  <div className="space-y-2">
                    <div className="text-xs font-semibold text-muted-foreground uppercase">Phụ kiện đã chọn</div>
                    {activeSummary.accessories.map((acc, i) => (
                      <div key={i} className="flex justify-between text-sm">
                        <span className="text-muted-foreground">
                          {acc.name} <span className="text-xs bg-gray-100 px-1.5 py-0.5 rounded ml-1 font-mono">x{acc.quantity}</span>
                        </span>
                        <span>{acc.price.toLocaleString("vi-VN")}đ</span>
                      </div>
                    ))}
                  </div>
                </>
              )}

              <Separator />
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Phí dịch vụ</span>
                <span>{platformFee.toLocaleString("vi-VN")}đ</span>
              </div>
              <Separator />
              <div className="flex justify-between items-center text-xl font-bold">
                <span className="text-gray-800">Tổng cộng</span>
                <span className="text-2xl text-emerald-600">{activeSummary.total.toLocaleString("vi-VN")}đ</span>
              </div>
            </CardContent>
          </Card>

          {/* Payment Method */}
          <Card className="mb-6 shadow-sm">
            <CardHeader>
              <h3 className="text-lg font-semibold text-gray-850">Chọn phương thức thanh toán</h3>
            </CardHeader>
            <CardContent>
              <RadioGroup value={paymentMethod} onValueChange={setPaymentMethod} className="space-y-3">
                <div
                  className={`flex items-center space-x-4 p-4 border-2 rounded-lg cursor-pointer transition-all ${
                    paymentMethod === "cash"
                      ? "border-emerald-600 bg-emerald-50/20"
                      : "border-border hover:border-gray-300"
                  }`}
                  onClick={() => setPaymentMethod("cash")}
                >
                  <RadioGroupItem value="cash" id="cash" />
                  <Banknote className="h-6 w-6 text-emerald-600" />
                  <Label htmlFor="cash" className="flex-1 cursor-pointer">
                    <div className="font-semibold text-gray-800">Thanh toán bằng tiền mặt tại sân</div>
                    <div className="text-xs text-muted-foreground mt-0.5">
                      Thanh toán trực tiếp bằng tiền mặt khi đến sân
                    </div>
                  </Label>
                </div>

                <div
                  className={`flex items-center space-x-4 p-4 border-2 rounded-lg cursor-pointer transition-all ${
                    paymentMethod === "vnpay"
                      ? "border-emerald-600 bg-emerald-50/20"
                      : "border-border hover:border-gray-300"
                  }`}
                  onClick={() => setPaymentMethod("vnpay")}
                >
                  <RadioGroupItem value="vnpay" id="vnpay" />
                  <CreditCard className="h-6 w-6 text-emerald-600" />
                  <Label htmlFor="vnpay" className="flex-1 cursor-pointer">
                    <div className="font-semibold text-gray-800">VNPay (Thanh toán toàn bộ)</div>
                    <div className="text-xs text-muted-foreground mt-0.5">
                      Thanh toán toàn bộ 100% giá trị đơn đặt sân qua mã QR hoặc thẻ ngân hàng
                    </div>
                  </Label>
                  <div className="relative w-16 h-6 flex-shrink-0 bg-blue-600 rounded border flex items-center justify-center p-1">
                    <span className="text-[10px] font-bold text-white">VNPAY</span>
                  </div>
                </div>

                <div
                  className={`flex items-center space-x-4 p-4 border-2 rounded-lg cursor-pointer transition-all ${
                    paymentMethod === "vnpay_deposit"
                      ? "border-emerald-600 bg-emerald-50/20"
                      : "border-border hover:border-gray-300"
                  }`}
                  onClick={() => setPaymentMethod("vnpay_deposit")}
                >
                  <RadioGroupItem value="vnpay_deposit" id="vnpay_deposit" />
                  <CreditCard className="h-6 w-6 text-emerald-600" />
                  <Label htmlFor="vnpay_deposit" className="flex-1 cursor-pointer">
                    <div className="font-semibold text-gray-800">VNPay (Thanh toán cọc 30%)</div>
                    <div className="text-xs text-muted-foreground mt-0.5">
                      Thanh toán trước 30% giá trị để giữ chỗ, phần còn lại thanh toán tại sân
                    </div>
                  </Label>
                  <div className="relative w-16 h-6 flex-shrink-0 bg-blue-600 rounded border flex items-center justify-center p-1">
                    <span className="text-[10px] font-bold text-white">VNPAY</span>
                  </div>
                </div>
              </RadioGroup>
            </CardContent>
          </Card>

          <Button
            className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-semibold py-6 rounded-lg text-lg shadow-sm transition-all"
            size="lg"
            onClick={handlePaymentSubmit}
            disabled={isSubmitting}
          >
            {isSubmitting
              ? "Đang xử lý..."
              : paymentMethod === "vnpay" || paymentMethod === "vnpay_deposit"
              ? "Thanh toán qua VNPay"
              : "Hoàn tất đặt sân"}
          </Button>

          {/* Security Notice */}
          <div className="flex items-center justify-center gap-2 mt-6 text-xs md:text-sm text-muted-foreground">
            <Shield className="h-4 w-4 text-emerald-600" />
            <span>Mọi giao dịch thanh toán đều được bảo mật 256-bit và mã hóa hoàn toàn.</span>
          </div>
        </div>
      </div>

      <Footer />
    </div>
  );
}

export default function PaymentPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen bg-background flex flex-col justify-between">
        <Header />
        <div className="flex-grow flex items-center justify-center p-8">
          <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-emerald-500"></div>
        </div>
        <Footer />
      </div>
    }>
      <PaymentContent />
    </Suspense>
  );
}
