'use client'

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
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
import { Plus, MoreVertical, Edit, Eye, Loader2, ImageOff } from "lucide-react";
import { stadiumService } from "@/lib/services/stadium";
import { StadiumResponse } from "@/types/stadium";
import { toast } from "sonner";

function VenueManagementPage() {
  const router = useRouter();
  const [venues, setVenues] = useState<StadiumResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    stadiumService.getMyStadiums()
      .then(setVenues)
      .catch(() => toast.error("Không thể tải danh sách sân"))
      .finally(() => setLoading(false));
  }, []);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "AVAILABLE":
        return <Badge className="bg-green-100 text-green-700">Hoạt động</Badge>;
      case "MAINTENANCE":
        return <Badge className="bg-yellow-100 text-yellow-700">Bảo trì</Badge>;
      case "CLOSED":
        return <Badge className="bg-red-100 text-red-700">Đóng cửa</Badge>;
      default:
        return <Badge className="bg-gray-100 text-gray-700">{status}</Badge>;
    }
  };

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <div className="container mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl font-bold">Quản lý sân</h1>
          <Button onClick={() => router.push("/owner/venues/new")}>
            <Plus className="mr-2 h-5 w-5" />
            Thêm sân mới
          </Button>
        </div>

        {loading ? (
          <div className="flex justify-center items-center py-20">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        ) : venues.length === 0 ? (
          <div className="text-center py-20 text-muted-foreground">
            <p className="text-lg mb-4">Bạn chưa có sân nào.</p>
            <Button onClick={() => router.push("/owner/venues/new")}>
              <Plus className="mr-2 h-5 w-5" />
              Thêm sân đầu tiên
            </Button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {venues.map((venue) => (
              <Card key={venue.stadiumId} className="overflow-hidden">
                <div className="relative h-48 bg-muted">
                  {venue.imageUrls && venue.imageUrls.length > 0 ? (
                    <img
                      src={venue.imageUrls[0]}
                      alt={venue.stadiumName}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-muted-foreground">
                      <ImageOff className="h-12 w-12" />
                    </div>
                  )}
                  <div className="absolute top-3 right-3">
                    {getStatusBadge(venue.stadiumStatus)}
                  </div>
                </div>

                <CardContent className="p-6">
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex-1 min-w-0">
                      <h3 className="font-semibold mb-2 truncate">{venue.stadiumName}</h3>
                      <Badge variant="outline">{venue.sportName}</Badge>
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
                          <Eye className="mr-2 h-4 w-4" />
                          Xem đặt sân
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </div>

                  <div className="space-y-2 text-sm text-muted-foreground mb-4">
                    <p className="truncate">{venue.address}</p>
                    <p className="text-primary font-medium">
                      {venue.pricePerHour.toLocaleString('vi-VN')}đ / giờ
                    </p>
                  </div>

                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" className="flex-1">
                      Chỉnh sửa
                    </Button>
                    <Button variant="outline" size="sm" className="flex-1">
                      Xem đặt sân
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default VenueManagementPage;
