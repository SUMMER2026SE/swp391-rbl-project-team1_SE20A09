"use client";

import { useEffect, useState, createContext, useContext } from "react";
import { useSession } from "next-auth/react";
import { usePathname, useRouter } from "next/navigation";
import { Lock, LogIn } from "lucide-react";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogAction,
  AlertDialogCancel,
} from "@/components/ui/alert-dialog";

const PROTECTED_PREFIXES = [
  "/profile",
  "/booking",
  "/owner",
  "/admin",
  "/chat",
  "/bookings",
  "/notifications",
  "/payments",
  "/complaints",
  "/community",
];

function isProtectedRoute(pathname: string): boolean {
  return PROTECTED_PREFIXES.some((prefix) => pathname.startsWith(prefix));
}

type RouteGuardContextType = {
  triggerLoginModal: (callbackUrl: string) => void;
};

const RouteGuardContext = createContext<RouteGuardContextType | null>(null);

export function useRouteGuard() {
  const context = useContext(RouteGuardContext);
  if (!context) {
    throw new Error("useRouteGuard must be used within a RouteGuard provider");
  }
  return context;
}

type RouteGuardProps = {
  children: React.ReactNode;
};

export function RouteGuard({ children }: RouteGuardProps) {
  const { status } = useSession();
  const pathname = usePathname();
  const router = useRouter();

  const [showLoginDialog, setShowLoginDialog] = useState(false);
  const [blockedPath, setBlockedPath] = useState<string>("");

  useEffect(() => {
    if (status === "loading") return;

    if (status === "unauthenticated" && isProtectedRoute(pathname)) {
      setBlockedPath(pathname);
      setShowLoginDialog(true);
    } else if (status === "authenticated") {
      // Tự động đóng nếu đăng nhập thành công
      setShowLoginDialog(false);
    }
  }, [status, pathname]);

  const triggerLoginModal = (callbackUrl: string) => {
    setBlockedPath(callbackUrl);
    setShowLoginDialog(true);
  };

  const handleGoToLogin = () => {
    setShowLoginDialog(false);
    router.push(`/login?callbackUrl=${encodeURIComponent(blockedPath)}`);
  };

  const handleCancel = () => {
    setShowLoginDialog(false);
    // Nếu đang ở route bị bảo vệ, quay lại trang chủ. Nếu ở trang công khai, giữ nguyên vị trí.
    if (isProtectedRoute(pathname)) {
      router.push("/");
    }
  };

  // Khi đang tải session trên route được bảo vệ, ẩn nội dung để tránh nhấp nháy
  if (status === "loading" && isProtectedRoute(pathname)) {
    return null;
  }

  const dialogElement = (
    <AlertDialog open={showLoginDialog} onOpenChange={setShowLoginDialog}>
      <AlertDialogContent className="max-w-md">
        <AlertDialogHeader>
          <div className="flex items-center gap-3 mb-1">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-emerald-100">
              <Lock className="h-5 w-5 text-emerald-700" />
            </div>
            <AlertDialogTitle className="text-lg">
              Yêu cầu đăng nhập
            </AlertDialogTitle>
          </div>
          <AlertDialogDescription className="text-sm text-muted-foreground leading-relaxed">
            Tính năng này chỉ dành cho thành viên đã đăng nhập. Vui lòng đăng
            nhập hoặc đăng ký tài khoản miễn phí để tiếp tục.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter className="flex-row justify-end gap-2 sm:flex-row">
          <AlertDialogCancel
            onClick={handleCancel}
            className="mt-0"
          >
            Hủy
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={handleGoToLogin}
            className="bg-emerald-600 hover:bg-emerald-700 text-white gap-2"
          >
            <LogIn className="h-4 w-4" />
            Đăng nhập
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );

  // Nếu là route bị bảo vệ và chưa đăng nhập, chỉ hiển thị dialog
  if (status === "unauthenticated" && isProtectedRoute(pathname)) {
    return dialogElement;
  }

  return (
    <RouteGuardContext.Provider value={{ triggerLoginModal }}>
      {children}
      {showLoginDialog && dialogElement}
    </RouteGuardContext.Provider>
  );
}
