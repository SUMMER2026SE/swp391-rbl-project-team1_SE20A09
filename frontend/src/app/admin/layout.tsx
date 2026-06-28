"use client";

import { useEffect, useState } from "react";
import { useSession } from "next-auth/react";
import { useRouter, usePathname } from "next/navigation";
import { AdminSidebar } from "@/components/layout/AdminSidebar";
import { UserAccountMenu } from "@/components/layout/Header";
import { Loader2, Menu, Calendar as CalendarIcon, Bell } from "lucide-react";
import { format } from "date-fns";
import { vi } from "date-fns/locale";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { data: session, status } = useSession();
  const router = useRouter();
  const pathname = usePathname();
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  useEffect(() => {
    if (status === "authenticated" && session?.user?.roleName !== "Admin") {
      router.push("/");
    }
  }, [status, session, router]);

  if (status === "loading") {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-emerald-600" />
      </div>
    );
  }

  if (status === "unauthenticated" || session?.user?.roleName !== "Admin") {
    return null;
  }

  const getPageTitle = (path: string) => {
    if (path === "/admin/dashboard") return { title: "Dashboard", subtitle: "Tổng quan hệ thống SportHub" };
    if (path.startsWith("/admin/customers")) return { title: "Khách hàng", subtitle: "Quản lý thông tin khách hàng" };
    if (path.startsWith("/admin/users")) return { title: "Người dùng", subtitle: "Quản lý tài khoản người dùng" };
    if (path.startsWith("/admin/owner-approvals")) return { title: "Duyệt chủ sân", subtitle: "Phê duyệt tài khoản chủ sân" };
    if (path.startsWith("/admin/stadium-approvals")) return { title: "Duyệt sân", subtitle: "Phê duyệt sân thể thao" };
    if (path.startsWith("/admin/sport-categories")) return { title: "Danh mục", subtitle: "Quản lý danh mục môn thể thao" };
    if (path.startsWith("/admin/complaints")) return { title: "Khiếu nại", subtitle: "Xử lý khiếu nại hệ thống" };
    return { title: "Quản trị", subtitle: "Hệ thống SportHub" };
  };

  const { title, subtitle } = getPageTitle(pathname);

  /** Tên hiển thị của admin — fallback "Admin SportHub" nếu chưa có session */
  const adminDisplayName = session?.user
    ? `${session.user.lastName ?? ""} ${session.user.firstName ?? ""}`.trim()
    : "Admin SportHub";

  return (
    <div className="min-h-screen bg-slate-50 flex font-sans text-slate-900 selection:bg-emerald-200">
      <AdminSidebar isOpen={isSidebarOpen} setIsOpen={setIsSidebarOpen} />

      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* HEADER */}
        <header className="h-16 bg-white border-b border-slate-200 px-4 md:px-8 flex items-center justify-between shrink-0 sticky top-0 z-40">
          <div className="flex items-center gap-4">
            <button
              className="md:hidden text-slate-500 p-2 -ml-2 rounded-lg hover:bg-slate-100"
              onClick={() => setIsSidebarOpen(true)}
              aria-label="Mở menu"
            >
              <Menu className="h-5 w-5" />
            </button>
            <div>
              <h1 className="text-lg md:text-xl font-bold text-slate-900 leading-tight">{title}</h1>
              <p className="text-xs text-slate-500 hidden md:block">{subtitle}</p>
            </div>
          </div>

          <div className="flex items-center gap-3 md:gap-5">
            <div className="hidden md:flex items-center gap-2 bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5 text-sm text-slate-600 cursor-pointer hover:bg-slate-100 transition-colors">
              <CalendarIcon className="h-4 w-4 text-slate-400" />
              <span className="capitalize">{format(new Date(), "EEEE, dd MMM yyyy", { locale: vi })}</span>
            </div>

            <button
              className="relative p-2 text-slate-500 hover:bg-slate-100 rounded-full transition-colors"
              aria-label="Thông báo"
            >
              <Bell className="h-5 w-5" />
              <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-rose-500 rounded-full border-2 border-white"></span>
            </button>

            <div className="h-8 w-px bg-slate-200 hidden md:block"></div>

            <div className="flex items-center gap-3">
              {session?.user ? (
                <UserAccountMenu user={session.user} />
              ) : (
                // Fallback avatar khi chưa có session — dùng initials thay vì URL bên ngoài
                <div
                  className="h-9 w-9 rounded-full bg-emerald-100 border border-slate-200 flex items-center justify-center text-emerald-700 font-bold text-sm select-none"
                  aria-label="Admin avatar"
                >
                  A
                </div>
              )}
              <div className="hidden md:block">
                <p className="text-sm font-semibold text-slate-900 leading-none">{adminDisplayName}</p>
                <p className="text-xs text-slate-500 mt-1">Super Admin</p>
              </div>
            </div>
          </div>
        </header>

        {/* CONTENT */}
        <main className="flex-1 overflow-y-auto p-4 md:p-8">
          <div className="max-w-7xl mx-auto">{children}</div>
        </main>
      </div>
    </div>
  );
}
