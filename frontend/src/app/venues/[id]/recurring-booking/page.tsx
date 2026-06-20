"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useSession } from "next-auth/react";
import { useQuery } from "@tanstack/react-query";
import { Calendar, ChevronLeft, MapPin, Star } from "lucide-react";

import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

import { getVenueDetail, type VenueDetail } from "@/lib/api/venue";
import { RecurringBookingForm } from "@/components/venues/RecurringBookingForm";

/**
 * UC-CUS-01: Trang đặt sân định kỳ.
 * - Guest: render thông báo + link tới /login?redirect=...
 * - Customer (và các role khác): render form đặt định kỳ.
 *   Backend sẽ từ chối (403) nếu role khác Customer gọi API.
 */
export default function RecurringBookingPage() {
  const params = useParams<{ id: string }>();
  const venueId = parseInt(params.id, 10);
  const { data: session, status } = useSession();
  const [mounted, setMounted] = useState(false);

  // Tránh hydration mismatch — useSession cần client
  useEffect(() => setMounted(true), []);

  const {
    data: venue,
    isLoading,
    error,
  } = useQuery<VenueDetail>({
    queryKey: ["venue-detail", venueId],
    queryFn: () => getVenueDetail(venueId),
    enabled: Number.isFinite(venueId),
    staleTime: 300_000,
  });

  if (!Number.isFinite(venueId)) {
    return (
      <div className="min-h-screen bg-white">
        <Header />
        <div className="container mx-auto p-8 max-w-md text-center">
          <h2 className="text-lg font-medium">ID sân không hợp lệ</h2>
          <Button asChild className="mt-4">
            <Link href="/search">Quay lại tìm kiếm</Link>
          </Button>
        </div>
      </div>
    );
  }

  if (isLoading || !mounted) {
    return (
      <div className="min-h-screen bg-white">
        <Header />
        <div className="container mx-auto px-4 py-8 max-w-5xl space-y-4">
          <Skeleton className="h-8 w-2/3" />
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-48 w-full" />
          <Skeleton className="h-48 w-full" />
        </div>
      </div>
    );
  }

  if (error || !venue) {
    return (
      <div className="min-h-screen bg-white">
        <Header />
        <div className="container mx-auto p-8 max-w-md text-center">
          <h2 className="text-lg font-medium">Không tìm thấy sân</h2>
          <p className="text-sm text-muted-foreground mt-1">
            Sân bạn đang tìm không tồn tại hoặc đã bị xóa.
          </p>
          <Button asChild className="mt-4 bg-emerald-700 hover:bg-emerald-800">
            <Link href="/search">Quay lại tìm kiếm</Link>
          </Button>
        </div>
      </div>
    );
  }

  // Guest → yêu cầu đăng nhập trước khi đặt
  if (status !== "authenticated") {
    const redirect = `/venues/${venueId}/recurring-booking`;
    return (
      <div className="min-h-screen bg-white">
        <Header />
        <div className="container mx-auto px-4 py-10 max-w-2xl">
          <Card className="border-amber-200 bg-amber-50/40">
            <CardContent className="p-8 text-center space-y-3">
              <Calendar className="h-10 w-10 mx-auto text-amber-600" />
              <h2 className="text-xl font-semibold">
                Bạn cần đăng nhập để đặt sân định kỳ
              </h2>
              <p className="text-sm text-muted-foreground">
                Vui lòng đăng nhập tài khoản Customer để tiếp tục. Sau khi đăng
                nhập, bạn sẽ được đưa trở lại trang này.
              </p>
              <Button
                asChild
                className="mt-2 bg-emerald-700 hover:bg-emerald-800"
              >
                <Link href={`/login?redirect=${encodeURIComponent(redirect)}`}>
                  Đăng nhập ngay
                </Link>
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  // Authenticated — render form
  return (
    <div className="min-h-screen bg-gradient-to-b from-emerald-50/30 via-white to-white">
      <Header />

      <div className="container mx-auto px-4 py-6 max-w-5xl">
        {/* Breadcrumb / back link */}
        <Button
          variant="ghost"
          size="sm"
          asChild
          className="mb-3 -ml-2 text-slate-600"
        >
          <Link href={`/venues/${venueId}`}>
            <ChevronLeft className="mr-1 h-4 w-4" />
            Quay lại trang sân
          </Link>
        </Button>

        <div className="mb-6">
          <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight text-slate-900">
            Đặt sân định kỳ
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Tạo lịch đặt lặp lại theo tuần cho{" "}
            <span className="font-medium text-slate-700">
              {venue.stadiumName}
            </span>
            .
          </p>
        </div>

        {/* Venue summary card */}
        <Card className="mb-6 border-emerald-200/70 shadow-sm">
          <CardContent className="p-4 flex flex-col md:flex-row md:items-center gap-3 md:gap-6">
            <div className="flex-1 min-w-0">
              <h2 className="text-lg font-semibold truncate">
                {venue.stadiumName}
              </h2>
              <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-muted-foreground mt-1">
                <span className="flex items-center gap-1">
                  <MapPin className="h-3.5 w-3.5" />
                  {venue.address}
                </span>
                <span className="flex items-center gap-1">
                  <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-400" />
                  {venue.averageRating?.toFixed(1) ?? "0.0"} ({venue.totalReviews})
                </span>
                <span className="flex items-center gap-1">
                  <Calendar className="h-3.5 w-3.5" />
                  {venue.sportName}
                </span>
              </div>
            </div>
            <div className="text-right">
              <p className="text-xs text-muted-foreground">Giá tham khảo</p>
              <p className="text-xl font-bold text-emerald-700">
                {venue.pricePerHour?.toLocaleString("vi-VN")}đ
                <span className="text-sm font-normal text-muted-foreground">
                  /giờ
                </span>
              </p>
            </div>
          </CardContent>
        </Card>

        {/* Form */}
        <RecurringBookingForm venue={venue} />
      </div>
    </div>
  );
}