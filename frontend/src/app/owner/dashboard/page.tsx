import { getServerSession } from "next-auth/next";
import { authOptions } from "@/lib/auth";
import { get } from "@/lib/api";
import OwnerDashboardClient from "./components/OwnerDashboardClient";

interface ApiResponse<T> {
  code: number;
  message: string;
  result: T;
}

export default async function OwnerDashboardPage() {
  const session = await getServerSession(authOptions);
  const token = session?.accessToken;
  const headers = token ? { Authorization: `Bearer ${token}` } : {};

  // Calculate 30 days range
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - 30);
  const formatDate = (d: Date) => d.toISOString().split("T")[0];
  const startDateStr = formatDate(start);
  const endDateStr = formatDate(end);

  let initialReport = null;
  let initialSummary = null;
  let initialPendingBookings: any[] = [];

  try {
    const [reportRes, bookingsResponse, summaryRes] = await Promise.all([
      get<ApiResponse<any>>(`/owner/reports/revenue?startDate=${startDateStr}&endDate=${endDateStr}`, { headers }),
      get<any[] | { content?: any[] }>("/owner/bookings?page=0&size=100&status=PENDING", { headers }),
      get<ApiResponse<any>>("/owner/reports/summary", { headers }),
    ]);

    initialReport = reportRes?.result || null;
    initialSummary = summaryRes?.result || null;

    const bookingsList = Array.isArray(bookingsResponse)
      ? bookingsResponse
      : Array.isArray(bookingsResponse?.content)
        ? bookingsResponse.content
        : [];
    initialPendingBookings = bookingsList.filter(
      (booking: any) => booking.status?.toLowerCase() === "pending"
    );
  } catch (error) {
    console.error("Server-side owner dashboard fetch failed:", error);
  }

  return (
    <OwnerDashboardClient
      initialReport={initialReport}
      initialSummary={initialSummary}
      initialPendingBookings={initialPendingBookings}
    />
  );
}
