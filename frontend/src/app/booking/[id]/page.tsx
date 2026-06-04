'use client'

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { 
  ArrowLeft, 
  MapPin, 
  Clock, 
  Calendar, 
  Star, 
  MessageSquare, 
  X, 
  ShieldAlert, 
  CheckCircle2, 
  Info, 
  AlertTriangle,
  Receipt,
  CreditCard,
  Sparkles
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { toast } from 'sonner';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Textarea } from '@/components/ui/textarea';
import { get, put, post } from '@/lib/api';

const statusConfig: Record<string, { label: string; className: string; icon: any }> = {
  confirmed: { label: 'Đã xác nhận', className: 'bg-emerald-500/10 text-emerald-500 border border-emerald-500/20', icon: CheckCircle2 },
  pending:   { label: 'Chờ xác nhận', className: 'bg-amber-500/10 text-amber-500 border border-amber-500/20', icon: Info },
  cancelled: { label: 'Đã hủy bỏ', className: 'bg-rose-500/10 text-rose-500 border border-rose-500/20', icon: X },
  completed: { label: 'Đã hoàn thành', className: 'bg-blue-500/10 text-blue-500 border border-blue-500/20', icon: Sparkles },
};

const ratingLabels = ['Rất tệ', 'Không hài lòng', 'Bình thường', 'Hài lòng', 'Tuyệt vời'];

const presetReviewTags = [
  'Sân đẹp & mướt',
  'Đủ ánh sáng đêm',
  'Thân thiện, nhiệt tình',
  'Đồ thuê chất lượng',
  'Giá cả hợp lý',
  'Chỗ để xe rộng rãi'
];

const presetCancelReasons = [
  'Thay đổi kế hoạch đột xuất',
  'Thời tiết không thuận lợi',
  'Đặt nhầm sân / nhầm giờ chơi',
  'Tìm được sân chơi khác phù hợp hơn',
  'Có lý do cá nhân riêng'
];

