import { get, post, put } from "@/lib/api";

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

function buildQueryString(page: number, size: number, status?: string): string {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  if (status && status !== "all") {
    query.append("status", status);
  }
  return query.toString();
}

/**
 * Lấy lịch sử đặt sân của customer — hỗ trợ phân trang và lọc theo trạng thái.
 * Backend trả về PageResponse<CustomerBookingHistoryDto>.
 */
export async function fetchMyBookings(
  page = 0,
  size = 10,
  status?: string
): Promise<BookingPageResult> {
  const queryString = buildQueryString(page, size, status);
  const data = await get<PageResponse<BookingHistoryItem>>(
    `/bookings/me?${queryString}`
  );

  const bookings = (data.content ?? []).map((b) => ({
    ...b,
    price: typeof b.price === "number" ? b.price : Number(b.price),
  }));

  return {
    bookings,
    totalElements: data.totalElements,
    totalPages: data.totalPages,
    pageNo: data.pageNumber,
    last: data.last,
  };
}

type OwnerBookingDto = {
  bookingId: string | number;
  stadium?: {
    stadiumName: string;
    sportType: string;
    address: string;
  };
  slot?: {
    startTime: string;
    endTime: string;
  };
  totalPrice: number | string;
  bookingStatus: string;
};

export async function fetchOwnerBookings(
  page = 0,
  size = 10,
  status?: string
): Promise<BookingPageResult> {
  const queryString = buildQueryString(page, size, status);
  const data = await get<PageResponse<OwnerBookingDto>>(
    `/owner/bookings/page?${queryString}`
  );

  const bookings: BookingHistoryItem[] = (data.content ?? []).map((b) => ({
    id: String(b.bookingId),
    displayId: `#${b.bookingId}`,
    venue: b.stadium?.stadiumName || "Sân chưa biết",
    sportType: b.stadium?.sportType || "Khác",
    imageUrl: "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=300&auto=format&fit=crop",
    date: b.slot?.startTime ? new Date(b.slot.startTime).toLocaleDateString("vi-VN") : "Chưa có",
    time: b.slot?.startTime && b.slot?.endTime 
          ? `${new Date(b.slot.startTime).toLocaleTimeString("vi-VN", { hour: '2-digit', minute: '2-digit' })} - ${new Date(b.slot.endTime).toLocaleTimeString("vi-VN", { hour: '2-digit', minute: '2-digit' })}`
          : "Chưa có",
    location: b.stadium?.address || "Chưa có",
    price: typeof b.totalPrice === "number" ? b.totalPrice : Number(b.totalPrice),
    status: (b.bookingStatus?.toLowerCase() === "confirmed" ? "confirmed" : 
             b.bookingStatus?.toLowerCase() === "completed" ? "completed" :
             b.bookingStatus?.toLowerCase() === "cancelled" ? "cancelled" : "pending") as BookingHistoryItem["status"],
  }));

  return {
    bookings,
    totalElements: data.totalElements,
    totalPages: data.totalPages,
    pageNo: data.pageNo ?? data.pageNumber,
    last: data.last,
  };
}

export type BookingDetailItem = {
  id: string;
  displayId: string;
  venueName: string;
  sportType: string;
  imageUrl: string;
  playDate: string;
  startTime: string;
  endTime: string;
  address: string;
  totalPrice: number;
  status: "pending" | "pending_payment" | "confirmed" | "completed" | "cancelled";
  paymentStatus: string;
  createdAt: string;
  note: string | null;
};

export async function fetchBookingDetail(id: string | number): Promise<BookingDetailItem> {
  const data = await get<any>(`/bookings/${id}`);

  return {
    id: String(data.bookingId),
    displayId: data.displayId,
    venueName: data.stadium?.stadiumName || "Sân chưa biết",
    sportType: data.stadium?.sportType || "Khác",
    imageUrl: data.stadium?.imageUrl || "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=300&auto=format&fit=crop",
    playDate: data.reservationDate || "Chưa rõ",
    startTime: data.slot?.startTime || "Chưa rõ",
    endTime: data.slot?.endTime || "Chưa rõ",
    address: data.stadium?.address || "Chưa rõ",
    totalPrice: typeof data.totalPrice === "number" ? data.totalPrice : Number(data.totalPrice),
    status: data.status,
    paymentStatus: data.paymentStatus,
    createdAt: data.createdAt || "Chưa rõ",
    note: data.note || null,
  };
}

// ── UC-CUS-01: Single booking ───────────────────────────────────────────────

/** Một khung giờ của sân kèm cờ availability cho một ngày cụ thể. */
export type SlotAvailability = {
  slotId: number;
  stadiumId: number;
  /** HH:mm hoặc HH:mm:ss */
  startTime: string;
  /** HH:mm hoặc HH:mm:ss */
  endTime: string;
  pricePerSlot: number;
  slotStatus: "AVAILABLE" | "MAINTENANCE" | "BOOKED" | string;
  /** true = còn trống và chưa qua giờ. */
  available: boolean;
};

/** Payload tạo đơn đặt sân đơn lẻ (UC-CUS-01). */
export type AccessoryItemPayload = {
  accessoryId: number;
  quantity: number;
};

export type CreateBookingPayload = {
  stadiumId: number;
  slotId: number;
  /** ISO yyyy-MM-dd */
  reservationDate: string;
  note?: string;
  /** Optional: phụ kiện kèm theo. Server tự tính unitPrice — không gửi. */
  accessories?: AccessoryItemPayload[];
};

