'use client'

import { useState } from "react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Calendar, MapPin, Minus, Plus } from "lucide-react";
import { useRouter } from "next/navigation";

function BookingSlotPickerPage() {
  const router = useRouter();
  const [selectedDate, setSelectedDate] = useState<string>("");
  const [selectedSlot, setSelectedSlot] = useState<string>("");
  const [accessories, setAccessories] = useState({
    ball: 0,
    vest: 0,
    net: 0,
  });

  const timeSlots = [
    { id: "06-08", time: "06:00 - 08:00", status: "available" },
    { id: "08-10", time: "08:00 - 10:00", status: "booked" },
    { id: "10-12", time: "10:00 - 12:00", status: "available" },
    { id: "14-16", time: "14:00 - 16:00", status: "available" },
    { id: "16-18", time: "16:00 - 18:00", status: "booked" },
    { id: "18-20", time: "18:00 - 20:00", status: "available" },
    { id: "20-22", time: "20:00 - 22:00", status: "available" },
  ];

  const accessoryItems = [
    { id: "ball", name: "Bóng đá", price: 50000 },
    { id: "vest", name: "Áo đấu (bộ)", price: 100000 },
    { id: "net", name: "Lưới bóng", price: 30000 },
  ];

  const venuePrice = 500000;
  const platformFee = 20000;

  const calculateAccessoryTotal = () => {
    return Object.entries(accessories).reduce((total, [key, qty]) => {
      const item = accessoryItems.find((i) => i.id === key);
      return total + (item ? item.price * qty : 0);
    }, 0);
  };

  const total = venuePrice + calculateAccessoryTotal() + platformFee;

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <h1 className="text-3xl mb-8">Đặt sân</h1>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Left Column */}
          <div className="lg:col-span-2 space-y-6">
            {/* Venue Summary */}
            <Card>
              <CardContent className="p-6">
                <div className="flex gap-4">
                  <img
                    src="https://images.unsplash.com/photo-1705593813682-033ee2991df6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=300"
                    alt="Venue"
                    className="w-24 h-24 rounded-lg object-cover"
                  />
                  <div className="flex-1">
                    <h3 className="mb-2">Sân bóng Thành Công</h3>
                    <div className="flex items-center text-sm text-muted-foreground">
                      <MapPin className="h-4 w-4 mr-1" />
                      123 Đường ABC, Quận 1, TP.HCM
                    </div>
                    <Badge className="mt-2">Bóng đá</Badge>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Date Picker */}
            <Card>
              <CardHeader>
                <h3 className="flex items-center">
                  <Calendar className="h-5 w-5 mr-2" />
                  Chọn ngày
                </h3>
              </CardHeader>
              <CardContent>
                <input
                  type="date"
                  className="w-full border rounded-lg px-4 py-3"
                  value={selectedDate}
                  onChange={(e) => setSelectedDate(e.target.value)}
                />
              </CardContent>
            </Card>

            {/* Time Slots */}
            <Card>
              <CardHeader>
                <h3>Chọn khung giờ</h3>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                  {timeSlots.map((slot) => (
                    <button
                      key={slot.id}
                      onClick={() =>
                        slot.status === "available" && setSelectedSlot(slot.id)
                      }
                      disabled={slot.status === "booked"}
                      className={`p-4 rounded-lg border-2 transition-all ${
                        slot.status === "booked"
                          ? "bg-red-50 border-red-200 text-red-400 cursor-not-allowed"
                          : selectedSlot === slot.id
                          ? "bg-primary text-primary-foreground border-primary"
                          : "bg-green-50 border-green-200 text-green-700 hover:border-green-400"
                      }`}
                    >
                      <div className="text-sm">{slot.time}</div>
                      <div className="text-xs mt-1">
                        {slot.status === "booked" ? "Đã đặt" : "Trống"}
                      </div>
                    </button>
                  ))}
                </div>
              </CardContent>
            </Card>

            {/* Accessories */}
            <Card>
              <CardHeader>
                <h3>Phụ kiện cho thuê</h3>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {accessoryItems.map((item) => (
                    <div
                      key={item.id}
                      className="flex items-center justify-between p-4 border rounded-lg"
                    >
                      <div>
                        <div className="font-medium">{item.name}</div>
                        <div className="text-sm text-muted-foreground">
                          {item.price.toLocaleString('vi-VN')}đ
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() =>
                            setAccessories({
                              ...accessories,
                              [item.id]: Math.max(
                                0,
                                accessories[item.id as keyof typeof accessories] - 1
                              ),
                            })
                          }
                        >
                          <Minus className="h-4 w-4" />
                        </Button>
                        <span className="w-8 text-center">
                          {accessories[item.id as keyof typeof accessories]}
                        </span>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() =>
                            setAccessories({
                              ...accessories,
                              [item.id]:
                                accessories[item.id as keyof typeof accessories] + 1,
                            })
                          }
                        >
                          <Plus className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Right Column - Order Summary */}
          <div className="lg:col-span-1">
            <Card className="sticky top-24">
              <CardHeader>
                <h3>Tóm tắt đơn hàng</h3>
              </CardHeader>
              <CardContent className="space-y-4">
                {selectedDate && (
                  <div>
                    <div className="text-sm text-muted-foreground">Ngày đặt</div>
                    <div>{new Date(selectedDate).toLocaleDateString("vi-VN")}</div>
                  </div>
                )}

                {selectedSlot && (
                  <div>
                    <div className="text-sm text-muted-foreground">Giờ</div>
                    <div>
                      {timeSlots.find((s) => s.id === selectedSlot)?.time}
                    </div>
                  </div>
                )}

                <Separator />

                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span>Giá sân</span>
                    <span>{venuePrice.toLocaleString('vi-VN')}đ</span>
                  </div>

                  {calculateAccessoryTotal() > 0 && (
                    <div className="flex justify-between text-sm">
                      <span>Phụ kiện</span>
                      <span>{calculateAccessoryTotal().toLocaleString('vi-VN')}đ</span>
                    </div>
                  )}

                  <div className="flex justify-between text-sm">
                    <span>Phí dịch vụ</span>
                    <span>{platformFee.toLocaleString('vi-VN')}đ</span>
                  </div>

                  <Separator />

                  <div className="flex justify-between">
                    <span>Tổng cộng</span>
                    <span className="text-xl text-primary">
                      {total.toLocaleString('vi-VN')}đ
                    </span>
                  </div>
                </div>

                <Button
                  className="w-full"
                  size="lg"
                  disabled={!selectedDate || !selectedSlot}
                  onClick={() => {
                    const checkoutData = {
                      venueName: "Sân bóng Thành Công",
                      location: "123 Đường ABC, Quận 1, TP.HCM",
                      sportType: "Bóng đá",
                      date: selectedDate,
                      slotTime: timeSlots.find((s) => s.id === selectedSlot)?.time || "",
                      accessories: Object.entries(accessories)
                        .filter(([_, qty]) => qty > 0)
                        .map(([key, qty]) => {
                          const item = accessoryItems.find((i) => i.id === key);
                          return { name: item?.name || "", price: item?.price || 0, quantity: qty };
                        }),
                      venuePrice,
                      platformFee,
                      total
                    };
                    localStorage.setItem('sport_venue_checkout', JSON.stringify(checkoutData));
                    router.push("/booking/payment");
                  }}
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

export default BookingSlotPickerPage;
