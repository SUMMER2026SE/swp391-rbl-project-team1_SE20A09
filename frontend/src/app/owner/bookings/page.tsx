'use client'

import { useState } from "react";
import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Checkbox } from "@/components/ui/checkbox";
import { CheckCircle, XCircle, ChevronDown, ChevronUp } from "lucide-react";

function BookingManagementPage() {
  const [expandedRow, setExpandedRow] = useState<string | null>(null);

  const bookings = [
    {
      id: "BK001234",
      customer: {
        name: "Nguyễn Văn A",
        phone: "0901234567",
        email: "nguyenvana@email.com",
      },
      venue: "Sân 1",
      date: "22/05/2024",
      time: "18:00 - 20:00",
      amount: 500000,
      paymentStatus: "paid",
      status: "pending",
      notes: "Khách hàng muốn thuê thêm bóng",
    },
    {
      id: "BK001235",
      customer: {
        name: "Trần Thị B",
        phone: "0909876543",
        email: "tranthib@email.com",
      },
      venue: "Sân 2",
      date: "22/05/2024",
      time: "20:00 - 22:00",
      amount: 600000,
      paymentStatus: "paid",
      status: "confirmed",
      notes: "",
    },
    {
      id: "BK001236",
      customer: {
        name: "Lê Văn C",
        phone: "0903456789",
        email: "levanc@email.com",
      },
      venue: "Sân 1",
      date: "23/05/2024",
      time: "16:00 - 18:00",
      amount: 500000,
      paymentStatus: "paid",
      status: "completed",
      notes: "",
    },
  ];

  const getStatusBadge = (status: string) => {
    const config = {
      pending: { label: "Chờ xác nhận", className: "bg-yellow-100 text-yellow-700" },
      confirmed: { label: "Đã xác nhận", className: "bg-green-100 text-green-700" },
      rejected: { label: "Đã từ chối", className: "bg-red-100 text-red-700" },
      completed: { label: "Hoàn thành", className: "bg-gray-100 text-gray-700" },
    };
    const item = config[status as keyof typeof config];
    return <Badge className={item.className}>{item.label}</Badge>;
  };

  const filterBookings = (status?: string) => {
    if (!status) return bookings;
    return bookings.filter((b) => b.status === status);
  };

  const BookingRow = ({ booking }: { booking: typeof bookings[0] }) => {
    const isExpanded = expandedRow === booking.id;

    return (
      <>
        <tr className="border-b hover:bg-muted">
          <td className="p-3">
            <Checkbox />
          </td>
          <td className="p-3 font-mono text-sm">{booking.id}</td>
          <td className="p-3">{booking.customer.name}</td>
          <td className="p-3">{booking.venue}</td>
          <td className="p-3">{booking.date}</td>
          <td className="p-3">{booking.time}</td>
          <td className="p-3 text-right">{booking.amount.toLocaleString('vi-VN')}đ</td>
          <td className="p-3">{getStatusBadge(booking.status)}</td>
          <td className="p-3">
            <div className="flex gap-2">
              {booking.status === "pending" && (
                <>
                  <Button size="sm" variant="default">
                    <CheckCircle className="h-4 w-4 mr-1" />
                    Duyệt
                  </Button>
                  <Button size="sm" variant="destructive">
                    <XCircle className="h-4 w-4 mr-1" />
                    Từ chối
                  </Button>
                </>
              )}
              <Button
                size="sm"
                variant="ghost"
                onClick={() =>
                  setExpandedRow(isExpanded ? null : booking.id)
                }
              >
                {isExpanded ? (
                  <ChevronUp className="h-4 w-4" />
                ) : (
                  <ChevronDown className="h-4 w-4" />
                )}
              </Button>
            </div>
          </td>
        </tr>

        {isExpanded && (
          <tr className="bg-muted/50">
            <td colSpan={9} className="p-6">
              <div className="grid grid-cols-2 gap-6">
                <div>
                  <h4 className="mb-3">Thông tin khách hàng</h4>
                  <div className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Họ tên</span>
                      <span>{booking.customer.name}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Số điện thoại</span>
                      <span>{booking.customer.phone}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Email</span>
                      <span>{booking.customer.email}</span>
                    </div>
                  </div>
                </div>

                <div>
                  <h4 className="mb-3">Chi tiết đặt sân</h4>
                  <div className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Thanh toán</span>
                      <Badge className="bg-green-100 text-green-700">
                        Đã thanh toán
                      </Badge>
                    </div>
                    {booking.notes && (
                      <div>
                        <span className="text-muted-foreground block mb-1">
                          Ghi chú
                        </span>
                        <p>{booking.notes}</p>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </td>
          </tr>
        )}
      </>
    );
  };

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <h1 className="text-3xl mb-8">Quản lý đặt sân</h1>

        {/* Filters */}
        <Card className="mb-6">
          <CardContent className="p-4">
            <div className="flex gap-4 flex-wrap">
              <div className="flex-1 min-w-48">
                <input
                  type="date"
                  className="w-full border rounded-lg px-3 py-2"
                  placeholder="Từ ngày"
                />
              </div>
              <div className="flex-1 min-w-48">
                <input
                  type="date"
                  className="w-full border rounded-lg px-3 py-2"
                  placeholder="Đến ngày"
                />
              </div>
              <Select defaultValue="all">
                <SelectTrigger className="w-48">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Tất cả sân</SelectItem>
                  <SelectItem value="san1">Sân 1</SelectItem>
                  <SelectItem value="san2">Sân 2</SelectItem>
                  <SelectItem value="san3">Sân 3</SelectItem>
                </SelectContent>
              </Select>
              <Button variant="outline">Lọc</Button>
            </div>
          </CardContent>
        </Card>

        {/* Tabs */}
        <Tabs defaultValue="all">
          <div className="flex items-center justify-between mb-6">
            <TabsList>
              <TabsTrigger value="all">
                Tất cả ({bookings.length})
              </TabsTrigger>
              <TabsTrigger value="pending">
                Chờ xác nhận ({filterBookings("pending").length})
              </TabsTrigger>
              <TabsTrigger value="confirmed">
                Đã xác nhận ({filterBookings("confirmed").length})
              </TabsTrigger>
              <TabsTrigger value="rejected">
                Đã từ chối ({filterBookings("rejected").length})
              </TabsTrigger>
              <TabsTrigger value="completed">
                Hoàn thành ({filterBookings("completed").length})
              </TabsTrigger>
            </TabsList>

            <div className="flex gap-2">
              <Button variant="outline" size="sm">
                Xuất Excel
              </Button>
            </div>
          </div>

          <Card>
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-muted">
                    <tr>
                      <th className="p-3 text-left">
                        <Checkbox />
                      </th>
                      <th className="p-3 text-left">Mã đơn</th>
                      <th className="p-3 text-left">Khách hàng</th>
                      <th className="p-3 text-left">Sân</th>
                      <th className="p-3 text-left">Ngày</th>
                      <th className="p-3 text-left">Giờ</th>
                      <th className="p-3 text-right">Số tiền</th>
                      <th className="p-3 text-left">Trạng thái</th>
                      <th className="p-3 text-left">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    <TabsContent value="all" className="m-0">
                      {bookings.map((booking) => (
                        <BookingRow key={booking.id} booking={booking} />
                      ))}
                    </TabsContent>
                    <TabsContent value="pending" className="m-0">
                      {filterBookings("pending").map((booking) => (
                        <BookingRow key={booking.id} booking={booking} />
                      ))}
                    </TabsContent>
                    <TabsContent value="confirmed" className="m-0">
                      {filterBookings("confirmed").map((booking) => (
                        <BookingRow key={booking.id} booking={booking} />
                      ))}
                    </TabsContent>
                    <TabsContent value="rejected" className="m-0">
                      {filterBookings("rejected").map((booking) => (
                        <BookingRow key={booking.id} booking={booking} />
                      ))}
                    </TabsContent>
                    <TabsContent value="completed" className="m-0">
                      {filterBookings("completed").map((booking) => (
                        <BookingRow key={booking.id} booking={booking} />
                      ))}
                    </TabsContent>
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        </Tabs>
      </div>
    </div>
  );
}

export default BookingManagementPage;
