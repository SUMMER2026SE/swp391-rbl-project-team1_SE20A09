'use client'

import Link from "next/link";
import { Mail, ArrowLeft, Lock } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader } from "@/components/ui/card";

function ForgotPasswordPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-primary/10 via-background to-primary/5 flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center pb-8">
          <div className="mx-auto w-20 h-20 bg-primary/10 rounded-full flex items-center justify-center mb-4">
            <Lock className="h-10 w-10 text-primary" />
          </div>
          <h1 className="text-2xl mb-2">Quên mật khẩu?</h1>
          <p className="text-muted-foreground">
            Nhập email của bạn và chúng tôi sẽ gửi
            <br />
            link đặt lại mật khẩu
          </p>
        </CardHeader>

        <CardContent className="space-y-4">
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

          <Button className="w-full" size="lg">
            Gửi link đặt lại mật khẩu
          </Button>

          <Link href="/login"
            className="flex items-center justify-center text-sm text-primary hover:underline"
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Quay lại đăng nhập
          </Link>
        </CardContent>
      </Card>
    </div>
  );
}

export default ForgotPasswordPage;
