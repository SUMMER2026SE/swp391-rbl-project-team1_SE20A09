'use client'

import { Header } from "@/components/layout/Header";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Button } from "@/components/ui/button";
import {
  Home,
  BarChart3,
  Calendar,
  Wallet,
  Bell,
  AlertTriangle,
} from "lucide-react";

export default function OwnerLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();

  const menuItems = [
    { href: "/owner/dashboard", label: "Dashboard", icon: Home },
    { href: "/owner/venues", label: "Sân của tôi", icon: BarChart3 },
    { href: "/owner/bookings", label: "Lịch đặt", icon: Calendar },
    { href: "/owner/revenue", label: "Doanh thu", icon: Wallet },
    { href: "/owner/complaints", label: "Khiếu nại", icon: AlertTriangle },
    { href: "/owner/notifications", label: "Thông báo", icon: Bell },
  ];

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-900 flex flex-col">
      <Header />
      <div className="flex flex-1">
        {/* Sidebar */}
        <aside className="w-64 min-h-[calc(100vh-64px)] bg-white dark:bg-slate-950 border-r p-4 hidden md:block shrink-0">
          <h2 className="mb-6 px-3 text-lg font-bold text-slate-800 dark:text-slate-200">Quản lý chủ sân</h2>
          <nav className="space-y-1">
            {menuItems.map((item) => {
              const Icon = item.icon;
              const isActive = pathname.startsWith(item.href);
              return (
                <Link key={item.href} href={item.href} passHref className="block">
                  <Button
                    variant={isActive ? "default" : "ghost"}
                    className="w-full justify-start gap-3"
                    size="sm"
                  >
                    <Icon className="h-4 w-4" />
                    {item.label}
                  </Button>
                </Link>
              );
            })}
          </nav>
        </aside>

        {/* Page Content */}
        <div className="flex-1 overflow-x-hidden">
          {children}
        </div>
      </div>
    </div>
  );
}
