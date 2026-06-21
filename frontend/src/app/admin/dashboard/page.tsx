'use client'

import { Header } from "@/components/layout/Header";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import Link from "next/link";
import {
  AreaChart,
  Area,
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
  Users,
  Building,
  MapPin,
  DollarSign,
  TrendingUp,
  Home,
  UserCog,
  Settings,
  AlertCircle,
  CheckCircle,
} from "lucide-react";

function AdminDashboardPage() {
  const kpiData = [
    {
      title: "Tổng người dùng",
      value: "12,543",
      change: "+245",
      icon: <Users className="h-6 w-6" />,
    },
    {
      title: "Tổng chủ sân",
      value: "1,234",
      change: "+48",
      icon: <Building className="h-6 w-6" />,
    },
    {
      title: "Tổng sân",
      value: "3,567",
      change: "+89",
      icon: <MapPin className="h-6 w-6" />,
    },
    {
      title: "Đặt sân hôm nay",
      value: "542",
      change: "+67",
      icon: <TrendingUp className="h-6 w-6" />,
    },
    {
      title: "Doanh thu nền tảng",
      value: "245M",
      change: "+15%",
      icon: <DollarSign className="h-6 w-6" />,
    },
  ];

  const bookingTrend = [
    { date: "01/05", bookings: 420 },
    { date: "05/05", bookings: 480 },
    { date: "10/05", bookings: 520 },
    { date: "15/05", bookings: 580 },
    { date: "20/05", bookings: 610 },
    { date: "25/05", bookings: 650 },
    { date: "30/05", bookings: 540 },
  ];

  const revenueBySport = [
    { name: "Bóng đá", value: 65, color: "#2563EB" },
    { name: "Cầu lông", value: 20, color: "#10B981" },
    { name: "Quần vợt", value: 10, color: "#F59E0B" },
    { name: "Bóng rổ", value: 5, color: "#EF4444" },
  ];

  const pendingApprovals = [
    {
      id: 1,
      owner: "Nguyễn Văn A",
      email: "owner1@email.com",
      venue: "Sân bóng Phú Nhuận",
      date: "20/05/2024",
    },
    {
      id: 2,
      owner: "Trần Thị B",
      email: "owner2@email.com",
      venue: "Sân cầu lông Quận 3",
      date: "21/05/2024",
    },
  ];

  const recentComplaints = [
    {
      id: "CP001",
      user: "Lê Văn C",
      subject: "Sân không đúng mô tả",
      priority: "high",
      status: "open",
      date: "22/05/2024",
    },
    {
      id: "CP002",
      user: "Phạm Thị D",
      subject: "Chủ sân không phản hồi",
      priority: "medium",
      status: "in_progress",
      date: "21/05/2024",
    },
  ];

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="flex">
        {/* Sidebar */}
        <aside className="w-64 min-h-screen bg-sidebar border-r p-4">
          <h2 className="mb-6 px-3">Quản trị hệ thống</h2>
          <nav className="space-y-1">
            <Button variant="default" className="w-full justify-start" size="sm">
              <Home className="mr-3 h-4 w-4" />
              Dashboard
            </Button>
            <Button variant="ghost" className="w-full justify-start" size="sm">
              <Users className="mr-3 h-4 w-4" />
              Người dùng
            </Button>
            <Link href="/admin/owner-approvals" className="block w-full">
              <Button variant="ghost" className="w-full justify-start" size="sm">
                <Building className="mr-3 h-4 w-4" />
                Chủ sân
              </Button>
            </Link>
            <Link href="/admin/stadium-approvals" className="block w-full">
              <Button variant="ghost" className="w-full justify-start" size="sm">
                <MapPin className="mr-3 h-4 w-4" />
                Sân
              </Button>
            </Link>
            <Link href="/admin/sport-categories" className="block w-full">
              <Button variant="ghost" className="w-full justify-start" size="sm">
                <UserCog className="mr-3 h-4 w-4" />
                Danh mục
              </Button>
            </Link>
            <Link href="/admin/complaints" className="block w-full">
              <Button variant="ghost" className="w-full justify-start" size="sm">
                <AlertCircle className="mr-3 h-4 w-4" />
                Khiếu nại
              </Button>
            </Link>
            <Button variant="ghost" className="w-full justify-start" size="sm">
              <Settings className="mr-3 h-4 w-4" />
              Cài đặt
            </Button>
          </nav>
        </aside>

        {/* Main Content */}
        <main className="flex-1 p-8">
          <h1 className="text-3xl mb-8">Dashboard</h1>

          {/* KPIs */}
          <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-6 mb-8">
            {kpiData.map((kpi, idx) => (
              <Card key={idx}>
                <CardContent className="p-6">
                  <div className="text-primary bg-primary/10 p-3 rounded-lg w-fit mb-4">
                    {kpi.icon}
                  </div>
                  <div className="text-2xl mb-1">{kpi.value}</div>
                  <div className="text-sm text-muted-foreground mb-2">
                    {kpi.title}
                  </div>
                  <div className="text-sm text-green-600">{kpi.change}</div>
                </CardContent>
              </Card>
            ))}
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
            {/* Booking Trend */}
            <Card className="lg:col-span-2">
              <CardHeader>
                <h3>Lượt đặt sân 30 ngày qua</h3>
              </CardHeader>
              <CardContent>
                <ResponsiveContainer width="100%" height={300}>
                  <AreaChart data={bookingTrend}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="date" />
                    <YAxis />
                    <Tooltip />
                    <Area
                      type="monotone"
                      dataKey="bookings"
                      stroke="#2563EB"
                      fill="#2563EB"
                      fillOpacity={0.3}
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>

            {/* Revenue by Sport */}
            <Card>
              <CardHeader>
                <h3>Doanh thu theo thể loại</h3>
              </CardHeader>
              <CardContent>
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie
                      data={revenueBySport}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={(entry) => `${entry.value}%`}
                      outerRadius={80}
                      fill="#8884d8"
                      dataKey="value"
                    >
                      {revenueBySport.map((entry, index) => (
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

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Pending Approvals */}
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <h3>Chờ duyệt chủ sân</h3>
                  <Badge variant="destructive">
                    {pendingApprovals.length}
                  </Badge>
                </div>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {pendingApprovals.map((approval) => (
                    <div
                      key={approval.id}
                      className="flex items-center justify-between p-3 border rounded-lg"
                    >
                      <div className="flex-1">
                        <div className="font-medium">{approval.owner}</div>
                        <div className="text-sm text-muted-foreground">
                          {approval.venue}
                        </div>
                        <div className="text-xs text-muted-foreground">
                          {approval.date}
                        </div>
                      </div>
                      <div className="flex gap-2">
                        <Button size="sm" variant="default">
                          <CheckCircle className="h-4 w-4 mr-1" />
                          Duyệt
                        </Button>
                        <Button size="sm" variant="destructive">
                          Từ chối
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>

            {/* Recent Complaints */}
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <h3>Khiếu nại gần đây</h3>
                  <Badge variant="destructive">
                    {recentComplaints.length}
                  </Badge>
                </div>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {recentComplaints.map((complaint) => (
                    <div
                      key={complaint.id}
                      className="flex items-center justify-between p-3 border rounded-lg"
                    >
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                          <span className="font-mono text-sm">
                            {complaint.id}
                          </span>
                          <Badge
                            className={
                              complaint.priority === "high"
                                ? "bg-red-100 text-red-700"
                                : "bg-yellow-100 text-yellow-700"
                            }
                          >
                            {complaint.priority === "high"
                              ? "Cao"
                              : "Trung bình"}
                          </Badge>
                        </div>
                        <div className="font-medium">{complaint.subject}</div>
                        <div className="text-sm text-muted-foreground">
                          {complaint.user} - {complaint.date}
                        </div>
                      </div>
                      <Button size="sm" variant="outline">
                        Xử lý
                      </Button>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>
        </main>
      </div>
    </div>
  );
}

export default AdminDashboardPage;
