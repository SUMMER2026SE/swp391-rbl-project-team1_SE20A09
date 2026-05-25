'use client'

import { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Lock, Eye, EyeOff, Mail, Key, Loader2, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { post } from "@/lib/api";

function ResetPasswordPage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);
  const [countdown, setCountdown] = useState(3);

  // Auto pre-fill email from query parameters
  useEffect(() => {
    const emailParam = searchParams.get("email");
    if (emailParam) {
      setEmail(emailParam);
    }
  }, [searchParams]);

  // Countdown redirect on success
  useEffect(() => {
    if (isSuccess && countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    } else if (isSuccess && countdown === 0) {
      router.push("/login");
    }
  }, [isSuccess, countdown, router]);

  const getPasswordStrength = () => {
    if (password.length === 0) return 0;
    if (password.length < 6) return 25;
    if (password.length < 8) return 50;
    if (password.length >= 8 && /[A-Z]/.test(password) && /[0-9]/.test(password)) return 100;
    return 75;
  };

  const strength = getPasswordStrength();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!email.trim() || !otp.trim() || !password.trim()) {
      setError("Vui lòng điền đầy đủ tất cả các trường.");
      return;
    }

    if (password !== confirmPassword) {
      setError("Mật khẩu xác nhận không khớp.");
      return;
    }

    if (strength < 50) {
      setError("Mật khẩu quá yếu. Vui lòng nhập mật khẩu tối thiểu 6 ký tự.");
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      // Gọi API reset-password với email, otp, newPassword
      await post("/auth/reset-password", {
        email: email.trim(),
        otp: otp.trim(),
        newPassword: password
      });
      setIsSuccess(true);
    } catch (err: any) {
      setError(err.message ?? "Không thể đặt lại mật khẩu. Vui lòng kiểm tra mã OTP.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary/10 via-background to-primary/5 flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center pb-6">
          <div className="mx-auto w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mb-4">
            {isSuccess ? (
              <CheckCircle2 className="h-9 w-9 text-green-500" />
            ) : (
              <Lock className="h-8 w-8 text-primary" />
            )}
          </div>
          <h1 className="text-2xl font-semibold mb-2">
            {isSuccess ? "Đặt lại thành công!" : "Đặt lại mật khẩu"}
          </h1>
          <p className="text-muted-foreground text-sm">
            {isSuccess
              ? "Tài khoản của bạn đã được cập nhật mật khẩu mới."
              : "Nhập OTP được gửi về hòm thư để tạo mật khẩu mới cho tài khoản."}
          </p>
        </CardHeader>

        <CardContent className="space-y-4">
          {error && (
            <div className="p-3 bg-red-100 dark:bg-red-950/50 text-red-600 dark:text-red-400 text-sm rounded-lg border border-red-200 dark:border-red-900/50 text-center font-medium">
              {error}
            </div>
          )}

          {isSuccess ? (
            <div className="space-y-4">
              <div className="p-4 bg-green-100 dark:bg-green-950/30 text-green-700 dark:text-green-400 text-sm rounded-lg border border-green-200 dark:border-green-900/30 text-center font-medium">
                Mật khẩu của bạn đã được thay đổi thành công!
                <br />
                Đang chuyển hướng về trang Đăng nhập sau {countdown} giây...
              </div>

              <Button className="w-full" size="lg" onClick={() => router.push("/login")}>
                Đăng nhập ngay
              </Button>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Email (Show input only if not pre-filled via URL to keep UI clean) */}
              {searchParams.get("email") ? (
                <div className="text-center text-sm bg-primary/5 border border-primary/10 p-2.5 rounded-lg text-muted-foreground font-medium mb-2">
                  Đang khôi phục tài khoản: <br/>
                  <span className="text-foreground font-semibold">{email}</span>
                </div>
              ) : (
                <div className="space-y-2">
                  <Label htmlFor="email">Email</Label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
                    <Input
                      id="email"
                      type="email"
                      placeholder="your@email.com"
                      className="pl-10"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      disabled={isLoading}
                      required
                    />
                  </div>
                </div>
              )}

              {/* OTP Field */}
              <div className="space-y-2">
                <Label htmlFor="otp">Mã OTP (6 chữ số)</Label>
                <div className="relative">
                  <Key className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
                  <Input
                    id="otp"
                    type="text"
                    pattern="\d*"
                    maxLength={6}
                    placeholder="123456"
                    className="pl-10 tracking-widest font-semibold"
                    value={otp}
                    onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
                    disabled={isLoading}
                    required
                  />
                </div>
              </div>

              {/* New Password Field */}
              <div className="space-y-2">
                <Label htmlFor="password">Mật khẩu mới</Label>
                <div className="relative">
                  <Input
                    id="password"
                    type={showPassword ? "text" : "password"}
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    disabled={isLoading}
                    className="pr-10"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    disabled={isLoading}
                    className="absolute right-3 top-3 text-muted-foreground hover:text-foreground"
                  >
                    {showPassword ? (
                      <EyeOff className="h-5 w-5" />
                    ) : (
                      <Eye className="h-5 w-5" />
                    )}
                  </button>
                </div>

                {/* Password Strength Indicator */}
                <div className="flex gap-1 mt-2">
                  {[25, 50, 75, 100].map((level) => (
                    <div
                      key={level}
                      className={`h-1 flex-1 rounded ${
                        strength >= level
                          ? level === 100
                            ? "bg-green-500"
                            : level >= 75
                            ? "bg-yellow-500"
                            : "bg-orange-500"
                          : "bg-muted"
                      }`}
                    />
                  ))}
                </div>
                <p className="text-xs text-muted-foreground font-medium">
                  {strength === 0 && "Nhập mật khẩu"}
                  {strength === 25 && "Yếu - Cần ít nhất 6 ký tự"}
                  {strength === 50 && "Trung bình - Nên có 8+ ký tự"}
                  {strength === 75 && "Tốt - Thêm chữ hoa và số"}
                  {strength === 100 && "Mạnh - Mật khẩu cực kỳ an toàn"}
                </p>
              </div>

              {/* Confirm Password Field */}
              <div className="space-y-2">
                <Label htmlFor="confirm-password">Xác nhận mật khẩu</Label>
                <div className="relative">
                  <Input
                    id="confirm-password"
                    type={showConfirmPassword ? "text" : "password"}
                    placeholder="••••••••"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    disabled={isLoading}
                    className="pr-10"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    disabled={isLoading}
                    className="absolute right-3 top-3 text-muted-foreground hover:text-foreground"
                  >
                    {showConfirmPassword ? (
                      <EyeOff className="h-5 w-5" />
                    ) : (
                      <Eye className="h-5 w-5" />
                    )}
                  </button>
                </div>
                {confirmPassword && password !== confirmPassword && (
                  <p className="text-xs text-red-500 dark:text-red-400 font-medium">Mật khẩu không khớp</p>
                )}
              </div>

              <Button
                className="w-full"
                size="lg"
                type="submit"
                disabled={isLoading || !password || !confirmPassword || password !== confirmPassword || strength < 50}
              >
                {isLoading ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Đang đặt lại mật khẩu...
                  </>
                ) : (
                  "Đặt lại mật khẩu"
                )}
              </Button>
            </form>
          )}

          <p className="text-center text-sm text-muted-foreground mt-4">
            <Link href="/login" className="text-primary hover:underline inline-flex items-center">
              Quay lại đăng nhập
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  );
}

export default ResetPasswordPage;
