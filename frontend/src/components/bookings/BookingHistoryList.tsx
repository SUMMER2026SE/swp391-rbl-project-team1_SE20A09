"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { MapPin, Calendar, Clock, FileText, Loader2, AlertCircle, ChevronLeft, ChevronRight, PlusCircle } from "lucide-react";
import Image from "next/image";
import { useBookingHistory } from "@/hooks/useBookingHistory";
import { type BookingHistoryItem } from "@/lib/bookings-api";
import { CancelBookingDialog } from "@/components/bookings/CancelBookingDialog";

const STATUS_CONFIG = {
  confirmed: { label: "Đã xác nhận", className: "bg-green-50 text-green-700 border-green-200" },
  pending: { label: "Chờ xác nhận", className: "bg-amber-50 text-amber-700 border-amber-200" },
  completed: { label: "Hoàn thành", className: "bg-slate-50 text-slate-600 border-slate-200" },
  cancelled: { label: "Đã hủy", className: "bg-red-50 text-red-600 border-red-200" },
} as const;

function getStatusBadge(status: BookingHistoryItem["status"]) {
  const config = STATUS_CONFIG[status] || { label: status, className: "bg-slate-50 text-slate-600" };
  return (
    <Badge variant="outline" className={`${config.className} font-medium px-2.5 py-0.5 rounded-full`}>
      {config.label}
    </Badge>
  );
}

function getActionButtons(
  booking: BookingHistoryItem,
  isOwner: boolean = false,
  onRequestCancel: (bookingId: string) => void
) {
  const primaryBtnClass = "rounded-xl px-6 w-full sm:w-auto font-medium";
  const secondaryBtnClass = "rounded-xl px-6 w-full sm:w-auto font-medium border-slate-200 text-slate-600";

  const detailLink = isOwner 
    ? `/owner/bookings/${booking.id}` 
    : `/booking/${booking.id}`;

  switch (booking.status) {
    case "pending":
    case "confirmed":
      return (
        <>
          <Button asChild variant="outline" className={secondaryBtnClass}>
            <Link href={detailLink}>Xem chi tiết</Link>
          </Button>
          {!isOwner && (
            <Button
              type="button"
              variant="outline"
              className={`${primaryBtnClass} border-red-200 text-red-600 hover:bg-red-50 hover:text-red-700`}
              onClick={() => onRequestCancel(booking.id)}
            >
              Hủy đơn
            </Button>
          )}
        </>
      );
    case "completed":
      return (
        <>
          <Button asChild variant="outline" className={secondaryBtnClass}>
            <Link href={detailLink}>Xem chi tiết</Link>
          </Button>
          {!isOwner && (
            <Button asChild className={`${primaryBtnClass} bg-emerald-600 hover:bg-emerald-700 text-white`}>
              <Link href={`/booking/${booking.id}/review`}>Đánh giá</Link>
            </Button>
          )}
        </>
      );
    case "cancelled":
      return (
        <Button asChild variant="outline" className={`${secondaryBtnClass} w-full`}>
          <Link href={detailLink}>Xem chi tiết</Link>
        </Button>
      );
    default:
      return null;
  }
}

