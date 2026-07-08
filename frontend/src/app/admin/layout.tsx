"use client";

import { useEffect, useState, createContext, useContext } from "react";
import { useSession } from "next-auth/react";
import { useRouter, usePathname } from "next/navigation";
import { AdminSidebar } from "@/components/layout/AdminSidebar";
import { UserAccountMenu } from "@/components/layout/Header";
import { Loader2, Menu, Calendar as CalendarIcon, ChevronDown } from "lucide-react";
import { AdminNotificationBell } from "@/components/notifications/AdminNotificationBell";
import { format } from "date-fns";
import { vi } from "date-fns/locale";

// ── Date Filter Context ──────────────────────────────────────────────────────
export interface DateRange {
  startDate: string; // yyyy-MM-dd
  endDate: string;   // yyyy-MM-dd
}

interface DateFilterContextValue {
  dateRange: DateRange | null;
  isFilterOpen: boolean;
  setIsFilterOpen: (v: boolean) => void;
  applyDateRange: (range: DateRange) => void;
}

export const DateFilterContext = createContext<DateFilterContextValue>({
  dateRange: null,
  isFilterOpen: false,
  setIsFilterOpen: () => {},
  applyDateRange: () => {},
});

export function useDateFilter() {
  return useContext(DateFilterContext);
}

// ── Helpers ──────────────────────────────────────────────────────────────────
function formatLabel(range: DateRange): string {
  const [sy, sm, sd] = range.startDate.split("-");
  const [ey, em, ed] = range.endDate.split("-");
  return `${sd}/${sm}/${sy} – ${ed}/${em}/${ey}`;
}

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { data: session, status } = useSession();
  const router = useRouter();
  const pathname = usePathname();
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  // Date filter state
  const [dateRange, setDateRange] = useState<DateRange | null>(null);
  const [isFilterOpen, setIsFilterOpen] = useState(false);

  const applyDateRange = (range: DateRange) => {
    setDateRange(range);
    setIsFilterOpen(false);
  };

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
    if (path === "/admin/dashboard") return { title: "Dashboard", subtitle: "Tổng quan hệ thống SportsBook" };
    if (path.startsWith("/admin/customers")) return { title: "Khách hàng", subtitle: "Quản lý thông tin khách hàng" };
    if (path.startsWith("/admin/users")) return { title: "Người dùng", subtitle: "Quản lý tài khoản người dùng" };
    if (path.startsWith("/admin/owner-approvals")) return { title: "Duyệt chủ sân", subtitle: "Phê duyệt tài khoản chủ sân" };
    if (path.startsWith("/admin/complex-approvals")) return { title: "Duyệt Tổ hợp", subtitle: "Phê duyệt Tổ hợp sân" };
    if (path.startsWith("/admin/sport-categories")) return { title: "Danh mục", subtitle: "Quản lý danh mục môn thể thao" };
    if (path.startsWith("/admin/complaints")) return { title: "Khiếu nại", subtitle: "Xử lý khiếu nại hệ thống" };
    return { title: "Quản trị", subtitle: "Hệ thống SportsBook" };
  };

  const { title, subtitle } = getPageTitle(pathname);

  /** Tên hiển thị của admin — fallback "Admin SportsBook" nếu chưa có session */
  const adminDisplayName = session?.user
    ? `${session.user.lastName ?? ""} ${session.user.firstName ?? ""}`.trim()
    : "Admin SportsBook";

  const isDashboardPage = pathname === "/admin/dashboard";
  const hasActiveFilter = !!dateRange;

  return (
    <DateFilterContext.Provider value={{ dateRange, isFilterOpen, setIsFilterOpen, applyDateRange }}>
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
              {/* Date filter button — only shown on dashboard page */}
              {isDashboardPage && (
                <button
                  type="button"
                  onClick={() => setIsFilterOpen((v) => !v)}
                  className={`hidden md:flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm transition-colors border
                    ${hasActiveFilter
                      ? "bg-emerald-50 border-emerald-400 text-emerald-700 font-medium"
                      : "bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100"
                    }`}
                  aria-expanded={isFilterOpen}
                  aria-label="Lọc theo ngày"
                >
                  <CalendarIcon className={`h-4 w-4 ${hasActiveFilter ? "text-emerald-500" : "text-slate-400"}`} />
                  <span>
                    {hasActiveFilter
                      ? formatLabel(dateRange!)
                      : format(new Date(), "EEEE, dd MMM yyyy", { locale: vi })}
                  </span>
                  <ChevronDown className={`h-3.5 w-3.5 transition-transform ${isFilterOpen ? "rotate-180" : ""} ${hasActiveFilter ? "text-emerald-500" : "text-slate-400"}`} />
                </button>
              )}

              {/* Non-dashboard pages just show static date */}
              {!isDashboardPage && (
                <div className="hidden md:flex items-center gap-2 bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5 text-sm text-slate-600">
                  <CalendarIcon className="h-4 w-4 text-slate-400" />
                  <span className="capitalize">{format(new Date(), "EEEE, dd MMM yyyy", { locale: vi })}</span>
                </div>
              )}

              <AdminNotificationBell />

              <div className="h-8 w-px bg-slate-200 hidden md:block"></div>

              <div className="flex items-center gap-3">
                {session?.user ? (
                  <UserAccountMenu user={session.user} />
                ) : (
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
    </DateFilterContext.Provider>
  );
}
