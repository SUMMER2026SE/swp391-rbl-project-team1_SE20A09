import { get } from "@/lib/api";
import type { FeaturedVenue } from "@/lib/home-data";

export type UpcomingBooking = {
  id: string;
  venueName: string;
  sportType: string;
  location: string;
  date: string;
  time: string;
  status: "confirmed" | "pending";
  imageUrl: string;
};

export type CommunityEvent = {
  id: string;
  title: string;
  sportType: string;
  datetime: string;
  location: string;
  slotsNeeded: number;
};

export type HomeDashboardResponse = {
  totalBookingCount: number;
  recentlyPlayedVenueCount: number;
  rewardPoints: number;
  upcomingBookings: UpcomingBooking[];
  recentlyPlayedVenues: VenueSummaryApi[];
  recommendedVenues: VenueSummaryApi[];
  communityEvents: CommunityEvent[];
  personalStats: {
    totalHours: number;
    venuesVisited: number;
    favoriteSport: string;
  };
};

type VenueSummaryApi = {
  id: number;
  name: string;
  sportType: string;
  sportKey: string;
  pricePerHour: number;
  rating: number;
  reviewCount: number;
  location: string;
  imageUrl: string;
  saved: boolean;
};

export function mapVenueToCard(venue: VenueSummaryApi): FeaturedVenue {
  return {
    id: venue.id,
    image: venue.imageUrl,
    name: venue.name,
    sportType: venue.sportType,
    sportKey: venue.sportKey,
    price: venue.pricePerHour,
    rating: venue.rating,
    reviewCount: venue.reviewCount,
    location: venue.location,
  };
}

export async function fetchHomeDashboard(): Promise<HomeDashboardResponse> {
  return get<HomeDashboardResponse>("/home/dashboard");
}
