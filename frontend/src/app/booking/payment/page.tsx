'use client'

import { useState, useEffect } from "react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { CreditCard, Building2, Smartphone, Shield, Clock } from "lucide-react";
import { useRouter } from "next/navigation";

function PaymentPage() {
  const router = useRouter();
  const [paymentMethod, setPaymentMethod] = useState("bank");
  const [countdown, setCountdown] = useState(300);
  const [checkout, setCheckout] = useState<any>(null);

  useEffect(() => {
    if (typeof window !== 'undefined') {
      const stored = localStorage.getItem('sport_venue_checkout');
      if (stored) {
        setCheckout(JSON.parse(stored));
      }
    }
  }, []);

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  };

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="max-w-3xl mx-auto">
          <h1 className="text-3xl mb-8">Thanh toán</h1>

          {/* Timer Notice */}
          <Card className="mb-6 border-primary/50 bg-primary/5">
            <CardContent className="p-4">
              <div className="flex items-center gap-3 text-primary">
                <Clock className="h-5 w-5" />
                <span>
                  Sân sẽ được giữ trong{" "}
                  <strong className="text-lg">{formatTime(countdown)}</strong> phút
                </span>
              </div>
            </CardContent>
          </Card>

          {/* Order Summary */}
          <Card className="mb-6">
            <CardHeader>
              <h3>Chi tiết đơn hàng</h3>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="flex justify-between">
                <span className="text-muted-foreground">{checkout?.venueName || "Sân bóng Thành Công"}</span>
                <span>{(checkout?.venuePrice || 500000).toLocaleString('vi-VN')}đ</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Ngày: {checkout?.date ? new Date(checkout.date).toLocaleDateString('vi-VN') : "22/05/2024"}</span>
                <span className="text-muted-foreground">Giờ: {checkout?.slotTime || "18:00 - 20:00"}</span>
              </div>
              <Separator />
              {checkout?.accessories && checkout.accessories.length > 0 ? (
                checkout.accessories.map((acc: any, index: number) => (
                  <div key={index} className="flex justify-between text-sm">
                    <span className="text-muted-foreground">{acc.name} (x{acc.quantity})</span>
                    <span>{(acc.price * acc.quantity).toLocaleString('vi-VN')}đ</span>
                  </div>
                ))
              ) : (
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Phụ kiện</span>
                  <span>0đ</span>
                </div>
              )}
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Phí dịch vụ</span>
                <span>{(checkout?.platformFee || 20000).toLocaleString('vi-VN')}đ</span>
              </div>
              <Separator />
              <div className="flex justify-between text-xl">
                <span>Tổng cộng</span>
                <span className="text-primary">{(checkout?.total || 570000).toLocaleString('vi-VN')}đ</span>
              </div>
            </CardContent>
          </Card>

          {/* Payment Method */}
          <Card className="mb-6">
            <CardHeader>
              <h3>Chọn phương thức thanh toán</h3>
            </CardHeader>
            <CardContent>
              <RadioGroup value={paymentMethod} onValueChange={setPaymentMethod}>
                <div
                  className={`flex items-center space-x-4 p-4 border-2 rounded-lg cursor-pointer transition-all ${
                    paymentMethod === "bank"
                      ? "border-primary bg-primary/5"
                      : "border-border"
                  }`}
                  onClick={() => setPaymentMethod("bank")}
                >
                  <RadioGroupItem value="bank" id="bank" />
                  <Building2 className="h-6 w-6 text-muted-foreground" />
                  <Label htmlFor="bank" className="flex-1 cursor-pointer">
                    <div className="font-medium">Chuyển khoản ngân hàng</div>
                    <div className="text-sm text-muted-foreground">
                      Chuyển khoản qua tài khoản ngân hàng
                    </div>
                  </Label>
                </div>

                <div
                  className={`flex items-center space-x-4 p-4 border-2 rounded-lg cursor-pointer transition-all ${
                    paymentMethod === "vnpay"
                      ? "border-primary bg-primary/5"
                      : "border-border"
                  }`}
                  onClick={() => setPaymentMethod("vnpay")}
                >
                  <RadioGroupItem value="vnpay" id="vnpay" />
                  <CreditCard className="h-6 w-6 text-muted-foreground" />
                  <Label htmlFor="vnpay" className="flex-1 cursor-pointer">
                    <div className="font-medium">VNPay</div>
                    <div className="text-sm text-muted-foreground">
                      Thanh toán qua VNPay QR
                    </div>
                  </Label>
                  <img
                    src="https://via.placeholder.com/60x20/0066CC/FFFFFF?text=VNPAY"
                    alt="VNPay"
                    className="h-6"
                  />
                </div>

                <div
                  className={`flex items-center space-x-4 p-4 border-2 rounded-lg cursor-pointer transition-all ${
                    paymentMethod === "momo"
                      ? "border-primary bg-primary/5"
                      : "border-border"
                  }`}
                  onClick={() => setPaymentMethod("momo")}
                >
                  <RadioGroupItem value="momo" id="momo" />
                  <Smartphone className="h-6 w-6 text-muted-foreground" />
                  <Label htmlFor="momo" className="flex-1 cursor-pointer">
                    <div className="font-medium">MoMo</div>
                    <div className="text-sm text-muted-foreground">
                      Ví điện tử MoMo
                    </div>
                  </Label>
                  <img
                    src="https://via.placeholder.com/60x20/A50064/FFFFFF?text=MoMo"
                    alt="MoMo"
                    className="h-6"
                  />
                </div>
              </RadioGroup>
            </CardContent>
          </Card>

          <Button 
            className="w-full" 
            size="lg"
            onClick={() => {
              if (typeof window !== 'undefined') {
                const stored = localStorage.getItem('sport_venue_bookings');
                const bookingsList = stored ? JSON.parse(stored) : [];
                
                const newBookingId = `BK00${1238 + bookingsList.length}`;
                const newBooking = {
                  id: newBookingId,
                  venueName: checkout?.venueName || "Sân bóng Thành Công",
                  venueImage: "https://images.unsplash.com/photo-1705593813682-033ee2991df6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
                  sportType: checkout?.sportType || "Bóng đá",
                  location: checkout?.location || "Quận 1, TP.HCM",
                  date: checkout?.date || new Date().toISOString().split('T')[0],
                  startTime: checkout?.slotTime ? checkout.slotTime.split(" - ")[0] : "18:00",
                  endTime: checkout?.slotTime ? checkout.slotTime.split(" - ")[1] : "20:00",
                  duration: 2,
                  pricePerHour: 250000,
                  totalPrice: checkout?.total || 570000,
                  status: "confirmed",
                  bookingCode: `BK-2026-${newBookingId.replace('BK', '')}`,
                  paymentMethod: paymentMethod === "bank" ? "Chuyển khoản ngân hàng" : paymentMethod === "vnpay" ? "Ví điện tử VNPay" : "Ví điện tử MoMo",
                  paidAt: new Date().toISOString(),
                  accessories: checkout?.accessories || [],
                };
                
                bookingsList.unshift(newBooking);
                localStorage.setItem('sport_venue_bookings', JSON.stringify(bookingsList));
                localStorage.removeItem('sport_venue_checkout');
              }
              router.push("/profile?tab=bookings");
            }}
          >
            Thanh toán ngay
          </Button>

          {/* Security Notice */}
          <div className="flex items-center justify-center gap-2 mt-6 text-sm text-muted-foreground">
            <Shield className="h-4 w-4" />
            <span>Giao dịch được bảo mật và mã hóa</span>
          </div>
        </div>
      </div>

      <Footer />
    </div>
  );
}

export default PaymentPage;
