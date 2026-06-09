import type { FeaturedVenue } from "@/lib/home-data";

export type BookingStatus = "confirmed" | "pending";

export type UpcomingBooking = {
  id: string;
  venueName: string;
  sportType: string;
  location: string;
  date: string;
  time: string;
  status: BookingStatus;
  image: string;
};

export type CommunityEvent = {
  id: string;
  title: string;
  sportType: string;
  datetime: string;
  location: string;
  slotsNeeded: number;
};

export const MOCK_UPCOMING_BOOKINGS: UpcomingBooking[] = [
  {
    id: "BK2026001",
    venueName: "Sân bóng Thành Công",
    sportType: "Bóng đá",
    location: "Quận 1, TP.HCM",
    date: "28/05/2026",
    time: "18:00 – 20:00",
    status: "confirmed",
    image:
      "https://images.unsplash.com/photo-1705593813682-033ee2991df6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400",
  },
  {
    id: "BK2026002",
    venueName: "Elite Badminton Club",
    sportType: "Cầu lông",
    location: "Quận Bình Thạnh, TP.HCM",
    date: "30/05/2026",
    time: "07:00 – 09:00",
    status: "pending",
    image:
      "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400",
  },
];

export const MOCK_FAVORITE_VENUES: FeaturedVenue[] = [
  {
    id: 1,
    image:
      "https://images.unsplash.com/photo-1705593813682-033ee2991df6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    name: "Sân bóng Thành Công",
    sportType: "Bóng đá",
    sportKey: "football",
    price: 500000,
    rating: 4.8,
    reviewCount: 124,
    location: "Quận 1, TP.HCM",
  },
  {
    id: 5,
    image:
      "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    name: "Elite Badminton Club",
    sportType: "Cầu lông",
    sportKey: "badminton",
    price: 180000,
    rating: 4.8,
    reviewCount: 142,
    location: "Quận Bình Thạnh, TP.HCM",
  },
  {
    id: 2,
    image:
      "https://images.unsplash.com/photo-1764703666646-acc2f7d48857?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    name: "Arena Sports Center",
    sportType: "Bóng đá",
    sportKey: "football",
    price: 700000,
    rating: 4.9,
    reviewCount: 256,
    location: "Quận 3, TP.HCM",
  },
];

export const MOCK_RECOMMENDED_VENUES: FeaturedVenue[] = [
  {
    id: 3,
    image:
      "https://images.unsplash.com/photo-1767729790212-661953ecaa90?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    name: "Sân Vận Động Quận 7",
    sportType: "Bóng đá",
    sportKey: "football",
    price: 600000,
    rating: 4.7,
    reviewCount: 89,
    location: "Quận 7, TP.HCM",
  },
  {
    id: 6,
    image:
      "https://images.unsplash.com/photo-1554068865-524cefe5f68f?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    name: "Green Court Tennis",
    sportType: "Quần vợt",
    sportKey: "tennis",
    price: 320000,
    rating: 4.5,
    reviewCount: 38,
    location: "Quận 2, TP.HCM",
  },
  {
    id: 4,
    image:
      "https://images.unsplash.com/photo-1765305460539-edf7a0838dad?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
    name: "Sân bóng Phú Mỹ Hưng",
    sportType: "Bóng đá",
    sportKey: "football",
    price: 550000,
    rating: 4.6,
    reviewCount: 67,
    location: "Quận 7, TP.HCM",
  },
];

export const MOCK_COMMUNITY_EVENTS: CommunityEvent[] = [
  {
    id: "ev1",
    title: "Giao hữu bóng đá 7 người",
    sportType: "Bóng đá",
    datetime: "CN, 01/06/2026 · 08:00",
    location: "Quận 7, TP.HCM",
    slotsNeeded: 3,
  },
  {
    id: "ev2",
    title: "Cầu lông đôi nam nữ",
    sportType: "Cầu lông",
    datetime: "T4, 04/06/2026 · 19:00",
    location: "Bình Thạnh, TP.HCM",
    slotsNeeded: 2,
  },
];

export type RecommendationFilter = "nearby" | "price" | "rating";

export const RECOMMENDATION_FILTERS: { key: RecommendationFilter; label: string }[] = [
  { key: "nearby", label: "Gần bạn" },
  { key: "price", label: "Giá tốt" },
  { key: "rating", label: "Đánh giá cao" },
];

export const MOCK_PERSONAL_STATS = {
  totalHours: 48,
  venuesVisited: 7,
  favoriteSport: "Bóng đá",
};
