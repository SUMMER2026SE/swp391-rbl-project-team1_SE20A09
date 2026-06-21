'use client'

import { Header } from '@/components/layout/Header'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { Button } from '@/components/ui/button'
import {
  Home,
  Users,
  Building,
  MapPin,
  UserCog,
  ShieldAlert,
  Settings,
} from 'lucide-react'

const menuItems = [
  { href: '/admin/dashboard', label: 'Dashboard', icon: Home },
  { href: '/admin/users', label: 'Người dùng', icon: Users },
  { href: '/admin/owner-approvals', label: 'Chủ sân', icon: Building },
  { href: '/admin/stadium-approvals', label: 'Sân bóng', icon: MapPin },
  { href: '/admin/sport-categories', label: 'Danh mục', icon: UserCog },
  { href: '/admin/complaints', label: 'Khiếu nại', icon: ShieldAlert },
  { href: '/admin/settings', label: 'Cài đặt', icon: Settings },
]

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const pathname = usePathname()

  return (
    <div className="min-h-screen bg-background flex flex-col">
      <Header />
      <div className="flex flex-1">
        <aside className="w-64 min-h-[calc(100vh-64px)] bg-sidebar border-r p-4 hidden md:flex flex-col justify-between shrink-0">
          <div>
            <h2 className="mb-6 px-3 text-xs font-bold tracking-widest text-muted-foreground uppercase">
              Quản trị hệ thống
            </h2>
            <nav className="space-y-1">
              {menuItems.map((item) => {
                const Icon = item.icon
                const isActive = pathname.startsWith(item.href)
                return (
                  <Link key={item.href} href={item.href} className="block w-full">
                    <Button
                      variant={isActive ? 'default' : 'ghost'}
                      className="w-full justify-start"
                      size="sm"
                    >
                      <Icon className="mr-3 h-4 w-4" />
                      {item.label}
                    </Button>
                  </Link>
                )
              })}
            </nav>
          </div>
          <div className="p-3 bg-muted/40 rounded-lg border">
            <p className="text-xs text-muted-foreground">Đang hoạt động</p>
            <p className="text-sm font-semibold text-primary">Quản trị viên Hệ thống</p>
          </div>
        </aside>

        <div className="flex-1 overflow-x-hidden">
          {children}
        </div>
      </div>
    </div>
  )
}
