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

export async function fetchMyBookings(): Promise<BookingHistoryItem[]> {
  const data = await get<BookingHistoryItem[]>("/bookings/me");
  return data.map((b) => ({
    ...b,
    price: typeof b.price === "number" ? b.price : Number(b.price),
  }));
}
