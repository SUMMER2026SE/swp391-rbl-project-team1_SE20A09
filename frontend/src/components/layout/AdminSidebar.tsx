"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  Users,
  Building2,
  ClipboardCheck,
  MapPin,
  LayoutList,
  AlertCircle,
  TrendingUp,
  CalendarRange,
  X,
  HelpCircle
} from "lucide-react";

interface AdminSidebarProps {
  isOpen: boolean;
  setIsOpen: (val: boolean) => void;
}

export function AdminSidebar({ isOpen, setIsOpen }: AdminSidebarProps) {
  const pathname = usePathname();

  const MENU_ITEMS = [
    { id: 'dashboard', href: '/admin/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { id: 'bookings', href: '/admin/bookings', label: 'Đặt sân', icon: CalendarRange },
    { id: 'customers', href: '/admin/customers', label: 'Khách hàng', icon: Users },
    { id: 'owners', href: '/admin/owners', label: 'Chủ sân', icon: Building2 },
    { id: 'owner-approvals', href: '/admin/owner-approvals', label: 'Duyệt chủ sân', icon: ClipboardCheck },
    { id: 'complexes', href: '/admin/complex-approvals', label: 'Tổ hợp', icon: MapPin },
    { id: 'categories', href: '/admin/sport-categories', label: 'Danh mục', icon: LayoutList },
    { id: 'complaints', href: '/admin/complaints', label: 'Khiếu nại', icon: AlertCircle },
    { id: 'refund-exceptions', href: '/admin/refund-exceptions', label: 'Ngoại lệ hoàn tiền', icon: HelpCircle },
  ];

  return (
    <>
      <aside className={`
        fixed inset-y-0 left-0 z-50 w-64 bg-white border-r border-slate-200 transform transition-transform duration-300 ease-in-out
        ${isOpen ? 'translate-x-0' : '-translate-x-full'} md:relative md:translate-x-0 flex flex-col shrink-0
      `}>
        <div className="h-16 flex items-center justify-between px-6 border-b border-slate-100 shrink-0">
          <div className="flex items-center gap-2 text-emerald-600">
            <div className="p-1.5 bg-emerald-600 rounded-lg">
              <TrendingUp className="h-5 w-5 text-white" />
            </div>
            <span className="text-xl font-bold tracking-tight">SportsBook</span>
          </div>
          <button className="md:hidden text-slate-500 hover:text-slate-900" onClick={() => setIsOpen(false)}>
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto py-6 space-y-2">
          <div className="text-[13px] font-bold text-slate-400 uppercase tracking-wider mb-4 px-6">Quản trị</div>
          {MENU_ITEMS.map((item) => {
            const Icon = item.icon;
            const isActive = item.href === '/admin/dashboard'
              ? pathname === item.href
              : pathname.startsWith(item.href);

            return (
              <Link key={item.id} href={item.href} className={`
                w-[calc(100%-1.25rem)] flex items-center gap-4 pl-6 pr-4 py-3 text-[15px] font-medium transition-all duration-200 relative rounded-r-full
                ${isActive
                  ? 'bg-emerald-50 text-emerald-700'
                  : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'}
              `} onClick={() => setIsOpen(false)}>
                {isActive && (
                  <div className="absolute left-0 top-1/2 -translate-y-1/2 h-7 w-[5px] bg-emerald-600 rounded-r-full" />
                )}
                <Icon className={`h-5 w-5 ${isActive ? 'text-emerald-600' : 'text-slate-400'}`} strokeWidth={1.75} />
                <span className={isActive ? "font-semibold" : ""}>{item.label}</span>
              </Link>
            );
          })}
        </div>

        <div className="p-4 border-t border-slate-100 shrink-0">
          <div className="bg-slate-50 rounded-xl p-4 text-center">
            <p className="text-xs text-slate-500 mb-2">Phiên bản 2.4.0</p>
            <p className="text-xs font-medium text-slate-700">© 2026 SportsBook</p>
          </div>
        </div>
      </aside>

      {/* Mobile Sidebar Overlay */}
      {isOpen && (
        <div
          className="fixed inset-0 bg-slate-900/50 z-40 md:hidden backdrop-blur-sm"
          onClick={() => setIsOpen(false)}
        ></div>
      )}
    </>
  );
}
