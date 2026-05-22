'use client'

import { useState } from "react";
import Link from "next/link";
import { Mail } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { InputOTP, InputOTPGroup, InputOTPSlot } from "@/components/ui/input-otp";

export function VerifyOTPPage() {
  const [otp, setOtp] = useState("");
  const [countdown, setCountdown] = useState(150);

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary/10 via-background to-primary/5 flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center pb-8">
          <div className="mx-auto w-20 h-20 bg-primary/10 rounded-full flex items-center justify-center mb-4">
            <Mail className="h-10 w-10 text-primary" />
          </div>
          <h1 className="text-2xl mb-2">Xác thực email</h1>
          <p className="text-muted-foreground">
            Chúng tôi đã gửi mã xác thực đến
            <br />
            <strong>your***@email.com</strong>
          </p>
        </CardHeader>

        <CardContent className="space-y-6">
          <div className="flex flex-col items-center space-y-4">
            <InputOTP maxLength={6} value={otp} onChange={setOtp}>
              <InputOTPGroup>
                <InputOTPSlot index={0} />
                <InputOTPSlot index={1} />
                <InputOTPSlot index={2} />
                <InputOTPSlot index={3} />
                <InputOTPSlot index={4} />
                <InputOTPSlot index={5} />
              </InputOTPGroup>
            </InputOTP>

            <p className="text-sm text-muted-foreground">
              Gửi lại mã sau{" "}
              <span className="text-primary font-medium">{formatTime(countdown)}</span>
            </p>
          </div>

          <Button className="w-full" size="lg" disabled={otp.length < 6}>
            Xác thực
          </Button>

          <div className="text-center">
            <button
              className={`text-sm ${
                countdown > 0
                  ? "text-muted-foreground cursor-not-allowed"
                  : "text-primary hover:underline"
              }`}
              disabled={countdown > 0}
            >
              Gửi lại mã OTP
            </button>
          </div>

          <p className="text-center text-sm text-muted-foreground">
            <Link href="/login" className="text-primary hover:underline">
              Quay lại đăng nhập
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  );
}


export default VerifyOTPPage;
