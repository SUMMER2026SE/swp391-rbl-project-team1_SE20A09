"use client";

import { useSession, signOut } from "next-auth/react";
import { Menu, LogOut, User as UserIcon, Settings, BarChart2 } from "lucide-react";
import { Button } from "../ui/button";
import Link from "next/link";
import { Avatar, AvatarFallback, AvatarImage } from "../ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "../ui/dropdown-menu";

export function Header() {
  const { data: session, status } = useSession();
  const user = session?.user;

  const handleLogout = () => {
    signOut({ callbackUrl: "/" });
  };

  // Get initials for avatar fallback
  const getInitials = () => {
    if (!user) return "U";
    const first = user.firstName ? user.firstName.charAt(0) : "";
    const last = user.lastName ? user.lastName.charAt(0) : "";
    return (first + last).toUpperCase() || user.email.charAt(0).toUpperCase();
  };

  return (
    <header className="border-b bg-card sticky top-0 z-50">
      <div className="container mx-auto px-4 py-4">
        <div className="flex items-center justify-between">
          <Link href="/" className="flex items-center space-x-2">
            <div className="w-10 h-10 bg-primary rounded-lg flex items-center justify-center text-primary-foreground font-bold">
              SH
            </div>
            <span className="text-xl font-bold text-primary">SportHub</span>
          </Link>

          <nav className="hidden md:flex items-center space-x-6">
            <Link href="/search" className="text-sm hover:text-primary transition-colors">
              Tìm sân
            </Link>
            <Link href="/community" className="text-sm hover:text-primary transition-colors">
              Cộng đồng
            </Link>

            {status === "loading" ? (
              <div className="w-8 h-8 rounded-full bg-muted animate-pulse" />
            ) : user ? (
              <>
                <Button variant="outline" size="sm" onClick={handleLogout}>
                  <LogOut className="mr-2 h-4 w-4" />
                  Đăng xuất
                </Button>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" className="relative h-10 w-10 rounded-full p-0">
                    <Avatar className="h-10 w-10 border border-primary/20 hover:border-primary/50 transition-colors">
                      <AvatarImage src={user.avatarUrl} alt={user.email} />
                      <AvatarFallback className="bg-primary/10 text-primary font-bold">
                        {getInitials()}
                      </AvatarFallback>
                    </Avatar>
                    </Button>
                  </DropdownMenuTrigger>
                <DropdownMenuContent className="w-56" align="end" forceMount>
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
                  
                  {/* Role-specific dashboard links */}
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
                  <DropdownMenuItem onClick={handleLogout} className="text-red-600 focus:text-red-600 cursor-pointer">
                    <LogOut className="mr-2 h-4 w-4" />
                    <span>Đăng xuất</span>
                  </DropdownMenuItem>
                </DropdownMenuContent>
                </DropdownMenu>
              </>
            ) : (
              <div className="flex items-center space-x-3">
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
          </nav>

          <button className="md:hidden">
            <Menu className="h-6 w-6" />
          </button>
        </div>
      </div>
    </header>
  );
}