/** Response trả về sau khi tạo booking thành công (BookingDetailResponse). */
export type CreateBookingResponse = {
  bookingId: number;
  displayId: string;
  reservationDate: string;
  slot: { slotId: number; startTime: string; endTime: string };
  stadium: { stadiumId: number; stadiumName: string; address: string };
  totalPrice: number;
  status: string;
  paymentStatus: string;
  note: string | null;
};

/**
 * Lấy slot của sân kèm cờ availability cho ngày cụ thể — FE dùng để render UI.
 * `get()` đã tự động thêm prefix `/api/v1`, nên path chỉ là `/stadiums/{id}/slots`.
 */
export async function getSlotsByDate(
  stadiumId: number,
  date: string
): Promise<SlotAvailability[]> {
  const data = await get<SlotAvailability[]>(
    `/stadiums/${stadiumId}/slots?date=${encodeURIComponent(date)}`
  );
  return data ?? [];
}

// ── UC-CUS-01: Weekly schedule ──────────────────────────────────────────────

/** Trạng thái của một slot trong weekly grid. */
export type WeeklySlotStatus = "AVAILABLE" | "BOOKED" | "PAST" | "OWNER_CLOSED" | "MAINTENANCE";

/** Một khung giờ của sân trong weekly grid — kèm trạng thái cho một ngày cụ thể. */
export type WeeklySlotItem = {
  slotId: number;
  /** HH:mm */
  startTime: string;
  /** HH:mm */
  endTime: string;
  price: number;
  status: WeeklySlotStatus;
};

/** Một ngày trong weekly grid — kèm tên thứ tiếng Việt. */
export type WeeklySlotDay = {
  /** ISO yyyy-MM-dd */
  date: string;
  /** "Thứ 2" / "Chủ nhật" / ... */
  dayName: string;
  slots: WeeklySlotItem[];
};

/**
 * Response của `GET /api/v1/stadiums/{id}/weekly-slots?weekStart=YYYY-MM-DD`.
 *
 * Trả về 7 ngày (thứ 2 → chủ nhật) của tuần chứa {@link weekStart}.
 * BE tự snap về thứ 2 nếu FE truyền ngày khác.
 */
export type WeeklySlotsResponse = {
  /** ISO yyyy-MM-dd — luôn là thứ 2. */
  weekStart: string;
  /** ISO yyyy-MM-dd — luôn là chủ nhật (weekStart + 6). */
  weekEnd: string;
  days: WeeklySlotDay[];
};

/**
 * Lấy lịch khung giờ theo tuần của một sân.
 * Public endpoint — không yêu cầu auth.
 *
 * @param stadiumId ID sân
 * @param weekStart một ngày bất kỳ trong tuần (ISO yyyy-MM-dd); BE sẽ snap về thứ 2.
 */
export async function getWeeklySlots(
  stadiumId: number,
  weekStart: string
): Promise<WeeklySlotsResponse> {
  return get<WeeklySlotsResponse>(
    `/stadiums/${stadiumId}/weekly-slots?weekStart=${encodeURIComponent(weekStart)}`
  );
}

/**
 * Tạo đơn đặt sân đơn lẻ — POST /api/v1/bookings.
 * Trả 409 nếu slot đã bị đặt active trên cùng ngày; 400 nếu đã qua giờ.
 *
 * @param idempotencyKey UUID sinh 1 lần khi mở form — dùng generateIdempotencyKey().
 *   Server dùng key này để dedup double-submit: nếu gửi lại cùng key, trả booking cũ thay vì tạo mới.
 */
export async function createBooking(
  payload: CreateBookingPayload,
  idempotencyKey?: string
): Promise<CreateBookingResponse> {
  return post<CreateBookingResponse>("/bookings", payload, {
    headers: idempotencyKey ? { "X-Idempotency-Key": idempotencyKey } : undefined,
  });
}

export type BookingDetailResponse = CreateBookingResponse;

// ── UC-CUS-03: Cancel booking ───────────────────────────────────────────────

/**
 * Hủy một đơn đặt sân — PUT /api/v1/bookings/{bookingId}/cancel.
 * Customer (chủ booking) hoặc Owner (chủ sân) đều có thể gọi.
 *
 * @param bookingId id của booking cần hủy.
 * @param reason   lý do hủy (tùy chọn, tối đa 255 ký tự — server validate).
 * @returns booking detail sau khi hủy (status=CANCELLED, cancelReason đã lưu).
 * @throws Error với message từ BE nếu booking không tồn tại / không có quyền /
 *         đang ở trạng thái COMPLETED hoặc CANCELLED.
 */
export async function cancelBooking(
  bookingId: number,
  reason?: string
): Promise<BookingDetailResponse> {
  return put<BookingDetailResponse>(
    `/bookings/${bookingId}/cancel`,
    { reason: reason || null }
  );
}

export type RefundPreviewResponse = {
  bookingId: number;
  stadiumName: string;
  customerName: string;
  playTime: string;
  originalPrice: number;
  refundAmount: number;
  refundPercentage: number;
  bookingStatus: string;
  paymentStatus: string;
  processedAt: string;
  reason: string;
};

/**
 * Lấy thông tin xem trước hoàn tiền cho customer.
 */
export async function previewRefund(bookingId: number): Promise<RefundPreviewResponse> {
  return get<RefundPreviewResponse>(`/bookings/${bookingId}/refund-preview`);
}

