'use client'

import { useState } from "react";
import Link from "next/link";
import { Lock, Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader } from "@/components/ui/card";

export function ResetPasswordPage() {
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const getPasswordStrength = () => {
    if (password.length === 0) return 0;
    if (password.length < 6) return 25;
    if (password.length < 8) return 50;
    if (password.length >= 8 && /[A-Z]/.test(password) && /[0-9]/.test(password)) return 100;
    return 75;
  };

  const strength = getPasswordStrength();

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary/10 via-background to-primary/5 flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center pb-8">
          <div className="mx-auto w-20 h-20 bg-primary/10 rounded-full flex items-center justify-center mb-4">
            <Lock className="h-10 w-10 text-primary" />
          </div>
          <h1 className="text-2xl mb-2">Đặt lại mật khẩu</h1>
          <p className="text-muted-foreground">
            Nhập mật khẩu mới cho tài khoản của bạn
          </p>
        </CardHeader>

        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="password">Mật khẩu mới</Label>
            <div className="relative">
              <Input
                id="password"
                type={showPassword ? "text" : "password"}
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="pr-10"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
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
            <p className="text-xs text-muted-foreground">
              {strength === 0 && "Nhập mật khẩu"}
              {strength === 25 && "Yếu - Cần ít nhất 6 ký tự"}
              {strength === 50 && "Trung bình - Nên có 8+ ký tự"}
              {strength === 75 && "Tốt - Thêm chữ hoa và số"}
              {strength === 100 && "Mạnh - Mật khẩu an toàn"}
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="confirm-password">Xác nhận mật khẩu</Label>
            <div className="relative">
              <Input
                id="confirm-password"
                type={showConfirmPassword ? "text" : "password"}
                placeholder="••••••••"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="pr-10"
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
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
              <p className="text-xs text-destructive">Mật khẩu không khớp</p>
            )}
          </div>

          <Button
            className="w-full"
            size="lg"
            disabled={!password || !confirmPassword || password !== confirmPassword || strength < 50}
          >
            Đặt lại mật khẩu
          </Button>

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


export default ResetPasswordPage;
