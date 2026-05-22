'use client'

import { useState } from "react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { VenueCard } from "@/components/landing/VenueCard";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Slider } from "@/components/ui/slider";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Search } from "lucide-react";

function VenueSearchPage() {
  const [priceRange, setPriceRange] = useState([0, 1000000]);

  const venues = [
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
    {
      id: 5,
      image: "https://images.unsplash.com/photo-1771344164616-3582e4dc2f07?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
      name: "Sân bóng Bình Thạnh",
      sportType: "Bóng đá",
      price: 450000,
      rating: 4.5,
      location: "Quận Bình Thạnh, TP.HCM",
    },
  ];

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Filter Sidebar */}
          <aside className="lg:col-span-1">
            <div className="bg-card border rounded-lg p-6 sticky top-24">
              <h3 className="mb-6">Bộ lọc</h3>

              {/* Price Range */}
              <div className="mb-6">
                <Label className="mb-3 block">Khoảng giá</Label>
                <Slider
                  value={priceRange}
                  onValueChange={setPriceRange}
                  max={1000000}
                  step={50000}
                  className="mb-3"
                />
                <div className="flex justify-between text-sm text-muted-foreground">
                  <span>{priceRange[0].toLocaleString('vi-VN')}đ</span>
                  <span>{priceRange[1].toLocaleString('vi-VN')}đ</span>
                </div>
              </div>

              {/* Sport Category */}
              <div className="mb-6">
                <Label className="mb-3 block">Loại sân</Label>
                <div className="space-y-3">
                  {["Bóng đá", "Cầu lông", "Quần vợt", "Bóng rổ"].map((sport) => (
                    <div key={sport} className="flex items-center space-x-2">
                      <Checkbox id={sport} />
                      <label htmlFor={sport} className="text-sm cursor-pointer">
                        {sport}
                      </label>
                    </div>
                  ))}
                </div>
              </div>

              {/* Amenities */}
              <div className="mb-6">
                <Label className="mb-3 block">Tiện ích</Label>
                <div className="space-y-3">
                  {["Bãi đỗ xe", "Phòng thay đồ", "Đèn chiếu sáng", "Wifi", "Căng tin"].map((amenity) => (
                    <div key={amenity} className="flex items-center space-x-2">
                      <Checkbox id={amenity} />
                      <label htmlFor={amenity} className="text-sm cursor-pointer">
                        {amenity}
                      </label>
                    </div>
                  ))}
                </div>
              </div>

              {/* Distance */}
              <div>
                <Label className="mb-3 block">Khoảng cách</Label>
                <Select defaultValue="10">
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="5">Trong vòng 5km</SelectItem>
                    <SelectItem value="10">Trong vòng 10km</SelectItem>
                    <SelectItem value="20">Trong vòng 20km</SelectItem>
                    <SelectItem value="50">Trong vòng 50km</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <Button className="w-full mt-6">Áp dụng bộ lọc</Button>
            </div>
          </aside>

          {/* Results */}
          <main className="lg:col-span-3">
            {/* Search Bar */}
            <div className="flex gap-4 mb-6">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
                <Input placeholder="Tìm kiếm sân..." className="pl-10" />
              </div>
              <Select defaultValue="rating">
                <SelectTrigger className="w-48">
                  <SelectValue placeholder="Sắp xếp" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="rating">Đánh giá cao nhất</SelectItem>
                  <SelectItem value="price-low">Giá thấp đến cao</SelectItem>
                  <SelectItem value="price-high">Giá cao đến thấp</SelectItem>
                  <SelectItem value="distance">Khoảng cách</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Results Count */}
            <p className="text-muted-foreground mb-4">
              Tìm thấy <strong>{venues.length}</strong> sân thể thao
            </p>

            {/* Venue List */}
            <div className="space-y-4">
              {venues.map((venue) => (
                <VenueCard key={venue.id} {...venue} />
              ))}
            </div>

            {/* Pagination */}
            <div className="flex justify-center gap-2 mt-8">
              <Button variant="outline" size="sm">Trước</Button>
              <Button variant="outline" size="sm">1</Button>
              <Button size="sm">2</Button>
              <Button variant="outline" size="sm">3</Button>
              <Button variant="outline" size="sm">Sau</Button>
            </div>
          </main>
        </div>
      </div>

      <Footer />
    </div>
  );
}

export default VenueSearchPage;
