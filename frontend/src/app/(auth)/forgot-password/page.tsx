'use client'

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Mail, ArrowLeft, Lock, Loader2, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { post } from "@/lib/api";

function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);
  const router = useRouter();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim()) {
      setError("Vui lòng nhập email của bạn.");
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      // Gọi API forgot-password
      await post("/auth/forgot-password", { email: email.trim() });
      setIsSuccess(true);
    } catch (err: any) {
      setError(err.message ?? "Đã xảy ra lỗi khi yêu cầu khôi phục mật khẩu.");
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
            {isSuccess ? "Đã gửi mã OTP!" : "Quên mật khẩu?"}
          </h1>
          <p className="text-muted-foreground text-sm">
            {isSuccess
              ? "Một email chứa mã OTP khôi phục mật khẩu đã được gửi đến địa chỉ email của bạn."
              : "Nhập email của bạn và chúng tôi sẽ gửi mã OTP để đặt lại mật khẩu mới."}
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
              <div className="p-3 bg-green-100 dark:bg-green-950/30 text-green-700 dark:text-green-400 text-sm rounded-lg border border-green-200 dark:border-green-900/30 text-center font-medium">
                Vui lòng kiểm tra hộp thư đến (hoặc hòm thư Spam) để lấy mã OTP gồm 6 chữ số.
              </div>

              <Button
                className="w-full"
                size="lg"
                onClick={() => router.push(`/reset-password?email=${encodeURIComponent(email)}`)}
              >
                Nhập mã OTP đặt lại mật khẩu
              </Button>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
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

              <Button className="w-full" size="lg" type="submit" disabled={isLoading}>
                {isLoading ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Đang gửi yêu cầu...
                  </>
                ) : (
                  "Gửi mã OTP"
                )}
              </Button>
            </form>
          )}

          <div className="pt-2 text-center">
            <Link
              href="/login"
              className="inline-flex items-center text-sm text-primary hover:underline"
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

export default ForgotPasswordPage;
