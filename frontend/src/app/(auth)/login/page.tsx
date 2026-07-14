'use client'

import { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { signIn, getSession } from "next-auth/react";
import { Mail, Lock, Chrome, Loader2, Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { Card, CardContent, CardHeader } from "@/components/ui/card";

function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isGoogleLoading, setIsGoogleLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const router = useRouter();

  useEffect(() => {
    if (typeof window === "undefined") return;
    const params = new URLSearchParams(window.location.search);
    const authError = params.get("error");
    if (authError === "session_expired" || authError === "SessionExpired") {
      setError("Phiên đăng nhập của bạn đã hết hạn. Vui lòng đăng nhập lại.");
      router.replace("/login", { scroll: false });
    }
  }, [router]);

  const getLoginErrorMessage = (message: string) => {
    if (message === "Bad credentials" || message.includes("Bad credentials")) {
      return "Mật khẩu không đúng";
    }

    return message;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      setError("Vui lòng điền đầy đủ email và mật khẩu.");
      return;
    }

    setError(null);
    setIsLoading(true);

    try {
      const result = await signIn("credentials", {
        redirect: false,
        email,
        password,
      });

      if (result?.error) {
        setError(getLoginErrorMessage(result.error));
      } else {
        setError(null);
        const session = await getSession();
        const roleName = session?.user?.roleName;
        const accountStatus = session?.user?.accountStatus;
        const params = new URLSearchParams(window.location.search);
        const rawRedirect = params.get("redirect") ?? params.get("callbackUrl");

        let destination: string;
        if (accountStatus === "BLOCKED") {
          destination = "/appeals";
        } else if (roleName === "Admin") {
          destination = "/admin/dashboard";
        } else if (roleName === "Owner") {
          destination = "/owner/dashboard";
        } else if (rawRedirect && /^\/[^/\\]/.test(rawRedirect)
                   && !rawRedirect.startsWith('/admin')
                   && !rawRedirect.startsWith('/owner')) {
          destination = rawRedirect;
        } else {
          destination = "/";
        }
        router.push(destination);
        router.refresh();
      }
    } catch (err: any) {
      setError("Đã xảy ra lỗi khi đăng nhập. Vui lòng thử lại.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleLogin = async () => {
    setError(null);
    setIsGoogleLoading(true);
    try {
      // After Google OAuth completes, /auth/redirect will read the role and forward accordingly.
      await signIn("google", { callbackUrl: "/auth/redirect" });
    } catch (err) {
      setError("Đã xảy ra lỗi khi đăng nhập bằng Google.");
      setIsGoogleLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary/10 via-background to-primary/5 flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center pb-8">
          <div className="mx-auto w-16 h-16 bg-primary rounded-lg flex items-center justify-center text-white text-2xl mb-4 font-bold">
            SH
          </div>
          <h1 className="text-2xl font-semibold mb-2">Đăng nhập</h1>
          <p className="text-muted-foreground text-sm">
            Đăng nhập để đặt sân và kết nối cộng đồng
          </p>
        </CardHeader>

        <CardContent className="space-y-4">
          {error && (
            <div className="p-3 bg-red-100 dark:bg-red-950/50 text-red-600 dark:text-red-400 text-sm rounded-lg border border-red-200 dark:border-red-900/50 text-center font-medium">
              {error}
            </div>
          )}

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
                  disabled={isLoading || isGoogleLoading}
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">Mật khẩu</Label>
              <div className="relative">
                <Lock className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  placeholder="••••••••"
                  className="pl-10 pr-10"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={isLoading || isGoogleLoading}
                  required
                />
                <button
                  type="button"
                  className="absolute right-3 top-3 text-muted-foreground hover:text-foreground disabled:cursor-not-allowed disabled:opacity-50"
                  onClick={() => setShowPassword((value) => !value)}
                  disabled={isLoading || isGoogleLoading}
                  aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                >
                  {showPassword ? (
                    <EyeOff className="h-5 w-5" />
                  ) : (
                    <Eye className="h-5 w-5" />
                  )}
                </button>
              </div>
            </div>

            <div className="flex justify-end">
              <Link href="/forgot-password"
                className="text-sm text-primary hover:underline"
              >
                Quên mật khẩu?
              </Link>
            </div>

            <Button className="w-full" size="lg" type="submit" disabled={isLoading || isGoogleLoading}>
              {isLoading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Đang đăng nhập...
                </>
              ) : (
                "Đăng nhập"
              )}
            </Button>
          </form>

          <div className="relative my-4">
            <Separator />
            <span className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 bg-card px-2 text-sm text-muted-foreground">
              hoặc
            </span>
          </div>

          <Button
            variant="outline"
            className="w-full"
            size="lg"
            onClick={handleGoogleLogin}
            disabled={isLoading || isGoogleLoading}
          >
            {isGoogleLoading ? (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            ) : (
              <Chrome className="mr-2 h-5 w-5" />
            )}
            Tiếp tục với Google
          </Button>

          <p className="text-center text-sm text-muted-foreground mt-4">
            Chưa có tài khoản?{" "}
            <Link href="/register" className="text-primary hover:underline">
              Đăng ký ngay
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  );
}

export default LoginPage;
