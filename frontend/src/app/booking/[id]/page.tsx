'use client'

import Link from 'next/link';
import { ArrowLeft, MapPin, Clock, Calendar, Star, MessageSquare, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';

const booking = {
  id: 1,
  venueName: 'Sân bóng Thành Công',
  venueImage: 'https://images.unsplash.com/photo-1705593813682-033ee2991df6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080',
  sportType: 'Bóng đá',
  location: 'Quận 1, TP.HCM',
  date: '2026-06-15',
  startTime: '08:00',
  endTime: '10:00',
  duration: 2,
  pricePerHour: 500000,
  totalPrice: 1000000,
  status: 'confirmed',
  bookingCode: 'BK-2026-001234',
  paymentMethod: 'Ví điện tử MoMo',
  paidAt: '2026-06-01T10:30:00',
  accessories: [
    { name: 'Thuê bóng', price: 50000, quantity: 2 },
  ],
};

const statusConfig: Record<string, { label: string; className: string }> = {
  confirmed: { label: 'Đã xác nhận', className: 'bg-green-100 text-green-700' },
  pending:   { label: 'Chờ xác nhận', className: 'bg-yellow-100 text-yellow-700' },
  cancelled: { label: 'Đã huỷ', className: 'bg-red-100 text-red-700' },
  completed: { label: 'Hoàn thành', className: 'bg-blue-100 text-blue-700' },
};

function BookingDetailPage() {
  const status = statusConfig[booking.status] ?? statusConfig['pending'];

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="border-b bg-card sticky top-0 z-50">
        <div className="container mx-auto px-4 py-4 flex items-center gap-4">
          <Link href="/bookings">
            <Button variant="ghost" size="sm">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Lịch sử đặt sân
            </Button>
          </Link>
          <h1 className="text-lg font-semibold">Chi tiết đặt sân</h1>
        </div>
      </header>

      <div className="container mx-auto px-4 py-6 max-w-2xl space-y-4">
        {/* Status Banner */}
        <Card>
          <CardContent className="p-4 flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground mb-1">Mã đặt sân</p>
              <p className="font-mono font-semibold">{booking.bookingCode}</p>
            </div>
            <Badge className={status.className}>{status.label}</Badge>
          </CardContent>
        </Card>

        {/* Venue Info */}
        <Card>
          <CardContent className="p-0 overflow-hidden">
            <img
              src={booking.venueImage}
              alt={booking.venueName}
              className="w-full h-40 object-cover"
            />
            <div className="p-4">
              <div className="flex items-start justify-between mb-2">
                <h2 className="text-lg font-semibold">{booking.venueName}</h2>
                <Badge variant="secondary">{booking.sportType}</Badge>
              </div>
              <div className="flex items-center text-sm text-muted-foreground">
                <MapPin className="h-4 w-4 mr-1" />
                {booking.location}
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Booking Details */}
        <Card>
          <CardHeader className="pb-2">
            <h3 className="font-semibold">Thông tin đặt sân</h3>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center text-sm text-muted-foreground">
                <Calendar className="h-4 w-4 mr-2" />
                Ngày chơi
              </div>
              <span className="text-sm font-medium">{booking.date}</span>
            </div>
            <div className="flex items-center justify-between">
              <div className="flex items-center text-sm text-muted-foreground">
                <Clock className="h-4 w-4 mr-2" />
                Giờ chơi
              </div>
              <span className="text-sm font-medium">
                {booking.startTime} – {booking.endTime} ({booking.duration}h)
              </span>
            </div>
            <Separator />
            <div className="flex items-center justify-between text-sm">
              <span className="text-muted-foreground">Giá sân ({booking.duration}h)</span>
              <span>{(booking.pricePerHour * booking.duration).toLocaleString('vi-VN')}đ</span>
            </div>
            {booking.accessories.map((acc) => (
              <div key={acc.name} className="flex items-center justify-between text-sm">
                <span className="text-muted-foreground">{acc.name} x{acc.quantity}</span>
                <span>{(acc.price * acc.quantity).toLocaleString('vi-VN')}đ</span>
              </div>
            ))}
            <Separator />
            <div className="flex items-center justify-between font-semibold">
              <span>Tổng cộng</span>
              <span className="text-primary text-lg">{booking.totalPrice.toLocaleString('vi-VN')}đ</span>
            </div>
          </CardContent>
        </Card>

        {/* Payment Info */}
        <Card>
          <CardHeader className="pb-2">
            <h3 className="font-semibold">Thông tin thanh toán</h3>
          </CardHeader>
          <CardContent className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Phương thức</span>
              <span>{booking.paymentMethod}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Thanh toán lúc</span>
              <span>{new Date(booking.paidAt).toLocaleString('vi-VN')}</span>
            </div>
          </CardContent>
        </Card>

        {/* Actions */}
        {booking.status === 'confirmed' && (
          <div className="flex flex-col sm:flex-row gap-3">
            <Link href={`/booking/${booking.id}/review`} className="flex-1">
              <Button variant="outline" className="w-full">
                <Star className="h-4 w-4 mr-2" />
                Viết đánh giá
              </Button>
            </Link>
            <Link href="/chat" className="flex-1">
              <Button variant="outline" className="w-full">
                <MessageSquare className="h-4 w-4 mr-2" />
                Liên hệ chủ sân
              </Button>
            </Link>
            <Link href={`/booking/${booking.id}/cancel`} className="flex-1">
              <Button variant="destructive" className="w-full">
                <X className="h-4 w-4 mr-2" />
                Huỷ đặt sân
              </Button>
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}

export default BookingDetailPage;
