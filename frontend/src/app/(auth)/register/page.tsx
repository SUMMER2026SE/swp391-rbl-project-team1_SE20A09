'use client'

import Link from "next/link";
import { Mail, Lock, User, Chrome } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { Separator } from "@/components/ui/separator";
import { Card, CardContent, CardHeader } from "@/components/ui/card";

export function RegisterPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-primary/10 via-background to-primary/5 flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center pb-8">
          <div className="mx-auto w-16 h-16 bg-primary rounded-lg flex items-center justify-center text-white text-2xl mb-4">
            SH
          </div>
          <h1 className="text-2xl mb-2">Đăng ký tài khoản</h1>
          <p className="text-muted-foreground">
            Tạo tài khoản để bắt đầu đặt sân
          </p>
        </CardHeader>

        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="fullname">Họ và tên</Label>
            <div className="relative">
              <User className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
              <Input
                id="fullname"
                type="text"
                placeholder="Nguyễn Văn A"
                className="pl-10"
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <div className="relative">
              <Mail className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
              <Input
                id="email"
                type="email"
                placeholder="your@email.com"
                className="pl-10"
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="password">Mật khẩu</Label>
            <div className="relative">
              <Lock className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
              <Input
                id="password"
                type="password"
                placeholder="••••••••"
                className="pl-10"
              />
            </div>
            <div className="flex gap-1 mt-2">
              <div className="h-1 flex-1 rounded bg-muted"></div>
              <div className="h-1 flex-1 rounded bg-muted"></div>
              <div className="h-1 flex-1 rounded bg-muted"></div>
              <div className="h-1 flex-1 rounded bg-muted"></div>
            </div>
            <p className="text-xs text-muted-foreground">
              Mật khẩu phải có ít nhất 8 ký tự
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="confirm-password">Xác nhận mật khẩu</Label>
            <div className="relative">
              <Lock className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
              <Input
                id="confirm-password"
                type="password"
                placeholder="••••••••"
                className="pl-10"
              />
            </div>
          </div>

          <div className="flex items-start space-x-2">
            <Checkbox id="terms" className="mt-1" />
            <label htmlFor="terms" className="text-sm text-muted-foreground cursor-pointer">
              Tôi đồng ý với{" "}
              <a href="#" className="text-primary hover:underline">
                Điều khoản sử dụng
              </a>{" "}
              và{" "}
              <a href="#" className="text-primary hover:underline">
                Chính sách bảo mật
              </a>
            </label>
          </div>

          <Button className="w-full" size="lg">
            Tạo tài khoản
          </Button>

          <div className="relative">
            <Separator />
            <span className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 bg-card px-2 text-sm text-muted-foreground">
              hoặc
            </span>
          </div>

          <Button variant="outline" className="w-full" size="lg">
            <Chrome className="mr-2 h-5 w-5" />
            Đăng ký với Google
          </Button>

          <p className="text-center text-sm text-muted-foreground">
            Đã có tài khoản?{" "}
            <Link href="/login" className="text-primary hover:underline">
              Đăng nhập
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  );
}


export default RegisterPage;
