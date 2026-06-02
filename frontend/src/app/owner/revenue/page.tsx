'use client'

import { useEffect, useState } from "react";
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
import { Download, TrendingUp, Calendar, DollarSign, Loader2 } from "lucide-react";
import { reportService, RevenueReportResponse } from "@/lib/services/report";
import { startOfWeek, endOfWeek, startOfMonth, endOfMonth, subMonths, format, parseISO, eachDayOfInterval } from "date-fns";
import { toast } from "sonner";

function RevenueReportPage() {
  const [dateRange, setDateRange] = useState("this-month");
  
  // Default to this month
  const [startDate, setStartDate] = useState(format(startOfMonth(new Date()), 'yyyy-MM-dd'));
  const [endDate, setEndDate] = useState(format(endOfMonth(new Date()), 'yyyy-MM-dd'));
  
  const [loading, setLoading] = useState(true);
  const [reportData, setReportData] = useState<RevenueReportResponse | null>(null);

  const fetchReport = async (start: string, end: string) => {
    try {
      setLoading(true);
      const res = await reportService.getRevenueReport(start, end);
      setReportData(res.result);
    } catch (error: any) {
      toast.error(error.message || "Không thể tải dữ liệu báo cáo");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReport(startDate, endDate);
  }, [startDate, endDate]);

  const handleDateRangeChange = (value: string) => {
    setDateRange(value);
    const now = new Date();
    
    let newStart = startDate;
    let newEnd = endDate;

    if (value === 'this-week') {
      newStart = format(startOfWeek(now, { weekStartsOn: 1 }), 'yyyy-MM-dd');
      newEnd = format(endOfWeek(now, { weekStartsOn: 1 }), 'yyyy-MM-dd');
    } else if (value === 'this-month') {
      newStart = format(startOfMonth(now), 'yyyy-MM-dd');
      newEnd = format(endOfMonth(now), 'yyyy-MM-dd');
    } else if (value === 'last-month') {
      const lastMonth = subMonths(now, 1);
      newStart = format(startOfMonth(lastMonth), 'yyyy-MM-dd');
      newEnd = format(endOfMonth(lastMonth), 'yyyy-MM-dd');
    }
    
    if (value !== 'custom') {
      setStartDate(newStart);
      setEndDate(newEnd);
    }
  };

  // Format data for Recharts (generate all dates in interval to prevent UI layout issues)
  const allDays = eachDayOfInterval({
    start: parseISO(startDate),
    end: parseISO(endDate)
  });

  const chartData = allDays.map(day => {
    const formattedDayStr = format(day, 'yyyy-MM-dd');
    const existingData = reportData?.details?.find(d => d.date === formattedDayStr);
    return {
      date: format(day, 'dd/MM'),
      revenue: existingData ? existingData.revenue : 0
    };
  });

  const averagePerBooking = reportData?.totalBookings 
    ? (reportData.totalRevenue / reportData.totalBookings) 
    : 0;

  const summaryCards = [
    { 
      title: "Tổng doanh thu", 
      value: `${reportData?.totalRevenue?.toLocaleString('vi-VN') || 0}đ`, 
      icon: <DollarSign className="h-6 w-6" /> 
    },
    { 
      title: "Tổng đặt sân", 
      value: reportData?.totalBookings || 0, 
      icon: <Calendar className="h-6 w-6" /> 
    },
    { 
      title: "Trung bình/đặt", 
      value: `${Math.round(averagePerBooking).toLocaleString('vi-VN')}đ`, 
      icon: <TrendingUp className="h-6 w-6" /> 
    },
  ];

  return (
    <div className="container mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl">Báo cáo doanh thu</h1>
          <div className="flex gap-4">
            <Select value={dateRange} onValueChange={handleDateRangeChange}>
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

        {loading ? (
          <div className="flex justify-center items-center h-64">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
          </div>
        ) : (
          <>
            {/* Summary Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
              {summaryCards.map((item, idx) => (
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
                <h3>Doanh thu theo ngày ({format(parseISO(startDate), 'dd/MM/yyyy')} - {format(parseISO(endDate), 'dd/MM/yyyy')})</h3>
              </CardHeader>
              <CardContent>
                {chartData.length > 0 ? (
                  <ResponsiveContainer width="100%" height={400}>
                    <BarChart data={chartData}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="date" />
                      <YAxis />
                      <Tooltip
                        formatter={(value) => `${Number(value).toLocaleString('vi-VN')}đ`}
                      />
                      <Bar dataKey="revenue" fill="#2563EB" radius={[4, 4, 0, 0]} maxBarSize={60} />
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="flex justify-center items-center h-[400px] text-muted-foreground">
                    Không có dữ liệu doanh thu trong khoảng thời gian này.
                  </div>
                )}
              </CardContent>
            </Card>
          </>
        )}
      </div>
  );
}

export default RevenueReportPage;
