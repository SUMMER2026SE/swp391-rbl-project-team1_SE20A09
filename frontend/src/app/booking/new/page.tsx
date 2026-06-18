'use client'

import { useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { getVenueDetail } from "@/lib/api/venue";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Calendar, MapPin, Minus, Plus } from "lucide-react";
import Image from "next/image";

function BookingContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const venueIdParam = searchParams.get("venueId") || searchParams.get("stadiumId");
  const venueId = venueIdParam ? parseInt(venueIdParam) : 1;

  const [selectedDate, setSelectedDate] = useState<string>(() => {
    return searchParams.get("date") || new Date().toISOString().split("T")[0];
  });

  const [selectedSlot, setSelectedSlot] = useState<string>(() => {
    return searchParams.get("slot") || "";
  });

  const [accessories, setAccessories] = useState<Record<number, number>>({});

  const { data: venue, isLoading, error } = useQuery({
    queryKey: ["venue-detail", venueId],
    queryFn: () => getVenueDetail(venueId),
    enabled: !!venueId,
  });

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex flex-col justify-between">
        <Header />
        <div className="flex-grow flex items-center justify-center p-8">
          <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-emerald-500"></div>
        </div>
        <Footer />
      </div>
    );
  }

  if (error || !venue) {
    return (
      <div className="min-h-screen bg-background flex flex-col justify-between">
        <Header />
        <div className="flex-grow flex flex-col items-center justify-center p-8">
          <h2 className="text-xl font-bold mb-4 text-destructive">Không tìm thấy sân</h2>
          <Button onClick={() => router.push("/search")}>Quay lại tìm kiếm</Button>
        </div>
        <Footer />
      </div>
    );
  }

  // Parse open and close times
  const parseHour = (timeStr: string) => {
    try {
      const parts = timeStr.split(":");
      return parseInt(parts[0]);
    } catch {
      return 6;
    }
  };

  const openHour = parseHour(venue.openTime || "06:00:00");
  const closeHour = parseHour(venue.closeTime || "22:00:00");

  // Generate dynamic slots based on open/close time
  const getSlotsForSelectedDate = () => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const selDate = new Date(selectedDate);
    selDate.setHours(0, 0, 0, 0);
    const isToday = selDate.getTime() === today.getTime();
    const currentHour = new Date().getHours();
    const currentMinutes = new Date().getMinutes();

    const slotsCount = closeHour - openHour;
    return Array.from({ length: Math.max(1, slotsCount) }, (_, i) => {
      const h = openHour + i;
      const hourStr = `${String(h).padStart(2, "0")}:00`;
      const timeLabel = `${hourStr} - ${String(h + 1).padStart(2, "0")}:00`;

      let isPast = false;
      if (isToday) {
        if (currentHour > h) {
          isPast = true;
        } else if (currentHour === h && currentMinutes > 0) {
          isPast = true;
        }
      }

      // Check slot status from backend
      // Backend time slots have startTime format like "2024-05-22T08:00:00" or just "08:00:00"
      const matched = venue.timeSlots?.find((s) => {
        const timePart = s.startTime.includes("T") ? s.startTime.split("T")[1] : s.startTime;
        return timePart.substring(0, 5) === hourStr;
      });

      const available = matched ? (matched.slotStatus === "AVAILABLE") : true;

      return {
        id: hourStr,
        time: timeLabel,
        status: isPast ? "past" : (available ? "available" : "booked"),
      };
    });
  };

  const timeSlots = getSlotsForSelectedDate();
  const accessoryItems = venue.accessories || [];
  const venuePrice = venue.pricePerHour || 0;
  const platformFee = 20000;

  const calculateAccessoryTotal = () => {
    return accessoryItems.reduce((total, item) => {
      const qty = accessories[item.accessoryId] || 0;
      return total + item.pricePerUnit * qty;
    }, 0);
  };

  const total = venuePrice + calculateAccessoryTotal() + platformFee;

  const handleContinueToPayment = () => {
    const selectedAccs = accessoryItems
      .filter((item) => (accessories[item.accessoryId] || 0) > 0)
      .map((item) => ({
        name: item.name,
        quantity: accessories[item.accessoryId],
        price: item.pricePerUnit * accessories[item.accessoryId],
      }));

    const bookingSummary = {
      venueId: venue.stadiumId,
      stadiumName: venue.stadiumName,
      imageUrl: venue.imageUrls?.[0] || "",
      address: venue.address,
      sportName: venue.sportName,
      date: selectedDate,
      time: timeSlots.find((s) => s.id === selectedSlot)?.time || selectedSlot,
      venuePrice: venuePrice,
      accessories: selectedAccs,
      accessoryTotal: calculateAccessoryTotal(),
      total: total,
    };

    sessionStorage.setItem("booking_summary", JSON.stringify(bookingSummary));
    router.push("/booking/payment");
  };

  return (
    <div className="min-h-screen bg-background flex flex-col justify-between">
      <Header />

      <div className="container mx-auto px-4 py-8 flex-grow">
        <h1 className="text-3xl font-bold mb-8">Đặt sân</h1>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Left Column */}
          <div className="lg:col-span-2 space-y-6">
            {/* Venue Summary */}
            <Card>
              <CardContent className="p-6">
                <div className="flex gap-4">
                  <div className="relative w-24 h-24 rounded-lg overflow-hidden flex-shrink-0 bg-emerald-50">
                    {venue.imageUrls?.[0] ? (
                      <Image
                        src={venue.imageUrls[0]}
                        alt={venue.stadiumName}
                        fill
                        className="object-cover"
                        unoptimized
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-emerald-300 text-xs">
                        No image
                      </div>
                    )}
                  </div>
                  <div className="flex-1">
                    <h3 className="text-xl font-semibold mb-2">{venue.stadiumName}</h3>
                    <div className="flex items-center text-sm text-muted-foreground">
                      <MapPin className="h-4 w-4 mr-1 text-emerald-600" />
                      {venue.address}
                    </div>
                    <Badge className="mt-2 bg-emerald-100 text-emerald-800 hover:bg-emerald-200 border-none">{venue.sportName}</Badge>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Date Picker */}
            <Card>
              <CardHeader>
                <h3 className="flex items-center text-lg font-semibold">
                  <Calendar className="h-5 w-5 mr-2 text-emerald-600" />
                  Chọn ngày
                </h3>
              </CardHeader>
              <CardContent>
                <input
                  type="date"
                  className="w-full border rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  value={selectedDate}
                  min={new Date().toISOString().split("T")[0]}
                  onChange={(e) => {
                    setSelectedDate(e.target.value);
                    setSelectedSlot(""); // Clear slot on date change
                  }}
                />
              </CardContent>
            </Card>

            {/* Time Slots */}
            <Card>
              <CardHeader>
                <h3 className="text-lg font-semibold">Chọn khung giờ</h3>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                  {timeSlots.map((slot) => {
                    const isBooked = slot.status === "booked";
                    const isPast = slot.status === "past";
                    const isSelected = selectedSlot === slot.id;

                    return (
                      <button
                        key={slot.id}
                        onClick={() =>
                          slot.status === "available" && setSelectedSlot(slot.id)
                        }
                        disabled={isBooked || isPast}
                        className={`p-4 rounded-lg border-2 transition-all text-center flex flex-col items-center justify-center min-h-[80px] ${
                          isBooked
                            ? "bg-red-50 border-red-200 text-red-400 cursor-not-allowed"
                            : isPast
                            ? "bg-gray-100 border-gray-200 text-gray-400 cursor-not-allowed"
                            : isSelected
                            ? "bg-emerald-600 text-white border-emerald-600 shadow-md"
                            : "bg-emerald-50/50 border-emerald-100 text-emerald-800 hover:border-emerald-400 hover:bg-emerald-50"
                        }`}
                      >
                        <div className="font-semibold text-sm">{slot.time}</div>
                        <div className="text-xs mt-1">
                          {isBooked ? "Đã đặt" : isPast ? "Đã qua" : "Trống"}
                        </div>
                      </button>
                    );
                  })}
                </div>
              </CardContent>
            </Card>

            {/* Accessories */}
            <Card>
              <CardHeader>
                <h3 className="text-lg font-semibold">Phụ kiện cho thuê</h3>
              </CardHeader>
              <CardContent>
                {accessoryItems.length === 0 ? (
                  <div className="text-center py-6 text-muted-foreground text-sm">
                    Sân này không hỗ trợ thuê phụ kiện.
                  </div>
                ) : (
                  <div className="space-y-4">
                    {accessoryItems.map((item) => {
                      const qty = accessories[item.accessoryId] || 0;
                      return (
                        <div
                          key={item.accessoryId}
                          className="flex items-center justify-between p-4 border rounded-lg hover:shadow-sm transition-all bg-card"
                        >
                          <div>
                            <div className="font-medium text-sm md:text-base">{item.name}</div>
                            <div className="text-xs md:text-sm text-emerald-600 font-semibold mt-1">
                              {item.pricePerUnit.toLocaleString("vi-VN")}đ / chiếc
                            </div>
                            <div className="text-xs text-muted-foreground mt-0.5">
                              Kho còn: {item.quantity} chiếc
                            </div>
                          </div>
                          <div className="flex items-center gap-3">
                            <Button
                              variant="outline"
                              size="icon"
                              className="h-8 w-8 rounded-full"
                              disabled={qty <= 0}
                              onClick={() =>
                                setAccessories({
                                  ...accessories,
                                  [item.accessoryId]: Math.max(0, qty - 1),
                                })
                              }
                            >
                              <Minus className="h-4 w-4" />
                            </Button>
                            <span className="w-8 text-center font-medium text-sm">
                              {qty}
                            </span>
                            <Button
                              variant="outline"
                              size="icon"
                              className="h-8 w-8 rounded-full"
                              disabled={qty >= item.quantity}
                              onClick={() =>
                                setAccessories({
                                  ...accessories,
                                  [item.accessoryId]: Math.min(item.quantity, qty + 1),
                                })
                              }
                            >
                              <Plus className="h-4 w-4" />
                            </Button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          {/* Right Column - Order Summary */}
          <div className="lg:col-span-1">
            <Card className="sticky top-24 shadow-sm border-emerald-100">
              <CardHeader className="bg-emerald-50/50 pb-4">
                <h3 className="text-lg font-bold text-emerald-900">Tóm tắt đơn hàng</h3>
              </CardHeader>
              <CardContent className="space-y-4 pt-6">
                {selectedDate && (
                  <div>
                    <div className="text-xs text-muted-foreground uppercase font-semibold">Ngày đặt</div>
                    <div className="text-sm font-medium">{new Date(selectedDate).toLocaleDateString("vi-VN", { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</div>
                  </div>
                )}

                {selectedSlot && (
                  <div>
                    <div className="text-xs text-muted-foreground uppercase font-semibold">Khung giờ</div>
                    <div className="text-sm font-medium">
                      {timeSlots.find((s) => s.id === selectedSlot)?.time}
                    </div>
                  </div>
                )}

                <Separator />

                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Giá thuê sân</span>
                    <span className="font-semibold">{venuePrice.toLocaleString("vi-VN")}đ</span>
                  </div>

                  {calculateAccessoryTotal() > 0 && (
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground">Phụ kiện thuê thêm</span>
                      <span className="font-semibold">{calculateAccessoryTotal().toLocaleString("vi-VN")}đ</span>
                    </div>
                  )}

                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Phí dịch vụ</span>
                    <span className="font-semibold">{platformFee.toLocaleString("vi-VN")}đ</span>
                  </div>

                  <Separator />

                  <div className="flex justify-between items-center">
                    <span className="font-bold text-gray-800">Tổng cộng</span>
                    <span className="text-2xl font-bold text-emerald-600">
                      {total.toLocaleString("vi-VN")}đ
                    </span>
                  </div>
                </div>

                <Button
                  className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-semibold py-6 rounded-lg text-base shadow-sm transition-all"
                  disabled={!selectedDate || !selectedSlot}
                  onClick={handleContinueToPayment}
                >
                  Tiếp tục thanh toán
                </Button>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>

      <Footer />
    </div>
  );
}

export default function BookingSlotPickerPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen bg-background flex flex-col justify-between">
        <Header />
        <div className="flex-grow flex items-center justify-center p-8">
          <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-emerald-500"></div>
        </div>
        <Footer />
      </div>
    }>
      <BookingContent />
    </Suspense>
  );
}
