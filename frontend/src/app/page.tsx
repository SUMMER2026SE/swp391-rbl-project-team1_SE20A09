'use client'

import { Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { SearchBar } from "@/components/landing/SearchBar";
import { VenueCard } from "@/components/landing/VenueCard";
import { HowItWorks } from "@/components/landing/HowItWorks";
import { Footer } from "@/components/landing/Footer";
import { Header } from "@/components/layout/Header";

export function LandingPage() {
  const featuredVenues = [
    {
      id: 1,
      image: "https://images.unsplash.com/photo-1705593813682-033ee2991df6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
      name: "Sân bóng Thành Công",
      sportType: "Bóng đá",
      price: 500000,
      rating: 4.8,
      location: "Quận 1, TP.HCM",
    },
    {
      id: 2,
      image: "https://images.unsplash.com/photo-1764703666646-acc2f7d48857?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
      name: "Arena Sports Center",
      sportType: "Bóng đá",
      price: 700000,
      rating: 4.9,
      location: "Quận 3, TP.HCM",
    },
    {
      id: 3,
      image: "https://images.unsplash.com/photo-1767729790212-661953ecaa90?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
      name: "Sân Vận Động Quận 7",
      sportType: "Bóng đá",
      price: 600000,
      rating: 4.7,
      location: "Quận 7, TP.HCM",
    },
    {
      id: 4,
      image: "https://images.unsplash.com/photo-1765305460539-edf7a0838dad?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
      name: "Sân bóng Phú Mỹ Hưng",
      sportType: "Bóng đá",
      price: 550000,
      rating: 4.6,
      location: "Quận 7, TP.HCM",
    },
  ];

  return (
    <div className="min-h-screen bg-background">
      <Header />

      {/* Hero Section */}
      <section className="relative bg-gradient-to-br from-primary/10 via-background to-primary/5 py-20">
        <div className="container mx-auto px-4">
          <div className="text-center mb-12">
            <h1 className="text-4xl md:text-5xl mb-4">
              Đặt sân thể thao
              <br />
              <span className="text-primary">Nhanh chóng & Tiện lợi</span>
            </h1>
            <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
              Tìm kiếm và đặt sân thể thao yêu thích chỉ với vài cú click.
              Kết nối với cộng đồng và tận hưởng niềm đam mê thể thao.
            </p>
          </div>

          <SearchBar />
        </div>
      </section>

      {/* Featured Venues */}
      <section className="py-16">
        <div className="container mx-auto px-4">
          <div className="flex items-center justify-between mb-8">
            <div>
              <h2 className="mb-2">Sân nổi bật</h2>
              <p className="text-muted-foreground">
                Các sân thể thao được yêu thích nhất
              </p>
            </div>
            <Button variant="outline">
              Xem tất cả
              <Search className="ml-2 h-4 w-4" />
            </Button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {featuredVenues.map((venue) => (
              <VenueCard key={venue.id} {...venue} />
            ))}
          </div>
        </div>
      </section>

      <HowItWorks />
      <Footer />
    </div>
  );
}


export default LandingPage;
