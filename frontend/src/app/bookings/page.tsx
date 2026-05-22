'use client'

import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { MapPin, Calendar, Clock, FileText } from "lucide-react";

function BookingHistoryPage() {
  const bookings = [
    {
      id: "BK001234",
      venue: "Sân bóng Thành Công",
      image: "https://images.unsplash.com/photo-1705593813682-033ee2991df6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=300",
      sportType: "Bóng đá",
      date: "22/05/2024",
      time: "18:00 - 20:00",
      location: "Quận 1, TP.HCM",
      price: 570000,
      status: "confirmed",
    },
    {
      id: "BK001235",
      venue: "Arena Sports Center",
      image: "https://images.unsplash.com/photo-1764703666646-acc2f7d48857?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=300",
      sportType: "Bóng đá",
      date: "20/05/2024",
      time: "16:00 - 18:00",
      location: "Quận 3, TP.HCM",
      price: 720000,
      status: "pending",
    },
    {
      id: "BK001236",
      venue: "Sân Vận Động Quận 7",
      image: "https://images.unsplash.com/photo-1767729790212-661953ecaa90?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=300",
      sportType: "Bóng đá",
      date: "18/05/2024",
      time: "10:00 - 12:00",
      location: "Quận 7, TP.HCM",
      price: 620000,
      status: "completed",
    },
    {
      id: "BK001237",
      venue: "Sân bóng Phú Mỹ Hưng",
      image: "https://images.unsplash.com/photo-1765305460539-edf7a0838dad?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=300",
      sportType: "Bóng đá",
      date: "15/05/2024",
      time: "20:00 - 22:00",
      location: "Quận 7, TP.HCM",
      price: 570000,
      status: "cancelled",
    },
  ];

  const getStatusBadge = (status: string) => {
    const statusConfig = {
      confirmed: { label: "Đã xác nhận", className: "bg-green-100 text-green-700" },
      pending: { label: "Chờ xác nhận", className: "bg-yellow-100 text-yellow-700" },
      completed: { label: "Hoàn thành", className: "bg-gray-100 text-gray-700" },
      cancelled: { label: "Đã hủy", className: "bg-red-100 text-red-700" },
    };
    const config = statusConfig[status as keyof typeof statusConfig];
    return <Badge className={config.className}>{config.label}</Badge>;
  };

  const getActionButtons = (status: string) => {
    switch (status) {
      case "confirmed":
        return (
          <>
            <Button variant="outline" size="sm">
              Xem chi tiết
            </Button>
            <Button variant="destructive" size="sm">
              Hủy đặt sân
            </Button>
          </>
        );
      case "pending":
        return (
          <>
            <Button variant="outline" size="sm">
              Xem chi tiết
            </Button>
            <Button variant="destructive" size="sm">
              Hủy đặt sân
            </Button>
          </>
        );
      case "completed":
        return (
          <>
            <Button variant="outline" size="sm">
              Xem chi tiết
            </Button>
            <Button size="sm">Viết đánh giá</Button>
          </>
        );
      case "cancelled":
        return (
          <Button variant="outline" size="sm">
            Xem chi tiết
          </Button>
        );
      default:
        return null;
    }
  };

  const filterBookings = (status?: string) => {
    if (!status) return bookings;
    return bookings.filter((b) => b.status === status);
  };

  const BookingCard = ({ booking }: { booking: typeof bookings[0] }) => (
    <Card>
      <CardContent className="p-6">
        <div className="flex gap-4">
          <img
            src={booking.image}
            alt={booking.venue}
            className="w-24 h-24 rounded-lg object-cover"
          />
          <div className="flex-1">
            <div className="flex items-start justify-between mb-2">
              <div>
                <h3 className="mb-1">{booking.venue}</h3>
                <Badge variant="outline">{booking.sportType}</Badge>
              </div>
              {getStatusBadge(booking.status)}
            </div>

            <div className="grid grid-cols-2 gap-2 text-sm text-muted-foreground mt-3">
              <div className="flex items-center">
                <Calendar className="h-4 w-4 mr-2" />
                {booking.date}
              </div>
              <div className="flex items-center">
                <Clock className="h-4 w-4 mr-2" />
                {booking.time}
              </div>
              <div className="flex items-center">
                <MapPin className="h-4 w-4 mr-2" />
                {booking.location}
              </div>
              <div className="flex items-center">
                <FileText className="h-4 w-4 mr-2" />
                {booking.id}
              </div>
            </div>

            <div className="flex items-center justify-between mt-4">
              <div className="text-xl text-primary">
                {booking.price.toLocaleString('vi-VN')}đ
              </div>
              <div className="flex gap-2">{getActionButtons(booking.status)}</div>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <h1 className="text-3xl mb-8">Lịch sử đặt sân</h1>

        <Tabs defaultValue="all">
          <TabsList className="mb-6">
            <TabsTrigger value="all">Tất cả</TabsTrigger>
            <TabsTrigger value="confirmed">Sắp tới</TabsTrigger>
            <TabsTrigger value="completed">Hoàn thành</TabsTrigger>
            <TabsTrigger value="cancelled">Đã hủy</TabsTrigger>
          </TabsList>

          <TabsContent value="all" className="space-y-4">
            {bookings.map((booking) => (
              <BookingCard key={booking.id} booking={booking} />
            ))}
          </TabsContent>

          <TabsContent value="confirmed" className="space-y-4">
            {filterBookings("confirmed").map((booking) => (
              <BookingCard key={booking.id} booking={booking} />
            ))}
          </TabsContent>

          <TabsContent value="completed" className="space-y-4">
            {filterBookings("completed").map((booking) => (
              <BookingCard key={booking.id} booking={booking} />
            ))}
          </TabsContent>

          <TabsContent value="cancelled" className="space-y-4">
            {filterBookings("cancelled").map((booking) => (
              <BookingCard key={booking.id} booking={booking} />
            ))}
          </TabsContent>
        </Tabs>
      </div>

      <Footer />
    </div>
  );
}

export default BookingHistoryPage;
