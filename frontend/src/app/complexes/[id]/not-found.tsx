'use client'

import Link from 'next/link'
import { Button } from '@/components/ui/button'
import { IconBuildingStadium } from '@tabler/icons-react'
import { Header } from '@/components/layout/Header'

export default function NotFound() {
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Header />
      <div className="flex-1 flex items-center justify-center p-4">
        <div className="text-center max-w-sm">
          <div className="w-20 h-20 bg-emerald-50 rounded-full flex items-center justify-center mx-auto mb-4 border border-emerald-100 shadow-sm">
            <IconBuildingStadium className="w-10 h-10 text-emerald-600" />
          </div>
          <h2 className="text-2xl font-bold text-gray-900 mb-2">
            Không tìm thấy cơ sở sân
          </h2>
          <p className="text-sm text-gray-500 mb-6 leading-relaxed">
            Cơ sở này không tồn tại, đã bị xóa hoặc đường dẫn bị sai. Vui lòng thử tìm kiếm lại.
          </p>
          <Button asChild className="rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white px-6">
            <Link href="/search">Quay lại tìm kiếm</Link>
          </Button>
        </div>
      </div>
    </div>
  )
}
