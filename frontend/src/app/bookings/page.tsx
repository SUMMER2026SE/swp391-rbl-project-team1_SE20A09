"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { MapPin, Calendar, Clock, FileText, Loader2, AlertCircle } from "lucide-react";
import {
  fetchMyBookings,
  type BookingHistoryItem,
} from "@/lib/bookings-api";

const STATUS_CONFIG = {
  confirmed: { label: "Đã xác nhận", className: "bg-green-100 text-green-700" },
  pending: { label: "Chờ xác nhận", className: "bg-yellow-100 text-yellow-700" },
  completed: { label: "Hoàn thành", className: "bg-gray-100 text-gray-700" },
  cancelled: { label: "Đã hủy", className: "bg-red-100 text-red-700" },
} as const;

function getStatusBadge(status: BookingHistoryItem["status"]) {
  const config = STATUS_CONFIG[status];
  return <Badge className={config.className}>{config.label}</Badge>;
}

function getActionButtons(booking: BookingHistoryItem) {
  switch (booking.status) {
    case "confirmed":
    case "pending":
      return (
        <>
          <Button variant="outline" size="sm" asChild>
            <Link href={`/booking/${booking.id}`}>Xem chi tiết</Link>
          </Button>
          <Button variant="destructive" size="sm">
            Hủy đặt sân
          </Button>
        </>
      );
    case "completed":
      return (
        <>
          <Button variant="outline" size="sm" asChild>
            <Link href={`/booking/${booking.id}`}>Xem chi tiết</Link>
          </Button>
          <Button size="sm">Viết đánh giá</Button>
        </>
      );
    case "cancelled":
      return (
        <Button variant="outline" size="sm" asChild>
          <Link href={`/booking/${booking.id}`}>Xem chi tiết</Link>
        </Button>
      );
    default:
      return null;
  }
}

function BookingCard({ booking }: { booking: BookingHistoryItem }) {
  return (
    <Card>
      <CardContent className="p-6">
        <div className="flex gap-4">
          <img
            src={booking.imageUrl}
            alt={booking.venue}
            className="h-24 w-24 rounded-lg object-cover"
          />
          <div className="flex-1">
            <div className="mb-2 flex items-start justify-between">
              <div>
                <h3 className="mb-1 font-semibold">{booking.venue}</h3>
                <Badge variant="outline">{booking.sportType}</Badge>
              </div>
              {getStatusBadge(booking.status)}
            </div>

            <div className="mt-3 grid grid-cols-1 gap-2 text-sm text-muted-foreground sm:grid-cols-2">
              <div className="flex items-center">
                <Calendar className="mr-2 h-4 w-4 shrink-0" />
                {booking.date}
              </div>
              <div className="flex items-center">
                <Clock className="mr-2 h-4 w-4 shrink-0" />
                {booking.time}
              </div>
              <div className="flex items-center">
                <MapPin className="mr-2 h-4 w-4 shrink-0" />
                {booking.location}
              </div>
              <div className="flex items-center">
                <FileText className="mr-2 h-4 w-4 shrink-0" />
                {booking.displayId}
              </div>
            </div>

            <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
              <div className="text-xl font-semibold text-primary">
                {booking.price.toLocaleString("vi-VN")}đ
              </div>
              <div className="flex flex-wrap gap-2">{getActionButtons(booking)}</div>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

function EmptyTabMessage({ message }: { message: string }) {
  return (
    <Card className="border-dashed">
      <CardContent className="py-12 text-center text-muted-foreground">{message}</CardContent>
    </Card>
  );
}

function BookingHistoryPage() {
  const [bookings, setBookings] = useState<BookingHistoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadBookings = () => {
    setLoading(true);
    setError(null);
    fetchMyBookings()
      .then(setBookings)
      .catch((err: unknown) => {
        setError(err instanceof Error ? err.message : "Không tải được lịch sử đặt sân.");
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadBookings();
  }, []);

  const upcoming = useMemo(
    () => bookings.filter((b) => b.status === "confirmed" || b.status === "pending"),
    [bookings],
  );

  const filterByStatus = (status?: BookingHistoryItem["status"]) => {
    if (!status) return bookings;
    return bookings.filter((b) => b.status === status);
  };

  const renderList = (items: BookingHistoryItem[], emptyMessage: string) => {
    if (items.length === 0) {
      return <EmptyTabMessage message={emptyMessage} />;
    }
    return (
      <div className="space-y-4">
        {items.map((booking) => (
          <BookingCard key={booking.id} booking={booking} />
        ))}
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <h1 className="mb-8 text-3xl font-bold">Lịch sử đặt sân</h1>

        {loading && (
          <div className="flex flex-col items-center justify-center gap-3 py-20">
            <Loader2 className="h-10 w-10 animate-spin text-green-800" />
            <p className="text-sm text-muted-foreground">Đang tải lịch sử đặt sân...</p>
          </div>
        )}

        {!loading && error && (
          <div className="flex flex-col items-center justify-center gap-4 py-20">
            <AlertCircle className="h-12 w-12 text-destructive" />
            <p className="text-center text-muted-foreground">{error}</p>
            <Button onClick={loadBookings} className="bg-green-800 hover:bg-green-900">
              Thử lại
            </Button>
          </div>
        )}

        {!loading && !error && (
          <Tabs defaultValue="all">
            <TabsList className="mb-6">
              <TabsTrigger value="all">Tất cả ({bookings.length})</TabsTrigger>
              <TabsTrigger value="upcoming">Sắp tới ({upcoming.length})</TabsTrigger>
              <TabsTrigger value="completed">
                Hoàn thành ({filterByStatus("completed").length})
              </TabsTrigger>
              <TabsTrigger value="cancelled">
                Đã hủy ({filterByStatus("cancelled").length})
              </TabsTrigger>
            </TabsList>

            <TabsContent value="all">
              {renderList(bookings, "Bạn chưa có đơn đặt sân nào.")}
            </TabsContent>

            <TabsContent value="upcoming">
              {renderList(
                upcoming,
                "Không có lịch sắp tới. Hãy đặt sân để bắt đầu!",
              )}
            </TabsContent>

            <TabsContent value="completed">
              {renderList(
                filterByStatus("completed"),
                "Chưa có đơn đã hoàn thành.",
              )}
            </TabsContent>

            <TabsContent value="cancelled">
              {renderList(filterByStatus("cancelled"), "Không có đơn đã hủy.")}
            </TabsContent>
          </Tabs>
        )}
      </div>

      <Footer />
    </div>
  );
}

export default BookingHistoryPage;
