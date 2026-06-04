'use client'

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { get } from "@/lib/api";
import { MapPin, Calendar, Clock, FileText } from "lucide-react";

type BookingStatus = "pending" | "confirmed" | "completed" | "cancelled";

type CustomerBooking = {
  id: number;
  bookingCode?: string;
  venueName: string;
  venueImage?: string | null;
  sportType: string;
  location: string;
  date: string;
  startTime: string;
  endTime: string;
  time?: string;
  totalPrice: number | string;
  status: BookingStatus;
  paymentStatus?: string;
  hasReviewed?: boolean;
};

const fallbackImage =
  "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080";

const statusConfig: Record<BookingStatus, { label: string; className: string }> = {
  confirmed: { label: "Đã xác nhận", className: "bg-emerald-500/10 text-emerald-600 border border-emerald-500/20" },
  pending: { label: "Chờ xác nhận", className: "bg-amber-500/10 text-amber-600 border border-amber-500/20" },
  completed: { label: "Hoàn thành", className: "bg-blue-500/10 text-blue-600 border border-blue-500/20" },
  cancelled: { label: "Đã hủy", className: "bg-rose-500/10 text-rose-600 border border-rose-500/20" },
};

const normalizeBookings = (data: CustomerBooking[] | { content?: CustomerBooking[] }) =>
  Array.isArray(data) ? data : data.content ?? [];

const formatPrice = (value: number | string) =>
  Number(value).toLocaleString("vi-VN");

function BookingHistoryPage() {
  const { data: session } = useSession();
  const router = useRouter();
  
  const [bookings, setBookings] = useState<CustomerBooking[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (session?.user?.roleName === "Owner") {
      router.push("/owner/bookings");
    }
  }, [session, router]);

  useEffect(() => {
    let active = true;

    async function loadBookings() {
      try {
        setIsLoading(true);
        setError(null);
        const data = await get<CustomerBooking[] | { content?: CustomerBooking[] }>("/bookings/my");
        if (active) {
          setBookings(normalizeBookings(data));
        }
      } catch (err) {
        if (active) {
          setError(err instanceof Error ? err.message : "Không tải được lịch sử đặt sân.");
        }
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    }

    loadBookings();

    return () => {
      active = false;
    };
  }, []);

  const upcomingBookings = useMemo(
    () => bookings.filter((booking) => booking.status === "confirmed" || booking.status === "pending"),
    [bookings],
  );
  const completedBookings = useMemo(
    () => bookings.filter((booking) => booking.status === "completed"),
    [bookings],
  );
  const cancelledBookings = useMemo(
    () => bookings.filter((booking) => booking.status === "cancelled"),
    [bookings],
  );

  const getStatusBadge = (status: BookingStatus) => {
    const config = statusConfig[status] ?? statusConfig.pending;
    return <Badge className={`rounded-full px-3 py-1 font-semibold ${config.className}`}>{config.label}</Badge>;
  };

  const BookingCard = ({ booking }: { booking: CustomerBooking }) => (
    <Card className="overflow-hidden rounded-2xl border border-neutral-100 shadow-sm transition-all hover:shadow-md">
      <CardContent className="p-6">
        <div className="flex flex-col gap-5 sm:flex-row">
          <img
            src={booking.venueImage || fallbackImage}
            alt={booking.venueName}
            className="h-28 w-full rounded-xl object-cover ring-1 ring-neutral-100 sm:w-28"
          />
          <div className="flex-1">
            <div className="mb-2 flex items-start justify-between gap-3">
              <div>
                <h3 className="mb-1 text-lg font-bold text-neutral-850">{booking.venueName}</h3>
                <Badge variant="outline" className="bg-neutral-50 text-neutral-500">{booking.sportType}</Badge>
              </div>
              {getStatusBadge(booking.status)}
            </div>

            <div className="mt-4 grid grid-cols-1 gap-2 text-sm text-neutral-500 sm:grid-cols-2">
              <div className="flex items-center">
                <Calendar className="mr-2.5 h-4 w-4 text-emerald-500" />
                {booking.date}
              </div>
              <div className="flex items-center">
                <Clock className="mr-2.5 h-4 w-4 text-emerald-500" />
                {booking.time || `${booking.startTime} - ${booking.endTime}`}
              </div>
              <div className="flex items-center">
                <MapPin className="mr-2.5 h-4 w-4 text-emerald-500" />
                {booking.location}
              </div>
              <div className="flex items-center">
                <FileText className="mr-2.5 h-4 w-4 text-emerald-500" />
                Mã đơn: <span className="ml-1 font-mono">{booking.bookingCode || `BK-${String(booking.id).padStart(6, "0")}`}</span>
              </div>
            </div>

            <div className="mt-5 flex items-center justify-between border-t border-neutral-100/60 pt-3">
              <div className="text-xl font-bold text-emerald-600">
                {formatPrice(booking.totalPrice)}đ
              </div>
              <Link href={`/booking/${booking.id}`}>
                <Button variant="outline" size="sm" className="rounded-xl font-semibold">
                  Xem chi tiết
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );

  const BookingList = ({ items }: { items: CustomerBooking[] }) => {
    if (isLoading) {
      return (
        <Card>
          <CardContent className="p-6 text-neutral-500">Đang tải lịch sử đặt sân...</CardContent>
        </Card>
      );
    }

    if (error) {
      return (
        <Card>
          <CardContent className="p-6 text-red-600">{error}</CardContent>
        </Card>
      );
    }

    if (items.length === 0) {
      return (
        <Card>
          <CardContent className="p-10 text-center text-neutral-500">
            Chưa có đơn đặt sân nào trong database cho tài khoản này.
          </CardContent>
        </Card>
      );
    }

    return items.map((booking) => <BookingCard key={booking.id} booking={booking} />);
  };

  return (
    <div className="min-h-screen bg-neutral-50/50 pb-12 font-sans">
      <Header />

      <div className="container mx-auto max-w-4xl space-y-6 px-4 py-8">
        <div className="flex flex-col gap-2">
          <h1 className="text-3xl font-extrabold text-neutral-800">Lịch sử đặt sân</h1>
          <p className="text-sm text-neutral-500">Dữ liệu được lấy trực tiếp từ database qua API.</p>
        </div>

        <Tabs defaultValue="all" className="w-full">
          <TabsList className="mb-6 rounded-2xl bg-neutral-100 p-1.5">
            <TabsTrigger value="all" className="rounded-xl px-5 py-2">Tất cả</TabsTrigger>
            <TabsTrigger value="confirmed" className="rounded-xl px-5 py-2">Sắp tới</TabsTrigger>
            <TabsTrigger value="completed" className="rounded-xl px-5 py-2">Hoàn thành</TabsTrigger>
            <TabsTrigger value="cancelled" className="rounded-xl px-5 py-2">Đã hủy</TabsTrigger>
          </TabsList>

          <TabsContent value="all" className="space-y-4">
            <BookingList items={bookings} />
          </TabsContent>

          <TabsContent value="confirmed" className="space-y-4">
            <BookingList items={upcomingBookings} />
          </TabsContent>

          <TabsContent value="completed" className="space-y-4">
            <BookingList items={completedBookings} />
          </TabsContent>

          <TabsContent value="cancelled" className="space-y-4">
            <BookingList items={cancelledBookings} />
          </TabsContent>
        </Tabs>
      </div>

      <Footer />
    </div>
  );
}

export default BookingHistoryPage;
