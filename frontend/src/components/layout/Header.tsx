"use client";

import { useSession, signOut } from "next-auth/react";
import type { Session } from "next-auth";
import { Menu, LogOut, User as UserIcon, Settings, BarChart2 } from "lucide-react";
import { Button } from "../ui/button";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { NotificationBell } from "../notifications/NotificationBell";
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

function getInitials(user: NonNullable<Session["user"]>) {
  const first = user.firstName ? user.firstName.charAt(0) : "";
  const last = user.lastName ? user.lastName.charAt(0) : "";
  return (first + last).toUpperCase() || user.email.charAt(0).toUpperCase();
}

function UserAccountMenu({ user }: { user: NonNullable<Session["user"]> }) {
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
            <Link href="/admin" className="cursor-pointer">
              <Settings className="mr-2 h-4 w-4" />
              <span>Trang Admin</span>
            </Link>
          </DropdownMenuItem>
        )}
        {user.roleName === "Owner" && (
          <DropdownMenuItem asChild>
            <Link href="/owner" className="cursor-pointer">
              <BarChart2 className="mr-2 h-4 w-4" />
              <span>Trang Chủ Sân</span>
            </Link>
          </DropdownMenuItem>
        )}

        <DropdownMenuItem asChild>
          <Link href="/profile" className="cursor-pointer">
            <UserIcon className="mr-2 h-4 w-4" />
            <span>Hồ sơ cá nhân</span>
          </Link>
        </DropdownMenuItem>

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

function MobileNavSheet({ user }: { user?: Session["user"] }) {
  const handleLogout = () => {
    signOut({ callbackUrl: "/" });
  };

  const navLinkClass =
    "flex items-center rounded-md px-3 py-2.5 text-base font-medium hover:bg-accent hover:text-accent-foreground transition-colors";

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
            <Link href="/search" className={navLinkClass}>
              Tìm sân
            </Link>
          </SheetClose>
          <SheetClose asChild>
            <Link href="/community" className={navLinkClass}>
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
                  <Link href="/admin" className={navLinkClass}>
                    <Settings className="mr-2 h-4 w-4" />
                    Trang Admin
                  </Link>
                </SheetClose>
              )}
              {user.roleName === "Owner" && (
                <SheetClose asChild>
                  <Link href="/owner" className={navLinkClass}>
                    <BarChart2 className="mr-2 h-4 w-4" />
                    Trang Chủ Sân
                  </Link>
                </SheetClose>
              )}
              <SheetClose asChild>
                <Link href="/profile" className={navLinkClass}>
                  <UserIcon className="mr-2 h-4 w-4" />
                  Hồ sơ cá nhân
                </Link>
              </SheetClose>
              <button
                type="button"
                onClick={handleLogout}
                className={`${navLinkClass} w-full text-left text-red-600 hover:text-red-600`}
              >
                <LogOut className="mr-2 h-4 w-4" />
                Đăng xuất
              </button>
            </>
          ) : (
            <>
              <div className="my-3 border-t" />
              <SheetClose asChild>
                <Link href="/login" className={navLinkClass}>
                  Đăng nhập
                </Link>
              </SheetClose>
              <SheetClose asChild>
                <Link href="/register" className={navLinkClass}>
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

export function Header() {
  const { data: session, status } = useSession();
  const user = session?.user;
  const pathname = usePathname();
  const isOwnerArea = pathname.startsWith("/owner");

  return (
    <header className="border-b bg-card sticky top-0 z-50">
      <div className="container mx-auto px-4 py-4">
        <div className="flex items-center justify-between gap-4">
          <Link href="/" className="flex items-center space-x-2 shrink-0">
            <div className="w-10 h-10 bg-primary rounded-lg flex items-center justify-center text-primary-foreground font-bold">
              SH
            </div>
            <span className="text-xl font-bold text-primary">SportHub</span>
          </Link>

          <div className="flex items-center gap-2 md:gap-6">
            <nav className="hidden md:flex items-center space-x-6">
              <Link href="/search" className="text-sm hover:text-primary transition-colors">
                Tìm sân
              </Link>
              <Link href="/community" className="text-sm hover:text-primary transition-colors">
                Cộng đồng
              </Link>
            </nav>

            {status === "loading" ? (
              <div className="w-10 h-10 rounded-full bg-muted animate-pulse" />
            ) : user ? (
              <div className="flex items-center gap-3">
                {isOwnerArea && user.roleName === "Owner" && <NotificationBell />}
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

            <MobileNavSheet user={user} />
          </div>
        </div>
      </div>
    </header>
  );
}
