"use client";

import { useSession, signOut } from "next-auth/react";
import type { Session } from "next-auth";
import {
  Menu,
  LogOut,
  User as UserIcon,
  Settings,
  BarChart2,
  Clock,
  Home,
  BarChart3,
  Calendar,
  Wallet,
  Bell,
  AlertTriangle,
  ChevronRight,
  Crown,
} from "lucide-react";
import { Button } from "../ui/button";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";

import { OwnerNotificationBell } from "../notifications/OwnerNotificationBell";
import { CustomerNotificationBell } from "../notifications/CustomerNotificationBell";
import { ChatBadge } from "../chat/ChatBadge";
import { Avatar, AvatarFallback, AvatarImage } from "../ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "../ui/dropdown-menu";
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "../ui/sheet";

// ── Helpers ──────────────────────────────────────────────────────────────────

function getInitials(user: NonNullable<Session["user"]>) {
  const first = user.firstName ? user.firstName.charAt(0) : "";
  const last = user.lastName ? user.lastName.charAt(0) : "";
  return (first + last).toUpperCase() || user.email.charAt(0).toUpperCase();
}

// Owner navigation menu items (mirrors owner/layout.tsx sidebar)
const OWNER_MENU_ITEMS = [
  { href: "/owner/dashboard", label: "Dashboard", icon: Home },
  { href: "/owner/venues", label: "Sân của tôi", icon: BarChart3 },
  { href: "/owner/bookings", label: "Lịch đặt", icon: Calendar },
  { href: "/owner/revenue", label: "Doanh thu", icon: Wallet },
  { href: "/owner/complaints", label: "Khiếu nại", icon: AlertTriangle },
  { href: "/owner/notifications", label: "Thông báo", icon: Bell },
];

// ── UserAccountMenu (Avatar dropdown — unchanged) ─────────────────────────

