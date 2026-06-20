import { post } from "@/lib/api";

/**
 * UC-CUS-02: Khởi tạo thanh toán VNPay cho một booking.
 * Backend trả về URL đã ký HMAC-SHA512 — frontend redirect browser sang đó.
 * Số tiền đọc từ DB phía backend, không nhận từ client.
 */
export async function initiateVnpayPayment(
  bookingId: number
): Promise<{ paymentUrl: string }> {
  return post<{ paymentUrl: string }>(`/bookings/${bookingId}/pay`);
}
