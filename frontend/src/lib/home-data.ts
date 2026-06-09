export type FeaturedVenue = {
  id: number;
  image: string;
  name: string;
  sportType: string;
  sportKey: string;
  price: number;
  rating: number;
  reviewCount: number;
  location: string;
  featured?: boolean;
  openHours?: string;
};

export const HOME_STATS = [
  { label: "Sân đối tác", value: "520+", icon: "venues" as const },
  { label: "Lượt đặt/tháng", value: "12K+", icon: "bookings" as const },
  { label: "Đánh giá TB", value: "4.8", icon: "rating" as const },
  { label: "Thành phố", value: "15+", icon: "cities" as const },
];

export const SPORT_CATEGORIES = [
  { key: "all", label: "Tất cả", emoji: "🏟️" },
  { key: "football", label: "Bóng đá", emoji: "⚽" },
  { key: "badminton", label: "Cầu lông", emoji: "🏸" },
  { key: "tennis", label: "Quần vợt", emoji: "🎾" },
  { key: "basketball", label: "Bóng rổ", emoji: "🏀" },
  { key: "pickleball", label: "Pickleball", emoji: "🏓" },
];

export const PLATFORM_HIGHLIGHTS = [
  {
    title: "Tìm sân trong 30 giây",
    description:
      "Bộ lọc thông minh theo vị trí, môn thể thao, khung giờ và mức giá — phù hợp lịch trình của bạn.",
    icon: "search" as const,
  },
  {
    title: "Đặt lịch & thanh toán an toàn",
    description:
      "Xác nhận slot real-time, thanh toán trực tuyến và nhận biên lai ngay trên ứng dụng.",
    icon: "payment" as const,
  },
  {
    title: "Cộng đồng thể thao",
    description:
      "Tìm đối thủ, tham gia sự kiện và chia sẻ đam mê cùng hàng ngàn người chơi.",
    icon: "community" as const,
  },
  {
    title: "Trợ lý AI đặt sân",
    description:
      "Gợi ý sân phù hợp theo thói quen chơi và thời tiết — tiết kiệm thời gian tìm kiếm.",
    icon: "ai" as const,
  },
];

export const FEATURED_VENUES: FeaturedVenue[] = [
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
    featured: true,
    openHours: "06:00 – 23:00",
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
    featured: true,
    openHours: "05:30 – 22:30",
  },
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
    openHours: "06:00 – 22:00",
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
    openHours: "07:00 – 21:00",
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
    featured: true,
    openHours: "06:00 – 22:00",
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
    openHours: "07:00 – 20:00",
  },
];
