'use client'

import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import {
  TrendingUp,
  TrendingDown,
  Calendar,
  DollarSign,
  Users,
  AlertCircle,
  CheckCircle,
  Home,
  BarChart3,
  Wallet,
  Bell,
} from "lucide-react";

function OwnerDashboardPage() {
  const kpiData = [
    {
      title: "Đặt sân hôm nay",
      value: "12",
      change: "+8%",
      trend: "up",
      icon: <Calendar className="h-6 w-6" />,
    },
    {
      title: "Doanh thu tháng này",
      value: "45.5M",
      change: "+15%",
      trend: "up",
      icon: <DollarSign className="h-6 w-6" />,
    },
    {
      title: "Tỷ lệ lấp đầy",
      value: "78%",
      change: "-3%",
      trend: "down",
      icon: <BarChart3 className="h-6 w-6" />,
    },
    {
      title: "Chờ xác nhận",
      value: "5",
      change: "",
      trend: "neutral",
      icon: <AlertCircle className="h-6 w-6" />,
    },
  ];

  const revenueData = [
    { date: "01/05", revenue: 1200000 },
    { date: "05/05", revenue: 1800000 },
    { date: "10/05", revenue: 1500000 },
    { date: "15/05", revenue: 2100000 },
    { date: "20/05", revenue: 1900000 },
    { date: "25/05", revenue: 2400000 },
    { date: "30/05", revenue: 2200000 },
  ];

  const sportTypeData = [
    { name: "Bóng đá", value: 65, color: "#2563EB" },
    { name: "Cầu lông", value: 20, color: "#10B981" },
    { name: "Quần vợt", value: 10, color: "#F59E0B" },
    { name: "Bóng rổ", value: 5, color: "#EF4444" },
  ];

  const pendingBookings = [
    {
      id: "BK001234",
      customer: "Nguyễn Văn A",
      venue: "Sân 1",
      date: "22/05/2024",
      time: "18:00 - 20:00",
      amount: 500000,
    },
    {
      id: "BK001235",
      customer: "Trần Thị B",
      venue: "Sân 2",
      date: "22/05/2024",
      time: "20:00 - 22:00",
      amount: 600000,
    },
    {
      id: "BK001236",
      customer: "Lê Văn C",
      venue: "Sân 1",
      date: "23/05/2024",
      time: "16:00 - 18:00",
      amount: 500000,
    },
  ];

  return (
    <div className="flex">
        {/* Sidebar */}
        <aside className="w-64 min-h-screen bg-sidebar border-r p-4">
          <h2 className="mb-6 px-3">Quản lý chủ sân</h2>
          <nav className="space-y-1">
            <Button
              variant="default"
              className="w-full justify-start"
              size="sm"
            >
              <Home className="mr-3 h-4 w-4" />
              Dashboard
            </Button>
            <Button variant="ghost" className="w-full justify-start" size="sm">
              <BarChart3 className="mr-3 h-4 w-4" />
              Sân của tôi
            </Button>
            <Button variant="ghost" className="w-full justify-start" size="sm">
              <Calendar className="mr-3 h-4 w-4" />
              Lịch đặt
            </Button>
            <Button variant="ghost" className="w-full justify-start" size="sm">
              <Wallet className="mr-3 h-4 w-4" />
              Doanh thu
            </Button>
            <Button variant="ghost" className="w-full justify-start" size="sm">
              <Bell className="mr-3 h-4 w-4" />
              Thông báo
            </Button>
          </nav>
        </aside>

        {/* Main Content */}
        <main className="flex-1 p-8">
          <h1 className="text-3xl mb-8">Dashboard</h1>

          {/* KPIs */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
            {kpiData.map((kpi, idx) => (
              <Card key={idx}>
                <CardContent className="p-6">
                  <div className="flex items-center justify-between mb-4">
                    <div className="text-primary bg-primary/10 p-3 rounded-lg">
                      {kpi.icon}
                    </div>
                    {kpi.change && (
                      <div
                        className={`flex items-center text-sm ${
                          kpi.trend === "up"
                            ? "text-green-600"
                            : "text-red-600"
                        }`}
                      >
                        {kpi.trend === "up" ? (
                          <TrendingUp className="h-4 w-4 mr-1" />
                        ) : (
                          <TrendingDown className="h-4 w-4 mr-1" />
                        )}
                        {kpi.change}
                      </div>
                    )}
                  </div>
                  <div className="text-2xl mb-1">{kpi.value}</div>
                  <div className="text-sm text-muted-foreground">
                    {kpi.title}
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
            {/* Revenue Chart */}
            <Card className="lg:col-span-2">
              <CardHeader>
                <h3>Doanh thu 30 ngày qua</h3>
              </CardHeader>
              <CardContent>
                <ResponsiveContainer width="100%" height={300}>
                  <LineChart data={revenueData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="date" />
                    <YAxis />
                    <Tooltip
                      formatter={(value) => `${Number(value).toLocaleString('vi-VN')}đ`}
                    />
                    <Line
                      type="monotone"
                      dataKey="revenue"
                      stroke="#2563EB"
                      strokeWidth={2}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>

            {/* Sport Type Distribution */}
            <Card>
              <CardHeader>
                <h3>Doanh thu theo môn</h3>
              </CardHeader>
              <CardContent>
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie
                      data={sportTypeData}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={(entry) => `${entry.value}%`}
                      outerRadius={80}
                      fill="#8884d8"
                      dataKey="value"
                    >
                      {sportTypeData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          </div>

          {/* Pending Bookings */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <h3>Đơn đặt chờ xác nhận</h3>
                <Badge variant="destructive">{pendingBookings.length}</Badge>
              </div>
            </CardHeader>
            <CardContent>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left p-3">Mã đơn</th>
                      <th className="text-left p-3">Khách hàng</th>
                      <th className="text-left p-3">Sân</th>
                      <th className="text-left p-3">Ngày</th>
                      <th className="text-left p-3">Giờ</th>
                      <th className="text-right p-3">Số tiền</th>
                      <th className="text-right p-3">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pendingBookings.map((booking) => (
                      <tr key={booking.id} className="border-b hover:bg-muted">
                        <td className="p-3 font-mono text-sm">{booking.id}</td>
                        <td className="p-3">{booking.customer}</td>
                        <td className="p-3">{booking.venue}</td>
                        <td className="p-3">{booking.date}</td>
                        <td className="p-3">{booking.time}</td>
                        <td className="p-3 text-right">
                          {booking.amount.toLocaleString('vi-VN')}đ
                        </td>
                        <td className="p-3 text-right">
                          <div className="flex gap-2 justify-end">
                            <Button size="sm" variant="default">
                              <CheckCircle className="h-4 w-4 mr-1" />
                              Duyệt
                            </Button>
                            <Button size="sm" variant="destructive">
                              Từ chối
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        </main>
      </div>
  );
}

export default OwnerDashboardPage;