export function UserAccountMenu({ user }: { user: NonNullable<Session["user"]> }) {
  const handleLogout = () => {
    signOut({ callbackUrl: "/" });
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          className="relative h-10 w-10 rounded-full p-0"
          aria-label="Mở menu tài khoản"
        >
          <Avatar className="h-10 w-10 border border-primary/20 hover:border-primary/50 transition-colors">
            <AvatarImage src={user.avatarUrl} alt={user.email} />
            <AvatarFallback className="bg-primary/10 text-primary font-bold">
              {getInitials(user)}
            </AvatarFallback>
          </Avatar>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-56" align="end">
        <DropdownMenuLabel className="font-normal">
          <div className="flex flex-col space-y-1">
            <p className="text-sm font-semibold leading-none">
              {user.firstName} {user.lastName}
            </p>
            <p className="text-xs leading-none text-muted-foreground mt-0.5">
              {user.email}
            </p>
            <span className="inline-block mt-1 text-[10px] font-bold px-1.5 py-0.5 rounded bg-primary/10 text-primary w-fit uppercase">
              {user.roleName}
            </span>
          </div>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />

        {user.roleName === "Admin" && (
          <DropdownMenuItem asChild>
            <Link href="/admin/dashboard" className="cursor-pointer">
              <Settings className="mr-2 h-4 w-4" />
              <span>Trang Admin</span>
            </Link>
          </DropdownMenuItem>
        )}
        {user.roleName === "Owner" && (
          <DropdownMenuItem asChild>
            <Link href="/owner/dashboard" className="cursor-pointer">
              <BarChart2 className="mr-2 h-4 w-4" />
              <span>Quản lý sân của tôi</span>
            </Link>
          </DropdownMenuItem>
        )}

        <DropdownMenuItem asChild>
          <Link href="/profile" className="cursor-pointer">
            <UserIcon className="mr-2 h-4 w-4" />
            <span>Hồ sơ cá nhân</span>
          </Link>
        </DropdownMenuItem>

        {user.roleName !== "Admin" && (
          <DropdownMenuItem asChild>
            <Link href="/profile?tab=bookings" className="cursor-pointer">
              <Clock className="mr-2 h-4 w-4" />
              <span>Lịch sử đặt sân</span>
            </Link>
          </DropdownMenuItem>
        )}

        <DropdownMenuSeparator />
        <DropdownMenuItem
          onClick={handleLogout}
          className="text-red-600 focus:text-red-600 cursor-pointer"
        >
          <LogOut className="mr-2 h-4 w-4" />
          <span>Đăng xuất</span>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

// ── OwnerNavSheet — full-featured Owner drawer (shown on ALL screen sizes) ──

function OwnerNavSheet({ user }: { user?: Session["user"] }) {
  const handleLogout = () => {
    signOut({ callbackUrl: "/" });
  };
  const pathname = usePathname();

  const isActive = (href: string) => pathname.startsWith(href);

  return (
    <Sheet>
      <SheetTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="md:hidden shrink-0 hover:bg-primary/10 transition-colors duration-200"
          aria-label="Mở menu quản lý"
        >
          <Menu className="h-5 w-5" />
        </Button>
      </SheetTrigger>

      <SheetContent side="left" className="w-[min(100vw-2rem,18rem)] p-0 flex flex-col">
        {/* ── Drawer Header ── */}
        <SheetHeader className="px-5 pt-6 pb-5 border-b bg-slate-50 dark:bg-slate-950/60">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 bg-primary rounded-lg flex items-center justify-center text-primary-foreground font-bold text-sm shadow-sm">
              SH
            </div>
            <div>
              <SheetTitle className="text-sm font-bold leading-none text-slate-900 dark:text-white">
                Quản lý sân của tôi
              </SheetTitle>
              <p className="text-[11px] text-muted-foreground mt-0.5">SportsBook</p>
            </div>
          </div>
        </SheetHeader>

        {/* ── Owner Nav Items ── */}
        <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-0.5">
          {OWNER_MENU_ITEMS.map((item) => {
            const Icon = item.icon;
            const active = isActive(item.href);
            return (
              <SheetClose asChild key={item.href}>
                <Link
                  href={item.href}
                  className={[
                    "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-150 group",
                    active
                      ? "bg-primary text-primary-foreground shadow-sm"
                      : "text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800/60 hover:text-slate-900 dark:hover:text-white",
                  ].join(" ")}
                >
                  <Icon
                    className={[
                      "h-4 w-4 shrink-0 transition-transform duration-150",
                      active ? "text-primary-foreground" : "text-slate-500 group-hover:text-slate-700 dark:group-hover:text-slate-200",
                    ].join(" ")}
                  />
                  <span className="flex-1">{item.label}</span>
                  {active && <ChevronRight className="h-3.5 w-3.5 opacity-70" />}
                </Link>
              </SheetClose>
            );
          })}
        </nav>

        {/* ── User Info + Logout Footer ── */}
        {user && (
          <div className="border-t bg-slate-50 dark:bg-slate-950/60 px-4 py-4 mt-auto">
            <div className="flex items-center gap-3 mb-3">
              <Avatar className="h-9 w-9 border border-primary/20">
                <AvatarImage src={user.avatarUrl} alt={user.email} />
                <AvatarFallback className="bg-primary/10 text-primary text-xs font-bold">
                  {user.firstName?.charAt(0)}{user.lastName?.charAt(0)}
                </AvatarFallback>
              </Avatar>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold text-slate-900 dark:text-white truncate">
                  {user.firstName} {user.lastName}
                </p>
                <p className="text-[11px] text-muted-foreground truncate">{user.email}</p>
              </div>
            </div>
            <button
              type="button"
              onClick={handleLogout}
              className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium text-red-600 hover:bg-red-50 dark:hover:bg-red-950/30 transition-colors duration-150"
            >
              <LogOut className="h-4 w-4 shrink-0" />
              Đăng xuất
            </button>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}

// ── MobileNavSheet — Customer-only mobile sheet (md:hidden) ──────────────

function MobileNavSheet({ user }: { user?: Session["user"] }) {
  const handleLogout = () => {
    signOut({ callbackUrl: "/" });
  };
  const pathname = usePathname();

  const getNavLinkClass = (href: string) => {
    const base = "flex items-center rounded-md px-3 py-2.5 text-base font-medium transition-colors";
    return pathname === href
      ? `${base} bg-primary/10 text-primary font-bold`
      : `${base} hover:bg-accent hover:text-accent-foreground text-slate-700`;
  };

  return (
    <Sheet>
      <SheetTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="md:hidden shrink-0"
          aria-label="Mở menu điều hướng"
        >
          <Menu className="h-6 w-6" />
        </Button>
      </SheetTrigger>
      <SheetContent side="right" className="w-[min(100vw-2rem,20rem)]">
        <SheetHeader>
          <SheetTitle>Menu</SheetTitle>
        </SheetHeader>
        <nav className="flex flex-col gap-1 px-2">
          <SheetClose asChild>
            <Link href="/search" className={getNavLinkClass("/search")}>
              Tìm sân
            </Link>
          </SheetClose>
          <SheetClose asChild>
            <Link href="/community" className={getNavLinkClass("/community")}>
              Cộng đồng
            </Link>
          </SheetClose>

          {user ? (
            <>
              <div className="my-3 border-t" />
              <div className="px-3 py-2">
                <p className="text-sm font-semibold">
                  {user.firstName} {user.lastName}
                </p>
                <p className="text-xs text-muted-foreground truncate">{user.email}</p>
              </div>
              {user.roleName === "Admin" && (
                <SheetClose asChild>
                  <Link href="/admin/dashboard" className={getNavLinkClass("/admin/dashboard")}>
                    <Settings className="mr-2 h-4 w-4" />
                    Trang Admin
                  </Link>
                </SheetClose>
              )}
              <SheetClose asChild>
                <Link href="/profile" className={getNavLinkClass("/profile")}>
                  <UserIcon className="mr-2 h-4 w-4" />
                  Hồ sơ cá nhân
                </Link>
              </SheetClose>
              <SheetClose asChild>
                <Link href="/profile?tab=bookings" className={getNavLinkClass("/profile?tab=bookings")}>
                  <Clock className="mr-2 h-4 w-4" />
                  Lịch sử đặt sân
                </Link>
              </SheetClose>
              <button
                type="button"
                onClick={handleLogout}
                className={`${getNavLinkClass("/logout")} w-full text-left text-red-600 hover:text-red-600`}
              >
                <LogOut className="mr-2 h-4 w-4" />
                Đăng xuất
              </button>
            </>
          ) : (
            <>
              <div className="my-3 border-t" />
              <SheetClose asChild>
                <Link href="/login" className={getNavLinkClass("/login")}>
                  Đăng nhập
                </Link>
              </SheetClose>
              <SheetClose asChild>
                <Link href="/register" className={getNavLinkClass("/register")}>
                  Đăng ký
                </Link>
              </SheetClose>
            </>
          )}
        </nav>
      </SheetContent>
    </Sheet>
  );
}

// ── Header (main export) ──────────────────────────────────────────────────

export function Header() {
  const { data: session, status } = useSession();
  const user = session?.user;
  const pathname = usePathname();
  const isOwnerArea = pathname.startsWith("/owner");
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <header
      className={[
        "border-b bg-card/95 backdrop-blur-sm sticky top-0 z-40 transition-shadow duration-300",
        scrolled ? "shadow-md" : "shadow-none",
      ].join(" ")}
    >
      <div className="container mx-auto px-4 py-3.5">
        <div className="flex items-center justify-between gap-4">

          {/* ── LEFT: Logo ── */}
          <div className="flex items-center gap-2">
            <Link href={isOwnerArea ? "/owner/dashboard" : "/"} className="flex items-center space-x-2 shrink-0 group">
              <div className="w-10 h-10 bg-primary rounded-lg flex items-center justify-center text-primary-foreground font-bold shadow-sm group-hover:shadow-primary/30 group-hover:scale-105 transition-all duration-200">
                SH
              </div>
              <span className="text-xl font-bold text-primary group-hover:text-primary/80 transition-colors duration-200">
                SportsBook
              </span>
            </Link>
          </div>

          {/* ── RIGHT: Nav + Auth actions ── */}
          <div className="flex items-center gap-6">
            {/* Customer nav links (hidden when Owner) */}
            {!isOwnerArea && (
              <nav className="hidden md:flex items-center space-x-6">
                <Link
                  href="/search"
                  className={`text-sm font-medium transition-colors duration-150 ${pathname === "/search"
                    ? "text-primary font-bold"
                    : "text-muted-foreground hover:text-primary"
                    }`}
                >
                  Tìm sân
                </Link>
                <Link
                  href="/community"
                  className={`text-sm font-medium transition-colors duration-150 ${pathname === "/community"
                    ? "text-primary font-bold"
                    : "text-muted-foreground hover:text-primary"
                    }`}
                >
                  Cộng đồng
                </Link>
              </nav>
            )}

            <div className="flex items-center gap-2.5">
              {status === "loading" ? (
                <div className="w-10 h-10 rounded-full bg-muted animate-pulse" />
              ) : user ? (
                <div className="flex items-center gap-3">
                  {/* Badge Role: Owner */}
                  {isOwnerArea && user.roleName === "Owner" && (
                    <div className="flex items-center gap-1.5 px-3 py-1 bg-orange-100 text-orange-600 rounded-full text-sm font-semibold">
                      <Crown className="w-3.5 h-3.5" />
                      <span>Role: Owner</span>
                    </div>
                  )}
                  {/* Chat Badge */}
                  <ChatBadge userId={(user as any)?.userId} />
                  {/* Notification Bell */}
                  {isOwnerArea && user.roleName === "Owner" && <OwnerNotificationBell />}
                  {!isOwnerArea && user.roleName === "Customer" && <CustomerNotificationBell />}
                  {/* User Avatar */}
                  <UserAccountMenu user={user} />
                </div>
              ) : (
                <div className="hidden md:flex items-center space-x-3">
                  <Link href="/login">
                    <Button variant="outline" size="sm">
                      Đăng nhập
                    </Button>
                  </Link>
                  <Link href="/register">
                    <Button size="sm">Đăng ký</Button>
                  </Link>
                </div>
              )}

              {/* Customer mobile sheet — only when NOT in Owner area */}
              {!isOwnerArea && <MobileNavSheet user={user} />}
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}
