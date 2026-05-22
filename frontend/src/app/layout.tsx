import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import './globals.css'

const inter = Inter({
  subsets: ['latin'],
  variable: '--font-inter',
  display: 'swap',
})

export const metadata: Metadata = {
  title: {
    default: '🏟️ Sport Venue — Đặt Sân Thể Thao Trực Tuyến',
    template: '%s | Sport Venue',
  },
  description:
    'Nền tảng đặt sân thể thao trực tuyến — tìm kiếm, đặt lịch, thanh toán và kết nối cộng đồng thể thao trong một ứng dụng duy nhất.',
  keywords: ['đặt sân', 'thể thao', 'sân bóng đá', 'cầu lông', 'tennis', 'sport venue'],
  authors: [{ name: 'Team 1 — SE20A09' }],
  robots: {
    index: true,
    follow: true,
  },
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="vi" suppressHydrationWarning>
      <body className={`${inter.variable} font-sans antialiased`}>
        {children}
      </body>
    </html>
  )
}