function BookingCard({
  booking,
  isOwner = false,
  onRequestCancel,
}: {
  booking: BookingHistoryItem;
  isOwner?: boolean;
  onRequestCancel: (bookingId: string) => void;
}) {
  return (
    <Card className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition hover:shadow-md">
      <CardContent className="space-y-4 p-4 md:p-5">
        <div className="flex items-start justify-between gap-3">
          <div className="flex gap-4">
            <div className="relative h-14 w-14 overflow-hidden rounded-xl border border-slate-100 bg-slate-50 shrink-0">
              <Image
                src={booking.imageUrl}
                alt={booking.venue}
                fill
                className="object-cover"
                unoptimized
              />
            </div>
            <div className="min-w-0">
              <h3 className="truncate text-base font-bold text-slate-900 md:text-lg">
                {booking.venue}
              </h3>
              <p className="text-sm font-medium text-slate-500">{booking.sportType}</p>
            </div>
          </div>
          {getStatusBadge(booking.status)}
        </div>

        <div className="grid grid-cols-1 gap-x-4 gap-y-2 text-sm text-slate-600 sm:grid-cols-2">
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4 text-slate-400" />
            <span className="font-medium">{booking.date}</span>
          </div>
          <div className="flex items-center gap-2">
            <Clock className="h-4 w-4 text-slate-400" />
            <span className="font-medium whitespace-nowrap">{booking.time}</span>
          </div>
          <div className="flex items-center gap-2 sm:col-span-2">
            <MapPin className="h-4 w-4 text-slate-400 shrink-0" />
            <span className="truncate">{booking.location}</span>
          </div>
          <div className="flex items-center gap-2 sm:col-span-2">
            <FileText className="h-4 w-4 text-slate-400" />
            <span className="text-slate-500">Mã đơn: <span className="text-slate-700 font-mono">{booking.displayId}</span></span>
          </div>
        </div>

        <div className="flex flex-col gap-4 border-t border-slate-100 pt-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-[10px] uppercase tracking-wider text-slate-400 font-bold">Tổng thanh toán</p>
            <p className="text-xl font-bold text-primary">
              {booking.price.toLocaleString("vi-VN")}đ
            </p>
          </div>
          <div className="flex flex-wrap gap-2 sm:justify-end">
            {getActionButtons(booking, isOwner, onRequestCancel)}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

function EmptyTabMessage({ 
  message, 
  icon: Icon = PlusCircle,
  actionLabel, 
  onAction 
}: { 
  message: string; 
  icon?: any;
  actionLabel?: string;
  onAction?: () => void;
}) {
  return (
    <Card className="border-dashed border-2 bg-slate-50/50">
      <CardContent className="flex flex-col items-center justify-center py-16 text-center">
        <div className="mb-4 rounded-full bg-slate-100 p-4">
          <Icon className="h-8 w-8 text-slate-400" />
        </div>
        <p className="max-w-[240px] text-slate-500 font-medium mb-6">
          {message}
        </p>
        {actionLabel && (
          <Button onClick={onAction} className="rounded-xl px-8 bg-green-800 hover:bg-green-900">
            {actionLabel}
          </Button>
        )}
      </CardContent>
    </Card>
  );
}

// ── SUBCOMPONENTS FOR CLEAN ARCHITECTURE ──────────────────────────────────────

function LoadingState() {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-24">
      <Loader2 className="h-10 w-10 animate-spin text-green-800" />
      <p className="text-sm font-medium text-slate-500">Đang tải lịch sử đặt sân...</p>
    </div>
  );
}

function ErrorState({ error, onRetry }: { error: string; onRetry: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center gap-4 py-20 text-center">
      <div className="rounded-full bg-red-50 p-4">
        <AlertCircle className="h-10 w-10 text-red-500" />
      </div>
      <p className="max-w-xs text-slate-600 font-medium">{error}</p>
      <Button onClick={onRetry} variant="outline" className="rounded-xl border-slate-200">
        Thử lại
      </Button>
    </div>
  );
}

function EmptyState({ 
  message, 
  actionLabel, 
  onAction 
}: { 
  message: string; 
  actionLabel?: string; 
  onAction?: () => void;
}) {
  return (
    <EmptyTabMessage 
      message={message} 
      actionLabel={actionLabel} 
      onAction={onAction}
    />
  );
}

interface BookingHistoryListProps {
  isOwner?: boolean;
}

export function BookingHistoryList({ isOwner = false }: BookingHistoryListProps) {
  const router = useRouter();
  const [activeTab, setActiveTab] = useState("all");
  const [cancelTargetId, setCancelTargetId] = useState<string | null>(null);

  const {
    bookings,
    totalPages,
    loading,
    error,
    page,
    setPage,
    reload
  } = useBookingHistory(isOwner, activeTab);

  const handleTabChange = (val: string) => {
    setActiveTab(val);
  };

  const handleRequestCancel = (bookingId: string) => {
    setCancelTargetId(bookingId);
  };

  const handleCancelDialogChange = (open: boolean) => {
    if (!open) {
      setCancelTargetId(null);
    }
  };

  const handleCancelled = () => {
    // refetch danh sách để phản ánh trạng thái CANCELLED mới nhất.
    reload();
  };

  const renderPagination = () => {
    if (totalPages <= 1) return null;

    return (
      <div className="mt-8 flex items-center justify-center gap-2">
        <Button
          variant="outline"
          size="icon"
          className="rounded-xl"
          onClick={() => setPage((p) => Math.max(0, p - 1))}
          disabled={page === 0}
        >
          <ChevronLeft className="h-4 w-4" />
        </Button>
        
        <div className="flex items-center gap-1.5 px-4 text-sm font-semibold text-slate-700">
          <span>Trang</span>
          <span className="rounded-lg bg-slate-100 px-2.5 py-1 text-primary">{page + 1}</span>
          <span className="text-slate-400">/</span>
          <span>{totalPages}</span>
        </div>

        <Button
          variant="outline"
          size="icon"
          className="rounded-xl"
          onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
          disabled={page >= totalPages - 1}
        >
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    );
  };

  const renderContent = (emptyMsg: string) => {
    if (loading) return <LoadingState />;
    if (error) return <ErrorState error={error} onRetry={reload} />;
    
    if (bookings.length === 0) {
      return (
        <EmptyState 
          message={emptyMsg} 
          actionLabel={activeTab !== "all" ? "Xem tất cả đơn" : "Đặt sân ngay"} 
          onAction={() => activeTab !== "all" ? handleTabChange("all") : router.push("/search")}
        />
      );
    }

    return (
      <div className="space-y-4">
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-1">
          {bookings.map((booking) => (
            <BookingCard
              key={booking.id}
              booking={booking}
              isOwner={isOwner}
              onRequestCancel={handleRequestCancel}
            />
          ))}
        </div>
        {renderPagination()}
      </div>
    );
  };

  return (
    <div className="w-full max-w-4xl mx-auto">
      <Tabs value={activeTab} onValueChange={handleTabChange} className="w-full space-y-6">
        <TabsList className="flex h-auto w-full gap-2 overflow-x-auto rounded-2xl bg-slate-100 p-1.5 scrollbar-hide">
          <TabsTrigger value="all" className="rounded-xl px-5 py-2.5 whitespace-nowrap data-[state=active]:bg-white data-[state=active]:shadow-sm font-semibold">Tất cả</TabsTrigger>
          <TabsTrigger value="upcoming" className="rounded-xl px-5 py-2.5 whitespace-nowrap data-[state=active]:bg-white data-[state=active]:shadow-sm font-semibold">Sắp tới</TabsTrigger>
          <TabsTrigger value="completed" className="rounded-xl px-5 py-2.5 whitespace-nowrap data-[state=active]:bg-white data-[state=active]:shadow-sm font-semibold">Hoàn thành</TabsTrigger>
          <TabsTrigger value="cancelled" className="rounded-xl px-5 py-2.5 whitespace-nowrap data-[state=active]:bg-white data-[state=active]:shadow-sm font-semibold">Đã hủy</TabsTrigger>
        </TabsList>

        <div className="relative">
          <TabsContent value="all" className="m-0 focus-visible:outline-none">
            {renderContent("Bạn chưa có đơn đặt sân nào. Hãy bắt đầu hành trình ngay!")}
          </TabsContent>
          <TabsContent value="upcoming" className="m-0 focus-visible:outline-none">
            {renderContent("Hiện tại không có lịch sắp tới. Đặt ngay để không lỡ trận nào nhé!")}
          </TabsContent>
          <TabsContent value="completed" className="m-0 focus-visible:outline-none">
            {renderContent("Chưa có đơn đã hoàn thành. Hãy cùng ra sân vận động nhé!")}
          </TabsContent>
          <TabsContent value="cancelled" className="m-0 focus-visible:outline-none">
            {renderContent("Tuyệt vời, bạn chưa có đơn nào bị hủy.")}
          </TabsContent>
        </div>
      </Tabs>

      <CancelBookingDialog
        bookingId={cancelTargetId !== null ? Number(cancelTargetId) : null}
        onOpenChange={handleCancelDialogChange}
        onCancelled={handleCancelled}
      />
    </div>
  );
}
