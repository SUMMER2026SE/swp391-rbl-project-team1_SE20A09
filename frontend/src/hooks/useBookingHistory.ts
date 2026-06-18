import { useCallback, useEffect, useState } from "react";
import { fetchMyBookings, fetchOwnerBookings, BookingPageResult } from "@/lib/bookings-api";

export function useBookingHistory(isOwner: boolean, activeTab: string) {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<BookingPageResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const pageSize = 10;

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    const fetchFn = isOwner ? fetchOwnerBookings : fetchMyBookings;

    fetchFn(page, pageSize, activeTab)
      .then(setData)
      .catch((err) => setError(err instanceof Error ? err.message : "Không tải được lịch sử đặt sân."))
      .finally(() => setLoading(false));
  }, [isOwner, page, activeTab]);

  useEffect(() => {
    load();
  }, [load]);

  // Reset page to 0 when activeTab changes
  useEffect(() => {
    setPage(0);
  }, [activeTab]);

  return { 
    bookings: data?.bookings ?? [],
    totalPages: data?.totalPages ?? 0,
    totalElements: data?.totalElements ?? 0,
    loading, 
    error, 
    page, 
    setPage, 
    pageSize, 
    reload: load 
  };
}
