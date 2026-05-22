'use client'

import { useState } from "react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { CreditCard, Building2, Smartphone, Shield, Clock } from "lucide-react";

export function PaymentPage() {
  const [paymentMethod, setPaymentMethod] = useState("bank");
  const [countdown, setCountdown] = useState(300);

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
                <span className="text-muted-foreground">Sân bóng Thành Công</span>
                <span>500,000đ</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Ngày: 22/05/2024</span>
                <span className="text-muted-foreground">Giờ: 18:00 - 20:00</span>
              </div>
              <Separator />
              <div className="flex justify-between">
                <span className="text-muted-foreground">Phụ kiện</span>
                <span>50,000đ</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Phí dịch vụ</span>
                <span>20,000đ</span>
              </div>
              <Separator />
              <div className="flex justify-between text-xl">
                <span>Tổng cộng</span>
                <span className="text-primary">570,000đ</span>
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

          <Button className="w-full" size="lg">
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
