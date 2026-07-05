'use client'

import { UserAccountMenu } from "@/components/layout/Header";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useSession } from "next-auth/react";
import {
  Home,
  BarChart3,
  Calendar,
  Wallet,
  Bell,
  AlertTriangle,
  TrendingUp,
  Menu,
  Calendar as CalendarIcon,
} from "lucide-react";
import { OwnerNotificationBell } from "@/components/notifications/OwnerNotificationBell";
import { ChatBadge } from "@/components/chat/ChatBadge";
import { format } from "date-fns";
import { vi } from "date-fns/locale";
import { useState } from "react";

export default function OwnerLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const { data: session } = useSession();
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  const menuItems = [
    { href: "/owner/dashboard", label: "Dashboard", icon: Home },
    { href: "/owner/venues", label: "Sân của tôi", icon: BarChart3 },
    { href: "/owner/bookings", label: "Lịch đặt", icon: Calendar },
    { href: "/owner/revenue", label: "Doanh thu", icon: Wallet },
    { href: "/owner/complaints", label: "Khiếu nại", icon: AlertTriangle },
    { href: "/owner/notifications", label: "Thông báo", icon: Bell },
  ];

  const getPageTitle = (path: string) => {
    if (path === "/owner/dashboard") return { title: "Dashboard", subtitle: "Tổng quan hệ thống SportHub" };
    if (path.startsWith("/owner/venues")) return { title: "Sân của tôi", subtitle: "Quản lý danh sách sân thể thao" };
    if (path.startsWith("/owner/bookings")) return { title: "Lịch đặt", subtitle: "Quản lý lịch đặt sân" };
    if (path.startsWith("/owner/revenue")) return { title: "Doanh thu", subtitle: "Thống kê doanh thu" };
    if (path.startsWith("/owner/complaints")) return { title: "Khiếu nại", subtitle: "Quản lý khiếu nại khách hàng" };
    if (path.startsWith("/owner/notifications")) return { title: "Thông báo", subtitle: "Thông báo hệ thống" };
    return { title: "Chủ sân", subtitle: "Hệ thống SportHub" };
  };

  const { title, subtitle } = getPageTitle(pathname);

  const ownerDisplayName = session?.user
    ? `${session.user.lastName ?? ""} ${session.user.firstName ?? ""}`.trim()
    : "Chủ Sân";

  return (
    <div className="min-h-screen bg-slate-50 flex font-sans text-slate-900 selection:bg-emerald-200">

      {/* Sidebar - Desktop */}
      <aside className={`
        fixed inset-y-0 left-0 z-50 w-64 bg-white border-r border-slate-200 
        md:relative flex flex-col shrink-0 transition-transform duration-300
        ${isSidebarOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0"}
      `}>
        <div className="h-16 flex items-center px-6 border-b border-slate-100 shrink-0">
          <div className="flex items-center gap-2 text-emerald-600">
            <div className="p-1.5 bg-emerald-600 rounded-lg">
              <TrendingUp className="h-5 w-5 text-white" />
            </div>
            <span className="text-xl font-bold tracking-tight">SportHub</span>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto py-6 space-y-2">
          <div className="text-[13px] font-bold text-slate-400 uppercase tracking-wider mb-4 px-6">Chủ Sân</div>
          <nav className="space-y-2">
            {menuItems.map((item) => {
              const Icon = item.icon;
              const isActive = item.href === '/owner/dashboard'
                ? pathname === item.href
                : pathname.startsWith(item.href);

              return (
                <Link key={item.href} href={item.href} className={`
                  w-[calc(100%-1.25rem)] flex items-center gap-4 pl-6 pr-4 py-3 text-[15px] font-medium transition-all duration-200 relative rounded-r-full
                  ${isActive
                    ? 'bg-emerald-50 text-emerald-700'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'}
                `}>
                  {isActive && (
                    <div className="absolute left-0 top-1/2 -translate-y-1/2 h-7 w-[5px] bg-emerald-600 rounded-r-full" />
                  )}
                  <Icon className={`h-5 w-5 ${isActive ? 'text-emerald-600' : 'text-slate-400'}`} strokeWidth={1.75} />
                  <span className={isActive ? "font-semibold" : ""}>{item.label}</span>
                </Link>
              );
            })}
          </nav>
        </div>
      </aside>

      {/* Overlay for mobile sidebar */}
      {isSidebarOpen && (
        <div
          className="fixed inset-0 bg-black/50 z-40 md:hidden"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}

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

            <ChatBadge userId={(session?.user as any)?.userId} />

            <OwnerNotificationBell />

            <div className="h-8 w-px bg-slate-200 hidden md:block"></div>

            <div className="flex items-center gap-3">
              {session?.user ? (
                <UserAccountMenu user={session.user} />
              ) : (
                <div
                  className="h-9 w-9 rounded-full bg-emerald-100 border border-slate-200 flex items-center justify-center text-emerald-700 font-bold text-sm select-none"
                  aria-label="Owner avatar"
                >
                  O
                </div>
              )}
              <div className="hidden md:block">
                <p className="text-sm font-semibold text-slate-900 leading-none">{ownerDisplayName}</p>
                <p className="text-xs text-slate-500 mt-1">Role: Owner</p>
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
