import { get } from "@/lib/api";

export type BookingHistoryItem = {
  id: string;
  displayId: string;
  venue: string;
  sportType: string;
  imageUrl: string;
  date: string;
  time: string;
  location: string;
  price: number;
  status: "pending" | "confirmed" | "completed" | "cancelled";
};

/** Cấu trúc PageResponse trả về từ backend */
type PageResponse<T> = {
  content: T[];
  pageNo: number;
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
};

export type BookingPageResult = {
  bookings: BookingHistoryItem[];
  totalElements: number;
  totalPages: number;
  pageNo: number;
  last: boolean;
};

/**
 * Lấy lịch sử đặt sân của customer — hỗ trợ phân trang.
 * Backend trả về PageResponse<CustomerBookingHistoryDto>.
 */
export async function fetchMyBookings(
  page = 0,
  size = 50
): Promise<BookingPageResult> {
  const data = await get<PageResponse<BookingHistoryItem>>(
    `/bookings/me?page=${page}&size=${size}`
  );

  const bookings = (data.content ?? []).map((b) => ({
    ...b,
    price: typeof b.price === "number" ? b.price : Number(b.price),
  }));

  return {
    bookings,
    totalElements: data.totalElements,
    totalPages: data.totalPages,
    pageNo: data.pageNo,
    last: data.last,
  };
}

export async function fetchOwnerBookings(
  page = 0,
  size = 50
): Promise<BookingPageResult> {
  const data = await get<PageResponse<any>>(
    `/owner/bookings/page?page=${page}&size=${size}`
  );

  const bookings: BookingHistoryItem[] = (data.content ?? []).map((b) => ({
    id: String(b.bookingId),
    displayId: `#${b.bookingId}`,
    venue: b.stadium?.stadiumName || "Sân chưa biết",
    sportType: b.stadium?.sportType || "Khác",
    imageUrl: "/images/stadium1.jpg", // Placeholder if no image provided
    date: b.slot?.startTime ? new Date(b.slot.startTime).toLocaleDateString("vi-VN") : "Chưa có",
    time: b.slot?.startTime && b.slot?.endTime 
          ? `${new Date(b.slot.startTime).toLocaleTimeString("vi-VN", { hour: '2-digit', minute: '2-digit' })} - ${new Date(b.slot.endTime).toLocaleTimeString("vi-VN", { hour: '2-digit', minute: '2-digit' })}`
          : "Chưa có",
    location: b.stadium?.address || "Chưa có",
    price: typeof b.totalPrice === "number" ? b.totalPrice : Number(b.totalPrice),
    status: (b.bookingStatus?.toLowerCase() === "confirmed" ? "confirmed" : 
             b.bookingStatus?.toLowerCase() === "completed" ? "completed" :
             b.bookingStatus?.toLowerCase() === "cancelled" ? "cancelled" : "pending") as any,
  }));

  return {
    bookings,
    totalElements: data.totalElements,
    totalPages: data.totalPages,
    pageNo: data.pageNo ?? data.pageNumber,
    last: data.last,
  };
}