function BookingDetailPage() {
  const params = useParams();
  const id = params?.id as string;

  const [booking, setBooking] = useState<any>(null);
  const [isCancelDialogOpen, setIsCancelDialogOpen] = useState(false);
  const [isReviewDialogOpen, setIsReviewDialogOpen] = useState(false);
  const [rating, setRating] = useState(5);
  const [hoverRating, setHoverRating] = useState<number | null>(null);
  const [reviewComment, setReviewComment] = useState('');
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [cancelReason, setCancelReason] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    let active = true;
    async function fetchBooking() {
      try {
        const data = await get<any[] | { content?: any[] }>("/bookings/my");
        if (!active) return;
        
        const list = Array.isArray(data) ? data : data.content ?? [];
        const found = list.find((b: any) => String(b.id) === id);
        
        if (found) {
          // Add default duration for display if not present
          if (!found.duration) {
            found.duration = 2; // Assuming 2 hours for simplicity if backend doesn't provide
          }
          setBooking(found);
        } else {
          toast.error("Không tìm thấy đơn đặt sân này");
        }
      } catch (err) {
        if (active) {
          toast.error("Không thể tải thông tin đặt sân");
        }
      }
    }

    if (id) {
      fetchBooking();
    }
    
    return () => {
      active = false;
    };
  }, [id]);

  if (!booking) {
    return (
      <div className="min-h-screen bg-neutral-50/50 flex items-center justify-center dark:bg-zinc-950">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-500"></div>
      </div>
    );
  }

  const status = statusConfig[booking.status] ?? statusConfig['pending'];
  const StatusIcon = status.icon;

  const toggleTag = (tag: string) => {
    if (selectedTags.includes(tag)) {
      setSelectedTags(prev => prev.filter(t => t !== tag));
      setReviewComment(prev => prev.replace(new RegExp(`\\[${tag}\\]\\s*`, 'g'), ''));
    } else {
      setSelectedTags(prev => [...prev, tag]);
      setReviewComment(prev => prev ? `${prev} [${tag}]` : `[${tag}]`);
    }
  };

  const handleCancelBooking = async () => {
    if (!cancelReason.trim()) {
      toast.error('Vui lòng chọn hoặc nhập lý do hủy đặt sân');
      return;
    }
    setIsSubmitting(true);
    
    try {
      // Call the real API
      const updatedBooking = await put<any>(`/bookings/${booking.id}/cancel`, {});
      
      if (!updatedBooking.duration) {
        updatedBooking.duration = booking.duration;
      }
      setBooking(updatedBooking);

      setIsCancelDialogOpen(false);
      toast.success('Hủy đặt sân thành công! Tiền sẽ được hoàn lại ví của bạn theo chính sách.', {
        description: 'Đã hoàn tất thủ tục hủy sân.',
      });
    } catch (err: any) {
      toast.error(err.message || 'Có lỗi xảy ra khi hủy đơn đặt sân.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSendReview = async () => {
    setIsSubmitting(true);
    try {
      await post('/reviews', {
        bookingId: booking.id,
        rating: rating,
        comment: reviewComment || "Khách hàng không để lại bình luận"
      });
      
      const updatedBooking = { ...booking, hasReviewed: true };
      setBooking(updatedBooking);

      setIsReviewDialogOpen(false);
      toast.success('Đánh giá thành công!', {
        description: `Cảm ơn ý kiến quý giá của bạn dành cho ${booking.venueName || 'sân bóng'}. Chủ sân đã nhận được thông báo.`,
      });
      setReviewComment('');
      setSelectedTags([]);
      setRating(5);
    } catch (err: any) {
      toast.error(err.message || 'Có lỗi xảy ra khi gửi đánh giá.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-neutral-50/50 pb-16 dark:bg-zinc-950">
      {/* Header */}
      <header className="border-b bg-white/80 backdrop-blur-md sticky top-0 z-50 dark:bg-zinc-900/80 dark:border-zinc-800">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between max-w-3xl">
          <Link href="/bookings">
            <Button variant="ghost" size="sm" className="hover:bg-neutral-100 transition-colors">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Lịch sử đặt sân
            </Button>
          </Link>
          <h1 className="text-base font-semibold text-neutral-800 dark:text-neutral-200">Chi tiết đơn đặt</h1>
          <div className="w-24"></div> {/* Balance spacer */}
        </div>
      </header>

      <div className="container mx-auto px-4 py-8 max-w-2xl space-y-6">
        {/* Banner Trạng thái Premium */}
        <div className="bg-white dark:bg-zinc-900 border border-neutral-100 dark:border-zinc-800 rounded-2xl p-5 shadow-sm flex items-center justify-between transition-all hover:shadow-md">
          <div className="space-y-1">
            <span className="text-xs font-semibold uppercase tracking-wider text-neutral-400">Mã đặt sân</span>
            <p className="font-mono text-lg font-bold text-neutral-800 dark:text-neutral-100">{booking.bookingCode || booking.id}</p>
          </div>
          <div className="flex items-center gap-3">
            <span className={`px-4 py-1.5 rounded-full text-xs font-semibold flex items-center gap-1.5 ${status.className}`}>
              <StatusIcon className="h-3.5 w-3.5" />
              {status.label}
            </span>
          </div>
        </div>

        {/* Thông tin Sân & Ảnh bìa nghệ thuật */}
        <Card className="overflow-hidden border border-neutral-100 dark:border-zinc-800 shadow-sm rounded-2xl hover:shadow-md transition-all">
          <div className="relative h-56 w-full">
            <img
              src={booking.venueImage}
              alt={booking.venueName}
              className="w-full h-full object-cover animate-fade-in"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent"></div>
            <div className="absolute bottom-4 left-4 right-4 text-white">
              <div className="flex items-center justify-between">
                <Badge className="bg-emerald-500 hover:bg-emerald-600 text-white font-medium px-3 py-0.5 rounded-md text-xs mb-2 border-0">
                  {booking.sportType}
                </Badge>
              </div>
              <h2 className="text-xl font-bold tracking-tight">{booking.venueName}</h2>
              <p className="text-xs text-neutral-200/90 flex items-center mt-1">
                <MapPin className="h-3 w-3 mr-1 text-emerald-400" />
                {booking.location}
              </p>
            </div>
          </div>
        </Card>

        {/* Chi tiết Đặt sân */}
        <Card className="border border-neutral-100 dark:border-zinc-800 shadow-sm rounded-2xl">
          <CardHeader className="pb-3 flex flex-row items-center gap-2">
            <Calendar className="h-5 w-5 text-emerald-500" />
            <h3 className="font-semibold text-neutral-800 dark:text-neutral-200">Thời gian & chi tiết</h3>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="bg-neutral-50 dark:bg-zinc-900/50 p-3 rounded-xl border border-neutral-100/50 dark:border-zinc-800/50">
                <span className="text-xs text-neutral-400 block mb-0.5">Ngày chơi</span>
                <span className="text-sm font-semibold text-neutral-800 dark:text-neutral-100">{booking.date}</span>
              </div>
              <div className="bg-neutral-50 dark:bg-zinc-900/50 p-3 rounded-xl border border-neutral-100/50 dark:border-zinc-800/50">
                <span className="text-xs text-neutral-400 block mb-0.5">Khung giờ</span>
                <span className="text-sm font-semibold text-neutral-800 dark:text-neutral-100">
                  {booking.startTime} – {booking.endTime} ({booking.duration}h)
                </span>
              </div>
            </div>

            <Separator className="bg-neutral-100/60 dark:bg-zinc-800/60" />

            <div className="space-y-3">
              <div className="flex justify-between items-center text-sm">
                <span className="text-neutral-500">Giá thuê sân ({booking.duration}h)</span>
                <span className="font-medium text-neutral-800 dark:text-neutral-100">
                  {(booking.pricePerHour * booking.duration).toLocaleString('vi-VN')}đ
                </span>
              </div>

              {booking.accessories && booking.accessories.length > 0 && (
                <div className="space-y-2">
                  <span className="text-xs font-semibold text-neutral-400 uppercase tracking-wider block mt-1">Dịch vụ đi kèm</span>
                  {booking.accessories.map((acc: any) => (
                    <div key={acc.name} className="flex justify-between items-center text-sm pl-2 border-l-2 border-emerald-500/50">
                      <span className="text-neutral-500">{acc.name} <span className="text-neutral-400">x{acc.quantity}</span></span>
                      <span className="font-medium text-neutral-800 dark:text-neutral-100">
                        {(acc.price * acc.quantity).toLocaleString('vi-VN')}đ
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <Separator className="bg-neutral-100/60 dark:bg-zinc-800/60" />

            <div className="flex justify-between items-center bg-emerald-50/40 dark:bg-emerald-950/20 p-4 rounded-xl border border-emerald-100/30">
              <span className="font-semibold text-neutral-700 dark:text-neutral-300">Tổng thanh toán</span>
              <span className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">
                {booking.totalPrice.toLocaleString('vi-VN')}đ
              </span>
            </div>
          </CardContent>
        </Card>

        {/* Giao dịch thanh toán */}
        <Card className="border border-neutral-100 dark:border-zinc-800 shadow-sm rounded-2xl">
          <CardHeader className="pb-3 flex flex-row items-center gap-2">
            <CreditCard className="h-5 w-5 text-emerald-500" />
            <h3 className="font-semibold text-neutral-800 dark:text-neutral-200">Giao dịch</h3>
          </CardHeader>
          <CardContent className="space-y-3 text-sm">
            <div className="flex justify-between">
              <span className="text-neutral-500">Phương thức</span>
              <span className="font-medium text-neutral-800 dark:text-neutral-200">{booking.paymentMethod}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-neutral-500">Thời gian giao dịch</span>
              <span className="font-medium text-neutral-800 dark:text-neutral-200">
                {new Date(booking.paidAt).toLocaleString('vi-VN')}
              </span>
            </div>
          </CardContent>
        </Card>

        {/* Thanh hành động */}
        <div className="flex flex-col sm:flex-row gap-3 pt-2">
          {(booking.status === 'completed' || booking.status === 'confirmed') && !booking.hasReviewed && (
            <Button 
              variant="outline" 
              className="flex-1 rounded-xl h-11 border-neutral-200 hover:bg-neutral-50 dark:border-zinc-800 dark:hover:bg-zinc-900 transition-all font-semibold" 
              onClick={() => setIsReviewDialogOpen(true)}
            >
              <Star className="h-4 w-4 mr-2 text-yellow-500 fill-yellow-500" />
              Viết đánh giá
            </Button>
          )}
          
          <Link href="/chat" className="flex-1">
            <Button 
              variant="outline" 
              className="w-full rounded-xl h-11 border-neutral-200 hover:bg-neutral-50 dark:border-zinc-800 dark:hover:bg-zinc-900 transition-all font-semibold"
            >
              <MessageSquare className="h-4 w-4 mr-2 text-blue-500" />
              Liên hệ chủ sân
            </Button>
          </Link>

          {(booking.status === 'confirmed' || booking.status === 'pending') && (
            <Button 
              variant="destructive" 
              className="flex-1 rounded-xl h-11 transition-all font-semibold hover:bg-red-650" 
              onClick={() => setIsCancelDialogOpen(true)}
            >
              <X className="h-4 w-4 mr-2" />
              Hủy đặt sân
            </Button>
          )}
        </div>
      </div>

      {/* Dialog: Hủy đặt sân */}
      <Dialog open={isCancelDialogOpen} onOpenChange={setIsCancelDialogOpen}>
        <DialogContent className="sm:max-w-[460px] rounded-2xl p-6 border-0 shadow-2xl bg-white dark:bg-zinc-900">
          <DialogHeader className="space-y-2">
            <DialogTitle className="text-lg font-bold flex items-center gap-2 text-rose-600">
              <AlertTriangle className="h-5 w-5 animate-bounce" />
              Xác nhận hủy đặt sân
            </DialogTitle>
            <DialogDescription className="text-neutral-500 text-sm">
              Bạn đang yêu cầu hủy đơn đặt sân tại **{booking.venueName}**. Vui lòng kiểm tra kỹ chính sách hoàn tiền bên dưới.
            </DialogDescription>
          </DialogHeader>

          {/* Banner chính sách hủy */}
          <div className="bg-rose-50 dark:bg-rose-950/20 border border-rose-100 dark:border-rose-900/30 rounded-xl p-4 flex gap-3 text-xs text-rose-800 dark:text-rose-300">
            <ShieldAlert className="h-5 w-5 shrink-0 mt-0.5" />
            <div className="space-y-1">
              <p className="font-semibold">Chính sách hoàn trả:</p>
              <ul className="list-disc pl-4 space-y-0.5">
                <li>Hoàn 100% nếu hủy trước giờ chơi 24 tiếng.</li>
                <li>Hoàn 50% nếu hủy trước giờ chơi từ 12 - 24 tiếng.</li>
                <li>Không hoàn tiền nếu hủy sát giờ dưới 12 tiếng.</li>
              </ul>
            </div>
          </div>

          <div className="space-y-4 py-2">
            <div className="flex flex-col gap-2">
              <label className="text-xs font-semibold uppercase tracking-wider text-neutral-400">Chọn lý do hủy sân</label>
              <div className="grid grid-cols-1 gap-2">
                {presetCancelReasons.map((reason) => (
                  <button
                    key={reason}
                    type="button"
                    onClick={() => setCancelReason(reason)}
                    className={`text-left text-xs px-3.5 py-2.5 rounded-xl border transition-all ${
                      cancelReason === reason 
                        ? 'border-rose-500 bg-rose-500/5 text-rose-700 dark:text-rose-400 font-semibold' 
                        : 'border-neutral-100 hover:bg-neutral-50 dark:border-zinc-800 dark:hover:bg-zinc-900 text-neutral-600 dark:text-neutral-400'
                    }`}
                  >
                    {reason}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex flex-col gap-2">
              <label htmlFor="reason" className="text-xs font-semibold uppercase tracking-wider text-neutral-400">Chi tiết lý do (Không bắt buộc)</label>
              <Textarea
                id="reason"
                placeholder="Vui lòng cho biết thêm lý do nếu có..."
                value={cancelReason && !presetCancelReasons.includes(cancelReason) ? cancelReason : ''}
                onChange={(e) => setCancelReason(e.target.value)}
                rows={2}
                className="rounded-xl border-neutral-200 dark:border-zinc-800 focus-visible:ring-rose-500 resize-none text-xs"
              />
            </div>
          </div>

          <DialogFooter className="gap-2 sm:gap-0 mt-2">
            <Button variant="outline" className="rounded-xl" onClick={() => setIsCancelDialogOpen(false)} disabled={isSubmitting}>
              Quay lại
            </Button>
            <Button variant="destructive" className="rounded-xl" onClick={handleCancelBooking} disabled={isSubmitting}>
              {isSubmitting ? 'Đang xử lý...' : 'Xác nhận hủy đặt'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Dialog: Viết đánh giá */}
      <Dialog open={isReviewDialogOpen} onOpenChange={setIsReviewDialogOpen}>
        <DialogContent className="sm:max-w-[480px] rounded-2xl p-6 border-0 shadow-2xl bg-white dark:bg-zinc-900">
          <DialogHeader className="space-y-2">
            <DialogTitle className="text-lg font-bold flex items-center gap-2 text-neutral-800 dark:text-neutral-200">
              <Sparkles className="h-5 w-5 text-yellow-500 fill-yellow-500" />
              Đánh giá trải nghiệm của bạn
            </DialogTitle>
            <DialogDescription className="text-neutral-500 text-sm">
              Ý kiến của bạn tại **{booking.venueName}** sẽ giúp nâng cao chất lượng sân chơi cho cả cộng đồng.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-5 py-2">
            {/* Đánh giá sao */}
            <div className="flex flex-col items-center justify-center bg-neutral-50 dark:bg-zinc-900/50 py-5 rounded-2xl border border-neutral-100 dark:border-zinc-800 gap-2">
              <div className="flex gap-2">
                {[1, 2, 3, 4, 5].map((star) => {
                  const isFilled = hoverRating !== null ? star <= hoverRating : star <= rating;
                  return (
                    <button
                      key={star}
                      type="button"
                      onClick={() => setRating(star)}
                      onMouseEnter={() => setHoverRating(star)}
                      onMouseLeave={() => setHoverRating(null)}
                      className="focus:outline-none transition-transform hover:scale-125 duration-150"
                    >
                      <Star
                        className={`h-9 w-9 transition-colors ${
                          isFilled 
                            ? 'fill-yellow-400 text-yellow-400' 
                            : 'text-neutral-300 dark:text-neutral-700'
                        }`}
                      />
                    </button>
                  );
                })}
              </div>
              <span className="text-xs font-semibold text-neutral-500">
                {ratingLabels[rating - 1]}
              </span>
            </div>

            {/* Quick tags gợi ý */}
            <div className="space-y-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-neutral-400 block">Đặc điểm nổi bật</span>
              <div className="flex flex-wrap gap-2">
                {presetReviewTags.map((tag) => {
                  const isSelected = selectedTags.includes(tag);
                  return (
                    <button
                      key={tag}
                      type="button"
                      onClick={() => toggleTag(tag)}
                      className={`text-xs px-3 py-1.5 rounded-full border transition-all ${
                        isSelected 
                          ? 'border-emerald-500 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 font-medium' 
                          : 'border-neutral-100 hover:bg-neutral-50 dark:border-zinc-800 dark:hover:bg-zinc-900 text-neutral-600 dark:text-neutral-400'
                      }`}
                    >
                      {isSelected ? '✓ ' : ''}{tag}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Nhập nhận xét */}
            <div className="flex flex-col gap-2">
              <label htmlFor="comment" className="text-xs font-semibold uppercase tracking-wider text-neutral-400">Nội dung nhận xét</label>
              <Textarea
                id="comment"
                placeholder="Nhập chi tiết ý kiến nhận xét của bạn về mặt sân, ánh sáng, dịch vụ, thái độ nhân viên..."
                value={reviewComment}
                onChange={(e) => setReviewComment(e.target.value)}
                rows={3}
                className="rounded-xl border-neutral-200 dark:border-zinc-800 focus-visible:ring-emerald-500 resize-none text-xs"
              />
            </div>
          </div>

          <DialogFooter className="gap-2 sm:gap-0 mt-2">
            <Button variant="outline" className="rounded-xl" onClick={() => setIsReviewDialogOpen(false)} disabled={isSubmitting}>
              Đóng
            </Button>
            <Button className="rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white border-0" onClick={handleSendReview} disabled={isSubmitting}>
              {isSubmitting ? 'Đang gửi...' : 'Hoàn tất & Gửi'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default BookingDetailPage;
