'use client'

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { ArrowLeft, MapPin, Clock, Calendar, Star, MessageSquare, X, AlertCircle, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { get, post } from '@/lib/api';
import { toast } from 'sonner';
import Image from "next/image";
import { useParams } from 'next/navigation';

interface BookingDetail {
  bookingId: number;
  bookingCode: string;
  venueName: string;
  venueImage: string;
  sportType: string;
  location: string;
  date: string;
  startTime: string;
  endTime: string;
  duration: number;
  pricePerHour: number;
  totalPrice: number;
  status: string;
  paymentMethod: string;
  paidAt: string | null;
  note: string | null;
  accessories: Array<{ name: string; price: number; quantity: number }>;
}

const statusConfig: Record<string, { label: string; className: string }> = {
  confirmed: { label: 'Đã xác nhận', className: 'bg-green-100 text-green-700' },
  pending:   { label: 'Chờ xác nhận', className: 'bg-yellow-100 text-yellow-700' },
  cancelled: { label: 'Đã huỷ', className: 'bg-red-100 text-red-700' },
  completed: { label: 'Hoàn thành', className: 'bg-blue-100 text-blue-700' },
};

function BookingDetailPage() {
  const params = useParams();
  const [booking, setBooking] = useState<BookingDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const [complaintOpen, setComplaintOpen] = useState(false);
  const [complaintText, setComplaintText] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const fetchBookingDetail = async () => {
      if (!params?.id) return;
      try {
        setLoading(true);
        // Replace with real endpoint when available or use a fallback mock if needed
        // For now, I will simulate fetching from a real endpoint
        const data = await get<any>(`/bookings/${params.id}`);
        
        // Transform backend response to UI structure
        const detail: BookingDetail = {
          bookingId: data.bookingId,
          bookingCode: `BK${String(data.bookingId).padStart(6, '0')}`,
          venueName: data.stadium?.stadiumName || 'Sân chưa biết',
          venueImage: data.stadium?.imageUrl || 'https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=800',
          sportType: data.stadium?.sportType || 'Khác',
          location: data.stadium?.address || 'Chưa có địa chỉ',
          date: new Date(data.bookingDate).toLocaleDateString('vi-VN'),
          startTime: data.slot?.startTime ? data.slot.startTime.substring(0, 5) : '00:00',
          endTime: data.slot?.endTime ? data.slot.endTime.substring(0, 5) : '00:00',
          duration: 1, // Calculate if possible
          pricePerHour: data.totalPrice, // Simplified
          totalPrice: data.totalPrice,
          status: data.bookingStatus.toLowerCase(),
          paymentMethod: data.paymentStatus === 'PAID' ? 'Đã thanh toán' : 'Chưa thanh toán',
          paidAt: data.bookingDate,
          note: data.note,
          accessories: []
        };
        setBooking(detail);
      } catch (err: any) {
        setError(err.message || 'Không thể tải chi tiết đặt sân');
      } finally {
        setLoading(false);
      }
    };

    fetchBookingDetail();
  }, [params?.id]);

  const handleSubmitComplaint = async () => {
    if (!complaintText.trim() || !booking) return;
    try {
      setSubmitting(true);
      await post(`/complaints`, {
        bookingId: booking.bookingId,
        subject: "Khiếu nại từ đơn đặt sân #" + booking.bookingId,
        description: complaintText.trim() 
      });
      toast.success("Đã gửi khiếu nại thành công! Chủ sân sẽ sớm phản hồi.");
      setComplaintOpen(false);
      setComplaintText("");
    } catch (err: any) {
      toast.error(err.message || "Có lỗi xảy ra khi gửi khiếu nại.");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (error || !booking) {
    return (
      <div className="flex h-screen flex-col items-center justify-center gap-4">
        <AlertCircle className="h-12 w-12 text-destructive" />
        <p className="text-lg font-medium">{error || 'Không tìm thấy đơn đặt sân'}</p>
        <Link href="/bookings">
          <Button variant="outline">Quay lại danh sách</Button>
        </Link>
      </div>
    );
  }

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
          <CardContent className="p-0 overflow-hidden relative h-40">
            <Image
              src={booking.venueImage}
              alt={booking.venueName}
              fill
              className="object-cover"
              unoptimized
            />
          </CardContent>
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
                {booking.startTime} – {booking.endTime}
              </span>
            </div>
            <Separator />
            <div className="flex items-center justify-between text-sm">
              <span className="text-muted-foreground">Tổng giá tiền</span>
              <span>{booking.totalPrice.toLocaleString('vi-VN')}đ</span>
            </div>
            {booking.note && (
              <div className="mt-2 text-sm italic text-muted-foreground">
                Ghi chú: {booking.note}
              </div>
            )}
            <Separator />
            <div className="flex items-center justify-between font-semibold">
              <span>Tổng cộng</span>
              <span className="text-primary text-lg">{booking.totalPrice.toLocaleString('vi-VN')}đ</span>
            </div>
          </CardContent>
        </Card>

        {/* Actions */}
        <div className="flex flex-col gap-3">
          {(booking.status === 'confirmed' || booking.status === 'pending') && (
            <div className="flex flex-col sm:flex-row gap-3">
              <Link href="/chat" className="flex-1">
                <Button variant="outline" className="w-full">
                  <MessageSquare className="h-4 w-4 mr-2" />
                  Liên hệ chủ sân
                </Button>
              </Link>
              <Link href={`/booking/${booking.bookingId}/cancel`} className="flex-1">
                <Button variant="destructive" className="w-full">
                  <X className="h-4 w-4 mr-2" />
                  Huỷ lịch
                </Button>
              </Link>
            </div>
          )}
          {booking.status === 'completed' && (
            <div className="flex flex-col sm:flex-row gap-3">
              <Link href={`/booking/${booking.bookingId}/review`} className="flex-1">
                <Button className="w-full bg-emerald-600 hover:bg-emerald-700">
                  <Star className="h-4 w-4 mr-2" />
                  Đánh giá
                </Button>
              </Link>
              <Button variant="outline" className="flex-1 text-red-600 border-red-200 hover:bg-red-50 hover:text-red-700" onClick={() => setComplaintOpen(true)}>
                <AlertCircle className="h-4 w-4 mr-2" />
                Gửi khiếu nại
              </Button>
            </div>
          )}
        </div>
      </div>

      <Dialog open={complaintOpen} onOpenChange={setComplaintOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Gửi khiếu nại</DialogTitle>
            <DialogDescription>
              Vui lòng mô tả chi tiết vấn đề bạn gặp phải với sân hoặc dịch vụ. Chủ sân sẽ nhận được khiếu nại và giải quyết cho bạn.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <Textarea
              placeholder="Nhập nội dung khiếu nại của bạn ở đây..."
              value={complaintText}
              onChange={(e) => setComplaintText(e.target.value)}
              className="min-h-[120px]"
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setComplaintOpen(false)}>Hủy</Button>
            <Button onClick={handleSubmitComplaint} disabled={!complaintText.trim() || submitting} className="bg-red-600 hover:bg-red-700">
              {submitting ? "Đang gửi..." : "Gửi khiếu nại"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default BookingDetailPage;
