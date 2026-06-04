"use client";

import { useEffect, useState } from "react";
import type { Session } from "next-auth";
import { Loader2, AlertCircle } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { WelcomeBar } from "@/components/home/authenticated/WelcomeBar";
import { UpcomingBookingsSection } from "@/components/home/authenticated/UpcomingBookingsSection";
import { FavoriteVenuesSection } from "@/components/home/authenticated/FavoriteVenuesSection";
import { AiRecommendationsSection } from "@/components/home/authenticated/AiRecommendationsSection";
import { CommunityFeedSection } from "@/components/home/authenticated/CommunityFeedSection";
import { PersonalStatsSection } from "@/components/home/authenticated/PersonalStatsSection";
import {
  fetchHomeDashboard,
  mapVenueToCard,
  type HomeDashboardResponse,
} from "@/lib/home-api";
import { Button } from "@/components/ui/button";

type AuthenticatedHomePageProps = {
  user: NonNullable<Session["user"]>;
};

export function AuthenticatedHomePage({ user }: AuthenticatedHomePageProps) {
  const displayName =
    [user.firstName, user.lastName].filter(Boolean).join(" ") || user.email || "bạn";

  const [dashboard, setDashboard] = useState<HomeDashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadDashboard = () => {
    setLoading(true);
    setError(null);
    fetchHomeDashboard()
      .then(setDashboard)
      .catch((err: unknown) => {
        setError(err instanceof Error ? err.message : "Không tải được dữ liệu trang chủ.");
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadDashboard();
  }, []);

  if (loading) {
    return (
      <div className="flex min-h-screen flex-col bg-background">
        <Header />
        <div className="flex flex-1 flex-col items-center justify-center gap-3 py-24">
          <Loader2 className="h-10 w-10 animate-spin text-green-800" />
          <p className="text-sm text-muted-foreground">Đang tải dữ liệu của bạn...</p>
        </div>
      </div>
    );
  }

  if (error || !dashboard) {
    return (
      <div className="flex min-h-screen flex-col bg-background">
        <Header />
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-4 py-24">
          <AlertCircle className="h-12 w-12 text-destructive" />
          <p className="text-center text-muted-foreground">{error ?? "Lỗi không xác định"}</p>
          <Button onClick={loadDashboard} className="bg-green-800 hover:bg-green-900">
            Thử lại
          </Button>
        </div>
      </div>
    );
  }

  const favoriteVenues = dashboard.favoriteVenues.map(mapVenueToCard);
  const recommendedVenues = dashboard.recommendedVenues.map(mapVenueToCard);

  return (
    <div className="min-h-screen bg-background">
      <Header />
      <main>
        <WelcomeBar
          displayName={displayName}
          bookingCount={dashboard.totalBookingCount}
          favoriteCount={dashboard.favoriteVenueCount}
          rewardPoints={dashboard.rewardPoints}
        />
        <UpcomingBookingsSection bookings={dashboard.upcomingBookings} />
        <FavoriteVenuesSection venues={favoriteVenues} />
        <AiRecommendationsSection venues={recommendedVenues} />
        {dashboard.communityEvents.length > 0 && (
          <CommunityFeedSection events={dashboard.communityEvents} />
        )}
        <PersonalStatsSection
          totalHours={dashboard.personalStats.totalHours}
          venuesVisited={dashboard.personalStats.venuesVisited}
          favoriteSport={dashboard.personalStats.favoriteSport}
        />
      </main>
      <Footer />
    </div>
  );
}
