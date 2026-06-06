'use client'

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Plus, MoreVertical, Edit, Settings, Eye, Pause, Trash, Loader2, ImageOff, Package, CalendarDays } from "lucide-react";
import { stadiumService } from "@/lib/services/stadium";
import { StadiumResponse } from "@/types/stadium";
import { toast } from "sonner";
import { AccessoryManagerDialog } from "@/components/venues/AccessoryManagerDialog";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { useConfirm } from "@/hooks/useConfirm";

function VenueManagementPage() {
  const router = useRouter();
  const [venues, setVenues] = useState<StadiumResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [isAccessoryOpen, setIsAccessoryOpen] = useState(false);
  const [selectedVenueForAccessory, setSelectedVenueForAccessory] = useState<{ id: number, name: string } | null>(null);

  // Custom confirm hook
  const { 
    isOpen: isConfirmOpen, 
    isLoading: isActionLoading, 
    options: confirmOptions, 
    confirm, 
    close: closeConfirm, 
    execute: executeConfirm 
  } = useConfirm()

  useEffect(() => {
    stadiumService.getMyStadiums()
      .then(setVenues)
      .catch(() => toast.error("Không thể tải danh sách sân"))
      .finally(() => setLoading(false));
  }, []);

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "AVAILABLE":
        return <Badge className="bg-green-100 text-green-700 hover:bg-green-100">Hoạt động</Badge>;
      case "MAINTENANCE":
        return <Badge className="bg-yellow-100 text-yellow-700 hover:bg-yellow-100">Bảo trì</Badge>;
      case "CLOSED":
        return <Badge className="bg-red-100 text-red-700 hover:bg-red-100">Đóng cửa</Badge>;
      default:
        return <Badge className="bg-gray-100 text-gray-700 hover:bg-gray-100">{status}</Badge>;
    }
  };

  const handleDeleteVenue = (venueId: number, venueName: string) => {
    confirm({
      title: "Xác nhận xóa sân",
      description: `Bạn có chắc chắn muốn xóa sân "${venueName}"? Mọi dữ liệu liên quan đến lịch đặt và doanh thu của sân này sẽ bị ảnh hưởng. Hành động này không thể hoàn tác.`,
      confirmText: "Xóa vĩnh viễn",
      variant: "destructive",
      onConfirm: async () => {
        // Mock delete since service might not have it yet
        // await stadiumService.deleteStadium(venueId);
        toast.info("Tính năng xóa sân đang được phát triển");
      }
    })
  };

  return (
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
            <Card key={venue.stadiumId} className="overflow-hidden border-slate-200 hover:shadow-md transition-shadow">
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
                    <h3 className="font-bold text-lg mb-1 truncate">{venue.stadiumName}</h3>
                    <Badge variant="outline" className="font-normal">{venue.sportName}</Badge>
                  </div>

                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon" className="h-8 w-8">
                        <MoreVertical className="h-5 w-5" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" className="w-48">
                      <DropdownMenuItem onClick={() => router.push(`/owner/venues/${venue.stadiumId}/edit`)}>
                        <Edit className="mr-2 h-4 w-4" />
                        Chỉnh sửa
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => router.push(`/owner/venues/${venue.stadiumId}/slots`)}>
                        <CalendarDays className="mr-2 h-4 w-4" />
                        Cấu hình lịch
                      </DropdownMenuItem>
                      <DropdownMenuItem>
                        <Eye className="mr-2 h-4 w-4" />
                        Xem đặt sân
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => {
                        setSelectedVenueForAccessory({ id: venue.stadiumId, name: venue.stadiumName });
                        setIsAccessoryOpen(true);
                      }}>
                        <Package className="mr-2 h-4 w-4" />
                        Quản lý phụ kiện
                      </DropdownMenuItem>
                      <DropdownMenuItem>
                        <Pause className="mr-2 h-4 w-4" />
                        Tạm dừng
                      </DropdownMenuItem>
                      <DropdownMenuItem 
                        className="text-destructive focus:bg-destructive focus:text-destructive-foreground"
                        onClick={() => handleDeleteVenue(venue.stadiumId, venue.stadiumName)}
                      >
                        <Trash className="mr-2 h-4 w-4" />
                        Xóa sân
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>

                <div className="space-y-2 text-sm text-muted-foreground mb-4">
                  <p className="truncate" title={venue.address}>{venue.address}</p>
                  <p className="text-primary font-semibold text-base">
                    {venue.pricePerHour.toLocaleString('vi-VN')}đ / giờ
                  </p>
                </div>

                <div className="flex gap-2 mt-4">
                  <Button
                    variant="outline"
                    size="sm"
                    className="flex-1 font-medium"
                    onClick={() => router.push(`/owner/venues/${venue.stadiumId}/slots`)}
                  >
                    <CalendarDays className="mr-1.5 h-4 w-4" />
                    Lịch sân
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    className="flex-1 font-medium text-primary hover:text-primary hover:bg-primary/5 border-primary/20"
                    onClick={() => {
                      setSelectedVenueForAccessory({ id: venue.stadiumId, name: venue.stadiumName });
                      setIsAccessoryOpen(true);
                    }}
                  >
                    <Package className="mr-1.5 h-4 w-4" />
                    Phụ kiện
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

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

      <ConfirmDialog
        isOpen={isConfirmOpen}
        onClose={closeConfirm}
        onConfirm={executeConfirm}
        isLoading={isActionLoading}
        title={confirmOptions?.title || ""}
        description={confirmOptions?.description || ""}
        confirmText={confirmOptions?.confirmText}
        variant={confirmOptions?.variant}
      />
    </div>
  );
}

export default VenueManagementPage;
