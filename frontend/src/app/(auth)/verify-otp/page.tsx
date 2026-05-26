'use client'

import { useState, useEffect, Suspense } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Mail, Loader2, ArrowLeft, CheckCircle2 } from "lucide-react";
import { toast } from "sonner";
import { signIn } from "next-auth/react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { InputOTP, InputOTPGroup, InputOTPSlot } from "@/components/ui/input-otp";
import api from "@/lib/api";

function VerifyOTPForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const email = searchParams.get("email") || "";

  const [otp, setOtp] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [countdown, setCountdown] = useState(60);

  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (countdown > 0) {
      timer = setTimeout(() => setCountdown(countdown - 1), 1000);
    }
    return () => clearTimeout(timer);
  }, [countdown]);

  const handleVerify = async () => {
    if (otp.length < 6) return;

    setIsLoading(true);
    try {
      await api.post(`/auth/verify-otp?email=${encodeURIComponent(email)}&otpCode=${otp}`);
      setIsSuccess(true);
      toast.success("Xác thực thành công! Đang đăng nhập...");

      const savedPassword = sessionStorage.getItem("pending_login_password");
      const savedEmail = sessionStorage.getItem("pending_login_email");

      if (savedPassword && savedEmail) {
        const result = await signIn("credentials", {
          redirect: false,
          email: savedEmail,
          password: savedPassword,
        });

        sessionStorage.removeItem("pending_login_email");
        sessionStorage.removeItem("pending_login_password");

        if (result?.error) {
          router.push("/login");
        } else {
          router.push("/");
          router.refresh();
        }
      } else {
        setTimeout(() => router.push("/login"), 1500);
      }
    } catch (error: any) {
      toast.error(error.message || "Mã OTP không chính xác hoặc đã hết hạn.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleResend = async () => {
    if (countdown > 0 || isResending) return;

    setIsResending(true);
    try {
      await api.post(`/auth/resend-otp?email=${encodeURIComponent(email)}`);
      toast.success("Mã OTP mới đã được gửi!");
      setCountdown(60);
      setOtp("");
    } catch (error: any) {
      toast.error(error.message || "Không thể gửi lại mã OTP.");
    } finally {
      setIsResending(false);
    }
  };

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  };

  if (!email) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-primary/10 via-background to-primary/5 flex items-center justify-center p-4">
        <Card className="w-full max-w-md text-center p-8">
          <h2 className="text-xl font-semibold mb-4">Thiếu thông tin email</h2>
          <p className="text-muted-foreground mb-6">Vui lòng quay lại trang đăng ký để tiếp tục.</p>
          <Button onClick={() => router.push("/register")} className="w-full">
            Quay lại đăng ký
          </Button>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary/10 via-background to-primary/5 flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center pb-8">
          <div className="mx-auto w-20 h-20 bg-primary/10 rounded-full flex items-center justify-center mb-4">
            {isSuccess ? (
              <CheckCircle2 className="h-10 w-10 text-green-500" />
            ) : (
              <Mail className="h-10 w-10 text-primary" />
            )}
          </div>
          <h1 className="text-2xl font-bold mb-2">
            {isSuccess ? "Xác thực thành công" : "Xác thực email"}
          </h1>
          <p className="text-muted-foreground">
            {isSuccess ? (
              "Tài khoản đã được kích hoạt. Đang đăng nhập..."
            ) : (
              <>
                Chúng tôi đã gửi mã xác thực đến
                <br />
                <span className="font-semibold text-foreground">{email}</span>
              </>
            )}
          </p>
        </CardHeader>

        <CardContent className="space-y-6">
          {!isSuccess && (
            <>
              <div className="flex flex-col items-center space-y-4">
                <InputOTP
                  maxLength={6}
                  value={otp}
                  onChange={setOtp}
                  disabled={isLoading}
                >
                  <InputOTPGroup>
                    <InputOTPSlot index={0} />
                    <InputOTPSlot index={1} />
                    <InputOTPSlot index={2} />
                    <InputOTPSlot index={3} />
                    <InputOTPSlot index={4} />
                    <InputOTPSlot index={5} />
                  </InputOTPGroup>
                </InputOTP>

                <div className="text-center">
                  {countdown > 0 ? (
                    <p className="text-sm text-muted-foreground">
                      Gửi lại mã sau{" "}
                      <span className="text-primary font-medium">{formatTime(countdown)}</span>
                    </p>
                  ) : (
                    <button
                      onClick={handleResend}
                      disabled={isResending}
                      className="text-sm text-primary hover:underline font-medium disabled:opacity-50"
                    >
                      {isResending ? "Đang gửi..." : "Gửi lại mã OTP"}
                    </button>
                  )}
                </div>
              </div>

              <Button
                className="w-full"
                size="lg"
                onClick={handleVerify}
                disabled={otp.length < 6 || isLoading}
              >
                {isLoading ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Đang xác thực...
                  </>
                ) : (
                  "Xác thực"
                )}
              </Button>
            </>
          )}

          {isSuccess && (
            <div className="flex justify-center">
              <Loader2 className="h-6 w-6 animate-spin text-primary" />
            </div>
          )}

          <div className="text-center">
            <Link
              href="/login"
              className="inline-flex items-center text-sm text-muted-foreground hover:text-primary transition-colors"
            >
              <ArrowLeft className="mr-2 h-4 w-4" />
              Quay lại đăng nhập
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export default function VerifyOTPPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen bg-gradient-to-br from-primary/10 via-background to-primary/5 flex items-center justify-center p-4">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    }>
      <VerifyOTPForm />
    </Suspense>
  );
}
