'use client'

import { useEffect, useState, useMemo } from "react";
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
import { stadiumService } from "@/lib/services/stadium";
import { StadiumResponse } from "@/types/stadium";
import {
  startOfWeek, endOfWeek,
  startOfMonth, endOfMonth,
  startOfYear, endOfYear,
  subMonths, subYears,
  format, parseISO, eachDayOfInterval,
} from "date-fns";
import { toast } from "sonner";

function RevenueReportPage() {
  const [dateRange, setDateRange] = useState("this-month");

  // Mặc định tháng này
  const [startDate, setStartDate] = useState(format(startOfMonth(new Date()), 'yyyy-MM-dd'));
  const [endDate, setEndDate] = useState(format(endOfMonth(new Date()), 'yyyy-MM-dd'));

  const [stadiumId, setStadiumId] = useState<number | undefined>(undefined);
  const [stadiums, setStadiums] = useState<StadiumResponse[]>([]);

  const [loading, setLoading] = useState(true);
  const [reportData, setReportData] = useState<RevenueReportResponse | null>(null);

  // Lấy danh sách sân của Owner để hiển thị dropdown filter
  useEffect(() => {
    stadiumService.getMyStadiums()
      .then((data) => setStadiums(data))
      .catch(() => {
        // Không chặn UX — chỉ log lỗi nhẹ, tính năng filter sân vẫn ẩn đi
        toast.error("Không thể tải danh sách sân");
      });
  }, []);

  const fetchReport = async (start: string, end: string, sid?: number) => {
    try {
      setLoading(true);
      const res = await reportService.getRevenueReport(start, end, sid);
      setReportData(res.result);
    } catch (error: unknown) {
      // RULE-02: Dùng unknown thay vì any
      const message = error instanceof Error ? error.message : "Không thể tải dữ liệu báo cáo";
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReport(startDate, endDate, stadiumId);
  }, [startDate, endDate, stadiumId]);

  const handleDateRangeChange = (value: string) => {
    setDateRange(value);
    const now = new Date();

    if (value === 'this-week') {
      setStartDate(format(startOfWeek(now, { weekStartsOn: 1 }), 'yyyy-MM-dd'));
      setEndDate(format(endOfWeek(now, { weekStartsOn: 1 }), 'yyyy-MM-dd'));
    } else if (value === 'this-month') {
      setStartDate(format(startOfMonth(now), 'yyyy-MM-dd'));
      setEndDate(format(endOfMonth(now), 'yyyy-MM-dd'));
    } else if (value === 'last-month') {
      const lastMonth = subMonths(now, 1);
      setStartDate(format(startOfMonth(lastMonth), 'yyyy-MM-dd'));
      setEndDate(format(endOfMonth(lastMonth), 'yyyy-MM-dd'));
    } else if (value === 'this-year') {
      // HIGH-05: Thêm filter theo năm
      setStartDate(format(startOfYear(now), 'yyyy-MM-dd'));
      setEndDate(format(endOfYear(now), 'yyyy-MM-dd'));
    } else if (value === 'last-year') {
      const lastYear = subYears(now, 1);
      setStartDate(format(startOfYear(lastYear), 'yyyy-MM-dd'));
      setEndDate(format(endOfYear(lastYear), 'yyyy-MM-dd'));
    }
    // 'custom': không tự đổi ngày — user sẽ nhập qua date input bên dưới
  };

  // QUALITY-03: Memoize chartData để tránh tính lại mỗi render
  const chartData = useMemo(() => {
    const allDays = eachDayOfInterval({
      start: parseISO(startDate),
      end: parseISO(endDate),
    });
    return allDays.map((day) => {
      const formattedDayStr = format(day, 'yyyy-MM-dd');
      const existingData = reportData?.details?.find((d) => d.date === formattedDayStr);
      return {
        date: format(day, 'dd/MM'),
        revenue: existingData ? existingData.revenue : 0,
      };
    });
  }, [reportData, startDate, endDate]);

  // QUALITY-03: Memoize summaryCards
  const summaryCards = useMemo(() => {
    const averagePerBooking = reportData?.totalBookings
      ? (reportData.totalRevenue / reportData.totalBookings)
      : 0;

    return [
      {
        title: "Tổng doanh thu",
        value: `${reportData?.totalRevenue?.toLocaleString('vi-VN') ?? 0}đ`,
        icon: <DollarSign className="h-6 w-6" />,
      },
      {
        title: "Tổng đặt sân",
        value: reportData?.totalBookings ?? 0,
        icon: <Calendar className="h-6 w-6" />,
      },
      {
        title: "Trung bình/đặt",
        value: `${Math.round(averagePerBooking).toLocaleString('vi-VN')}đ`,
        icon: <TrendingUp className="h-6 w-6" />,
      },
    ];
  }, [reportData]);

  const exportToExcel = () => {
    if (!reportData || !reportData.venueRevenues || reportData.venueRevenues.length === 0) {
      toast.error("Không có dữ liệu để xuất");
      return;
    }

    import("exceljs").then(async ({ Workbook }) => {
      const workbook = new Workbook();
      const sheet = workbook.addWorksheet("Doanh Thu");
      sheet.columns = [
        { width: 30 },
        { width: 15 },
        { width: 20 },
        { width: 18 },
        { width: 15 },
      ];

      sheet.addRow([`BÁO CÁO DOANH THU (${format(parseISO(startDate), 'dd/MM/yyyy')} - ${format(parseISO(endDate), 'dd/MM/yyyy')})`]);
      sheet.addRow([]);
      sheet.addRow(["Tên sân", "Số lượt đặt", "Doanh thu (VND)", "Tỷ lệ lấp đầy (%)", "Xu hướng"]);
      reportData.venueRevenues.forEach((v) => {
        sheet.addRow([
          v.stadiumName,
          v.totalBookings,
          v.totalRevenue.toLocaleString('vi-VN') + " đ",
          v.occupancy + "%",
          v.trend,
        ]);
      });

      const buffer = await workbook.xlsx.writeBuffer();
      const blob = new Blob([buffer], { type: "application/octet-stream" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `Bao_Cao_Doanh_Thu_${format(parseISO(startDate), 'yyyyMMdd')}_${format(parseISO(endDate), 'yyyyMMdd')}.xlsx`;
      link.click();
      URL.revokeObjectURL(url);
      toast.success("Xuất Excel thành công!");
    }).catch(() => {
      // RULE-03: Không dùng console.error trong production
      toast.error("Lỗi khi xuất Excel. Vui lòng thử lại.");
    });
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-3xl">Báo cáo doanh thu</h1>
        <div className="flex gap-4 flex-wrap">

          {/* HIGH-05: Dropdown lọc theo kỳ — có thêm Năm này / Năm ngoái */}
          <Select value={dateRange} onValueChange={handleDateRangeChange}>
            <SelectTrigger className="w-48">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="this-week">Tuần này</SelectItem>
              <SelectItem value="this-month">Tháng này</SelectItem>
              <SelectItem value="last-month">Tháng trước</SelectItem>
              <SelectItem value="this-year">Năm nay</SelectItem>
              <SelectItem value="last-year">Năm ngoái</SelectItem>
              <SelectItem value="custom">Tùy chỉnh</SelectItem>
            </SelectContent>
          </Select>

          {/* HIGH-06: Dropdown chọn sân */}
          {stadiums.length > 0 && (
            <Select
              value={stadiumId !== undefined ? String(stadiumId) : "all"}
              onValueChange={(val) => setStadiumId(val === "all" ? undefined : Number(val))}
            >
              <SelectTrigger className="w-52">
                <SelectValue placeholder="Tất cả sân" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Tất cả sân</SelectItem>
                {stadiums.map((s) => (
                  <SelectItem key={s.stadiumId} value={String(s.stadiumId)}>
                    {s.stadiumName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}

          <Button variant="outline" onClick={exportToExcel}>
            <Download className="mr-2 h-4 w-4" />
            Xuất Excel
          </Button>
        </div>
      </div>

      {/* HIGH-07: Custom date range picker — hiện khi chọn "Tùy chỉnh" */}
      {dateRange === 'custom' && (
        <div className="flex items-center gap-4 mb-6 p-4 border rounded-lg bg-muted/30">
          <div className="flex items-center gap-2">
            <label className="text-sm font-medium text-muted-foreground">Từ ngày</label>
            <input
              type="date"
              value={startDate}
              max={endDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="border rounded px-3 py-1.5 text-sm bg-background focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div className="flex items-center gap-2">
            <label className="text-sm font-medium text-muted-foreground">Đến ngày</label>
            <input
              type="date"
              value={endDate}
              min={startDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="border rounded px-3 py-1.5 text-sm bg-background focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
        </div>
      )}

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
              {chartData.some((d) => d.revenue > 0) ? (
                <ResponsiveContainer width="100%" height={400}>
                  <BarChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="date" />
                    <YAxis
                      width={80}
                      tickFormatter={(value) => {
                        if (value >= 1000000) {
                          return `${(value / 1000000).toFixed(1).replace('.0', '')} Tr`;
                        } else if (value >= 1000) {
                          return `${value / 1000}k`;
                        }
                        return String(value);
                      }}
                    />
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

          {/* Venue Breakdown */}
          {reportData?.venueRevenues && reportData.venueRevenues.length > 0 && (
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
                      {reportData.venueRevenues.map((venue, idx) => (
                        <tr key={idx} className="border-b">
                          <td className="p-3">{venue.stadiumName}</td>
                          <td className="p-3 text-center">{venue.totalBookings}</td>
                          <td className="p-3 text-right">
                            {venue.totalRevenue.toLocaleString('vi-VN')}đ
                          </td>
                          <td className="p-3 text-center">
                            <div className="flex items-center justify-center gap-2">
                              <div className="w-20 h-2 bg-muted rounded-full overflow-hidden">
                                <div
                                  className="h-full bg-primary"
                                  style={{ width: `${venue.occupancy || 0}%` }}
                                />
                              </div>
                              <span className="text-sm">{venue.occupancy || 0}%</span>
                            </div>
                          </td>
                          <td className="p-3 text-center">
                            <span
                              className={`text-sm font-medium ${
                                venue.trend === "N/A"
                                  ? "text-muted-foreground"
                                  : (venue.trend || "+0%").startsWith("+")
                                  ? "text-green-600"
                                  : "text-red-600"
                              }`}
                            >
                              {venue.trend || "+0%"}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>
          )}
        </>
      )}
    </div>
  );
}

export default RevenueReportPage;
