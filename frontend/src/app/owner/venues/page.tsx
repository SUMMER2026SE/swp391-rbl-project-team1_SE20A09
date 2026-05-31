'use client'

import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useState } from "react";
import { Plus, MoreVertical, Edit, Settings, Eye, Pause, Trash, Package } from "lucide-react";
import { AccessoryManagerDialog } from "@/components/venues/AccessoryManagerDialog";

function VenueManagementPage() {
  const [isAccessoryOpen, setIsAccessoryOpen] = useState(false);
  const [selectedVenueForAccessory, setSelectedVenueForAccessory] = useState<{ id: number, name: string } | null>(null);

  const venues = [
    {
      id: 1,
      name: "Sân bóng Thành Công - Sân 1",
      image: "https://images.unsplash.com/photo-1705593813682-033ee2991df6?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=300",
      sportType: "Bóng đá",
      status: "active",
      todayBookings: 8,
      monthRevenue: 15200000,
    },
    {
      id: 2,
      name: "Sân bóng Thành Công - Sân 2",
      image: "https://images.unsplash.com/photo-1764703666646-acc2f7d48857?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=300",
      sportType: "Bóng đá",
      status: "active",
      todayBookings: 6,
      monthRevenue: 12400000,
    },
    {
      id: 3,
      name: "Sân cầu lông - Court 1",
      image: "https://images.unsplash.com/photo-1767729790212-661953ecaa90?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=300",
      sportType: "Cầu lông",
      status: "inactive",
      todayBookings: 0,
      monthRevenue: 0,
    },
  ];

  const getStatusBadge = (status: string) => {
    if (status === "active") {
      return <Badge className="bg-green-100 text-green-700">Hoạt động</Badge>;
    } else if (status === "inactive") {
      return <Badge className="bg-gray-100 text-gray-700">Tạm dừng</Badge>;
    }
    return <Badge className="bg-red-100 text-red-700">Đình chỉ</Badge>;
  };

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl">Quản lý sân</h1>
          <Button>
            <Plus className="mr-2 h-5 w-5" />
            Thêm sân mới
          </Button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {venues.map((venue) => (
            <Card key={venue.id} className="overflow-hidden">
              <div className="relative h-48">
                <img
                  src={venue.image}
                  alt={venue.name}
                  className="w-full h-full object-cover"
                />
                <div className="absolute top-3 right-3">
                  {getStatusBadge(venue.status)}
                </div>
              </div>

              <CardContent className="p-6">
                <div className="flex items-start justify-between mb-3">
                  <div className="flex-1">
                    <h3 className="mb-2">{venue.name}</h3>
                    <Badge variant="outline">{venue.sportType}</Badge>
                  </div>

                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="sm">
                        <MoreVertical className="h-5 w-5" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem>
                        <Edit className="mr-2 h-4 w-4" />
                        Chỉnh sửa
                      </DropdownMenuItem>
                      <DropdownMenuItem>
                        <Settings className="mr-2 h-4 w-4" />
                        Cấu hình lịch
                      </DropdownMenuItem>
                      <DropdownMenuItem>
                        <Eye className="mr-2 h-4 w-4" />
                        Xem đặt sân
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => {
                        setSelectedVenueForAccessory({ id: venue.id, name: venue.name });
                        setIsAccessoryOpen(true);
                      }}>
                        <Package className="mr-2 h-4 w-4" />
                        Quản lý phụ kiện
                      </DropdownMenuItem>
                      <DropdownMenuItem>
                        <Pause className="mr-2 h-4 w-4" />
                        Tạm dừng
                      </DropdownMenuItem>
                      <DropdownMenuItem className="text-destructive">
                        <Trash className="mr-2 h-4 w-4" />
                        Xóa
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>

                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Đặt sân hôm nay</span>
                    <span>{venue.todayBookings}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Doanh thu tháng</span>
                    <span className="text-primary">
                      {venue.monthRevenue.toLocaleString('vi-VN')}đ
                    </span>
                  </div>
                </div>

                <div className="flex gap-2 mt-4">
                  <Button variant="outline" size="sm" className="flex-1">
                    Chỉnh sửa
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    className="flex-1 text-primary hover:text-primary hover:bg-primary/5 border-primary/20"
                    onClick={() => {
                      setSelectedVenueForAccessory({ id: venue.id, name: venue.name });
                      setIsAccessoryOpen(true);
                    }}
                  >
                    <Package className="mr-1 h-4 w-4" />
                    Phụ kiện
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>

      {selectedVenueForAccessory && (
        <AccessoryManagerDialog
          stadiumId={selectedVenueForAccessory.id}
          stadiumName={selectedVenueForAccessory.name}
          isOpen={isAccessoryOpen}
          onClose={() => {
            setIsAccessoryOpen(false);
            setSelectedVenueForAccessory(null);
          }}
        />
      )}
    </div>
  );
}

export default VenueManagementPage;
