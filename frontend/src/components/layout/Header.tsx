import { Menu } from "lucide-react";
import { Button } from "../ui/button";
import Link from "next/link";

export function Header() {
  return (
    <header className="border-b bg-card sticky top-0 z-50">
      <div className="container mx-auto px-4 py-4">
        <div className="flex items-center justify-between">
          <Link href="/" className="flex items-center space-x-2">
            <div className="w-10 h-10 bg-primary rounded-lg flex items-center justify-center text-primary-foreground">
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
            <Link href="/login">
              <Button variant="outline" size="sm">
                Đăng nhập
              </Button>
            </Link>
            <Link href="/register">
              <Button size="sm">Đăng ký</Button>
            </Link>
          </nav>

          <button className="md:hidden">
            <Menu className="h-6 w-6" />
          </button>
        </div>
      </div>
    </header>
  );
}
