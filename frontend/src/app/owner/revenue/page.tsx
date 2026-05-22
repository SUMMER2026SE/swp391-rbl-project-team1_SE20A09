'use client'

import { Header } from "@/components/layout/Header";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { Download, TrendingUp, Calendar, DollarSign } from "lucide-react";

function RevenueReportPage() {
  const summaryData = [
    { title: "Tổng doanh thu", value: "125,500,000đ", icon: <DollarSign className="h-6 w-6" /> },
    { title: "Tổng đặt sân", value: "245", icon: <Calendar className="h-6 w-6" /> },
    { title: "Trung bình/đặt", value: "512,000đ", icon: <TrendingUp className="h-6 w-6" /> },
  ];

  const dailyRevenue = [
    { date: "01/05", revenue: 4200000 },
    { date: "02/05", revenue: 3800000 },
    { date: "03/05", revenue: 4500000 },
    { date: "04/05", revenue: 3200000 },
    { date: "05/05", revenue: 5100000 },
    { date: "06/05", revenue: 4700000 },
    { date: "07/05", revenue: 3900000 },
    { date: "08/05", revenue: 4300000 },
    { date: "09/05", revenue: 4800000 },
    { date: "10/05", revenue: 4100000 },
  ];

  const venueBreakdown = [
    {
      venue: "Sân 1",
      bookings: 95,
      revenue: 47500000,
      occupancy: 85,
      trend: "+12%",
    },
    {
      venue: "Sân 2",
      bookings: 82,
      revenue: 49200000,
      occupancy: 78,
      trend: "+8%",
    },
    {
      venue: "Sân Cầu lông 1",
      bookings: 68,
      revenue: 28800000,
      occupancy: 65,
      trend: "-5%",
    },
  ];

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl">Báo cáo doanh thu</h1>
          <div className="flex gap-4">
            <Select defaultValue="this-month">
              <SelectTrigger className="w-48">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="this-week">Tuần này</SelectItem>
                <SelectItem value="this-month">Tháng này</SelectItem>
                <SelectItem value="last-month">Tháng trước</SelectItem>
                <SelectItem value="custom">Tùy chỉnh</SelectItem>
              </SelectContent>
            </Select>
            <Button variant="outline">
              <Download className="mr-2 h-4 w-4" />
              Xuất PDF
            </Button>
            <Button variant="outline">
              <Download className="mr-2 h-4 w-4" />
              Xuất Excel
            </Button>
          </div>
        </div>

        {/* Summary Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          {summaryData.map((item, idx) => (
            <Card key={idx}>
              <CardContent className="p-6">
                <div className="flex items-center justify-between mb-4">
                  <div className="text-primary bg-primary/10 p-3 rounded-lg">
                    {item.icon}
                  </div>
                </div>
                <div className="text-2xl mb-1">{item.value}</div>
                <div className="text-sm text-muted-foreground">{item.title}</div>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* Daily Revenue Chart */}
        <Card className="mb-8">
          <CardHeader>
            <h3>Doanh thu theo ngày</h3>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={400}>
              <BarChart data={dailyRevenue}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis />
                <Tooltip
                  formatter={(value) => `${Number(value).toLocaleString('vi-VN')}đ`}
                />
                <Bar dataKey="revenue" fill="#2563EB" />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        {/* Venue Breakdown */}
        <Card>
          <CardHeader>
            <h3>Chi tiết theo sân</h3>
          </CardHeader>
          <CardContent>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-muted">
                  <tr>
                    <th className="p-3 text-left">Tên sân</th>
                    <th className="p-3 text-center">Số lượt đặt</th>
                    <th className="p-3 text-right">Doanh thu</th>
                    <th className="p-3 text-center">Tỷ lệ lấp đầy</th>
                    <th className="p-3 text-center">Xu hướng</th>
                  </tr>
                </thead>
                <tbody>
                  {venueBreakdown.map((venue, idx) => (
                    <tr key={idx} className="border-b">
                      <td className="p-3">{venue.venue}</td>
                      <td className="p-3 text-center">{venue.bookings}</td>
                      <td className="p-3 text-right">
                        {venue.revenue.toLocaleString('vi-VN')}đ
                      </td>
                      <td className="p-3 text-center">
                        <div className="flex items-center justify-center gap-2">
                          <div className="w-20 h-2 bg-muted rounded-full overflow-hidden">
                            <div
                              className="h-full bg-primary"
                              style={{ width: `${venue.occupancy}%` }}
                            />
                          </div>
                          <span className="text-sm">{venue.occupancy}%</span>
                        </div>
                      </td>
                      <td className="p-3 text-center">
                        <span
                          className={`text-sm ${
                            venue.trend.startsWith("+")
                              ? "text-green-600"
                              : "text-red-600"
                          }`}
                        >
                          {venue.trend}
                        </span>
                      </td>
                    </tr>
                  ))}
                  <tr className="bg-muted/50 font-medium">
                    <td className="p-3">Tổng cộng</td>
                    <td className="p-3 text-center">
                      {venueBreakdown.reduce((sum, v) => sum + v.bookings, 0)}
                    </td>
                    <td className="p-3 text-right">
                      {venueBreakdown
                        .reduce((sum, v) => sum + v.revenue, 0)
                        .toLocaleString('vi-VN')}
                      đ
                    </td>
                    <td className="p-3 text-center">-</td>
                    <td className="p-3 text-center">-</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

export default RevenueReportPage;
